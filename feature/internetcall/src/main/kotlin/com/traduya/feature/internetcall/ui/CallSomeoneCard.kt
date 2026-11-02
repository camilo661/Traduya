package com.traduya.feature.internetcall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.traduya.core.designsystem.theme.CallGreen
import com.traduya.core.designsystem.theme.HangUpRed
import com.traduya.feature.internetcall.TargetStatus

/**
 * Llamar por internet a otro número con Traduya (en la app o en la página web).
 * La luz indica si ese número está activo ahora: verde activo, roja desconectado.
 */
@Composable
internal fun CallSomeoneCard(
    canCall: Boolean,
    number: String,
    status: TargetStatus,
    notice: String?,
    onNumberChange: (String) -> Unit,
    onCall: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.White.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Llamar a alguien con Traduya", style = MaterialTheme.typography.titleSmall, color = Color.White)
            if (!canCall) {
                Text(
                    text = "Registra tu número en Ajustes › Llamadas por WiFi para llamar por internet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                return@Column
            }
            OutlinedTextField(
                value = number,
                onValueChange = onNumberChange,
                singleLine = true,
                label = { Text("Número con código del país") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    cursorColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            StatusLight(status)
            notice?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
            Spacer(Modifier.height(2.dp))
            Button(
                onClick = onCall,
                enabled = status == TargetStatus.ACTIVE || status == TargetStatus.INACTIVE,
                colors = ButtonDefaults.buttonColors(containerColor = CallGreen, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Llamar por internet")
            }
        }
    }
}

@Composable
private fun StatusLight(status: TargetStatus) {
    val (color, text) = when (status) {
        TargetStatus.ACTIVE -> CallGreen to "Activo · puede contestar ahora"
        TargetStatus.INACTIVE -> HangUpRed to "Desconectado · no tiene Traduya abierto"
        TargetStatus.NOT_FOUND -> Color.White.copy(alpha = 0.5f) to "Este número no está en Traduya"
        TargetStatus.INVALID -> Color.White.copy(alpha = 0.5f) to "Escribe el número con el código del país"
        TargetStatus.CHECKING, TargetStatus.NONE -> return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}
