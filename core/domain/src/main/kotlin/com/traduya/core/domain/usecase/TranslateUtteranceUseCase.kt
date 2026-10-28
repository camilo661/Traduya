package com.traduya.core.domain.usecase

import com.traduya.core.domain.translation.TranslationEngine
import com.traduya.core.model.Language
import com.traduya.core.model.Speaker
import com.traduya.core.model.UtteranceTranslation
import javax.inject.Inject

/** Traduce al español lo que dijo la otra persona. */
class TranslateUtteranceUseCase @Inject constructor(
    private val engine: TranslationEngine,
) {
    suspend operator fun invoke(text: String, source: Language): UtteranceTranslation =
        UtteranceTranslation(
            speaker = Speaker.THEM,
            sourceLanguage = source,
            targetLanguage = Language.SPANISH,
            originalText = text,
            translatedText = engine.translate(text, source, Language.SPANISH),
        )
}
