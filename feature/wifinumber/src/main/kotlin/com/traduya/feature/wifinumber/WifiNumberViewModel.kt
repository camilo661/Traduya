package com.traduya.feature.wifinumber

import android.content.Context
import android.telephony.TelephonyManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traduya.core.domain.wifinumber.PhoneNumberRules
import com.traduya.core.domain.wifinumber.RegisterWifiNumberUseCase
import com.traduya.core.domain.wifinumber.WifiNumberException
import com.traduya.core.domain.wifinumber.WifiNumberRegistry
import com.traduya.core.model.WifiNumberAccount
import com.traduya.core.model.WifiNumberError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WifiNumberUiState(
    val isConfigured: Boolean = true,
    val account: WifiNumberAccount? = null,
    val input: String = "",
    val consentGiven: Boolean = false,
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
) {
    val canRegister: Boolean get() = consentGiven && input.isNotBlank() && !isWorking
}

/** Registro voluntario del número de teléfono para recibir llamadas por WiFi. */
@HiltViewModel
class WifiNumberViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val registry: WifiNumberRegistry,
    private val registerWifiNumber: RegisterWifiNumberUseCase,
) : ViewModel() {

    private data class Form(
        val input: String,
        val consentGiven: Boolean = false,
        val isWorking: Boolean = false,
        val errorMessage: String? = null,
    )

    private val form = MutableStateFlow(Form(input = simCallingCode(context)))

    val uiState: StateFlow<WifiNumberUiState> = combine(registry.account, form) { account, form ->
        WifiNumberUiState(
            isConfigured = registry.isConfigured,
            account = account,
            input = form.input,
            consentGiven = form.consentGiven,
            isWorking = form.isWorking,
            errorMessage = form.errorMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WifiNumberUiState())

    fun onInputChange(value: String) {
        form.update { it.copy(input = value.filter { c -> c.isDigit() || c in "+ " }.take(MAX_INPUT), errorMessage = null) }
    }

    fun onConsentChange(given: Boolean) {
        form.update { it.copy(consentGiven = given) }
    }

    fun register() {
        if (!uiState.value.canRegister) return
        form.update { it.copy(isWorking = true, errorMessage = null) }
        viewModelScope.launch {
            val result = registerWifiNumber(form.value.input)
            form.update { it.copy(isWorking = false, errorMessage = result.exceptionOrNull()?.toMessage()) }
        }
    }

    fun unregister() {
        form.update { it.copy(isWorking = true, errorMessage = null) }
        viewModelScope.launch {
            val error = runCatching { registry.unregister() }.exceptionOrNull()
            form.update { it.copy(isWorking = false, consentGiven = false, errorMessage = error?.toMessage()) }
        }
    }

    private fun Throwable.toMessage(): String = when ((this as? WifiNumberException)?.error) {
        WifiNumberError.INVALID_NUMBER -> "Escribe el número con el código del país, por ejemplo +57 300 123 4567."
        WifiNumberError.NUMBER_TAKEN -> "Ese número ya está registrado en otro teléfono."
        WifiNumberError.NO_CONNECTION -> "No hay conexión a internet. Inténtalo de nuevo."
        WifiNumberError.NOT_CONFIGURED -> "Esta función todavía no está disponible."
        null -> "Algo salió mal. Inténtalo de nuevo."
    }

    private companion object {
        const val MAX_INPUT = 20

        fun simCallingCode(context: Context): String {
            val telephony = context.getSystemService(TelephonyManager::class.java)
            val code = PhoneNumberRules.callingCodeFor(telephony?.simCountryIso)
            return code?.let { "$it " } ?: "+"
        }
    }
}
