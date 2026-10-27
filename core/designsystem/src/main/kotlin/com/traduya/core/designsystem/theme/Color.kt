package com.traduya.core.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Marca Traduya: azul claro
val SkyBlue = Color(0xFF4FC3F7)
val SkyBlueBright = Color(0xFF29B6F6)
val SkyBlueDeep = Color(0xFF0288D1)
val Navy = Color(0xFF0B3954)

// Acciones de llamada
val CallGreen = Color(0xFF22C55E)
val HangUpRed = Color(0xFFEF4444)

// Tema claro
internal val LightPrimary = SkyBlueDeep
internal val LightOnPrimary = Color.White
internal val LightPrimaryContainer = Color(0xFFD9F1FF)
internal val LightOnPrimaryContainer = Color(0xFF00344F)
internal val LightSecondary = SkyBlueBright
internal val LightSecondaryContainer = Color(0xFFE6F6FE)
internal val LightOnSecondaryContainer = Color(0xFF003549)
internal val LightBackground = Color(0xFFF4F9FD)
internal val LightSurface = Color.White
internal val LightSurfaceVariant = Color(0xFFE7F1F8)
internal val LightOnSurface = Color(0xFF0F1D28)
internal val LightOnSurfaceVariant = Color(0xFF52626F)
internal val LightOutline = Color(0xFFC6D5E0)

// Tema oscuro
internal val DarkPrimary = Color(0xFF8AD6FF)
internal val DarkOnPrimary = Color(0xFF00344F)
internal val DarkPrimaryContainer = Color(0xFF004B70)
internal val DarkOnPrimaryContainer = Color(0xFFD9F1FF)
internal val DarkSecondary = SkyBlue
internal val DarkSecondaryContainer = Color(0xFF16384C)
internal val DarkOnSecondaryContainer = Color(0xFFD3EFFF)
internal val DarkBackground = Color(0xFF0A141C)
internal val DarkSurface = Color(0xFF101C26)
internal val DarkSurfaceVariant = Color(0xFF1B2A36)
internal val DarkOnSurface = Color(0xFFE3EEF6)
internal val DarkOnSurfaceVariant = Color(0xFFA3B4C1)
internal val DarkOutline = Color(0xFF34495A)

object TraduyaGradients {
    val brand = Brush.linearGradient(listOf(SkyBlue, SkyBlueDeep))
    val call = Brush.verticalGradient(listOf(Color(0xFF0C4A6E), Color(0xFF0369A1), Color(0xFF0EA5E9)))
}
