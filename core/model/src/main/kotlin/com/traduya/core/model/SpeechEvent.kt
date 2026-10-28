package com.traduya.core.model

/** Eventos que emite el reconocimiento de voz continuo. */
sealed interface SpeechEvent {
    data object Listening : SpeechEvent

    /** Frase en curso: se actualiza varias veces por segundo mientras la persona habla. */
    data class Partial(val text: String, val language: Language) : SpeechEvent

    /** Frase terminada, con el idioma elegido y la confianza del reconocedor (0 a 1). */
    data class Final(val text: String, val language: Language, val confidence: Float) : SpeechEvent

    /** La frase era del propio usuario (en español) o ruido: se descarta. */
    data object Discarded : SpeechEvent

    data class Error(val message: String, val fatal: Boolean) : SpeechEvent
}

/**
 * @param foreignLanguages idiomas posibles de la otra persona.
 * @param ignoreUserSpeech escucha también el español para descartar lo que dice el usuario.
 */
data class TranscriptionConfig(
    val foreignLanguages: List<Language>,
    val ignoreUserSpeech: Boolean = true,
)
