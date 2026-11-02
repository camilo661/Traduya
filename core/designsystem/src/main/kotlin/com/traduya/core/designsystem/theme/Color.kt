package com.traduya.core.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Marca Traduya (tomada del logo): azul marino del nombre, azul del teléfono y celeste del globo
val Navy = Color(0xFF12284A)
val BrandBlue = Color(0xFF1F6FD1)
val SkyBlue = Color(0xFF4FB3F0)
val IceBlue = Color(0xFFEAF3FC)

// Acciones de llamada
val CallGreen = Color(0xFF22C55E)
val HangUpRed = Color(0xFFEF4444)

// Tema claro: cristal sobre azul hielo
internal val LightPrimary = BrandBlue
internal val LightOnPrimary = Color.White
internal val LightPrimaryContainer = Color(0xFFDCEAFB)
internal val LightOnPrimaryContainer = Navy
internal val LightSecondary = SkyBlue
internal val LightSecondaryContainer = Color(0xFFE3F1FC)
internal val LightOnSecondaryContainer = Navy
internal val LightBackground = Color(0xFFF2F7FC)
internal val LightSurface = Color.White
internal val LightSurfaceVariant = Color(0xFFE6EEF7)
internal val LightOnSurface = Navy
internal val LightOnSurfaceVariant = Color(0xFF55657C)
internal val LightOutline = Color(0xFFCBD8E6)

// Tema oscuro: cristal sobre azul marino
internal val DarkPrimary = Color(0xFF8CC4FF)
internal val DarkOnPrimary = Color(0xFF0A2547)
internal val DarkPrimaryContainer = Color(0xFF1B4478)
internal val DarkOnPrimaryContainer = Color(0xFFDCEAFB)
internal val DarkSecondary = SkyBlue
internal val DarkSecondaryContainer = Color(0xFF173659)
internal val DarkOnSecondaryContainer = Color(0xFFD6E9FB)
internal val DarkBackground = Color(0xFF0A1729)
internal val DarkSurface = Color(0xFF13243D)
internal val DarkSurfaceVariant = Color(0xFF1B2F4C)
internal val DarkOnSurface = Color(0xFFE6EEF8)
internal val DarkOnSurfaceVariant = Color(0xFFA6B6CB)
internal val DarkOutline = Color(0xFF2E4566)

object TraduyaGradients {
    val brand = Brush.linearGradient(listOf(SkyBlue, BrandBlue))
    val call = Brush.verticalGradient(listOf(Color(0xFF0A1D38), Color(0xFF123A6B), Color(0xFF1F6FD1)))
}
