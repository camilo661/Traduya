package com.traduya.feature.incall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.traduya.core.callui.CallScreen
import kotlinx.coroutines.delay

private const val CLOSE_DELAY_MILLIS = 1_500L

@Composable
fun InCallRoute(
    onFinished: () -> Unit,
    viewModel: InCallViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.hasEnded) {
        if (uiState.hasEnded) {
            delay(CLOSE_DELAY_MILLIS)
            onFinished()
        }
    }

    CallScreen(
        uiState = uiState,
        onAnswer = viewModel::answer,
        onHangUp = viewModel::hangUp,
        onToggleMute = viewModel::toggleMute,
        onToggleSpeaker = viewModel::toggleSpeaker,
        onToggleTranslation = viewModel::toggleTranslation,
        onDtmf = viewModel::sendDtmf,
    )
}
