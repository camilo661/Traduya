package com.traduya.core.model

data class AppSettings(
    val languagePreference: CallLanguagePreference = CallLanguagePreference.AUTOMATIC,
    val autoTranslate: Boolean = true,
    val autoSpeaker: Boolean = true,
    val onboardingCompleted: Boolean = false,
)
