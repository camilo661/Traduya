package com.traduya.core.domain.audio

import com.traduya.core.model.AudioChunk
import kotlinx.coroutines.flow.Flow

/**
 * Captura el audio de la llamada desde el micrófono.
 * Graba mientras se recolecta el Flow y suelta el micrófono al cancelarlo.
 */
interface AudioCapture {
    fun chunks(): Flow<AudioChunk>
}
