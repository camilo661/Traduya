package com.traduya.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traduya.core.domain.repository.SettingsRepository
import com.traduya.core.domain.translation.TranslationModels
import com.traduya.core.model.AppSettings
import com.traduya.core.model.CallLanguagePreference
import com.traduya.core.model.Language
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ModelState { MISSING, DOWNLOADING, READY, FAILED }

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val models: Map<Language, ModelState> = Language.entries.associateWith { ModelState.MISSING },
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val translationModels: TranslationModels,
) : ViewModel() {

    private val models = MutableStateFlow(Language.entries.associateWith { ModelState.MISSING })

    val uiState: StateFlow<SettingsUiState> = combine(settingsRepository.settings, models) { settings, models ->
        SettingsUiState(settings, models)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        refreshModels()
    }

    fun setLanguagePreference(preference: CallLanguagePreference) {
        viewModelScope.launch { settingsRepository.setLanguagePreference(preference) }
    }

    fun setAutoTranslate(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoTranslate(enabled) }
    }

    fun setAutoSpeaker(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoSpeaker(enabled) }
    }

    fun downloadModel(language: Language) {
        models.update { it + (language to ModelState.DOWNLOADING) }
        viewModelScope.launch {
            val result = runCatching { translationModels.download(language) }
            models.update { it + (language to if (result.isSuccess) ModelState.READY else ModelState.FAILED) }
        }
    }

    private fun refreshModels() {
        viewModelScope.launch {
            val downloaded = runCatching { translationModels.downloadedLanguages() }.getOrDefault(emptySet())
            models.value = Language.entries.associateWith {
                if (it in downloaded) ModelState.READY else ModelState.MISSING
            }
        }
    }
}
