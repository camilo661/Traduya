package com.traduya.feature.incall.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.traduya.core.designsystem.component.CallActionButton
import com.traduya.core.designsystem.theme.CallGreen
import com.traduya.core.designsystem.theme.HangUpRed
import com.traduya.core.designsystem.theme.SkyBlueDeep
import com.traduya.feature.incall.InCallUiState

private val activeContainer = Color.White
private val inactiveContainer = Color.White.copy(alpha = 0.16f)

@Composable
internal fun CallControls(
    uiState: InCallUiState,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleTranslation: () -> Unit,
    onShowKeypad: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ToggleButton(
            checked = uiState.isMuted,
            icon = if (uiState.isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
            label = "Silenciar",
            onClick = onToggleMute,
        )
        ToggleButton(
            checked = uiState.isSpeakerOn,
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            label = "Altavoz",
            onClick = onToggleSpeaker,
        )
        ToggleButton(
            checked = uiState.translationEnabled,
            icon = Icons.Rounded.Translate,
            label = "Traducir",
            onClick = onToggleTranslation,
        )
        ToggleButton(
            checked = false,
            icon = Icons.Rounded.Dialpad,
            label = "Teclado",
            onClick = onShowKeypad,
        )
    }
}

@Composable
private fun ToggleButton(
    checked: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    CallActionButton(
        icon = icon,
        label = label,
        onClick = onClick,
        containerColor = if (checked) activeContainer else inactiveContainer,
        contentColor = if (checked) SkyBlueDeep else Color.White,
    )
}

@Composable
internal fun IncomingCallActions(onAnswer: () -> Unit, onReject: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 48.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        CallActionButton(
            icon = Icons.Rounded.CallEnd,
            label = "Rechazar",
            onClick = onReject,
            containerColor = HangUpRed,
            size = 76.dp,
        )
        CallActionButton(
            icon = Icons.Rounded.Call,
            label = "Contestar",
            onClick = onAnswer,
            containerColor = CallGreen,
            size = 76.dp,
        )
    }
}
