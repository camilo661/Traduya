package com.traduya.core.domain.translation

import com.traduya.core.model.Language

/** Motor de IA que traduce texto entre dos idiomas. */
interface TranslationEngine {
    suspend fun translate(text: String, from: Language, to: Language): String
}
