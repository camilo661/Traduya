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
     * Solo la voz de la otra persona, leída de la línea: el altavoz no influye y la voz
     * del usuario nunca se incluye.
     * Falla si el acceso no está listo o el teléfono no ofrece la línea.
     */
    fun chunks(): Flow<AudioChunk>
}
