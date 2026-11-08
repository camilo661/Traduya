package com.traduya.feature.internetcall.captions

import com.traduya.core.domain.translation.SpeechTranscriber
import com.traduya.core.model.AudioChunk
import com.traduya.core.model.Language
import com.traduya.core.model.SpeechEvent
import com.traduya.core.model.TranscriptionConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Subtítulos de la voz del usuario (en su idioma, [language]) para quien está al otro lado en la página web:
 * su navegador no puede transcribir el audio que le llega, así que se lo enviamos ya en texto
 * y allí se traduce a su idioma.
 */
class OwnCaptions @Inject constructor(
    private val transcriber: SpeechTranscriber,
) {
    fun start(
        scope: CoroutineScope,
        language: Language,
        ownAudio: Flow<AudioChunk>,
        send: (text: String, final: Boolean) -> Unit,
    ): Job =
        scope.launch {
            val config = TranscriptionConfig(
                foreignLanguages = listOf(language),
                userLanguage = language,
                ignoreUserSpeech = false,
            )
            transcriber.transcribe(ownAudio, config).collect { event ->
                when (event) {
                    is SpeechEvent.Partial -> send(event.text, false)
                    is SpeechEvent.Final -> send(event.text, true)
                    else -> Unit
                }
            }
        }
}
