package com.traduya.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traduya.core.domain.repository.SettingsRepository
import com.traduya.core.domain.translation.TranslationModels
import com.traduya.core.model.Language
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ModelsStatus { PENDING, DOWNLOADING, READY, FAILED }

data class OnboardingUiState(
    val permissionsGranted: Boolean = false,
    val isDefaultDialer: Boolean = false,
    val modelsStatus: ModelsStatus = ModelsStatus.PENDING,
) {
    val canContinue: Boolean get() = permissionsGranted && isDefaultDialer
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val translationModels: TranslationModels,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val downloaded = runCatching { translationModels.downloadedLanguages() }.getOrDefault(emptySet())
            if (downloaded.containsAll(Language.entries)) {
                _uiState.update { it.copy(modelsStatus = ModelsStatus.READY) }
            }
        }
    }

    fun refreshSystemState(permissionsGranted: Boolean, isDefaultDialer: Boolean) {
        _uiState.update { it.copy(permissionsGranted = permissionsGranted, isDefaultDialer = isDefaultDialer) }
    }

    fun downloadModels() {
        if (_uiState.value.modelsStatus == ModelsStatus.DOWNLOADING) return
        _uiState.update { it.copy(modelsStatus = ModelsStatus.DOWNLOADING) }
        viewModelScope.launch {
            val result = runCatching { Language.entries.forEach { translationModels.download(it) } }
            _uiState.update {
                it.copy(modelsStatus = if (result.isSuccess) ModelsStatus.READY else ModelsStatus.FAILED)
            }
        }
    }

    fun finish() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted(true) }
    }
}
