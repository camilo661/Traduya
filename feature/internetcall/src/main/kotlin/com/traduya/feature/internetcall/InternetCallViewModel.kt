package com.traduya.feature.internetcall

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.traduya.core.callui.CallUiState
import com.traduya.core.domain.repository.HistoryRepository
import com.traduya.core.domain.translation.CallTranslationSession
import com.traduya.core.model.CallDirection
import com.traduya.core.model.CallStatus
import com.traduya.feature.internetcall.peer.CallAudioRouter
import com.traduya.feature.internetcall.peer.InternetCallIdentity
import com.traduya.feature.internetcall.peer.PeerBridge
import com.traduya.feature.internetcall.peer.PeerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Pantalla de llamadas por internet: esperar, contestar y traducir en vivo. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InternetCallViewModel @Inject constructor(
    application: Application,
    val identity: InternetCallIdentity,
    private val session: CallTranslationSession,
    historyRepository: HistoryRepository,
) : AndroidViewModel(application) {

    val bridge = PeerBridge()
    private val audioRouter = CallAudioRouter(application)

    private val muted = MutableStateFlow(false)
    private val speaker = MutableStateFlow(true)
    private var connectedAtMillis: Long? = null
    private var sessionOpen = false

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
    ) { (peer, isMuted, isSpeaker), translation, lines, now ->
        CallUiState(
            callerLabel = "Llamada por internet",
            number = "",
            status = when (peer) {
                is PeerState.Incoming -> CallStatus.RINGING
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
    }

    private suspend fun onPeerState(state: PeerState) {
        when (state) {
            is PeerState.Incoming -> if (!sessionOpen) {
                sessionOpen = true
                session.begin(number = "Internet", direction = CallDirection.INCOMING)
            }

            PeerState.InCall -> {
                connectedAtMillis = System.currentTimeMillis()
                audioRouter.startCall(speaker.value)
                session.connect(bridge.remoteAudio)
            }

            PeerState.Ended -> {
                closeSession()
                delay(ENDED_SCREEN_MILLIS)
                bridge.reset()
            }

            else -> Unit
        }
    }

    private suspend fun closeSession() {
        if (!sessionOpen) return
        sessionOpen = false
        connectedAtMillis = null
        muted.value = false
        audioRouter.endCall()
        session.finish()
    }

    fun answer() = bridge.answer()

    fun hangUp() {
        if (bridge.state.value is PeerState.Incoming) bridge.reject() else bridge.hangUp()
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
        const val ENDED_SCREEN_MILLIS = 1_500L
    }
}
