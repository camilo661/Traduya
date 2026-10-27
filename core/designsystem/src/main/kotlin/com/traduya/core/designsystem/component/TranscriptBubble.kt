package com.traduya.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.traduya.core.designsystem.theme.TraduyaTheme
import com.traduya.core.model.Language
import com.traduya.core.model.Speaker
import com.traduya.core.model.TranscriptLine

/**
 * Una frase traducida estilo chat:
 * la traducción va grande y el texto original pequeño debajo.
 * Lo que dice la otra persona va a la izquierda; lo que dice el usuario, a la derecha.
 */
@Composable
fun TranscriptBubble(
    line: TranscriptLine,
    modifier: Modifier = Modifier,
    onDarkBackground: Boolean = false,
) {
    val isMine = line.speaker == Speaker.ME
    val container = when {
        onDarkBackground && isMine -> Color.White.copy(alpha = 0.22f)
        onDarkBackground -> Color.White
        isMine -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }
    val content = when {
        onDarkBackground && isMine -> Color.White
        onDarkBackground -> Color(0xFF0F1D28)
        isMine -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val shape = if (isMine) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
    } else {
        RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = container,
            contentColor = content,
            shape = shape,
            shadowElevation = if (isMine) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = speakerLabel(line),
                    style = MaterialTheme.typography.labelSmall,
                    color = content.copy(alpha = 0.7f),
                )
                Text(
                    text = line.translatedText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = content.copy(alpha = 0.15f),
                )
                Text(
                    text = line.originalText,
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.75f),
                )
            }
        }
    }
}

private fun speakerLabel(line: TranscriptLine): String {
    val who = if (line.speaker == Speaker.ME) "Tú" else "Contacto"
    return "$who · ${line.sourceLanguage.flag} ${line.sourceLanguage.displayName} → " +
        "${line.targetLanguage.flag} ${line.targetLanguage.displayName}"
}

@Preview
@Composable
private fun TranscriptBubblePreview() {
    TraduyaTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TranscriptBubble(
                TranscriptLine(
                    callId = 1, timestampMillis = 0, speaker = Speaker.THEM,
                    sourceLanguage = Language.ENGLISH, targetLanguage = Language.SPANISH,
                    originalText = "Hi, how can I help you today?",
                    translatedText = "Hola, ¿en qué puedo ayudarte hoy?",
                ),
            )
            TranscriptBubble(
                TranscriptLine(
                    callId = 1, timestampMillis = 0, speaker = Speaker.ME,
                    sourceLanguage = Language.SPANISH, targetLanguage = Language.ENGLISH,
                    originalText = "Quiero cambiar mi reserva",
                    translatedText = "I want to change my booking",
                ),
            )
        }
    }
}
