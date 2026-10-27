package com.traduya.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.traduya.core.domain.repository.SettingsRepository
import com.traduya.core.model.AppSettings
import com.traduya.core.model.CallLanguagePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            languagePreference = prefs[Keys.LANGUAGE]
                ?.let { name -> CallLanguagePreference.entries.firstOrNull { it.name == name } }
                ?: defaults.languagePreference,
            autoTranslate = prefs[Keys.AUTO_TRANSLATE] ?: defaults.autoTranslate,
            autoSpeaker = prefs[Keys.AUTO_SPEAKER] ?: defaults.autoSpeaker,
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: defaults.onboardingCompleted,
        )
    }

    override suspend fun setLanguagePreference(preference: CallLanguagePreference) {
        dataStore.edit { it[Keys.LANGUAGE] = preference.name }
    }

    override suspend fun setAutoTranslate(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_TRANSLATE] = enabled }
    }

    override suspend fun setAutoSpeaker(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_SPEAKER] = enabled }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_DONE] = completed }
    }

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language_preference")
        val AUTO_TRANSLATE = booleanPreferencesKey("auto_translate")
        val AUTO_SPEAKER = booleanPreferencesKey("auto_speaker")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
    }
}
