package com.traduya.core.model

/** Cómo decide Traduya el idioma de la otra persona durante la llamada. */
enum class CallLanguagePreference(
    val label: String,
    val fixedLanguage: Language?,
) {
    AUTOMATIC(label = "Automático", fixedLanguage = null),
    SPANISH(label = "Español", fixedLanguage = Language.SPANISH),
    ENGLISH(label = "Inglés", fixedLanguage = Language.ENGLISH),
    CHINESE(label = "Chino", fixedLanguage = Language.CHINESE);

    /** Idiomas que se escuchan en la llamada para la otra persona; nunca el del propio usuario. */
    fun foreignCandidates(myLanguage: Language): List<Language> =
        listOfNotNull(fixedLanguage?.takeIf { it != myLanguage }).ifEmpty { Language.othersThan(myLanguage) }

    fun description(myLanguage: Language): String = when (fixedLanguage) {
        null, myLanguage -> "Detecta solo si habla " +
            Language.othersThan(myLanguage).joinToString(" o ") { it.displayName.lowercase() }
        else -> "La otra persona habla ${fixedLanguage.displayName.lowercase()}"
    }

    companion object {
        /** Opciones que tienen sentido si el usuario habla [myLanguage]. */
        fun choicesFor(myLanguage: Language): List<CallLanguagePreference> =
            entries.filter { it.fixedLanguage != myLanguage }
    }
}
