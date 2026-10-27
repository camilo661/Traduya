package com.traduya.core.domain.translation

import com.traduya.core.model.SpeechEvent
import com.traduya.core.model.TranscriptionConfig
import kotlinx.coroutines.flow.Flow

/**
 * Convierte voz en texto de forma continua.
 * El reconocimiento corre mientras se recolecta el Flow y se detiene al cancelarlo.
 */
interface SpeechTranscriber {
    fun transcribe(config: TranscriptionConfig): Flow<SpeechEvent>
}
