package com.traduya.feature.internetcall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.traduya.core.designsystem.theme.CallGreen
import com.traduya.core.designsystem.theme.TraduyaGradients
import com.traduya.feature.internetcall.peer.PeerState

/** Esperando llamadas: enlace, QR y estado de la conexión. */
@Composable
internal fun WaitingScreen(
    state: PeerState,
    code: String,
    link: String,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(TraduyaGradients.call),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Text(
                    text = "Llamada traducida por internet",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }

            Spacer(Modifier.height(16.dp))
            StatusPill(state)
            Spacer(Modifier.height(24.dp))

            Surface(shape = MaterialTheme.shapes.large, color = Color.White) {
                QrCode(content = link, modifier = Modifier.padding(16.dp).size(220.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Tu código: $code",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = link,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onShare) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Compartir enlace")
                }
                FilledTonalButton(onClick = onCopy) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copiar")
                }
            }

            Spacer(Modifier.height(28.dp))
            HowItWorks()
        }
    }
}

@Composable
private fun StatusPill(state: PeerState) {
    val (text, ready) = when (state) {
        PeerState.Ready -> "Lista para recibir llamadas" to true
        is PeerState.Failed -> state.reason to false
        else -> "Conectando…" to false
    }
    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.14f)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (ready) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(CallGreen, CircleShape),
                )
            } else {
                CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
            }
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}

@Composable
private fun HowItWorks() {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.White.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Cómo funciona", style = MaterialTheme.typography.titleSmall, color = Color.White)
            listOf(
                "Envía el enlace a la otra persona o que escanee el código QR.",
                "Lo abre en su navegador (iPhone, Android o computadora) y toca Call. No instala nada.",
                "Contestas aquí y ves en español, al instante, todo lo que te dice.",
            ).forEachIndexed { index, step ->
                Row {
                    Text("${index + 1}.", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(step, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                }
            }
            Text(
                text = "Mantén esta pantalla abierta mientras esperas la llamada.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}
