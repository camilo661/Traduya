package com.traduya.core.callui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.traduya.core.designsystem.component.CallActionButton
import com.traduya.core.designsystem.component.DialPad
import com.traduya.core.designsystem.theme.HangUpRed
import com.traduya.core.designsystem.theme.TraduyaGradients
import com.traduya.core.callui.components.CallControls
import com.traduya.core.callui.components.CallHeader
import com.traduya.core.callui.components.IncomingCallActions
import com.traduya.core.callui.components.KaraokeTranscript
import com.traduya.core.callui.components.MicBlockedBanner
import com.traduya.core.callui.components.TranslationStatusPill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallScreen(
    uiState: CallUiState,
    onAnswer: () -> Unit,
    onHangUp: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleTranslation: () -> Unit,
    onDtmf: (Char) -> Unit,
    showKeypad: Boolean = true,
) {
    var keypadOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TraduyaGradients.call),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TranslationStatusPill(uiState)

            AnimatedContent(targetState = uiState.isActive, label = "header") { compact ->
                CallHeader(uiState = uiState, compact = compact)
            }

            if (uiState.isActive && uiState.micBlocked) MicBlockedBanner()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            ) {
                if (uiState.isActive || uiState.lines.isNotEmpty()) {
                    KaraokeTranscript(
                        lines = uiState.lines,
                        liveText = uiState.liveText,
                        translationEnabled = uiState.translationEnabled,
                    )
                }
            }

            when {
                uiState.hasEnded -> Text(
                    text = "Llamada finalizada",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 48.dp),
                )

                uiState.isRinging -> IncomingCallActions(onAnswer = onAnswer, onReject = onHangUp)

                else -> {
                    CallControls(
                        uiState = uiState,
                        onToggleMute = onToggleMute,
                        onToggleSpeaker = onToggleSpeaker,
                        onToggleTranslation = onToggleTranslation,
                        onShowKeypad = if (showKeypad) ({ keypadOpen = true }) else null,
                    )
                    Spacer(Modifier.height(24.dp))
                    CallActionButton(
                        icon = Icons.Rounded.CallEnd,
                        label = null,
                        onClick = onHangUp,
                        containerColor = HangUpRed,
                        size = 72.dp,
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    if (keypadOpen) {
        ModalBottomSheet(onDismissRequest = { keypadOpen = false }) {
            Column(
                modifier = Modifier.padding(start = 32.dp, end = 32.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text("Teclado", style = MaterialTheme.typography.titleMedium)
                }
                DialPad(onKey = onDtmf)
            }
        }
    }
}
