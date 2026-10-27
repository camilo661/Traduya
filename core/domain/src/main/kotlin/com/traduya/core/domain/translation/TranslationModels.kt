package com.traduya.core.domain.translation

import com.traduya.core.model.Language

/** Modelos de traducción que se descargan al teléfono para funcionar sin internet. */
interface TranslationModels {
    suspend fun downloadedLanguages(): Set<Language>
    suspend fun download(language: Language)
}
