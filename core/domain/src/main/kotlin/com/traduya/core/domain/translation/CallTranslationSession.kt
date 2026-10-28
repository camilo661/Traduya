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

    /** La llamada se conectó: se graba y se traduce el audio de la otra persona. */
    fun connect(audio: Flow<AudioChunk>)

    /** Detiene todo y guarda duración, idioma y grabación en el historial. */
    suspend fun finish()
}
