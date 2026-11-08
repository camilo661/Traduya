package com.traduya.core.model

data class AppSettings(
    /** Idioma del usuario: en él se muestra la traducción y en él se transcribe su propia voz. */
    val myLanguage: Language = Language.SPANISH,
    val languagePreference: CallLanguagePreference = CallLanguagePreference.AUTOMATIC,
    val autoTranslate: Boolean = true,
    val autoSpeaker: Boolean = true,
    val recordCalls: Boolean = true,
    val onboardingCompleted: Boolean = false,
)
