package com.traduya.core.callcapture.shell

import kotlin.math.sqrt

/**
 * Deja pasar solo la voz de la otra persona cuando la fuente mezcla ambos lados de la llamada.
 *
 * Compara la mezcla con lo que sale del micrófono del usuario hacia la red (VOICE_UPLINK):
 * si el usuario está hablando, ese trozo se silencia, y se mantiene en silencio un instante
 * más para no dejar pasar el final de sus palabras.
 */
internal class UplinkGate(
    private val minUserLevel: Double = 0.015,
    private val userShareOfMix: Double = 0.6,
    private val holdChunks: Int = 3,
) {
    private var hold = 0

    /** Silencia [mix] en el sitio si el usuario habla en [uplink]. Devuelve true si se silenció. */
    fun apply(mix: ShortArray, mixCount: Int, uplink: ShortArray, uplinkCount: Int): Boolean {
        val userLevel = rms(uplink, uplinkCount)
        val userIsTalking = userLevel > minUserLevel && userLevel >= rms(mix, mixCount) * userShareOfMix
        if (userIsTalking) hold = holdChunks
        if (hold == 0) return false
        hold--
        mix.fill(0, 0, mixCount)
        return true
    }

    private fun rms(samples: ShortArray, count: Int): Double {
        if (count <= 0) return 0.0
        var sum = 0.0
        for (i in 0 until count) sum += samples[i] * samples[i].toDouble()
        return sqrt(sum / count) / Short.MAX_VALUE
    }
}
