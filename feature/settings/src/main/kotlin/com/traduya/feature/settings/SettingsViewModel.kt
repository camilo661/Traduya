package com.traduya.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traduya.core.domain.repository.SettingsRepository
import com.traduya.core.domain.translation.LanguagePacks
import com.traduya.core.model.AppSettings
import com.traduya.core.model.CallLanguagePreference
import com.traduya.core.model.Language
import com.traduya.core.model.LanguagePackStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val packs: Map<Language, LanguagePackStatus> = Language.entries.associateWith { LanguagePackStatus.Missing },
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val languagePacks: LanguagePacks,
) : ViewModel() {

    private val packs = MutableStateFlow(Language.entries.associateWith<Language, LanguagePackStatus> { LanguagePackStatus.Missing })

    val uiState: StateFlow<SettingsUiState> = combine(settingsRepository.settings, packs) { settings, packs ->
        SettingsUiState(settings, packs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        viewModelScope.launch {
            val installed = languagePacks.installed()
            packs.value = Language.entries.associateWith {
                if (it in installed) LanguagePackStatus.Ready else LanguagePackStatus.Missing
            }
        }
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

    fun setRecordCalls(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setRecordCalls(enabled) }
    }

    fun downloadPack(language: Language) {
        if (packs.value[language] is LanguagePackStatus.Downloading) return
        viewModelScope.launch {
            val result = runCatching {
                languagePacks.install(language).collect { progress ->
                    packs.update { it + (language to LanguagePackStatus.Downloading(progress)) }
                }
            }
            packs.update {
                it + (language to if (result.isSuccess) LanguagePackStatus.Ready else LanguagePackStatus.Failed)
            }
        }
    }
}
