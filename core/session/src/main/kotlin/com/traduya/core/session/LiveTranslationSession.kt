package com.traduya.core.session

import com.traduya.core.domain.audio.CallRecordingStore
import com.traduya.core.domain.audio.RecordingWriter
import com.traduya.core.domain.repository.HistoryRepository
import com.traduya.core.domain.repository.SettingsRepository
import com.traduya.core.domain.translation.CallTranslationSession
import com.traduya.core.domain.translation.SpeechTranscriber
import com.traduya.core.domain.translation.TranslationEngine
import com.traduya.core.domain.usecase.DominantLanguageTracker
import com.traduya.core.domain.usecase.TranslateUtteranceUseCase
import com.traduya.core.model.AppSettings
import com.traduya.core.model.AudioChunk
import com.traduya.core.model.CallDirection
import com.traduya.core.model.Language
import com.traduya.core.model.SpeechEvent
import com.traduya.core.model.TranscriptLine
import com.traduya.core.model.TranscriptionConfig
import com.traduya.core.model.TranslationSessionState
import com.traduya.core.model.TranslationStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
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
 * Motor de traducción en vivo de una llamada:
 * se graba la llamada completa y, a la vez, la voz de la otra persona se transcribe y traduce
 * palabra a palabra. En el historial solo se guardan las frases traducidas de la otra persona.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
internal class LiveTranslationSession @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val recordingStore: CallRecordingStore,
    private val transcriber: SpeechTranscriber,
    private val translationEngine: TranslationEngine,
    private val translateUtterance: TranslateUtteranceUseCase,
) : CallTranslationSession {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val languages = DominantLanguageTracker()

    private val _state = MutableStateFlow(TranslationSessionState())
    override val state: StateFlow<TranslationSessionState> = _state.asStateFlow()

    /** El audio se reparte entre la grabación y la traducción. */
    private val audio = MutableSharedFlow<AudioChunk>(
        extraBufferCapacity = AUDIO_BUFFER_CHUNKS,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Frase en curso; cada frase nueva tiene su número para ignorar traducciones atrasadas. */
    private data class LivePhrase(val id: Long, val text: String, val language: Language)

    private val livePhrase = MutableStateFlow<LivePhrase?>(null)
    private val phraseId = AtomicLong(0)

    private var connectedAtMillis: Long? = null
    private var settings = AppSettings()
    private var audioJob: Job? = null
    private var translationJob: Job? = null
    private var recording: RecordingWriter? = null

    init {
        scope.launch { translateLivePhrases() }
    }

    override suspend fun begin(number: String, direction: CallDirection) {
        settings = settingsRepository.settings.first()
        val historyId = historyRepository.startCall(number, direction, System.currentTimeMillis())
        _state.value = TranslationSessionState(historyId = historyId, enabled = settings.autoTranslate)

        // Mientras suena, dejamos la IA lista para que la primera frase salga al instante
        scope.launch {
            val foreign = settings.languagePreference.foreignCandidates
            transcriber.prepare(foreign + Language.SPANISH)
            foreign.forEach { runCatching { translationEngine.translate("hello", it, Language.SPANISH) } }
        }
    }

    override fun connect(audio: Flow<AudioChunk>) {
        if (connectedAtMillis != null) return
        connectedAtMillis = System.currentTimeMillis()
        if (settings.recordCalls || _state.value.enabled) startAudio(audio)
        if (_state.value.enabled) startTranslation()
    }

    override fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled, liveText = "") }
        if (connectedAtMillis == null) return
        if (enabled) startTranslation() else scope.launch { stopTranslation() }
    }

    override suspend fun finish() {
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
        connectedAtMillis = null
        languages.reset()
        livePhrase.value = null
        _state.value = TranslationSessionState()
    }

    private fun startAudio(source: Flow<AudioChunk>) {
        val historyId = _state.value.historyId ?: return
        if (settings.recordCalls) recording = recordingStore.create(historyId)
        _state.update { it.copy(isRecording = recording != null) }

        audioJob = scope.launch(Dispatchers.IO) {
            try {
                source.collect { chunk ->
                    // Se graba la llamada completa, pero solo se traduce a la otra persona
                    recording?.write(chunk.callMix ?: chunk.samples)
                    audio.emit(chunk)
                    _state.update { it.copy(audioLevel = chunk.level, micBlocked = chunk.blocked) }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update {
                    it.copy(status = TranslationStatus.ERROR, errorMessage = error.message ?: "Audio no disponible")
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

    private companion object {
        const val AUDIO_BUFFER_CHUNKS = 50 // 5 segundos de audio
    }
}
