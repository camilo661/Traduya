package com.traduya.session

import com.traduya.core.domain.audio.AudioCapture
import com.traduya.core.domain.audio.CallRecordingStore
import com.traduya.core.domain.audio.RecordingWriter
import com.traduya.core.domain.repository.HistoryRepository
import com.traduya.core.domain.repository.SettingsRepository
import com.traduya.core.domain.translation.CallTranslationSession
import com.traduya.core.domain.translation.SpeechTranscriber
import com.traduya.core.domain.translation.TranslationEngine
import com.traduya.core.domain.usecase.DominantLanguageTracker
import com.traduya.core.domain.usecase.TranslateUtteranceUseCase
import com.traduya.core.model.ActiveCall
import com.traduya.core.model.AppSettings
import com.traduya.core.model.AudioChunk
import com.traduya.core.model.CallStatus
import com.traduya.core.model.Language
import com.traduya.core.model.SpeechEvent
import com.traduya.core.model.TranscriptLine
import com.traduya.core.model.TranscriptionConfig
import com.traduya.core.model.TranslationSessionState
import com.traduya.core.model.TranslationStatus
import com.traduya.core.telecom.CallManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orquesta cada llamada de principio a fin:
 * 1. Al aparecer la llamada crea la entrada del historial y precarga los modelos de IA.
 * 2. Al conectar activa el altavoz y abre el micrófono una sola vez.
 * 3. Ese audio se graba en un archivo y a la vez se traduce en vivo, palabra a palabra.
 * 4. Al colgar guarda duración, idioma detectado y la grabación.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class CallSessionCoordinator @Inject constructor(
    private val callManager: CallManager,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val audioCapture: AudioCapture,
    private val recordingStore: CallRecordingStore,
    private val transcriber: SpeechTranscriber,
    private val translationEngine: TranslationEngine,
    private val translateUtterance: TranslateUtteranceUseCase,
) : CallTranslationSession {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val languages = DominantLanguageTracker()

    private val _state = MutableStateFlow(TranslationSessionState())
    override val state: StateFlow<TranslationSessionState> = _state.asStateFlow()

    /** El audio del micrófono se reparte entre la grabación y la traducción. */
    private val audio = MutableSharedFlow<AudioChunk>(
        extraBufferCapacity = AUDIO_BUFFER_CHUNKS,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Frase en curso; cada frase nueva tiene su número para ignorar traducciones atrasadas. */
    private data class LivePhrase(val id: Long, val text: String, val language: Language)

    private val livePhrase = MutableStateFlow<LivePhrase?>(null)
    private val phraseId = AtomicLong(0)

    private var trackedCallId: String? = null
    private var connectedAtMillis: Long? = null
    private var settings = AppSettings()
    private var audioJob: Job? = null
    private var translationJob: Job? = null
    private var recording: RecordingWriter? = null

    fun start() {
        scope.launch { callManager.currentCall.collect { onCallChanged(it) } }
        scope.launch { translateLivePhrases() }
    }

    override fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled, liveText = "") }
        if (connectedAtMillis == null) return
        if (enabled) {
            startAudio()
            startTranslation()
        } else {
            scope.launch { stopTranslation() }
        }
    }

    // region Ciclo de vida de la llamada

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

        // Mientras suena o marca, dejamos la IA lista para que la primera frase salga al instante
        scope.launch {
            val foreign = settings.languagePreference.foreignCandidates
            transcriber.prepare(foreign + Language.SPANISH)
            foreign.forEach { runCatching { translationEngine.translate("hello", it, Language.SPANISH) } }
        }
    }

    private fun onConnected() {
        // Con el altavoz el micrófono capta también la voz de la otra persona
        if (settings.autoSpeaker) callManager.setSpeakerOn(true)
        if (settings.recordCalls || _state.value.enabled) startAudio()
        if (_state.value.enabled) startTranslation()
    }

    private suspend fun finishSession() {
        val historyId = _state.value.historyId
        stopTranslation()
        audioJob?.cancelAndJoin()
        audioJob = null
        val recordingPath = withContext(Dispatchers.IO) { recording?.finish() }
        recording = null
        if (historyId != null) {
            val duration = connectedAtMillis?.let { (System.currentTimeMillis() - it) / 1_000 } ?: 0
            historyRepository.finishCall(historyId, duration, languages.dominant(), recordingPath)
        }
        trackedCallId = null
        connectedAtMillis = null
        languages.reset()
        livePhrase.value = null
        _state.value = TranslationSessionState()
    }

    // endregion

    // region Audio y traducción

    private fun startAudio() {
        if (audioJob?.isActive == true) return
        val historyId = _state.value.historyId ?: return
        if (settings.recordCalls && recording == null) recording = recordingStore.create(historyId)
        _state.update { it.copy(isRecording = recording != null) }

        audioJob = scope.launch(Dispatchers.IO) {
            runCatching {
                audioCapture.chunks().collect { chunk ->
                    recording?.write(chunk.samples)
                    audio.emit(chunk)
                    _state.update { it.copy(audioLevel = chunk.level, micBlocked = chunk.blocked) }
                }
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                _state.update {
                    it.copy(status = TranslationStatus.ERROR, errorMessage = error.message ?: "Micrófono no disponible")
                }
            }
        }
    }

    private fun startTranslation() {
        if (translationJob?.isActive == true) return
        val config = TranscriptionConfig(foreignLanguages = settings.languagePreference.foreignCandidates)
        _state.update { it.copy(status = TranslationStatus.PREPARING, errorMessage = null) }
        translationJob = scope.launch {
            transcriber.transcribe(audio, config).collect(::onSpeechEvent)
        }
    }

    private suspend fun stopTranslation() {
        translationJob?.cancelAndJoin()
        translationJob = null
        livePhrase.value = null
        _state.update { it.copy(status = TranslationStatus.IDLE, liveText = "") }
    }

    private fun onSpeechEvent(event: SpeechEvent) {
        when (event) {
            SpeechEvent.Listening -> _state.update { it.copy(status = TranslationStatus.LISTENING) }

            is SpeechEvent.Partial -> {
                livePhrase.value = LivePhrase(phraseId.get(), event.text, event.language)
                _state.update { it.copy(detectedLanguage = event.language) }
            }

            is SpeechEvent.Final -> {
                phraseId.incrementAndGet()
                livePhrase.value = null
                scope.launch { translateAndSave(event) }
            }

            SpeechEvent.Discarded -> {
                phraseId.incrementAndGet()
                livePhrase.value = null
                _state.update { it.copy(liveText = "") }
            }

            is SpeechEvent.Error -> _state.update {
                it.copy(status = TranslationStatus.ERROR, errorMessage = event.message)
            }
        }
    }

    /** Traduce la frase en curso cada vez que cambia; si llega una más nueva, la anterior se abandona. */
    private suspend fun translateLivePhrases() {
        livePhrase
            .filterNotNull()
            .mapLatest { phrase ->
                phrase to runCatching {
                    translationEngine.translate(phrase.text, phrase.language, Language.SPANISH)
                }.getOrNull()
            }
            .collect { (phrase, translated) ->
                if (translated != null && phrase.id == phraseId.get()) {
                    _state.update { it.copy(liveText = translated) }
                }
            }
    }

    private suspend fun translateAndSave(event: SpeechEvent.Final) {
        val historyId = _state.value.historyId ?: return
        runCatching { translateUtterance(event.text, event.language) }
            .onSuccess { result ->
                languages.record(result.sourceLanguage)
                withContext(NonCancellable) {
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
                }
                _state.update {
                    it.copy(
                        status = TranslationStatus.LISTENING,
                        detectedLanguage = result.sourceLanguage,
                        liveText = if (livePhrase.value == null) "" else it.liveText,
                        errorMessage = null,
                    )
                }
            }
            .onFailure {
                _state.update {
                    it.copy(
                        status = TranslationStatus.ERROR,
                        errorMessage = "No se pudo traducir. Revisa los idiomas en Ajustes",
                    )
                }
            }
    }

    // endregion

    private companion object {
        const val AUDIO_BUFFER_CHUNKS = 50 // 5 segundos de audio
    }
}
