package com.traduya.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traduya.core.domain.repository.SettingsRepository
import com.traduya.core.domain.translation.LanguagePacks
import com.traduya.core.domain.wifinumber.WifiNumberRegistry
import com.traduya.core.model.Language
import com.traduya.core.model.LanguagePackStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val permissionsGranted: Boolean = false,
    val isDefaultDialer: Boolean = false,
    val packsStatus: LanguagePackStatus = LanguagePackStatus.Missing,
    val wifiNumberRegistered: Boolean = false,
) {
    val canContinue: Boolean
        get() = permissionsGranted && packsStatus == LanguagePackStatus.Ready
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val languagePacks: LanguagePacks,
    wifiNumbers: WifiNumberRegistry,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            wifiNumbers.account.collect { account ->
                _uiState.update { it.copy(wifiNumberRegistered = account != null) }
            }
        }
        viewModelScope.launch {
            if (languagePacks.installed().containsAll(Language.entries)) {
                _uiState.update { it.copy(packsStatus = LanguagePackStatus.Ready) }
            }
        }
    }

    fun refreshSystemState(permissionsGranted: Boolean, isDefaultDialer: Boolean) {
        _uiState.update { it.copy(permissionsGranted = permissionsGranted, isDefaultDialer = isDefaultDialer) }
    }

    /** Descarga los tres idiomas uno detrás de otro, mostrando el progreso total. */
    fun downloadPacks() {
        if (_uiState.value.packsStatus is LanguagePackStatus.Downloading) return
        _uiState.update { it.copy(packsStatus = LanguagePackStatus.Downloading(0f)) }
        viewModelScope.launch {
            val result = runCatching {
                val pending = Language.entries - languagePacks.installed()
                pending.forEachIndexed { index, language ->
                    languagePacks.install(language).collect { progress ->
                        val total = (index + progress) / pending.size
                        _uiState.update { it.copy(packsStatus = LanguagePackStatus.Downloading(total)) }
                    }
                }
            }
            _uiState.update {
                it.copy(packsStatus = if (result.isSuccess) LanguagePackStatus.Ready else LanguagePackStatus.Failed)
            }
        }
    }

    fun finish() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted(true) }
    }
}
