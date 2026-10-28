package com.traduya.feature.incall.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Aviso cuando el teléfono no deja que Traduya escuche durante la llamada. */
@Composable
internal fun MicBlockedBanner() {
    Surface(
        color = Color(0xFFB91C1C).copy(alpha = 0.9f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.MicOff, contentDescription = null, tint = Color.White)
            Text(
                text = "Android está bloqueando el micrófono de Traduya en esta llamada, " +
                    "así que no puede oír a la otra persona ni traducirla.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
