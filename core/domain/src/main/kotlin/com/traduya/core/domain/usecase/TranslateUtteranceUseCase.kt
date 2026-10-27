package com.traduya.core.domain.usecase

import com.traduya.core.domain.translation.LanguageDetector
import com.traduya.core.domain.translation.TranslationEngine
import com.traduya.core.model.CallLanguagePreference
import com.traduya.core.model.Language
import com.traduya.core.model.Speaker
import com.traduya.core.model.UtteranceTranslation
import javax.inject.Inject

/**
 * Decide quién habló y hacia qué idioma traducir:
 * - Si la frase está en español la dijo el usuario y se traduce al idioma de la otra persona.
 * - Si está en inglés o chino la dijo la otra persona y se traduce al español.
 */
class TranslateUtteranceUseCase @Inject constructor(
    private val detector: LanguageDetector,
    private val engine: TranslationEngine,
) {
    suspend operator fun invoke(
        text: String,
        recognizedLanguage: Language?,
        preference: CallLanguagePreference,
        lastForeignLanguage: Language?,
    ): UtteranceTranslation {
        val source = recognizedLanguage
            ?: detector.detect(text)
            ?: preference.primaryLanguage

        return if (source == Language.SPANISH) {
            val target = preference.fixedLanguage ?: lastForeignLanguage ?: Language.ENGLISH
            UtteranceTranslation(
                speaker = Speaker.ME,
                sourceLanguage = source,
                targetLanguage = target,
                originalText = text,
                translatedText = engine.translate(text, source, target),
            )
        } else {
            UtteranceTranslation(
                speaker = Speaker.THEM,
                sourceLanguage = source,
                targetLanguage = Language.SPANISH,
                originalText = text,
                translatedText = engine.translate(text, source, Language.SPANISH),
            )
        }
    }
}
