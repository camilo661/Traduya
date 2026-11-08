package com.traduya.core.domain

import com.traduya.core.domain.translation.TranslationEngine
import com.traduya.core.domain.usecase.TranslateUtteranceUseCase
import com.traduya.core.model.Language
import com.traduya.core.model.Speaker
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslateUtteranceUseCaseTest {

    private val fakeEngine = object : TranslationEngine {
        override suspend fun translate(text: String, from: Language, to: Language) = "[${from.code}->${to.code}] $text"
    }

    @Test
    fun `english is translated to spanish`() = runTest {
        val result = TranslateUtteranceUseCase(fakeEngine)("Hello", Language.ENGLISH, Language.SPANISH)

        assertEquals(Speaker.THEM, result.speaker)
        assertEquals(Language.SPANISH, result.targetLanguage)
        assertEquals("[en->es] Hello", result.translatedText)
    }

    @Test
    fun `chinese is translated to spanish`() = runTest {
        val result = TranslateUtteranceUseCase(fakeEngine)("你好", Language.CHINESE, Language.SPANISH)

        assertEquals(Language.CHINESE, result.sourceLanguage)
        assertEquals("[zh->es] 你好", result.translatedText)
    }

    @Test
    fun `spanish is translated to the language the user chose`() = runTest {
        val result = TranslateUtteranceUseCase(fakeEngine)("Hola", Language.SPANISH, Language.ENGLISH)

        assertEquals(Language.ENGLISH, result.targetLanguage)
        assertEquals("[es->en] Hola", result.translatedText)
    }

    @Test
    fun `the same language is not translated`() = runTest {
        val result = TranslateUtteranceUseCase(fakeEngine)("Hello", Language.ENGLISH, Language.ENGLISH)

        assertEquals("Hello", result.translatedText)
    }
}
