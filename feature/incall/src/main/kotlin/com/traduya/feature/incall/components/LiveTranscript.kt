package com.traduya.feature.incall.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.traduya.core.designsystem.component.TranscriptBubble
import com.traduya.core.model.TranscriptLine

private const val PARTIAL_KEY = "partial"

/** Conversación traducida en vivo; baja sola a la última frase. */
@Composable
internal fun LiveTranscript(
    lines: List<TranscriptLine>,
    partialText: String,
    translationEnabled: Boolean,
) {
    val listState = rememberLazyListState()
    val itemCount = lines.size + if (partialText.isNotBlank()) 1 else 0

    LaunchedEffect(itemCount, partialText) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    if (lines.isEmpty() && partialText.isBlank()) {
        TranscriptPlaceholder(translationEnabled)
        return
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(lines, key = { it.id }) { line ->
            TranscriptBubble(line = line, onDarkBackground = true)
        }
        if (partialText.isNotBlank()) {
            item(key = PARTIAL_KEY) {
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "$partialText…",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TranscriptPlaceholder(translationEnabled: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (translationEnabled) Icons.Rounded.GraphicEq else Icons.Rounded.PauseCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = if (translationEnabled) {
                    "Habla con normalidad.\nLas traducciones aparecerán aquí."
                } else {
                    "La traducción está en pausa.\nActívala con el botón Traducir."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
