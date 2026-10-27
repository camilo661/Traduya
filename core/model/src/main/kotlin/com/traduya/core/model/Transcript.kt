package com.traduya.core.model

/** Quién dijo la frase: el usuario (español) o la otra persona. */
enum class Speaker { ME, THEM }

/** Una frase de la llamada con su traducción. */
data class TranscriptLine(
    val id: Long = 0,
    val callId: Long,
    val timestampMillis: Long,
    val speaker: Speaker,
    val sourceLanguage: Language,
    val targetLanguage: Language,
    val originalText: String,
    val translatedText: String,
)

/** Resultado de traducir una frase reconocida, antes de guardarla. */
data class UtteranceTranslation(
    val speaker: Speaker,
    val sourceLanguage: Language,
    val targetLanguage: Language,
    val originalText: String,
    val translatedText: String,
)
