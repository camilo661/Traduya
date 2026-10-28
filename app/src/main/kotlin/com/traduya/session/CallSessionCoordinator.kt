package com.traduya.session

import com.traduya.core.domain.audio.AudioCapture
import com.traduya.core.domain.repository.SettingsRepository
import com.traduya.core.domain.translation.CallTranslationSession
import com.traduya.core.model.ActiveCall
import com.traduya.core.model.CallStatus
import com.traduya.core.telecom.CallManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Conecta las llamadas telefónicas con la sesión de traducción.
 * El audio sale del micrófono con el altavoz activado (Android puede bloquearlo en llamadas).
 */
@Singleton
class CallSessionCoordinator @Inject constructor(
    private val callManager: CallManager,
    private val settingsRepository: SettingsRepository,
    private val audioCapture: AudioCapture,
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
            if (settingsRepository.settings.first().autoSpeaker) callManager.setSpeakerOn(true)
            session.connect(audioCapture.chunks())
        }
    }

    private suspend fun end() {
        if (trackedCallId == null) return
        trackedCallId = null
        connected = false
        session.finish()
    }
}
