package com.traduya.feature.incall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traduya.core.domain.repository.HistoryRepository
import com.traduya.core.domain.translation.CallTranslationSession
import com.traduya.core.model.ActiveCall
import com.traduya.core.model.CallStatus
import com.traduya.core.model.TranscriptLine
import com.traduya.core.telecom.CallManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InCallViewModel @Inject constructor(
    private val callManager: CallManager,
    private val session: CallTranslationSession,
    historyRepository: HistoryRepository,
) : ViewModel() {

    /** Recordamos la última llamada para seguir mostrándola un momento después de colgar. */
    private var lastCall: ActiveCall? = null

    private val transcript: Flow<List<TranscriptLine>> = session.state
        .map { it.historyId }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { historyRepository.observeTranscript(it) }
        .onStart { emit(emptyList()) }

    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000)
        }
    }

    val uiState: StateFlow<InCallUiState> = combine(
        callManager.currentCall,
        session.state,
        transcript,
        ticker,
    ) { call, translation, lines, now ->
        if (call != null) lastCall = call
        val shown = call ?: lastCall
        val ended = call == null || call.status == CallStatus.DISCONNECTED
        InCallUiState(
            callerLabel = shown?.label.orEmpty(),
            number = shown?.number.orEmpty(),
            status = if (ended) CallStatus.DISCONNECTED else call!!.status,
            elapsedSeconds = shown?.connectedAtMillis?.let { (now - it).coerceAtLeast(0) / 1_000 } ?: 0,
            isMuted = shown?.isMuted ?: false,
            isSpeakerOn = shown?.isSpeakerOn ?: false,
            translationEnabled = translation.enabled,
            translationStatus = translation.status,
            translationError = translation.errorMessage,
            partialText = translation.partialText,
            detectedLanguage = translation.detectedLanguage,
            lines = lines,
            hasEnded = ended,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InCallUiState())

    fun answer() = callManager.answer()

    fun hangUp() = callManager.hangUp()

    fun toggleMute() = callManager.setMuted(!uiState.value.isMuted)

    fun toggleSpeaker() = callManager.setSpeakerOn(!uiState.value.isSpeakerOn)

    fun toggleTranslation() = session.setEnabled(!uiState.value.translationEnabled)

    fun sendDtmf(digit: Char) = callManager.playDtmf(digit)
}
