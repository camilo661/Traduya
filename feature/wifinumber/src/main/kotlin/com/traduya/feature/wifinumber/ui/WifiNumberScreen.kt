package com.traduya.feature.wifinumber.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.traduya.feature.wifinumber.WifiNumberUiState
import com.traduya.feature.wifinumber.WifiNumberViewModel

@Composable
fun WifiNumberRoute(onBack: () -> Unit, viewModel: WifiNumberViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WifiNumberScreen(
        uiState = uiState,
        onBack = onBack,
        onInputChange = viewModel::onInputChange,
        onConsentChange = viewModel::onConsentChange,
        onRegister = viewModel::register,
        onUnregister = viewModel::unregister,
    )
}

@Composable
internal fun WifiNumberScreen(
    uiState: WifiNumberUiState,
    onBack: () -> Unit,
    onInputChange: (String) -> Unit,
    onConsentChange: (Boolean) -> Unit,
    onRegister: () -> Unit,
    onUnregister: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                }
                Text("Llamadas por WiFi", style = MaterialTheme.typography.headlineSmall)
            }

            HowItWorksCard()

            when {
                !uiState.isConfigured -> NotAvailableYet()
                uiState.account != null -> RegisteredNumber(
                    phoneNumber = uiState.account.phoneNumber,
                    isWorking = uiState.isWorking,
                    errorMessage = uiState.errorMessage,
                    onUnregister = onUnregister,
                )
                else -> RegisterNumberForm(
                    uiState = uiState,
                    onInputChange = onInputChange,
                    onConsentChange = onConsentChange,
                    onRegister = onRegister,
                )
            }
        }
    }
}

@Composable
private fun NotAvailableYet() {
    Text(
        text = "Esta función estará disponible en una próxima versión de Traduya. " +
            "Mientras tanto, comparte tu enlace o tu código QR desde «Llamada traducida».",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}
