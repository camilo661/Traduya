package com.traduya.feature.internetcall

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.traduya.core.callui.CallUiState
import com.traduya.core.domain.internetcall.CallerVerifier
import com.traduya.core.domain.internetcall.InternetDirectory
import com.traduya.core.domain.wifinumber.PhoneNumberRules
import com.traduya.core.domain.wifinumber.WifiNumberRegistry
import com.traduya.core.domain.repository.HistoryRepository
import com.traduya.core.domain.translation.CallTranslationSession
import com.traduya.core.model.CallDirection
import com.traduya.core.model.CallStatus
import com.traduya.core.model.VerifiedCaller
import com.traduya.feature.internetcall.captions.OwnCaptions
import com.traduya.feature.internetcall.peer.CallAudioRouter
import com.traduya.feature.internetcall.peer.InternetCallIdentity
import com.traduya.feature.internetcall.peer.PeerBridge
import com.traduya.feature.internetcall.peer.PeerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado del número que se quiere llamar (luz verde o roja). */
enum class TargetStatus { NONE, CHECKING, ACTIVE, INACTIVE, NOT_FOUND, INVALID }

/** Pantalla de llamadas por internet: esperar, contestar, llamar a otro número y traducir en vivo. */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class InternetCallViewModel @Inject constructor(
    application: Application,
    val identity: InternetCallIdentity,
    private val session: CallTranslationSession,
    private val callerVerifier: CallerVerifier,
    private val directory: InternetDirectory,
    private val ownCaptions: OwnCaptions,
    wifiNumbers: WifiNumberRegistry,
    historyRepository: HistoryRepository,
) : AndroidViewModel(application) {

    val bridge = PeerBridge()
    private val audioRouter = CallAudioRouter(application)

    private val muted = MutableStateFlow(false)
    private val speaker = MutableStateFlow(true)

    /** Quién llama, ya comprobado; null mientras se verifica o si no hay llamada. */
    private val caller = MutableStateFlow<VerifiedCaller?>(null)
    private var connectedAtMillis: Long? = null
    private var sessionOpen = false
    private var captionsJob: Job? = null

    /** Para llamar a otros números hace falta tener el propio registrado (Ajustes › Llamadas por WiFi). */
    val canCall: StateFlow<Boolean> = wifiNumbers.account
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, wifiNumbers.account.value != null)

    /** Lo que se marca para llamar, con el prefijo del país de la SIM para empezar. */
    val dialInput = MutableStateFlow(
        PhoneNumberRules.callingCodeFor(
            application.getSystemService(android.telephony.TelephonyManager::class.java)?.simCountryIso,
        )?.let { "$it " } ?: "+",
    )

    /** Luz del número marcado: se busca en el directorio medio segundo después de dejar de escribir. */
    val targetStatus: StateFlow<TargetStatus> = dialInput
        .debounce(500)
        .mapLatest { raw ->
            val number = PhoneNumberRules.normalize(raw)
            when {
                raw.count { it.isDigit() } < 8 -> TargetStatus.NONE
                number == null -> TargetStatus.INVALID
                else -> runCatching { directory.lookup(number) }.fold(
                    onSuccess = { entry ->
                        when {
                            entry == null -> TargetStatus.NOT_FOUND
                            entry.isActive -> TargetStatus.ACTIVE
                            else -> TargetStatus.INACTIVE
                        }
                    },
                    onFailure = { TargetStatus.NONE },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TargetStatus.NONE)

    val notice: StateFlow<String?> = bridge.notice

    private val transcript = session.state
        .map { it.historyId }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { historyRepository.observeTranscript(it) }
        .onStart { emit(emptyList()) }

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000)
        }
    }

    val peerState: StateFlow<PeerState> = bridge.state

    val callState: StateFlow<CallUiState> = combine(
        combine(bridge.state, muted, speaker, ::Triple),
        session.state,
        transcript,
        ticker,
        caller,
    ) { (peer, isMuted, isSpeaker), translation, lines, now, verifiedCaller ->
        CallUiState(
            callerLabel = verifiedCaller?.fullName ?: "Comprobando quién llama…",
            number = verifiedCaller?.phoneNumber.orEmpty(),
            status = when (peer) {
                is PeerState.Incoming -> CallStatus.RINGING
                is PeerState.Dialing -> CallStatus.DIALING
                PeerState.InCall -> CallStatus.ACTIVE
                else -> CallStatus.DISCONNECTED
            },
            elapsedSeconds = connectedAtMillis?.let { (now - it) / 1_000 } ?: 0,
            isMuted = isMuted,
            isSpeakerOn = isSpeaker,
            translationEnabled = translation.enabled,
            translationStatus = translation.status,
            translationError = translation.errorMessage,
            liveText = translation.liveText,
            audioLevel = translation.audioLevel,
            micBlocked = false,
            isRecording = translation.isRecording,
            detectedLanguage = translation.detectedLanguage,
            lines = lines,
            hasEnded = peer is PeerState.Ended,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CallUiState())

    init {
        viewModelScope.launch { bridge.state.collect(::onPeerState) }
        // Mientras esta pantalla está abierta, el número propio aparece activo (luz verde)
        viewModelScope.launch {
            while (true) {
                runCatching { directory.markActive() }
                delay(PRESENCE_INTERVAL_MILLIS)
            }
        }
    }

    fun onDialInputChange(value: String) {
        dialInput.value = value.filter { it.isDigit() || it in "+ " }.take(MAX_DIAL_LENGTH)
        bridge.clearNotice()
    }

    /** Llama al número marcado por internet. */
    fun callNumber() {
        val number = PhoneNumberRules.normalize(dialInput.value) ?: return
        if (bridge.state.value != PeerState.Ready) return
        viewModelScope.launch {
            val entry = runCatching { directory.lookup(number) }.getOrElse {
                bridge.showNotice("No hay conexión. Inténtalo de nuevo.")
                return@launch
            }
            if (entry == null) {
                bridge.showNotice("Ese número no está en Traduya")
                return@launch
            }
            val token = runCatching { directory.sessionToken() }.getOrNull()
                ?: return@launch bridge.showNotice("No hay conexión. Inténtalo de nuevo.")
            bridge.call(entry.peerId, token, number)
        }
    }

    private suspend fun onPeerState(state: PeerState) {
        when (state) {
            is PeerState.Incoming -> if (!sessionOpen) onIncoming(state)

            is PeerState.Dialing -> if (!sessionOpen) {
                caller.value = VerifiedCaller(fullName = state.target, phoneNumber = state.target, email = "")
                sessionOpen = true
                session.begin(number = state.target, direction = CallDirection.OUTGOING)
                // Si nadie contesta, no suena para siempre
                viewModelScope.launch {
                    delay(RING_TIMEOUT_MILLIS)
                    if (bridge.state.value == state) {
                        bridge.hangUp()
                        bridge.showNotice("No contesta. Inténtalo más tarde.")
                    }
                }
            }

            PeerState.InCall -> {
                connectedAtMillis = System.currentTimeMillis()
                audioRouter.startCall(speaker.value)
                // Por internet solo llega la voz de la otra persona
                session.connect(bridge.remoteAudio, includesUserVoice = false)
                startOwnCaptionsForWeb()
            }

            PeerState.Ended -> {
                closeSession()
                delay(ENDED_SCREEN_MILLIS)
                bridge.reset()
            }

            else -> Unit
        }
    }

    /** Solo suena si quien llama está registrado en la página web; si no, se rechaza. */
    private suspend fun onIncoming(state: PeerState.Incoming) {
        val verified = callerVerifier.verify(state.idToken)
        if (verified == null) {
            Log.i(TAG, "Llamada rechazada: quien llama no está registrado o su sesión no es válida")
            bridge.reject()
            return
        }
        caller.value = verified
        sessionOpen = true
        // En el historial queda su teléfono, como en las llamadas normales
        session.begin(number = verified.phoneNumber, direction = CallDirection.INCOMING)
    }

    /** Si la otra persona está en la web, le enviamos los subtítulos de nuestra voz para que los traduzca. */
    private fun startOwnCaptionsForWeb() {
        captionsJob?.cancel()
        captionsJob = viewModelScope.launch {
            bridge.remoteKind.filter { it == "web" }.first()
            ownCaptions.start(this, bridge.ownAudio, bridge::sendCaption)
        }
    }

    private suspend fun closeSession() {
        captionsJob?.cancel()
        captionsJob = null
        if (!sessionOpen) return
        sessionOpen = false
        connectedAtMillis = null
        caller.value = null
        muted.value = false
        audioRouter.endCall()
        session.finish()
    }

    fun answer() {
        if (caller.value != null) bridge.answer()
    }

    fun hangUp() {
        when (bridge.state.value) {
            is PeerState.Incoming -> bridge.reject()
            else -> bridge.hangUp()
        }
    }

    fun toggleMute() {
        muted.value = !muted.value
        bridge.setMuted(muted.value)
    }

    fun toggleSpeaker() {
        speaker.value = !speaker.value
        audioRouter.setSpeaker(speaker.value)
    }

    fun toggleTranslation() = session.setEnabled(!session.state.value.enabled)

    override fun onCleared() {
        bridge.attach(null)
        if (sessionOpen) {
            audioRouter.endCall()
            // La sesión vive en un singleton: la cerramos aunque el ViewModel muera
            kotlinx.coroutines.MainScope().launch { session.finish() }
        }
    }

    private companion object {
        const val TAG = "TraduyaInternetCall"
        const val ENDED_SCREEN_MILLIS = 1_500L
        const val PRESENCE_INTERVAL_MILLIS = 40_000L
        const val RING_TIMEOUT_MILLIS = 45_000L
        const val MAX_DIAL_LENGTH = 20
    }
}
