package com.traduya.core.domain.usecase

import com.traduya.core.model.Language

/** Lleva la cuenta de los idiomas de la otra persona para saber cuál predominó. */
class DominantLanguageTracker {
    private val counts = mutableMapOf<Language, Int>()

    var lastForeign: Language? = null
        private set

    fun record(language: Language) {
        if (language == Language.SPANISH) return
        counts[language] = (counts[language] ?: 0) + 1
        lastForeign = language
    }

    fun dominant(): Language? = counts.maxByOrNull { it.value }?.key

    fun reset() {
        counts.clear()
        lastForeign = null
    }
}
