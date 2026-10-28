package com.traduya.core.model

enum class TranslationStatus { IDLE, PREPARING, LISTENING, TRANSLATING, ERROR }

/** Estado en vivo de la traducción de la llamada en curso. */
data class TranslationSessionState(
    val historyId: Long? = null,
    val enabled: Boolean = true,
    val status: TranslationStatus = TranslationStatus.IDLE,
    /** Traducción al español de la frase que se está diciendo ahora mismo. */
    val liveText: String = "",
    val detectedLanguage: Language? = null,
    val errorMessage: String? = null,
    val audioLevel: Float = 0f,
    val micBlocked: Boolean = false,
    val isRecording: Boolean = false,
)
