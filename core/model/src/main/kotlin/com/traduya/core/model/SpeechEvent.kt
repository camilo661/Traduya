package com.traduya.core.model

/** Eventos que emite el reconocimiento de voz continuo. */
sealed interface SpeechEvent {
    data object Listening : SpeechEvent
    data class Partial(val text: String) : SpeechEvent
    data class Final(val text: String, val detectedLanguage: Language?) : SpeechEvent
    data class Error(val message: String, val fatal: Boolean) : SpeechEvent
}

data class TranscriptionConfig(
    val primaryLanguage: Language,
    val candidateLanguages: List<Language>,
)
