package com.traduya.core.callcapture

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.IBinder
import android.os.ParcelFileDescriptor
import com.traduya.core.callcapture.shell.CallAudioUserService
import com.traduya.core.domain.audio.CallAudioTap
import com.traduya.core.model.AudioChunk
import com.traduya.core.model.CallAudioTapStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import java.io.DataInputStream
import java.io.EOFException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Escucha la llamada a través de Shizuku: un proceso con permisos de shell graba la línea
 * telefónica, que Android no silencia, y nos pasa el audio por una tubería.
 */
@Singleton
internal class ShizukuCallAudioTap @Inject constructor(
    @ApplicationContext private val context: Context,
) : CallAudioTap {

    private val _status = MutableStateFlow(CallAudioTapStatus.NOT_INSTALLED)
    override val status: StateFlow<CallAudioTapStatus> = _status.asStateFlow()

    private val serviceArgs = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, CallAudioUserService::class.java.name),
    )
        .daemon(false)
        .processNameSuffix("callaudio")
        .version(appVersion())

    @Volatile
    private var service: ICallAudioService? = null

    init {
        Shizuku.addBinderReceivedListenerSticky { refresh() }
        Shizuku.addBinderDeadListener {
            service = null
            refresh()
        }
        Shizuku.addRequestPermissionResultListener { _, _ -> refresh() }
        refresh()
    }

    override fun refresh() {
        _status.value = when {
            Shizuku.pingBinder() && hasPermission() -> CallAudioTapStatus.READY
            Shizuku.pingBinder() -> CallAudioTapStatus.PERMISSION_NEEDED
            isShizukuInstalled() -> CallAudioTapStatus.NOT_RUNNING
            else -> CallAudioTapStatus.NOT_INSTALLED
        }
    }

    override fun requestPermission() {
        if (Shizuku.pingBinder() && !hasPermission()) Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
    }

    override fun chunks(onSpeakerNeeded: () -> Unit): Flow<AudioChunk> = flow {
        refresh()
        check(status.value == CallAudioTapStatus.READY) { "Shizuku no está listo" }
        val remote = connect()
        try {
            for ((index, source) in SOURCES.withIndex()) {
                val pipe = runCatching { remote.start(source) }.getOrNull() ?: continue
                val isLastSource = index == SOURCES.lastIndex
                if (source == MediaRecorder.AudioSource.MIC) onSpeakerNeeded()
                val tryNextSource = relay(pipe, isLastSource)
                if (!tryNextSource) return@flow
            }
            error("Ninguna fuente de audio funcionó con Shizuku")
        } finally {
            runCatching { remote.stop() }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Reenvía el audio de la tubería.
     * Devuelve true si la fuente se cortó o solo entregó silencio absoluto y hay que probar la siguiente.
     */
    private suspend fun FlowCollector<AudioChunk>.relay(pipe: ParcelFileDescriptor, isLastSource: Boolean): Boolean {
        val bytes = ByteArray(CHUNK_SAMPLES * 2)
        var silentMillis = 0
        DataInputStream(ParcelFileDescriptor.AutoCloseInputStream(pipe)).use { input ->
            while (currentCoroutineContext().isActive) {
                try {
                    input.readFully(bytes)
                } catch (e: EOFException) {
                    return true
                }
                val samples = ShortArray(CHUNK_SAMPLES) { i ->
                    ((bytes[i * 2].toInt() and 0xFF) or (bytes[i * 2 + 1].toInt() shl 8)).toShort()
                }
                silentMillis = if (samples.all { it == ZERO }) silentMillis + CHUNK_MILLIS else 0
                val blocked = silentMillis >= SILENT_SOURCE_MILLIS
                emit(AudioChunk(samples, AudioChunk.levelOf(samples), blocked && isLastSource))
                if (blocked && !isLastSource) return true
            }
        }
        return false
    }

    private suspend fun connect(): ICallAudioService {
        service?.takeIf { it.asBinder().pingBinder() }?.let { return it }
        return withContext(Dispatchers.Main) {
            withTimeout(CONNECT_TIMEOUT_MILLIS) {
                suspendCancellableCoroutine { continuation ->
                    val connection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                            if (binder == null || !binder.pingBinder() || !continuation.isActive) return
                            val remote = ICallAudioService.Stub.asInterface(binder)
                            service = remote
                            continuation.resume(remote)
                        }

                        override fun onServiceDisconnected(name: ComponentName?) {
                            service = null
                        }
                    }
                    Shizuku.bindUserService(serviceArgs, connection)
                }
            }
        }
    }

    private fun hasPermission(): Boolean = runCatching {
        !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    private fun isShizukuInstalled(): Boolean = runCatching {
        context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
    }.isSuccess

    private fun appVersion(): Int = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
    }.getOrDefault(1)

    private companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val PERMISSION_REQUEST_CODE = 4_201
        const val CONNECT_TIMEOUT_MILLIS = 8_000L
        const val CHUNK_MILLIS = 100
        const val CHUNK_SAMPLES = AudioChunk.SAMPLE_RATE * CHUNK_MILLIS / 1000

        // La línea entrante solo trae a la otra persona; algunos teléfonos solo dan la mezcla
        // de ambos lados; como último recurso, el micrófono sin restricciones (con altavoz).
        val SOURCES = listOf(
            MediaRecorder.AudioSource.VOICE_DOWNLINK,
            MediaRecorder.AudioSource.VOICE_CALL,
            MediaRecorder.AudioSource.MIC,
        )
        const val SILENT_SOURCE_MILLIS = 4_000
        const val ZERO: Short = 0
    }
}
