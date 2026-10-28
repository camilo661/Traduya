package com.traduya.feature.internetcall.peer

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

/** Pone el audio en modo llamada y elige altavoz o auricular. */
class CallAudioRouter(context: Context) {

    private val audioManager = context.getSystemService(AudioManager::class.java)

    var isSpeakerOn: Boolean = false
        private set

    fun startCall(speaker: Boolean) {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        setSpeaker(speaker)
    }

    fun setSpeaker(enabled: Boolean) {
        isSpeakerOn = enabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val wanted = if (enabled) AudioDeviceInfo.TYPE_BUILTIN_SPEAKER else AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            audioManager.availableCommunicationDevices
                .firstOrNull { it.type == wanted }
                ?.let { audioManager.setCommunicationDevice(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = enabled
        }
    }

    fun endCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
        }
        audioManager.mode = AudioManager.MODE_NORMAL
    }
}
