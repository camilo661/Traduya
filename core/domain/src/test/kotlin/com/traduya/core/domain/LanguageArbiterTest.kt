package com.traduya.core.domain

import com.traduya.core.domain.usecase.Hypothesis
import com.traduya.core.domain.usecase.LanguageArbiter
import com.traduya.core.model.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageArbiterTest {

    private fun en(conf: Float, words: Int = 4) = Hypothesis(Language.ENGLISH, "hello there my friend", conf, words)
    private fun zh(conf: Float, words: Int = 4) = Hypothesis(Language.CHINESE, "你好朋友", conf, words)
    private fun es(conf: Float, words: Int = 4) = Hypothesis(Language.SPANISH, "hola amigo", conf, words)

    @Test
    fun `picks the most confident language`() {
        val arbiter = LanguageArbiter(listOf(Language.ENGLISH, Language.CHINESE))

        val result = arbiter.decide(listOf(en(0.55f), zh(0.92f)))

        assertEquals(Language.CHINESE, result?.language)
        assertEquals(Language.CHINESE, arbiter.leader)
    }

    @Test
    fun `discards low confidence noise`() {
        val arbiter = LanguageArbiter(listOf(Language.ENGLISH, Language.CHINESE))

        assertNull(arbiter.decide(listOf(en(0.3f), zh(0.2f))))
    }

    @Test
    fun `locks after three wins in a row`() {
        val arbiter = LanguageArbiter(listOf(Language.ENGLISH, Language.CHINESE))

        repeat(2) { arbiter.decide(listOf(en(0.9f), zh(0.4f))) }
        assertFalse(arbiter.isLocked)
        arbiter.decide(listOf(en(0.9f), zh(0.4f)))

        assertTrue(arbiter.isLocked)
        assertEquals(Language.ENGLISH, arbiter.leader)
    }

    @Test
    fun `spanish wins are returned but never become the leader`() {
        val arbiter = LanguageArbiter(listOf(Language.ENGLISH, Language.CHINESE))

        val result = arbiter.decide(listOf(en(0.5f), es(0.95f)))

        assertEquals(Language.SPANISH, result?.language)
        assertEquals(Language.ENGLISH, arbiter.leader)
    }

    @Test
    fun `a single confident word loses to a full sentence`() {
        val arbiter = LanguageArbiter(listOf(Language.ENGLISH, Language.CHINESE))

        val result = arbiter.decide(listOf(en(1f, words = 1), zh(0.8f, words = 6)))

        assertEquals(Language.CHINESE, result?.language)
    }

    @Test
    fun `fixed language starts locked`() {
        assertTrue(LanguageArbiter(listOf(Language.ENGLISH)).isLocked)
    }
}
