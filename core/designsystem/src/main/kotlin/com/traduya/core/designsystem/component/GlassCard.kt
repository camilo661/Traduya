package com.traduya.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.traduya.core.designsystem.theme.BrandBlue

/** Colores del cristal según el tema: blanco translúcido en claro, azul marino translúcido en oscuro. */
object GlassDefaults {
    @Composable
    fun containerColor(): Color =
        if (isSystemInDarkTheme()) Color(0xFF1B2F4C).copy(alpha = 0.62f) else Color.White.copy(alpha = 0.66f)

    @Composable
    fun border(): BorderStroke =
        BorderStroke(1.dp, if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.9f))
}

/**
 * Tarjeta de cristal: translúcida, con borde luminoso y una sombra azul muy suave.
 * Es la base de las secciones, avisos y tarjetas de toda la app.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val glassModifier = modifier.shadow(
        elevation = 10.dp,
        shape = shape,
        ambientColor = BrandBlue.copy(alpha = 0.10f),
        spotColor = BrandBlue.copy(alpha = 0.14f),
    )
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = GlassDefaults.containerColor(),
            border = GlassDefaults.border(),
            modifier = glassModifier,
            content = content,
        )
    } else {
        Surface(
            shape = shape,
            color = GlassDefaults.containerColor(),
            border = GlassDefaults.border(),
            modifier = glassModifier,
            content = content,
        )
    }
}
