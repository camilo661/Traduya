package com.traduya.core.model

/**
 * Un trozo de audio de la llamada (PCM 16 bits, mono, 16 kHz).
 *
 * @param level volumen de 0 a 1, para el medidor de la pantalla.
 * @param blocked el sistema entrega silencio absoluto: el micrófono está bloqueado.
 */
class AudioChunk(
    val samples: ShortArray,
    val level: Float,
    val blocked: Boolean,
) {
    companion object {
        const val SAMPLE_RATE = 16_000
    }
}
