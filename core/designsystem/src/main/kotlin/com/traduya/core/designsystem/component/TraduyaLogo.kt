package com.traduya.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.traduya.core.designsystem.theme.TraduyaGradients
import com.traduya.core.designsystem.theme.TraduyaTheme

/** Logo de Traduya: una "C" blanca sobre un cuadrado redondeado azul claro. */
@Composable
fun TraduyaLogo(modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val shape = RoundedCornerShape(size * 0.3f)
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = size * 0.12f, shape = shape)
            .clip(shape)
            .background(TraduyaGradients.brand),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(size * 0.24f)) {
            val strokeWidth = this.size.minDimension * 0.2f
            drawArc(
                color = Color.White,
                startAngle = 45f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                size = androidx.compose.ui.geometry.Size(
                    this.size.width - strokeWidth,
                    this.size.height - strokeWidth,
                ),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}

/** Logo junto al nombre de la app. */
@Composable
fun TraduyaWordmark(modifier: Modifier = Modifier, logoSize: Dp = 36.dp) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
        TraduyaLogo(size = logoSize)
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Traduya",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Preview
@Composable
private fun TraduyaLogoPreview() {
    TraduyaTheme { TraduyaWordmark() }
}
