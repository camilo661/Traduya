package com.traduya.core.callui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.traduya.core.designsystem.theme.CallGreen
import com.traduya.core.designsystem.theme.HangUpRed
import com.traduya.core.model.TranslationStatus
import com.traduya.core.callui.CallUiState

/** Barra superior: estado de la traducción, medidor de voz y aviso de grabación. */
@Composable
fun TranslationStatusPill(uiState: CallUiState) {
    val text = when {
        !uiState.isActive -> "Traduya"
        !uiState.translationEnabled -> "Traducción en pausa"
        uiState.translationStatus == TranslationStatus.ERROR -> uiState.translationError ?: "Error de traducción"
        uiState.translationStatus == TranslationStatus.PREPARING -> "Preparando IA…"
        uiState.detectedLanguage != null ->
            "${uiState.detectedLanguage.flag} ${uiState.detectedLanguage.displayName} → 🇪🇸 Español"

        else -> "Detectando idioma…"
    }
    val listening = uiState.isActive && uiState.translationEnabled &&
        uiState.translationStatus != TranslationStatus.ERROR

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.14f),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (listening) {
                    VoiceMeter(uiState.audioLevel)
                } else {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(Color.White.copy(alpha = 0.6f), CircleShape),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (uiState.isActive && uiState.isRecording) RecordingBadge()
    }
}

/** Cinco barras que suben con la voz: confirma que Traduya está oyendo la llamada. */
@Composable
private fun VoiceMeter(level: Float) {
    val animated by animateFloatAsState(level, animationSpec = tween(120), label = "level")
    val weights = listOf(0.5f, 0.8f, 1f, 0.8f, 0.5f)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        weights.forEach { weight ->
            Box(
                Modifier
                    .width(3.dp)
                    .height((4 + 12 * animated * weight).dp)
                    .background(CallGreen, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun RecordingBadge() {
    val pulse by rememberInfiniteTransition(label = "rec").animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "recAlpha",
    )
    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.14f)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .alpha(pulse)
                    .background(HangUpRed, CircleShape),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "REC",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
