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
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed interface PeerState {
    data object Connecting : PeerState
    data object Ready : PeerState
    /** Llamada entrante; [idToken] es la sesión de quien llama, todavía sin comprobar. */
    data class Incoming(val from: String, val idToken: String) : PeerState

    /** Llamada saliente a [target] (número marcado), esperando que contesten. */
    data class Dialing(val target: String) : PeerState
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

    private val _ownAudio = MutableSharedFlow<AudioChunk>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** La voz del usuario, para enviar sus subtítulos a quien está en la web. */
    val ownAudio: Flow<AudioChunk> = _ownAudio.asSharedFlow()

    private val _remoteKind = MutableStateFlow<String?>(null)

    /** "web" o "app": qué usa la otra persona (llega por el canal de subtítulos). */
    val remoteKind: StateFlow<String?> = _remoteKind.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)

    /** Aviso corto para la pantalla de espera (por ejemplo, que el número no contestó). */
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private var webView: WebView? = null
    private val main = Handler(Looper.getMainLooper())

    fun attach(view: WebView?) {
        webView = view
    }

    fun answer() = run("answer()")

    /** Llama a [peerId] (el número [target]); [idToken] identifica a este teléfono. */
    fun call(peerId: String, idToken: String, target: String) {
        _notice.value = null
        _state.value = PeerState.Dialing(target)
        run("callPeer(${JSONObject.quote(peerId)}, ${JSONObject.quote(idToken)})")
    }

    fun sendCaption(text: String, final: Boolean) = run("sendCaption(${JSONObject.quote(text)}, $final)")

    fun showNotice(text: String) {
        _notice.value = text
    }

    fun clearNotice() {
        _notice.value = null
    }
    fun reject() = run("reject()")
    fun hangUp() = run("hangUp()")
    fun setMuted(muted: Boolean) = run("setMuted($muted)")

    /** Tras colgar volvemos a esperar llamadas. */
    fun reset() {
        _remoteKind.value = null
        if (_state.value is PeerState.Ended) _state.value = PeerState.Ready
    }

    @SuppressLint("JavascriptInterface")
    private fun run(script: String) {
        main.post { webView?.evaluateJavascript(script, null) }
    }

    // region Llamadas desde JavaScript (llegan en un hilo de fondo)

    @JavascriptInterface
    fun onReady(id: String) {
        val current = _state.value
        if (current !is PeerState.InCall && current !is PeerState.Incoming && current !is PeerState.Dialing) {
            _state.value = PeerState.Ready
        }
    }

    @JavascriptInterface
    fun onError(type: String) {
        if (_state.value is PeerState.InCall) return
        // El número marcado no tiene la página ni la app abiertas
        if (_state.value is PeerState.Dialing) {
            _notice.value = when (type) {
                "peer-unavailable" -> "No contesta: ese número no está activo ahora"
                "microphone" -> "No se pudo usar el micrófono"
                else -> "No se pudo conectar la llamada"
            }
            _state.value = PeerState.Ended
            return
        }
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
    fun onIncoming(from: String, idToken: String) {
        _state.value = PeerState.Incoming(from, idToken)
    }

    @JavascriptInterface
    fun onConnected() {
        _state.value = PeerState.InCall
    }

    @JavascriptInterface
    fun onEnded() {
        val current = _state.value
        if (current is PeerState.InCall || current is PeerState.Incoming || current is PeerState.Dialing) {
            _state.value = PeerState.Ended
        }
    }

    @JavascriptInterface
    fun onAudio(voiceBase64: String, callMixBase64: String, ownBase64: String) {
        val voice = decodePcm(voiceBase64)
        val callMix = decodePcm(callMixBase64)
        _remoteAudio.tryEmit(AudioChunk(voice, AudioChunk.levelOf(voice), blocked = false, callMix = callMix))
        val own = decodePcm(ownBase64)
        _ownAudio.tryEmit(AudioChunk(own, AudioChunk.levelOf(own), blocked = false))
    }

    @JavascriptInterface
    fun onRemoteHello(kind: String, lang: String) {
        _remoteKind.value = kind.ifBlank { null }
    }

    // endregion

    private fun decodePcm(base64: String): ShortArray {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        val samples = ShortArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples)
        return samples
    }
}
