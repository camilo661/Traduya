package com.traduya.core.translation.speech

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import com.traduya.core.domain.translation.SpeechTranscriber
import com.traduya.core.model.Language
import com.traduya.core.model.SpeechEvent
import com.traduya.core.model.TranscriptionConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Reconocimiento de voz continuo con el SpeechRecognizer de Android.
 * El reconocedor de Android se detiene tras cada frase, así que se reinicia
 * automáticamente para cubrir llamadas de varios minutos.
 */
internal class AndroidSpeechTranscriber @Inject constructor(
    @ApplicationContext private val context: Context,
) : SpeechTranscriber {

    override fun transcribe(config: TranscriptionConfig): Flow<SpeechEvent> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(SpeechEvent.Error("Este teléfono no tiene reconocimiento de voz disponible", fatal = true))
            close()
            awaitClose()
            return@callbackFlow
        }

        val handler = Handler(Looper.getMainLooper())
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        var autoDetect = true
        var detectedLanguage: Language? = null
        var running = true

        fun listen() {
            if (!running) return
            detectedLanguage = null
            recognizer.startListening(RecognizerIntents.build(context, config, autoDetect))
        }

        fun restart(delayMillis: Long) {
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed(::listen, delayMillis)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(SpeechEvent.Listening)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults.bestText()?.let { trySend(SpeechEvent.Partial(it)) }
            }

            override fun onResults(results: Bundle?) {
                results.bestText()?.let { trySend(SpeechEvent.Final(it, detectedLanguage)) }
                restart(RESTART_AFTER_RESULT_MILLIS)
            }

            override fun onLanguageDetection(results: Bundle) {
                detectedLanguage = Language.fromCode(results.getString(SpeechRecognizer.DETECTED_LANGUAGE))
            }

            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        trySend(SpeechEvent.Error("Falta el permiso de micrófono", fatal = true))
                        running = false
                    }

                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
                    -> {
                        // El teléfono no soporta la detección automática: seguimos sin ella
                        autoDetect = false
                        restart(RESTART_AFTER_ERROR_MILLIS)
                    }

                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    -> restart(RESTART_AFTER_RESULT_MILLIS)

                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> restart(RESTART_WHEN_BUSY_MILLIS)

                    else -> restart(RESTART_AFTER_ERROR_MILLIS)
                }
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        listen()

        awaitClose {
            running = false
            handler.removeCallbacksAndMessages(null)
            recognizer.cancel()
            recognizer.destroy()
        }
    }.flowOn(Dispatchers.Main.immediate)

    private fun Bundle?.bestText(): String? =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private companion object {
        const val RESTART_AFTER_RESULT_MILLIS = 50L
        const val RESTART_AFTER_ERROR_MILLIS = 500L
        const val RESTART_WHEN_BUSY_MILLIS = 1_000L
    }
}
