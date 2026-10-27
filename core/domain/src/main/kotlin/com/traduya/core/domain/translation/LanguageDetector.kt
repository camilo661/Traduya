package com.traduya.core.domain.translation

import com.traduya.core.model.Language

/** Identifica el idioma de un texto. Devuelve null si no está seguro. */
interface LanguageDetector {
    suspend fun detect(text: String): Language?
}
