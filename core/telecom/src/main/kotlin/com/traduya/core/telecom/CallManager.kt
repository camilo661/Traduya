package com.traduya.core.telecom

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import androidx.core.content.ContextCompat
import com.traduya.core.model.ActiveCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente única de verdad de la llamada en curso.
 * Recibe las llamadas del [TraduyaInCallService] y expone acciones para controlarlas.
 */
@Singleton
class CallManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callUiLauncher: CallUiLauncher,
) {
    private val _currentCall = MutableStateFlow<ActiveCall?>(null)
    val currentCall: StateFlow<ActiveCall?> = _currentCall.asStateFlow()

    private val calls = mutableListOf<Call>()
    private var inCallService: InCallService? = null
    private var isMuted = false
    private var isSpeakerOn = false

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            if (state == Call.STATE_SELECT_PHONE_ACCOUNT) selectDefaultAccount(call)
            publish()
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) = publish()
    }

    // region Eventos del sistema (los llama TraduyaInCallService)

    internal fun attachService(service: InCallService?) {
        inCallService = service
    }

    internal fun onCallAdded(call: Call) {
        calls += call
        call.registerCallback(callCallback)
        if (call.currentState == Call.STATE_SELECT_PHONE_ACCOUNT) selectDefaultAccount(call)
        publish()
    }

    internal fun onCallRemoved(call: Call) {
        call.unregisterCallback(callCallback)
        calls -= call
        publish()
    }

    internal fun onAudioStateChanged(state: CallAudioState) {
        isMuted = state.isMuted
        isSpeakerOn = state.route == CallAudioState.ROUTE_SPEAKER
        publish()
    }

    // endregion

    // region Acciones del usuario

    /**
     * Inicia una llamada a través del sistema de telefonía de Android y abre nuestra pantalla.
     *
     * La pantalla se abre desde aquí, con la app en primer plano, porque algunos fabricantes
     * (Xiaomi, Samsung…) bloquean que el servicio de llamadas la abra desde segundo plano.
     */
    @SuppressLint("MissingPermission")
    fun placeCall(number: String): Result<Unit> = runCatching {
        check(hasPermission(Manifest.permission.CALL_PHONE)) { "Falta el permiso para llamar" }
        val telecom = context.getSystemService(TelecomManager::class.java)
        val extras = Bundle()
        defaultOutgoingAccount()?.let { extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it) }
        telecom.placeCall(Uri.fromParts("tel", number, null), extras)
        showCallScreen()
    }

    /** Abre (o trae al frente) la pantalla de la llamada en curso. */
    fun showCallScreen() {
        runCatching {
            context.startActivity(callUiLauncher.inCallIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun answer() {
        primaryCall()?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun hangUp() {
        val call = primaryCall() ?: return
        if (call.currentState == Call.STATE_RINGING) call.reject(false, null) else call.disconnect()
    }

    fun setMuted(muted: Boolean) {
        inCallService?.setMuted(muted)
    }

    fun setSpeakerOn(enabled: Boolean) {
        val route = if (enabled) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_WIRED_OR_EARPIECE
        inCallService?.setAudioRoute(route)
    }

    fun playDtmf(digit: Char) {
        val call = primaryCall() ?: return
        call.playDtmfTone(digit)
        call.stopDtmfTone()
    }

    // endregion

    /** La llamada que sonando tiene prioridad; si no, la más reciente. */
    private fun primaryCall(): Call? =
        calls.firstOrNull { it.currentState == Call.STATE_RINGING } ?: calls.lastOrNull()

    private fun publish() {
        _currentCall.value = primaryCall()?.toActiveCall(isMuted, isSpeakerOn)
    }

    /** Con doble SIM y sin SIM predeterminada, Android nos pide elegir una. */
    private fun selectDefaultAccount(call: Call) {
        val account = defaultOutgoingAccount() ?: return
        call.phoneAccountSelected(account, false)
    }

    /** La SIM que el usuario eligió para llamar o, si no eligió ninguna, la primera disponible. */
    @SuppressLint("MissingPermission")
    private fun defaultOutgoingAccount(): PhoneAccountHandle? = runCatching {
        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) return null
        val telecom = context.getSystemService(TelecomManager::class.java)
        telecom.getDefaultOutgoingPhoneAccount("tel") ?: telecom.callCapablePhoneAccounts.firstOrNull()
    }.getOrNull()

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
