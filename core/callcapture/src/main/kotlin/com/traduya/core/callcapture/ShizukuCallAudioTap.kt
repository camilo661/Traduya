package com.traduya.core.callcapture

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
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

    override fun chunks(): Flow<AudioChunk> = flow {
        refresh()
        check(status.value == CallAudioTapStatus.READY) { "Shizuku no está listo" }
        val remote = connect()
        try {
            for (line in LINES) {
                val pipe = runCatching { remote.start(line.source, line.muteUser) }
                    .onFailure { Log.w(TAG, "La línea ${line.source} no está disponible", it) }
                    .getOrNull() ?: continue
                val tryNextLine = relay(pipe)
                if (!tryNextLine) return@flow
            }
            error("El teléfono no deja leer la línea de la llamada")
        } finally {
            runCatching { remote.stop() }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Reenvía el audio de la tubería.
     * Devuelve true si la línea se cortó o nunca entregó sonido (el teléfono no la ofrece)
     * y hay que probar la siguiente. Una línea que ya dio sonido no se abandona por silencios.
     */
    private suspend fun FlowCollector<AudioChunk>.relay(pipe: ParcelFileDescriptor): Boolean {
        val bytes = ByteArray(CHUNK_SAMPLES * FRAME_BYTES)
        var heardSound = false
        var silentMillis = 0
        DataInputStream(ParcelFileDescriptor.AutoCloseInputStream(pipe)).use { input ->
            while (currentCoroutineContext().isActive) {
                try {
                    input.readFully(bytes)
                } catch (e: EOFException) {
                    return true
                }
                val voice = ShortArray(CHUNK_SAMPLES) { i -> sampleAt(bytes, i * FRAME_BYTES) }
                val callMix = ShortArray(CHUNK_SAMPLES) { i -> sampleAt(bytes, i * FRAME_BYTES + 2) }
                if (voice.any { it != ZERO }) heardSound = true else silentMillis += CHUNK_MILLIS
                emit(AudioChunk(voice, AudioChunk.levelOf(voice), blocked = false, callMix = callMix))
                if (!heardSound && silentMillis >= SILENT_LINE_MILLIS) return true
            }
        }
        return false
    }

    private fun sampleAt(bytes: ByteArray, offset: Int): Short =
        ((bytes[offset].toInt() and 0xFF) or (bytes[offset + 1].toInt() shl 8)).toShort()

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
        const val TAG = "TraduyaCallAudio"
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val PERMISSION_REQUEST_CODE = 4_201
        const val CONNECT_TIMEOUT_MILLIS = 8_000L
        const val CHUNK_MILLIS = 100
        const val CHUNK_SAMPLES = AudioChunk.SAMPLE_RATE * CHUNK_MILLIS / 1000
        const val FRAME_BYTES = 4 // dos canales de 16 bits

        // Solo fuentes de la línea, nunca el micrófono: así la voz del usuario no se traduce.
        // VOICE_DOWNLINK trae solo a la otra persona; VOICE_CALL mezcla ambos lados y se
        // silencia mientras el usuario habla.
        val LINES = listOf(
            Line(MediaRecorder.AudioSource.VOICE_DOWNLINK, muteUser = false),
            Line(MediaRecorder.AudioSource.VOICE_CALL, muteUser = true),
        )
        const val SILENT_LINE_MILLIS = 4_000
        const val ZERO: Short = 0
    }
}

private class Line(val source: Int, val muteUser: Boolean)
