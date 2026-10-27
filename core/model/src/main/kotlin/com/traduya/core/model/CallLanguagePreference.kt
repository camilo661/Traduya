package com.traduya.core.model

/** Cómo decide Traduya el idioma de la otra persona durante la llamada. */
enum class CallLanguagePreference(
    val label: String,
    val description: String,
    val fixedLanguage: Language?,
) {
    AUTOMATIC(
        label = "Automático",
        description = "Detecta solo si hablan inglés o chino",
        fixedLanguage = null,
    ),
    ENGLISH(
        label = "Inglés",
        description = "La otra persona habla inglés",
        fixedLanguage = Language.ENGLISH,
    ),
    CHINESE(
        label = "Chino",
        description = "La otra persona habla chino",
        fixedLanguage = Language.CHINESE,
    );

    /** Idioma con el que arranca el reconocimiento de voz. */
    val primaryLanguage: Language get() = fixedLanguage ?: Language.ENGLISH

    /** Idiomas que el reconocedor puede detectar en esta llamada. */
    val candidateLanguages: List<Language>
        get() = listOfNotNull(fixedLanguage).ifEmpty { Language.foreignLanguages } + Language.SPANISH
}
