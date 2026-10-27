package com.traduya.core.domain

import com.traduya.core.domain.translation.LanguageDetector
import com.traduya.core.domain.translation.TranslationEngine
import com.traduya.core.domain.usecase.TranslateUtteranceUseCase
import com.traduya.core.model.CallLanguagePreference
import com.traduya.core.model.Language
import com.traduya.core.model.Speaker
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslateUtteranceUseCaseTest {

    private val fakeEngine = object : TranslationEngine {
        override suspend fun translate(text: String, from: Language, to: Language) = "[${to.code}] $text"
    }

    private fun detectorReturning(language: Language?) = object : LanguageDetector {
        override suspend fun detect(text: String) = language
    }

    @Test
    fun `english from the other person is translated to spanish`() = runTest {
        val useCase = TranslateUtteranceUseCase(detectorReturning(Language.ENGLISH), fakeEngine)

        val result = useCase("Hello", null, CallLanguagePreference.AUTOMATIC, null)

        assertEquals(Speaker.THEM, result.speaker)
        assertEquals(Language.SPANISH, result.targetLanguage)
        assertEquals("[es] Hello", result.translatedText)
    }

    @Test
    fun `spanish from the user goes to the last foreign language`() = runTest {
        val useCase = TranslateUtteranceUseCase(detectorReturning(Language.SPANISH), fakeEngine)

        val result = useCase("Hola", null, CallLanguagePreference.AUTOMATIC, Language.CHINESE)

        assertEquals(Speaker.ME, result.speaker)
        assertEquals(Language.CHINESE, result.targetLanguage)
    }

    @Test
    fun `recognizer language wins over the text detector`() = runTest {
        val useCase = TranslateUtteranceUseCase(detectorReturning(Language.ENGLISH), fakeEngine)

        val result = useCase("你好", Language.CHINESE, CallLanguagePreference.AUTOMATIC, null)

        assertEquals(Language.CHINESE, result.sourceLanguage)
    }

    @Test
    fun `unknown language falls back to the preference`() = runTest {
        val useCase = TranslateUtteranceUseCase(detectorReturning(null), fakeEngine)

        val result = useCase("???", null, CallLanguagePreference.CHINESE, null)

        assertEquals(Language.CHINESE, result.sourceLanguage)
    }
}
