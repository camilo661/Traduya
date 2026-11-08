package com.traduya.core.translation.vosk

import com.traduya.core.domain.translation.SpeechTranscriber
import com.traduya.core.domain.usecase.Hypothesis
import com.traduya.core.domain.usecase.LanguageArbiter
import com.traduya.core.model.AudioChunk
import com.traduya.core.model.Language
import com.traduya.core.model.SpeechEvent
import com.traduya.core.model.TranscriptionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.vosk.Recognizer
import javax.inject.Inject

/**
 * Reconocimiento de voz en tiempo real con Vosk, sin internet.
 *
 * Un reconocedor por idioma escucha el mismo audio a la vez, cada uno en su propio núcleo del procesador
 * para no quedarse atrás del tiempo real. La frase la cierra el idioma que va ganando: los demás guardan
 * lo que oyeron si cierran antes (una pausa en otro idioma no corta la frase). Al cerrar, se comparan las
 * frases completas y el [LanguageArbiter] decide qué idioma era. En cuanto el idioma se confirma, solo
 * sigue escuchando ese reconocedor.
 *
 * Con [TranscriptionConfig.ignoreUserSpeech] también escucha el idioma del usuario para descartar su voz;
 * ese reconocedor solo cierra la frase si de verdad oyó hablar ese idioma, para no cortar las frases de la
 * otra persona a la mitad.
 */
