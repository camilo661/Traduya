package com.traduya.core.domain.audio

import com.traduya.core.model.AudioChunk
import com.traduya.core.model.CallAudioTapStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Escucha el audio de la línea telefónica sin pasar por el micrófono de la app,
 * que Android silencia durante las llamadas normales.
 */
interface CallAudioTap {
    val status: StateFlow<CallAudioTapStatus>

    /** Vuelve a comprobar si Shizuku está instalado y en marcha. */
    fun refresh()

    fun requestPermission()

    /**
     * Audio de la llamada, preferiblemente solo la voz de la otra persona.
     * Si el teléfono no deja leer la línea y hay que usar el micrófono, avisa con [onSpeakerNeeded].
     * Falla si el acceso no está listo; quien lo use debe tener otra fuente de respaldo.
     */
    fun chunks(onSpeakerNeeded: () -> Unit): Flow<AudioChunk>
}
