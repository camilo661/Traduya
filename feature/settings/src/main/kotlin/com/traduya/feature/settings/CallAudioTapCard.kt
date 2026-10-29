package com.traduya.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.traduya.core.designsystem.component.SectionCard
import com.traduya.core.model.CallAudioTapStatus

/**
 * Guía para activar Shizuku, que permite a Traduya escuchar las llamadas normales.
 * Cada estado muestra solo el siguiente paso.
 */
@Composable
internal fun CallAudioTapCard(
    status: CallAudioTapStatus,
    onInstall: () -> Unit,
    onOpenShizuku: () -> Unit,
    onAllow: () -> Unit,
) {
    SectionCard(title = "Traducir llamadas normales") {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (status == CallAudioTapStatus.READY) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Activo · Traduya escucha la llamada directamente, sin altavoz",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
                Hint("Si reinicias el teléfono, abre Shizuku y vuelve a pulsar «Iniciar».")
                return@Column
            }

            Hint(
                "Android no deja que las apps oigan las llamadas normales. Con la app gratuita " +
                    "Shizuku, Traduya puede escuchar la línea y traducir en tiempo real.",
            )
            Step(1, "Instala Shizuku", done = status != CallAudioTapStatus.NOT_INSTALLED)
            Step(
                2,
                "En Shizuku pulsa «Iniciar» con depuración inalámbrica (Ajustes › Opciones de " +
                    "desarrollador › Depuración inalámbrica)",
                done = status == CallAudioTapStatus.PERMISSION_NEEDED,
            )
            Step(3, "Permite que Traduya use Shizuku", done = false)

            when (status) {
                CallAudioTapStatus.NOT_INSTALLED -> ActionButton("Instalar Shizuku", onInstall)
                CallAudioTapStatus.NOT_RUNNING -> ActionButton("Abrir Shizuku", onOpenShizuku)
                CallAudioTapStatus.PERMISSION_NEEDED -> ActionButton("Permitir", onAllow)
                CallAudioTapStatus.READY -> Unit
            }
        }
    }
}

@Composable
private fun Step(number: Int, text: String, done: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = if (done) "✓" else "$number.",
            style = MaterialTheme.typography.bodyLarge,
            color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
}