internal class VoskSpeechTranscriber @Inject constructor(
    private val models: VoskModelStore,
) : SpeechTranscriber {

    override suspend fun prepare(languages: List<Language>) {
        languages.forEach { runCatching { models.load(it) } }
    }

    override fun transcribe(audio: Flow<AudioChunk>, config: TranscriptionConfig): Flow<SpeechEvent> = flow {
        val mine = config.userLanguage
        val wanted = config.foreignLanguages + if (config.ignoreUserSpeech) listOf(mine) else emptyList()
        val recognizers = LinkedHashMap<Language, Recognizer>()
        for (language in wanted.distinct()) {
            val model = models.load(language) ?: continue
            recognizers[language] = Recognizer(model, AudioChunk.SAMPLE_RATE.toFloat()).apply {
                setWords(true)
                // Cierra la frase tras medio segundo de silencio y nunca la deja crecer más de 15 s
                setEndpointerDelays(START_MAX_SECONDS, END_SILENCE_SECONDS, MAX_PHRASE_SECONDS)
            }
        }
        // Con ignoreUserSpeech su idioma es la voz del usuario; sin él, también se transcribe (subtítulos propios)
        val userFilter = config.ignoreUserSpeech && mine in recognizers && mine !in config.foreignLanguages
        val foreign = recognizers.keys.filter { !userFilter || it != mine }
        if (foreign.isEmpty()) {
            emit(SpeechEvent.Error("Descarga los idiomas en Ajustes para traducir", fatal = true))
            recognizers.values.forEach { it.close() }
            return@flow
        }

        val arbiter = LanguageArbiter(foreign)
        // Lo que ya oyeron los demás idiomas en esta frase (cerraron antes que el que va ganando)
        val heardSoFar = mutableMapOf<Language, MutableList<Hypothesis>>()
        var lastPartial = ""
        emit(SpeechEvent.Listening)

        try {
            audio.collect { chunk ->
                val endpointed = acceptInParallel(recognizers, chunk)

                // El filtro de la voz del usuario cerró su frase solo: si no oyó su idioma claro, la frase sigue
                if (userFilter && mine in endpointed && arbiter.leader !in endpointed) {
                    val heard = VoskResults.final(recognizers.getValue(mine).result, mine)
                    if (heard.confidence >= USER_SPEECH_CONFIDENCE && heard.wordCount > 0) {
                        // El usuario habló: se cierra la frase para descartarla
                        finishPhrase(recognizers, endpointed - mine, heardSoFar, arbiter, userFilter, heard)
                        lastPartial = ""
                        return@collect
                    }
                }

                if (arbiter.leader !in endpointed) {
                    // Otro idioma hizo una pausa: guarda lo que oyó y la frase sigue
                    endpointed.filter { it in foreign }.forEach { language ->
                        heardSoFar.getOrPut(language) { mutableListOf() } +=
                            VoskResults.final(recognizers.getValue(language).result, language)
                    }
                    lastPartial = emitPartial(recognizers, arbiter, lastPartial)
                    return@collect
                }

                finishPhrase(recognizers, endpointed, heardSoFar, arbiter, userFilter)
                lastPartial = ""
            }
        } finally {
            recognizers.values.forEach { it.close() }
        }
    }.flowOn(Dispatchers.Default)

    /** Pasa el trozo de audio a todos los reconocedores a la vez; devuelve los que cerraron frase. */
    private suspend fun acceptInParallel(recognizers: Map<Language, Recognizer>, chunk: AudioChunk): Set<Language> =
        coroutineScope {
            recognizers.map { (language, recognizer) ->
                async { language.takeIf { recognizer.acceptWaveForm(chunk.samples, chunk.samples.size) } }
            }.awaitAll().filterNotNull().toSet()
        }

    /** La frase en curso según el idioma que va ganando. Devuelve el último texto emitido. */
    private suspend fun FlowCollector<SpeechEvent>.emitPartial(
        recognizers: Map<Language, Recognizer>,
        arbiter: LanguageArbiter,
        lastPartial: String,
    ): String {
        val leader = recognizers[arbiter.leader] ?: return lastPartial
        val partial = VoskResults.partial(leader.partialResult, arbiter.leader)
        if (partial.isBlank() || partial == lastPartial) return lastPartial
        emit(SpeechEvent.Partial(partial, arbiter.leader))
        return partial
    }

    /** Cierra la frase en todos los reconocedores a la vez, compara las frases completas y emite el resultado. */
    private suspend fun FlowCollector<SpeechEvent>.finishPhrase(
        recognizers: LinkedHashMap<Language, Recognizer>,
        endpointed: Set<Language>,
        heardSoFar: MutableMap<Language, MutableList<Hypothesis>>,
        arbiter: LanguageArbiter,
        userFilter: Boolean,
        userSpeech: Hypothesis? = null,
    ) {
        val hypotheses = recognizers.map { (language, recognizer) ->
            if (userSpeech != null && language == userSpeech.language) return@map userSpeech
            val json = if (language in endpointed) recognizer.result else recognizer.finalResult
            joined(heardSoFar.remove(language).orEmpty() + VoskResults.final(json, language))
        }
        heardSoFar.clear()
        val winner = arbiter.decide(hypotheses)
        // Si ganó un idioma que no es de la otra persona, era la voz del usuario
        if (winner == null || (userFilter && !arbiter.isForeign(winner.language))) {
            emit(SpeechEvent.Discarded)
        } else {
            emit(SpeechEvent.Final(winner.text, winner.language, winner.confidence))
        }
        if (arbiter.isLocked) {
            // Idioma confirmado: dejamos de procesar los demás idiomas extranjeros
            recognizers.keys.filter { it != arbiter.leader && arbiter.isForeign(it) }.forEach {
                recognizers.remove(it)?.close()
            }
        }
    }

    /** Une los trozos de una frase: el texto seguido y la confianza media ponderada por palabras. */
    private fun joined(parts: List<Hypothesis>): Hypothesis {
        val spoken = parts.filter { it.wordCount > 0 }
        if (spoken.size <= 1) return spoken.firstOrNull() ?: parts.last()
        val words = spoken.sumOf { it.wordCount }
        return Hypothesis(
            language = spoken.first().language,
            text = spoken.joinToString(" ") { it.text.trim() },
            confidence = spoken.sumOf { it.confidence.toDouble() * it.wordCount }.toFloat() / words,
            wordCount = words,
        )
    }

    private companion object {
        const val START_MAX_SECONDS = 5f
        const val END_SILENCE_SECONDS = 0.5f
        const val MAX_PHRASE_SECONDS = 15f
        /** El filtro de la voz del usuario solo cierra la frase si oyó su idioma con esta confianza o más. */
        const val USER_SPEECH_CONFIDENCE = 0.7f
    }
}
