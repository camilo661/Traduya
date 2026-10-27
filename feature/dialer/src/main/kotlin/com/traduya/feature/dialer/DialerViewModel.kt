package com.traduya.feature.dialer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traduya.core.domain.repository.SettingsRepository
import com.traduya.core.model.CallLanguagePreference
import com.traduya.core.telecom.CallManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class DialerUiState(
    val number: String = "",
    val languagePreference: CallLanguagePreference = CallLanguagePreference.AUTOMATIC,
    val errorMessage: String? = null,
)

@HiltViewModel
class DialerViewModel @Inject constructor(
    private val callManager: CallManager,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val number = MutableStateFlow("")
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DialerUiState> = combine(
        number,
        error,
        settingsRepository.settings,
    ) { number, error, settings ->
        DialerUiState(number = number, languagePreference = settings.languagePreference, errorMessage = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DialerUiState())

    fun setNumber(value: String) {
        number.value = value.filter { it.isDigit() || it in "+*#" }.take(MAX_LENGTH)
    }

    fun onKey(key: Char) {
        number.update { (it + key).take(MAX_LENGTH) }
    }

    fun deleteLast() {
        number.update { it.dropLast(1) }
    }

    fun clear() {
        number.value = ""
    }

    fun call() {
        val target = number.value
        if (target.isBlank()) return
        callManager.placeCall(target)
            .onSuccess { error.value = null }
            .onFailure { error.value = it.message ?: "No se pudo iniciar la llamada" }
    }

    fun dismissError() {
        error.value = null
    }

    private companion object {
        const val MAX_LENGTH = 20
    }
}
