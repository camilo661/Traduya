package com.traduya.core.translation.mlkit

import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.traduya.core.domain.translation.TranslationEngine
import com.traduya.core.model.Language
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Traducción neuronal en el propio teléfono (Google ML Kit).
 * Cada par de idiomas reutiliza su traductor durante toda la llamada.
 */
@Singleton
internal class MlKitTranslationEngine @Inject constructor() : TranslationEngine {

    private val translators = mutableMapOf<Pair<Language, Language>, Translator>()
    private val mutex = Mutex()

    override suspend fun translate(text: String, from: Language, to: Language): String {
        if (from == to || text.isBlank()) return text
        val translator = translatorFor(from, to)
        translator.downloadModelIfNeeded().await()
        return translator.translate(text).await()
    }

    private suspend fun translatorFor(from: Language, to: Language): Translator = mutex.withLock {
        translators.getOrPut(from to to) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(from.mlKitCode)
                .setTargetLanguage(to.mlKitCode)
                .build()
            Translation.getClient(options)
        }
    }
}
