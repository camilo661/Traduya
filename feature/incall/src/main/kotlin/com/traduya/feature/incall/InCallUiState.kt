package com.traduya.feature.incall

import com.traduya.core.model.CallStatus
import com.traduya.core.model.Language
import com.traduya.core.model.TranscriptLine
import com.traduya.core.model.TranslationStatus

data class InCallUiState(
    val callerLabel: String = "",
    val number: String = "",
    val status: CallStatus = CallStatus.DIALING,
    val elapsedSeconds: Long = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val translationEnabled: Boolean = true,
    val translationStatus: TranslationStatus = TranslationStatus.IDLE,
    val translationError: String? = null,
    val liveText: String = "",
    val audioLevel: Float = 0f,
    val micBlocked: Boolean = false,
    val isRecording: Boolean = false,
    val detectedLanguage: Language? = null,
    val lines: List<TranscriptLine> = emptyList(),
    val hasEnded: Boolean = false,
) {
    val isRinging: Boolean get() = status == CallStatus.RINGING
    val isActive: Boolean get() = status == CallStatus.ACTIVE
}
