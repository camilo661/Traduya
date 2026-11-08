package com.traduya.core.domain.usecase

import com.traduya.core.domain.translation.TranslationEngine
import com.traduya.core.model.Language
import com.traduya.core.model.Speaker
import com.traduya.core.model.UtteranceTranslation
import javax.inject.Inject

/** Traduce al idioma del usuario ([target]) lo que dijo la otra persona. */
class TranslateUtteranceUseCase @Inject constructor(
    private val engine: TranslationEngine,
) {
    suspend operator fun invoke(text: String, source: Language, target: Language): UtteranceTranslation =
        UtteranceTranslation(
            speaker = Speaker.THEM,
            sourceLanguage = source,
            targetLanguage = target,
            originalText = text,
            translatedText = if (source == target) text else engine.translate(text, source, target),
        )
}
