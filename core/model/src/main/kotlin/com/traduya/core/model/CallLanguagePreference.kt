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

    /** Idiomas que se escuchan en la llamada para la otra persona. */
    val foreignCandidates: List<Language>
        get() = listOfNotNull(fixedLanguage).ifEmpty { Language.foreignLanguages }
}
