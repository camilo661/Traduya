package com.traduya.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.traduya.core.designsystem.theme.BrandBlue
import com.traduya.core.designsystem.theme.SkyBlue

/**
 * Fondo de la app: degradado azul hielo con dos luces suaves.
 * Sobre él, las tarjetas de cristal ([GlassCard]) dejan ver el color.
 */
@Composable
fun TraduyaBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val dark = isSystemInDarkTheme()
    val base = if (dark) {
        Brush.verticalGradient(listOf(Color(0xFF0A1729), Color(0xFF0E2340)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFF7FAFE), Color(0xFFE6F0FA)))
    }
    val glow = if (dark) 0.22f else 0.20f
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(base)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(SkyBlue.copy(alpha = glow), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.08f),
                        radius = size.width * 0.75f,
                    ),
                    radius = size.width * 0.75f,
                    center = Offset(size.width * 0.9f, size.height * 0.08f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(BrandBlue.copy(alpha = glow * 0.7f), Color.Transparent),
                        center = Offset(size.width * 0.05f, size.height * 0.75f),
                        radius = size.width * 0.8f,
                    ),
                    radius = size.width * 0.8f,
                    center = Offset(size.width * 0.05f, size.height * 0.75f),
                )
            },
        content = content,
    )
}
