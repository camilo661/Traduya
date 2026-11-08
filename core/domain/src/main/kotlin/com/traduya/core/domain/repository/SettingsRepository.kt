package com.traduya.core.domain.repository

import com.traduya.core.model.AppSettings
import com.traduya.core.model.CallLanguagePreference
import com.traduya.core.model.Language
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setMyLanguage(language: Language)
    suspend fun setLanguagePreference(preference: CallLanguagePreference)
    suspend fun setAutoTranslate(enabled: Boolean)
    suspend fun setAutoSpeaker(enabled: Boolean)
    suspend fun setRecordCalls(enabled: Boolean)
    suspend fun setOnboardingCompleted(completed: Boolean)
}
