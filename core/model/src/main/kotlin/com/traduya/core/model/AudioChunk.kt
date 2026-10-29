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
        private const val LEVEL_GAIN = 6.0

        /** Volumen RMS normalizado y ampliado para que la voz normal llene el medidor. */
        fun levelOf(samples: ShortArray): Float {
            if (samples.isEmpty()) return 0f
            var sum = 0.0
            for (sample in samples) sum += sample * sample.toDouble()
            val rms = kotlin.math.sqrt(sum / samples.size) / Short.MAX_VALUE
            return (rms * LEVEL_GAIN).toFloat().coerceIn(0f, 1f)
        }
    }
}
