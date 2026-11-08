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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.system.measureTimeMillis

/**
 * Mide la rapidez de la traducción con voz real, entregando el audio a ritmo real (100 ms cada 100 ms):
 * carga de modelos, primera palabra en vivo, tiempo hasta cerrar la frase y traducción.
 * Los resultados se ven en logcat con la etiqueta TraduyaLatency.
 */
@RunWith(AndroidJUnit4::class)
class TranslationLatencyTest {

    companion object {
        private const val TAG = "TraduyaLatency"
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private val store = VoskModelStore(context)

        @BeforeClass
        @JvmStatic
        fun loadModels() = runBlocking {
            LanguagePacksImpl(store).let { packs -> Language.entries.forEach { packs.install(it).toList() } }
            Language.entries.forEach { language ->
                val millis = measureTimeMillis { store.load(language) }
                Log.i(TAG, "carga del modelo ${language.code}: $millis ms")
            }
        }
    }

    private val transcriber = VoskSpeechTranscriber(store)
    private val translator = MlKitTranslationEngine()

    @Test
    fun automaticWithUserSpeechFilter() = measure("automático + filtro de español", TranscriptionConfig(Language.othersThan(Language.SPANISH)))

    @Test
    fun automaticWithoutUserSpeechFilter() =
        measure("automático sin filtro de español", TranscriptionConfig(Language.othersThan(Language.SPANISH), ignoreUserSpeech = false))

    @Test
    fun englishOnly() = measure("solo inglés", TranscriptionConfig(listOf(Language.ENGLISH), ignoreUserSpeech = false))

    /** Cuánto procesador gasta: el audio entero lo más rápido posible. Menos del 100 % de la duración = va al día. */
    @Test
    fun processingCost() = runBlocking<Unit> {
        val (samples, _) = speech("english.wav")
        val chunks = samples.toList().chunked(1_600).map { AudioChunk(it.toShortArray(), level = 0.5f, blocked = false) }
        val audioMillis = samples.size / 16
        listOf(
            "automático + filtro de español" to TranscriptionConfig(Language.othersThan(Language.SPANISH)),
            "automático sin filtro de español" to TranscriptionConfig(Language.othersThan(Language.SPANISH), ignoreUserSpeech = false),
            "solo inglés" to TranscriptionConfig(listOf(Language.ENGLISH), ignoreUserSpeech = false),
        ).forEach { (name, config) ->
            val millis = measureTimeMillis { transcriber.transcribe(flow { chunks.forEach { emit(it) } }, config).toList() }
            Log.i(TAG, "[$name] procesar $audioMillis ms de audio tomó $millis ms (${millis * 100 / audioMillis} % del tiempo real)")
        }
    }

    private fun measure(name: String, config: TranscriptionConfig) = runBlocking<Unit> {
        translator.translate("hello", Language.ENGLISH, Language.SPANISH) // modelo de traducción listo
        val (samples, speechMillis) = speech("english.wav")
        val start = System.currentTimeMillis()
        var firstPartial = -1L
        var finalAt = -1L
        var finalText = ""
        var partials = 0
        transcriber.transcribe(realTime(samples), config).collect { event ->
            val now = System.currentTimeMillis() - start
            when (event) {
                is SpeechEvent.Partial -> {
                    partials++
                    if (firstPartial < 0) firstPartial = now
                }
                is SpeechEvent.Final -> if (finalAt < 0) {
                    finalAt = now
                    finalText = event.text
                }
                else -> Unit
            }
        }
        val total = System.currentTimeMillis() - start
        var spanish = ""
        val translateMillis = measureTimeMillis {
            spanish = translator.translate(finalText, Language.ENGLISH, Language.SPANISH)
        }
        Log.i(
            TAG,
            "[$name] voz=${speechMillis} ms | 1ª palabra en vivo=$firstPartial ms | frase cerrada=$finalAt ms " +
                "(${finalAt - speechMillis} ms tras callar) | parciales=$partials | traducción=$translateMillis ms | " +
                "audio entregado en $total ms (ideal ${samples.size / 16}) | '$finalText' → '$spanish'",
        )
    }

    /** El WAV con 2 s de silencio al final; devuelve también cuánto dura la voz. */
    private fun speech(asset: String): Pair<ShortArray, Long> {
        val bytes = context.assets.open(asset).use { it.readBytes() }
        val pcm = ByteBuffer.wrap(bytes, 44, bytes.size - 44).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val voice = ShortArray(pcm.remaining()).also { pcm.get(it) }
        return (voice + ShortArray(AudioChunk.SAMPLE_RATE * 2)) to voice.size / 16L
    }

    /** Entrega el audio a ritmo real: 100 ms cada 100 ms, como el micrófono o la llamada. */
    private fun realTime(samples: ShortArray): Flow<AudioChunk> = flow {
        val start = System.currentTimeMillis()
        samples.toList().chunked(1_600).forEachIndexed { index, chunk ->
            val due = start + index * 100L
            delay((due - System.currentTimeMillis()).coerceAtLeast(0))
            emit(AudioChunk(chunk.toShortArray(), level = 0.5f, blocked = false))
        }
    }
}
