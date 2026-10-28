package com.traduya.feature.incall.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traduya.core.designsystem.theme.SkyBlue
import com.traduya.core.model.TranscriptLine

private const val LIVE_KEY = "live"
private const val HIGHLIGHTED_WORDS = 2

/**
 * Traducción en vivo estilo karaoke, solo en español:
 * la frase que se está diciendo va abajo en letra grande y sus palabras más nuevas
 * se iluminan en azul; las frases anteriores suben y se atenúan.
 */
@Composable
internal fun KaraokeTranscript(
    lines: List<TranscriptLine>,
    liveText: String,
    translationEnabled: Boolean,
) {
    val listState = rememberLazyListState()

    // Con la lista invertida, el elemento 0 es el más reciente: así siempre se ve lo último
    LaunchedEffect(lines.size, liveText.isNotBlank()) {
        if (listState.firstVisibleItemIndex <= 1) listState.animateScrollToItem(0)
    }

    if (lines.isEmpty() && liveText.isBlank()) {
        KaraokePlaceholder(translationEnabled)
        return
    }

    val recentFirst = remember(lines) { lines.asReversed() }
    LazyColumn(
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (liveText.isNotBlank()) {
            item(key = LIVE_KEY) { LiveLine(liveText) }
        }
        itemsIndexed(recentFirst, key = { _, line -> line.id }) { index, line ->
            val isNewest = index == 0 && liveText.isBlank()
            Text(
                text = line.translatedText,
                color = Color.White.copy(alpha = if (isNewest) 1f else 0.55f),
                fontSize = if (isNewest) 26.sp else 20.sp,
                lineHeight = if (isNewest) 34.sp else 27.sp,
                fontWeight = if (isNewest) FontWeight.SemiBold else FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(),
            )
        }
    }
}

/** Frase en curso: blanca, con las últimas palabras en azul y un cursor que parpadea. */
@Composable
private fun LiveLine(text: String) {
    val cursor by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "cursorAlpha",
    )
    val words = text.split(" ").filter { it.isNotEmpty() }
    val splitAt = (words.size - HIGHLIGHTED_WORDS).coerceAtLeast(0)

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.White)) { append(words.take(splitAt).joinToString(" ")) }
            if (splitAt > 0) append(" ")
            withStyle(SpanStyle(color = SkyBlue)) { append(words.drop(splitAt).joinToString(" ")) }
            withStyle(SpanStyle(color = SkyBlue.copy(alpha = cursor))) { append(" ▍") }
        },
        fontSize = 32.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun KaraokePlaceholder(translationEnabled: Boolean) {
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
                    "Cuando la otra persona hable,\naquí verás la traducción en español."
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
