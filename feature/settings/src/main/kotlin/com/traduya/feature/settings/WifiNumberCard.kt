package com.traduya.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.traduya.core.designsystem.component.SectionCard

/** Acceso al registro voluntario del número para recibir llamadas por WiFi. */
@Composable
internal fun WifiNumberCard(phoneNumber: String?, onOpen: () -> Unit) {
    SectionCard(title = "Llamadas por WiFi") {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(phoneNumber ?: "Con tu número (opcional)", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (phoneNumber != null) {
                        "Te pueden llamar por WiFi escribiendo tu número"
                    } else {
                        "Para que te llamen por WiFi sin enlace ni código"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onOpen) { Text(if (phoneNumber != null) "Ver" else "Configurar") }
        }
    }
}
