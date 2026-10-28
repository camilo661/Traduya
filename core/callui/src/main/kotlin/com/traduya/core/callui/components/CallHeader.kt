package com.traduya.core.callui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.traduya.core.designsystem.component.ContactAvatar
import com.traduya.core.designsystem.util.formatElapsed
import com.traduya.core.designsystem.util.formatPhoneNumber
import com.traduya.core.model.CallStatus
import com.traduya.core.callui.CallUiState

private val avatarBackground = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.12f)))

/**
 * Cabecera con el contacto. Grande mientras suena o marca;
 * compacta durante la llamada para dejar espacio a la traducción.
 */
@Composable
fun CallHeader(uiState: CallUiState, compact: Boolean) {
    val title = formatPhoneNumber(uiState.callerLabel)
    if (compact) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContactAvatar(label = uiState.callerLabel, size = 48.dp, background = avatarBackground)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = statusText(uiState),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ContactAvatar(label = uiState.callerLabel, size = 112.dp, background = avatarBackground)
            Spacer(Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = statusText(uiState),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

private fun statusText(uiState: CallUiState): String = when (uiState.status) {
    CallStatus.DIALING -> "Llamando…"
    CallStatus.RINGING -> "Llamada entrante"
    CallStatus.ACTIVE -> formatElapsed(uiState.elapsedSeconds)
    CallStatus.HOLDING -> "En espera"
    CallStatus.DISCONNECTED -> "Finalizada"
}
