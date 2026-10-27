package com.traduya.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traduya.core.designsystem.theme.TraduyaGradients

/** Círculo con la inicial del contacto, o un icono si es un número. */
@Composable
fun ContactAvatar(
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    background: Brush = TraduyaGradients.brand,
) {
    val initial = label.firstOrNull { it.isLetter() }?.uppercaseChar()
    Box(
        modifier = modifier
            .size(size)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (initial != null) {
            Text(
                text = initial.toString(),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.42f).sp,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}
