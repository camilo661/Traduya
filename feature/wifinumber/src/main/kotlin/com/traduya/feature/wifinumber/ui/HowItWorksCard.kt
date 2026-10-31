package com.traduya.feature.wifinumber.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.traduya.core.designsystem.component.SectionCard

/** Aviso claro de qué implica registrar el número, antes de que el usuario acepte. */
@Composable
internal fun HowItWorksCard() {
    SectionCard(title = "Cómo funciona") {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Point("Es opcional. Quien escriba tu número en la página de Traduya podrá llamarte por WiFi desde cualquier teléfono o computadora.")
            Point("Para recibir la llamada, abre «Llamada traducida» en Traduya.")
            Point("Antes de llamarte, la otra persona ve un aviso de que la llamada se traduce y se graba.")
            Point("Tu número solo se usa para esto. Nadie puede ver la lista de números registrados.")
            Point("Puedes borrarlo cuando quieras desde aquí.")
            Point("Todavía no se verifica por SMS: registra solo tu propio número.")
        }
    }
}

@Composable
private fun Point(text: String) {
    Row {
        Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
    }
}
