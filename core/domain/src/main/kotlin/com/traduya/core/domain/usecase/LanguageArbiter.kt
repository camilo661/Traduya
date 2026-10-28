package com.traduya.core.domain.usecase

import com.traduya.core.model.Language

/** Lo que entendió el reconocedor de un idioma para la misma frase. */
data class Hypothesis(
    val language: Language,
    val text: String,
    val confidence: Float,
    val wordCount: Int,
)

/**
 * Detección automática de idioma: varios reconocedores escuchan la misma frase
 * y gana el que la entendió con más confianza.
 *
 * Tras [winsToLock] victorias seguidas del mismo idioma extranjero se fija ese idioma,
 * así el teléfono deja de procesar los demás y va más rápido.
 * El español se trata aparte: significa que habló el usuario y nunca se fija.
 */
class LanguageArbiter(
    foreignLanguages: List<Language>,
    private val minConfidence: Float = 0.5f,
    private val winsToLock: Int = 3,
) {
    init {
        require(foreignLanguages.isNotEmpty()) { "Se necesita al menos un idioma extranjero" }
    }

    var leader: Language = foreignLanguages.first()
        private set

    var isLocked: Boolean = foreignLanguages.size == 1
        private set

    private var streak = 0

    /** Devuelve la hipótesis ganadora, o null si ninguna es fiable. */
    fun decide(hypotheses: List<Hypothesis>): Hypothesis? {
        val best = hypotheses
            .filter { it.text.isNotBlank() && it.wordCount > 0 }
            .maxByOrNull { score(it) }
            ?: return null
        if (best.confidence < minConfidence) return null
        if (best.language == Language.SPANISH) return best

        if (best.language == leader) streak++ else {
            leader = best.language
            streak = 1
        }
        if (streak >= winsToLock) isLocked = true
        return best
    }

    /** Una sola palabra con mucha confianza no debe ganarle a una frase completa. */
    private fun score(h: Hypothesis): Float = h.confidence * (h.wordCount / (h.wordCount + 1f))
}
