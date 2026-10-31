package com.traduya.feature.wifinumber.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.traduya.feature.wifinumber.WifiNumberUiState

@Composable
internal fun RegisterNumberForm(
    uiState: WifiNumberUiState,
    onInputChange: (String) -> Unit,
    onConsentChange: (Boolean) -> Unit,
    onRegister: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = uiState.input,
            onValueChange = onInputChange,
            label = { Text("Tu número de teléfono") },
            supportingText = { Text(uiState.errorMessage ?: "Con el código del país, por ejemplo +57 300 123 4567") },
            isError = uiState.errorMessage != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.toggleable(
                value = uiState.consentGiven,
                role = Role.Checkbox,
                onValueChange = onConsentChange,
            ),
        ) {
            Checkbox(checked = uiState.consentGiven, onCheckedChange = null)
            Text(
                text = "Acepto que mi número se use para recibir llamadas por WiFi a través de Traduya.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Button(
            onClick = onRegister,
            enabled = uiState.canRegister,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (uiState.isWorking) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Text("Registrar mi número")
            }
        }
        Text(
            text = "También puedes seguir usando tu enlace o tu código QR sin registrar el número.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
