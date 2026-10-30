package com.traduya.core.callcapture.shell

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class UplinkGateTest {

    private fun tone(amplitude: Double) = ShortArray(1600) { i ->
        (sin(2 * PI * 220 * i / 16_000) * amplitude * Short.MAX_VALUE).toInt().toShort()
    }

    private val silence = ShortArray(1600)

    @Test
    fun `lets the other person through while the user is silent`() {
        val gate = UplinkGate()
        val mix = tone(0.3)

        val muted = gate.apply(mix, mix.size, silence, silence.size)

        assertFalse(muted)
        assertTrue(mix.any { it != 0.toShort() })
    }

    @Test
    fun `mutes the mix while the user talks`() {
        val gate = UplinkGate()
        val mix = tone(0.3)

        val muted = gate.apply(mix, mix.size, tone(0.3), 1600)

        assertTrue(muted)
        assertTrue(mix.all { it == 0.toShort() })
    }

    @Test
    fun `keeps muting briefly after the user stops`() {
        val gate = UplinkGate(holdChunks = 2)
        gate.apply(tone(0.3), 1600, tone(0.3), 1600)

        assertTrue(gate.apply(tone(0.3), 1600, silence, 1600))
        assertFalse(gate.apply(tone(0.3), 1600, silence, 1600))
    }

    @Test
    fun `ignores faint noise on the user line`() {
        val gate = UplinkGate()

        assertFalse(gate.apply(tone(0.3), 1600, tone(0.005), 1600))
    }
}
