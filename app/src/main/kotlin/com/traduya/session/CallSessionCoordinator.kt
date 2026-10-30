package com.traduya.session

import com.traduya.core.domain.audio.AudioCapture
import com.traduya.core.domain.audio.CallAudioTap
import com.traduya.core.domain.repository.SettingsRepository
import com.traduya.core.domain.translation.CallTranslationSession
import com.traduya.core.model.ActiveCall
import com.traduya.core.model.AudioChunk
import com.traduya.core.model.CallAudioTapStatus
import com.traduya.core.model.CallStatus
import com.traduya.core.telecom.CallManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Conecta las llamadas telefónicas con la sesión de traducción.
 *
 * Con Shizuku activo, el audio sale directamente de la línea telefónica: solo la voz de la otra
 * persona, con o sin altavoz. Sin él, se usa el micrófono (muchos teléfonos lo silencian) y
 * la voz del usuario se descarta porque se reconoce en español.
 */
@Singleton
class CallSessionCoordinator @Inject constructor(
    private val callManager: CallManager,
    private val settingsRepository: SettingsRepository,
    private val audioCapture: AudioCapture,
    private val callAudioTap: CallAudioTap,
    private val session: CallTranslationSession,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var trackedCallId: String? = null
    private var connected = false

    fun start() {
        scope.launch { callManager.currentCall.collect { onCallChanged(it) } }
    }

    private suspend fun onCallChanged(call: ActiveCall?) {
        if (call == null || call.status == CallStatus.DISCONNECTED) {
            end()
            return
        }
        if (call.id != trackedCallId) {
            end()
            trackedCallId = call.id
            session.begin(call.number, call.direction)
        }
        if (call.status == CallStatus.ACTIVE && !connected) {
            connected = true
            session.connect(callAudio(settingsRepository.settings.first().autoSpeaker))
        }
    }

    private fun callAudio(autoSpeaker: Boolean): Flow<AudioChunk> {
        // Con Shizuku el altavoz no cambia lo que se traduce: se lee la línea, no el micrófono
        if (autoSpeaker) callManager.setSpeakerOn(true)
        callAudioTap.refresh()
        if (callAudioTap.status.value != CallAudioTapStatus.READY) return audioCapture.chunks()
        return callAudioTap.chunks().catch { emitAll(audioCapture.chunks()) }
    }

    private suspend fun end() {
        if (trackedCallId == null) return
        trackedCallId = null
        connected = false
        session.finish()
    }
}
