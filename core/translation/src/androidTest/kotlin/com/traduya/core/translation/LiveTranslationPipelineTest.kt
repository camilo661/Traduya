package com.traduya.core.translation

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.traduya.core.model.AudioChunk
import com.traduya.core.model.Language
import com.traduya.core.model.SpeechEvent
import com.traduya.core.model.TranscriptionConfig
import com.traduya.core.translation.mlkit.MlKitTranslationEngine
import com.traduya.core.translation.vosk.VoskModelStore
import com.traduya.core.translation.vosk.VoskSpeechTranscriber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Prueba de punta a punta con voz real: audio → Vosk → idioma detectado → ML Kit → español.
 * Necesita internet la primera vez para descargar los paquetes de idioma.
 */
@RunWith(AndroidJUnit4::class)
class LiveTranslationPipelineTest {

    companion object {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private val store = VoskModelStore(context)
        private val packs = LanguagePacksImpl(store)

        @BeforeClass
        @JvmStatic
        fun downloadPacks() = runBlocking {
            Language.entries.forEach { packs.install(it).toList() }
        }
    }

    private val transcriber = VoskSpeechTranscriber(store)
    private val translator = MlKitTranslationEngine()
    private val auto = TranscriptionConfig(Language.foreignLanguages)

    @Test
    fun englishIsDetectedAndTranslated() = runBlocking {
        val events = transcribe("english.wav")
        val final = events.filterIsInstance<SpeechEvent.Final>().first()
        val spanish = translator.translate(final.text, final.language, Language.SPANISH)
        Log.i("TraduyaTest", "EN partials=${events.count { it is SpeechEvent.Partial }} final='${final.text}' -> '$spanish'")

        assertEquals(Language.ENGLISH, final.language)
        assertTrue(final.text.contains("tomorrow"))
        assertTrue(events.any { it is SpeechEvent.Partial })
    }

    @Test
    fun chineseIsDetectedAndTranslated() = runBlocking {
        val events = transcribe("chinese.wav")
        val final = events.filterIsInstance<SpeechEvent.Final>().first()
        val spanish = translator.translate(final.text, final.language, Language.SPANISH)
        Log.i("TraduyaTest", "ZH final='${final.text}' conf=${final.confidence} -> '$spanish'")

        assertEquals(Language.CHINESE, final.language)
    }

    @Test
    fun userSpanishIsDiscarded() = runBlocking {
        val events = transcribe("spanish.wav")
        Log.i("TraduyaTest", "ES events=${events.filter { it !is SpeechEvent.Partial }}")

        assertTrue(events.none { it is SpeechEvent.Final })
        assertTrue(events.any { it is SpeechEvent.Discarded })
    }

    private suspend fun transcribe(asset: String): List<SpeechEvent> =
        transcriber.transcribe(chunksOf(asset), auto).toList()

    /** Lee el WAV y lo entrega en trozos de 100 ms, como el micrófono, con 1,5 s de silencio al final. */
    private fun chunksOf(asset: String): Flow<AudioChunk> {
        val bytes = context.assets.open(asset).use { it.readBytes() }
        val pcm = ByteBuffer.wrap(bytes, 44, bytes.size - 44).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val samples = ShortArray(pcm.remaining()).also { pcm.get(it) } + ShortArray(AudioChunk.SAMPLE_RATE * 3 / 2)
        return samples.toList().chunked(1_600)
            .map { AudioChunk(it.toShortArray(), level = 0.5f, blocked = false) }
            .asFlow()
    }
}
