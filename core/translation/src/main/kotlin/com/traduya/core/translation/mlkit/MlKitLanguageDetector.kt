package com.traduya.core.translation.mlkit

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.traduya.core.domain.translation.LanguageDetector
import com.traduya.core.model.Language
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MlKitLanguageDetector @Inject constructor() : LanguageDetector {

    private val identifier = LanguageIdentification.getClient(
        LanguageIdentificationOptions.Builder()
            .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
            .build(),
    )

    override suspend fun detect(text: String): Language? {
        val code = runCatching { identifier.identifyLanguage(text).await() }.getOrNull()
        if (code == null || code == UNDETERMINED) return null
        return Language.fromCode(code)
    }

    private companion object {
        const val CONFIDENCE_THRESHOLD = 0.35f
        const val UNDETERMINED = "und"
    }
}
