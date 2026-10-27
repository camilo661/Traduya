package com.traduya.feature.incall.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.traduya.core.designsystem.theme.CallGreen
import com.traduya.core.model.TranslationStatus
import com.traduya.feature.incall.InCallUiState

/** Indicador superior: "Traduciendo · Inglés detectado", con un punto que late. */
@Composable
internal fun TranslationStatusPill(uiState: InCallUiState) {
    val (text, dotColor) = when {
        !uiState.isActive -> "Traduya" to Color.White.copy(alpha = 0.6f)
        !uiState.translationEnabled -> "Traducción en pausa" to Color.White.copy(alpha = 0.6f)
        uiState.translationStatus == TranslationStatus.ERROR ->
            (uiState.translationError ?: "Error de traducción") to Color(0xFFFFB4AB)

        uiState.translationStatus == TranslationStatus.TRANSLATING -> "Traduciendo…" to CallGreen
        uiState.detectedLanguage != null ->
            "Escuchando · ${uiState.detectedLanguage.flag} ${uiState.detectedLanguage.displayName}" to CallGreen

        else -> "Escuchando · detectando idioma" to CallGreen
    }
    val pulsing = uiState.isActive && uiState.translationEnabled &&
        uiState.translationStatus != TranslationStatus.ERROR

    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulseAlpha",
    )

    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.14f)) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .alpha(if (pulsing) pulse else 1f)
                    .background(dotColor, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}
