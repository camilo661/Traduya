package com.traduya.feature.history.detail

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.traduya.core.designsystem.util.formatElapsed
import kotlinx.coroutines.delay
import java.io.File

/** Reproductor sencillo de la grabación de la llamada. */
@Composable
internal fun RecordingPlayer(path: String) {
    val player = remember(path) {
        runCatching {
            MediaPlayer().apply {
                setDataSource(path)
                prepare()
            }
        }.getOrNull()
    }
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableIntStateOf(0) }
    val duration = player?.duration ?: 0

    DisposableEffect(player) {
        player?.setOnCompletionListener {
            isPlaying = false
            position = 0
            it.seekTo(0)
        }
        onDispose { player?.release() }
    }
    LaunchedEffect(isPlaying) {
        while (isPlaying && player != null) {
            position = player.currentPosition
            delay(250)
        }
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (player == null || !File(path).exists()) {
            Text(
                text = "La grabación no está disponible",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
            return@Surface
        }
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = {
                    if (isPlaying) player.pause() else player.start()
                    isPlaying = !isPlaying
                },
                shape = CircleShape,
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text("Grabación de la llamada", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = if (duration > 0) position / duration.toFloat() else 0f,
                    onValueChange = {
                        position = (it * duration).toInt()
                        player.seekTo(position)
                    },
                )
                Text(
                    text = "${formatElapsed(position / 1000L)} / ${formatElapsed(duration / 1000L)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
