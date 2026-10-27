package com.traduya.core.model

enum class TranslationStatus { IDLE, LISTENING, TRANSLATING, ERROR }

/** Estado en vivo de la traducción de la llamada en curso. */
data class TranslationSessionState(
    val historyId: Long? = null,
    val enabled: Boolean = true,
    val status: TranslationStatus = TranslationStatus.IDLE,
    val partialText: String = "",
    val detectedLanguage: Language? = null,
    val errorMessage: String? = null,
)
