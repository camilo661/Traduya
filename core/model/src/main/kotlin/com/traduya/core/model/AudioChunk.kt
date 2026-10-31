package com.traduya.core.model

/**
 * Un trozo de audio de la llamada (PCM 16 bits, mono, 16 kHz).
 *
 * @param samples voz de la otra persona: es lo que se traduce.
 * @param level volumen de 0 a 1, para el medidor de la pantalla.
 * @param blocked el sistema entrega silencio absoluto: el micrófono está bloqueado.
 * @param callMix la llamada completa (ambas personas) para la grabación; null si es igual a [samples].
 */
class AudioChunk(
    val samples: ShortArray,
    val level: Float,
    val blocked: Boolean,
    val callMix: ShortArray? = null,
) {
    companion object {
        const val SAMPLE_RATE = 16_000
        private const val LEVEL_GAIN = 6.0

        /** Volumen RMS normalizado y ampliado para que la voz normal llene el medidor. */
        /** Suma dos señales sin desbordar, para grabar ambos lados de la llamada. */
        fun mix(a: ShortArray, b: ShortArray): ShortArray = ShortArray(minOf(a.size, b.size)) { i ->
            (a[i] + b[i]).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        fun levelOf(samples: ShortArray): Float {
            if (samples.isEmpty()) return 0f
            var sum = 0.0
            for (sample in samples) sum += sample * sample.toDouble()
            val rms = kotlin.math.sqrt(sum / samples.size) / Short.MAX_VALUE
            return (rms * LEVEL_GAIN).toFloat().coerceIn(0f, 1f)
        }
    }
}
