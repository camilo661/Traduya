package com.traduya.core.translation.vosk

import com.traduya.core.domain.translation.SpeechTranscriber
import com.traduya.core.domain.usecase.LanguageArbiter
import com.traduya.core.model.AudioChunk
import com.traduya.core.model.Language
import com.traduya.core.model.SpeechEvent
import com.traduya.core.model.TranscriptionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.vosk.Recognizer
import javax.inject.Inject

/**
 * Reconocimiento de voz en tiempo real con Vosk, sin internet.
 *
 * Un reconocedor por idioma escucha el mismo audio a la vez. Cuando uno detecta el final
 * de una frase, se cierran todos en el mismo punto y el [LanguageArbiter] decide qué idioma
 * era. Las frases en español son del propio usuario y se descartan.
 */
internal class VoskSpeechTranscriber @Inject constructor(
    private val models: VoskModelStore,
) : SpeechTranscriber {

    override suspend fun prepare(languages: List<Language>) {
        languages.forEach { runCatching { models.load(it) } }
    }

    override fun transcribe(audio: Flow<AudioChunk>, config: TranscriptionConfig): Flow<SpeechEvent> = flow {
        val wanted = config.foreignLanguages + if (config.ignoreUserSpeech) listOf(Language.SPANISH) else emptyList()
        val recognizers = LinkedHashMap<Language, Recognizer>()
        for (language in wanted) {
            val model = models.load(language) ?: continue
            recognizers[language] = Recognizer(model, AudioChunk.SAMPLE_RATE.toFloat()).apply { setWords(true) }
        }
        val foreign = recognizers.keys.filter { it != Language.SPANISH }
        if (foreign.isEmpty()) {
            emit(SpeechEvent.Error("Descarga los idiomas en Ajustes para traducir", fatal = true))
            recognizers.values.forEach { it.close() }
            return@flow
        }

        val arbiter = LanguageArbiter(foreign)
        var lastPartial = ""
        emit(SpeechEvent.Listening)

        try {
            audio.collect { chunk ->
                val endpointed = recognizers.filter { (_, recognizer) ->
                    recognizer.acceptWaveForm(chunk.samples, chunk.samples.size)
                }.keys

                if (endpointed.isEmpty()) {
                    val leader = recognizers[arbiter.leader] ?: return@collect
                    val partial = VoskResults.partial(leader.partialResult, arbiter.leader)
                    if (partial.isNotBlank() && partial != lastPartial) {
                        lastPartial = partial
                        emit(SpeechEvent.Partial(partial, arbiter.leader))
                    }
                    return@collect
                }

                // Cerramos la frase en todos los reconocedores a la vez para compararlos
                val hypotheses = recognizers.map { (language, recognizer) ->
                    val json = if (language in endpointed) recognizer.result else recognizer.finalResult
                    VoskResults.final(json, language)
                }
                lastPartial = ""
                val winner = arbiter.decide(hypotheses)
                if (winner == null || winner.language == Language.SPANISH) {
                    emit(SpeechEvent.Discarded)
                } else {
                    emit(SpeechEvent.Final(winner.text, winner.language, winner.confidence))
                }

                if (arbiter.isLocked) {
                    // Idioma confirmado: dejamos de procesar los demás idiomas extranjeros
                    recognizers.keys.filter { it != arbiter.leader && it != Language.SPANISH }.forEach {
                        recognizers.remove(it)?.close()
                    }
                }
            }
        } finally {
            recognizers.values.forEach { it.close() }
        }
    }.flowOn(Dispatchers.Default)
}
