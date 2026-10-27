package com.traduya.core.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Android se conecta a este servicio cuando Traduya es la app de teléfono predeterminada.
 * Aquí llegan todas las llamadas y desde aquí se muestra nuestra pantalla de llamada.
 */
@AndroidEntryPoint
class TraduyaInCallService : InCallService() {

    @Inject lateinit var callManager: CallManager

    @Inject lateinit var notifier: CallNotifier

    @Inject lateinit var launcher: CallUiLauncher

    private val scope = MainScope()

    override fun onCreate() {
        super.onCreate()
        callManager.attachService(this)
        scope.launch { callManager.currentCall.collect(notifier::update) }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        callManager.onCallAdded(call)
        showCallScreen()
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        callManager.onCallRemoved(call)
    }

    @Deprecated("Reemplazado por onCallEndpointChanged en Android 14, pero sigue funcionando")
    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        callManager.onAudioStateChanged(audioState)
    }

    override fun onDestroy() {
        notifier.update(null)
        callManager.attachService(null)
        scope.cancel()
        super.onDestroy()
    }

    /** Si el sistema bloquea abrir la pantalla, la notificación a pantalla completa la muestra. */
    private fun showCallScreen() {
        runCatching {
            startActivity(launcher.inCallIntent(this).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
