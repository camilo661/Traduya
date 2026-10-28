package com.traduya.feature.internetcall.peer

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.traduya.core.model.AudioChunk
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

sealed interface PeerState {
    data object Connecting : PeerState
    data object Ready : PeerState
    data class Incoming(val from: String) : PeerState
    data object InCall : PeerState
    data object Ended : PeerState
    data class Failed(val reason: String) : PeerState
}

/**
 * Puente entre la página WebRTC (receiver.html) y Kotlin.
 * La página avisa de los eventos de la llamada y entrega el audio de la otra persona;
 * Kotlin le envía las órdenes (contestar, colgar, silenciar).
 */
class PeerBridge {

    private val _state = MutableStateFlow<PeerState>(PeerState.Connecting)
    val state: StateFlow<PeerState> = _state.asStateFlow()

    private val _remoteAudio = MutableSharedFlow<AudioChunk>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Audio de la otra persona en PCM de 16 kHz, listo para la IA. */
    val remoteAudio: Flow<AudioChunk> = _remoteAudio.asSharedFlow()

    private var webView: WebView? = null
    private val main = Handler(Looper.getMainLooper())

    fun attach(view: WebView?) {
        webView = view
    }

    fun answer() = run("answer()")
    fun reject() = run("reject()")
    fun hangUp() = run("hangUp()")
    fun setMuted(muted: Boolean) = run("setMuted($muted)")

    /** Tras colgar volvemos a esperar llamadas. */
    fun reset() {
        if (_state.value is PeerState.Ended) _state.value = PeerState.Ready
    }

    @SuppressLint("JavascriptInterface")
    private fun run(script: String) {
        main.post { webView?.evaluateJavascript(script, null) }
    }

    // region Llamadas desde JavaScript (llegan en un hilo de fondo)

    @JavascriptInterface
    fun onReady(id: String) {
        if (_state.value !is PeerState.InCall && _state.value !is PeerState.Incoming) _state.value = PeerState.Ready
    }

    @JavascriptInterface
    fun onError(type: String) {
        if (_state.value is PeerState.InCall) return
        _state.value = PeerState.Failed(
            when (type) {
                "network", "server-error", "socket-error", "socket-closed" -> "Sin conexión con el servidor. Reintentando…"
                "unavailable-id" -> "Reconectando tu enlace…"
                "microphone" -> "No se pudo usar el micrófono"
                else -> "Error de conexión ($type). Reintentando…"
            },
        )
    }

    @JavascriptInterface
    fun onIncoming(from: String) {
        _state.value = PeerState.Incoming(from)
    }

    @JavascriptInterface
    fun onConnected() {
        _state.value = PeerState.InCall
    }

    @JavascriptInterface
    fun onEnded() {
        val current = _state.value
        if (current is PeerState.InCall || current is PeerState.Incoming) _state.value = PeerState.Ended
    }

    @JavascriptInterface
    fun onAudio(base64: String) {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        val samples = ShortArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples)
        _remoteAudio.tryEmit(AudioChunk(samples, level(samples), blocked = false))
    }

    // endregion

    private fun level(samples: ShortArray): Float {
        if (samples.isEmpty()) return 0f
        var sum = 0.0
        for (sample in samples) sum += sample * sample.toDouble()
        return (sqrt(sum / samples.size) / Short.MAX_VALUE * LEVEL_GAIN).toFloat().coerceIn(0f, 1f)
    }

    private companion object {
        const val LEVEL_GAIN = 6.0
    }
}
