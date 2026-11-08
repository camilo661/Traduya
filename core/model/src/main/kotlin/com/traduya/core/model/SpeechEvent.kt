package com.traduya.core.model

/** Eventos que emite el reconocimiento de voz continuo. */
sealed interface SpeechEvent {
    data object Listening : SpeechEvent

    /** Frase en curso: se actualiza varias veces por segundo mientras la persona habla. */
    data class Partial(val text: String, val language: Language) : SpeechEvent

    /** Frase terminada, con el idioma elegido y la confianza del reconocedor (0 a 1). */
    data class Final(val text: String, val language: Language, val confidence: Float) : SpeechEvent

    /** La frase era del propio usuario (en su idioma) o ruido: se descarta. */
    data object Discarded : SpeechEvent

    data class Error(val message: String, val fatal: Boolean) : SpeechEvent
}

/**
 * @param foreignLanguages idiomas posibles de quien habla.
 * @param userLanguage idioma del usuario.
 * @param ignoreUserSpeech escucha también [userLanguage] para descartar lo que dice el usuario.
 *   Con false se transcribe lo que llega en [foreignLanguages], aunque sea la propia voz.
 */
data class TranscriptionConfig(
    val foreignLanguages: List<Language>,
    val userLanguage: Language = Language.SPANISH,
    val ignoreUserSpeech: Boolean = true,
)
