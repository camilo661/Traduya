package com.traduya.core.domain.translation

import com.traduya.core.model.AudioChunk
import com.traduya.core.model.CallDirection
import com.traduya.core.model.TranslationSessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Sesión de traducción de una llamada, sea telefónica o por internet.
 * Quien maneja la llamada solo indica cuándo empieza, de dónde sale el audio y cuándo termina.
 */
interface CallTranslationSession {
    val state: StateFlow<TranslationSessionState>

    fun setEnabled(enabled: Boolean)

    /** Crea la entrada del historial y precarga la IA mientras la llamada suena. */
    suspend fun begin(number: String, direction: CallDirection)

    /**
     * La llamada se conectó: se graba y se traduce el audio de la otra persona.
     * [includesUserVoice] indica si en ese audio también se oye al usuario
     * (micrófono con altavoz): entonces se escucha su idioma para descartar su voz. Si solo llega la voz de
     * la otra persona (internet, línea con Shizuku), no hace falta y la traducción va más rápida.
     */
    fun connect(audio: Flow<AudioChunk>, includesUserVoice: Boolean = true)

    /** Detiene todo y guarda duración, idioma y grabación en el historial. */
    suspend fun finish()
}
