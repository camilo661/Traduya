package com.traduya.feature.history.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.traduya.core.designsystem.component.ContactAvatar
import com.traduya.core.designsystem.component.EmptyState
import com.traduya.core.designsystem.component.LanguageBadge
import com.traduya.core.designsystem.component.TranscriptBubble
import com.traduya.core.designsystem.util.formatCallDate
import com.traduya.core.designsystem.util.formatDuration
import com.traduya.core.designsystem.util.formatPhoneNumber
import com.traduya.core.model.CallDirection
import com.traduya.core.model.CallRecord

@Composable
fun HistoryDetailRoute(
    onBack: () -> Unit,
    viewModel: HistoryDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryDetailScreen(uiState = uiState, onBack = onBack, onDelete = { viewModel.delete(onBack) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryDetailScreen(
    uiState: HistoryDetailUiState,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Detalle de la llamada") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                }
            },
            actions = {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Eliminar llamada")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )

        val call = uiState.call
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            call == null -> Unit

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { CallSummary(call) }
                if (uiState.lines.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Rounded.Translate,
                            title = "Sin traducciones",
                            message = "En esta llamada no se tradujo ninguna frase.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    items(uiState.lines, key = { it.id }) { TranscriptBubble(it) }
                }
            }
        }
    }
}

@Composable
private fun CallSummary(call: CallRecord) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ContactAvatar(label = call.number, size = 64.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = formatPhoneNumber(call.number).ifBlank { "Número privado" },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = buildString {
                    append(if (call.direction == CallDirection.INCOMING) "Entrante" else "Saliente")
                    append(" · ")
                    append(formatCallDate(call.startedAtMillis))
                    append(" · ")
                    append(formatDuration(call.durationSeconds))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
            call.detectedLanguage?.let {
                Row(Modifier.padding(top = 10.dp)) {
                    LanguageBadge(it, containerColor = MaterialTheme.colorScheme.surface)
                }
            }
        }
    }
}
