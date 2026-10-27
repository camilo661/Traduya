package com.traduya.session

import com.traduya.core.domain.repository.HistoryRepository
import com.traduya.core.domain.repository.SettingsRepository
import com.traduya.core.domain.translation.CallTranslationSession
import com.traduya.core.domain.translation.SpeechTranscriber
import com.traduya.core.domain.usecase.DominantLanguageTracker
import com.traduya.core.domain.usecase.TranslateUtteranceUseCase
import com.traduya.core.model.ActiveCall
import com.traduya.core.model.AppSettings
import com.traduya.core.model.CallStatus
import com.traduya.core.model.SpeechEvent
import com.traduya.core.model.TranscriptLine
import com.traduya.core.model.TranscriptionConfig
import com.traduya.core.model.TranslationSessionState
import com.traduya.core.model.TranslationStatus
import com.traduya.core.telecom.CallManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orquesta cada llamada de principio a fin:
 * 1. Crea la entrada en el historial cuando aparece la llamada.
 * 2. Al conectar, activa el altavoz y arranca el reconocimiento de voz.
 * 3. Traduce cada frase y la guarda.
 * 4. Al colgar, detiene todo y guarda duración e idioma detectado.
 */
@Singleton
class CallSessionCoordinator @Inject constructor(
    private val callManager: CallManager,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val transcriber: SpeechTranscriber,
    private val translateUtterance: TranslateUtteranceUseCase,
) : CallTranslationSession {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val languages = DominantLanguageTracker()

    private val _state = MutableStateFlow(TranslationSessionState())
    override val state: StateFlow<TranslationSessionState> = _state.asStateFlow()

    private var trackedCallId: String? = null
    private var connectedAtMillis: Long? = null
    private var settings = AppSettings()
    private var translationJob: Job? = null

    fun start() {
        scope.launch {
            callManager.currentCall.collect { onCallChanged(it) }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled, partialText = "") }
        if (enabled && connectedAtMillis != null) startTranslation() else stopTranslation()
    }

    private suspend fun onCallChanged(call: ActiveCall?) {
        if (call == null || call.status == CallStatus.DISCONNECTED) {
            finishSession()
            return
        }
        if (call.id != trackedCallId) {
            finishSession()
            beginSession(call)
        }
        if (call.status == CallStatus.ACTIVE && connectedAtMillis == null) {
            connectedAtMillis = call.connectedAtMillis ?: System.currentTimeMillis()
            onConnected()
        }
    }

    private suspend fun beginSession(call: ActiveCall) {
        trackedCallId = call.id
        settings = settingsRepository.settings.first()
        val historyId = historyRepository.startCall(call.number, call.direction, System.currentTimeMillis())
        _state.value = TranslationSessionState(historyId = historyId, enabled = settings.autoTranslate)
    }

    private fun onConnected() {
        // Con el altavoz el micrófono capta también la voz de la otra persona
        if (settings.autoSpeaker) callManager.setSpeakerOn(true)
        if (_state.value.enabled) startTranslation()
    }

    private fun startTranslation() {
        if (translationJob?.isActive == true) return
        val preference = settings.languagePreference
        val config = TranscriptionConfig(
            primaryLanguage = preference.primaryLanguage,
            candidateLanguages = preference.candidateLanguages,
        )
        translationJob = scope.launch {
            transcriber.transcribe(config).collect { event -> onSpeechEvent(event) }
        }
    }

    private fun stopTranslation() {
        translationJob?.cancel()
        translationJob = null
        _state.update { it.copy(status = TranslationStatus.IDLE, partialText = "") }
    }

    private fun onSpeechEvent(event: SpeechEvent) {
        when (event) {
            SpeechEvent.Listening -> _state.update {
                if (it.status == TranslationStatus.TRANSLATING) it else it.copy(status = TranslationStatus.LISTENING, errorMessage = null)
            }

            is SpeechEvent.Partial -> _state.update { it.copy(partialText = event.text) }

            is SpeechEvent.Final -> {
                _state.update { it.copy(partialText = "", status = TranslationStatus.TRANSLATING) }
                scope.launch { translateAndSave(event) }
            }

            is SpeechEvent.Error -> _state.update {
                it.copy(status = TranslationStatus.ERROR, errorMessage = event.message)
            }
        }
    }

    private suspend fun translateAndSave(event: SpeechEvent.Final) {
        val historyId = _state.value.historyId ?: return
        runCatching {
            translateUtterance(
                text = event.text,
                recognizedLanguage = event.detectedLanguage,
                preference = settings.languagePreference,
                lastForeignLanguage = languages.lastForeign,
            )
        }.onSuccess { result ->
            languages.record(result.sourceLanguage)
            historyRepository.addLine(
                TranscriptLine(
                    callId = historyId,
                    timestampMillis = System.currentTimeMillis(),
                    speaker = result.speaker,
                    sourceLanguage = result.sourceLanguage,
                    targetLanguage = result.targetLanguage,
                    originalText = result.originalText,
                    translatedText = result.translatedText,
                ),
            )
            _state.update {
                it.copy(
                    status = TranslationStatus.LISTENING,
                    detectedLanguage = languages.lastForeign ?: it.detectedLanguage,
                    errorMessage = null,
                )
            }
        }.onFailure {
            _state.update {
                it.copy(
                    status = TranslationStatus.ERROR,
                    errorMessage = "No se pudo traducir. Revisa los modelos en Ajustes",
                )
            }
        }
    }

    private suspend fun finishSession() {
        val historyId = _state.value.historyId
        stopTranslation()
        if (historyId != null) {
            val duration = connectedAtMillis?.let { (System.currentTimeMillis() - it) / 1_000 } ?: 0
            historyRepository.finishCall(historyId, duration, languages.dominant())
        }
        trackedCallId = null
        connectedAtMillis = null
        languages.reset()
        _state.value = TranslationSessionState()
    }
}
