package com.traduya.core.directory

import android.content.Context
import android.util.Log
import com.traduya.core.directory.auth.AnonymousSession
import com.traduya.core.directory.numbers.ClaimResult
import com.traduya.core.directory.numbers.NumbersApi
import com.traduya.core.directory.numbers.UnauthorizedException
import com.traduya.core.domain.wifinumber.WifiNumberException
import com.traduya.core.domain.wifinumber.WifiNumberRegistry
import com.traduya.core.model.WifiNumberAccount
import com.traduya.core.model.WifiNumberError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/** Directorio de números en Firebase (usuario anónimo + Firestore por REST). */
@Singleton
internal class FirebaseWifiNumberRegistry @Inject constructor(
    @ApplicationContext context: Context,
) : WifiNumberRegistry {

    private val config = FirebaseConfig.fromBuildConfig()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val session = AnonymousSession(config, prefs)
    private val numbers = NumbersApi(config)

    override val isConfigured: Boolean get() = config.isConfigured

    private val _account = MutableStateFlow(savedAccount())
    override val account: StateFlow<WifiNumberAccount?> = _account.asStateFlow()

    override suspend fun register(phoneNumber: String): WifiNumberAccount {
        if (!isConfigured) throw WifiNumberException(WifiNumberError.NOT_CONFIGURED)
        val previous = _account.value
        // Se conserva el identificador si se vuelve a registrar el mismo número
        val peerId = previous?.takeIf { it.phoneNumber == phoneNumber }?.peerId ?: newPeerId()
        val result = withSession { token ->
            val uid = session.uid ?: throw IOException("Sesión sin usuario")
            numbers.claim(phoneNumber, peerId, uid, token)
        }
        if (result == ClaimResult.TAKEN) throw WifiNumberException(WifiNumberError.NUMBER_TAKEN)
        if (previous != null && previous.phoneNumber != phoneNumber) {
            runCatching { withSession { token -> numbers.release(previous.phoneNumber, token) } }
        }
        return WifiNumberAccount(phoneNumber, peerId).also(::save)
    }

    override suspend fun unregister() {
        val current = _account.value ?: return
        withSession { token -> numbers.release(current.phoneNumber, token) }
        save(null)
    }

    /** Ejecuta [block] con un token válido; si el servidor lo rechaza, pide otro y reintenta una vez. */
    private suspend fun <T> withSession(block: suspend (token: String) -> T): T = try {
        try {
            block(session.idToken())
        } catch (e: UnauthorizedException) {
            session.invalidate()
            block(session.idToken())
        }
    } catch (e: IOException) {
        Log.w(TAG, "Fallo al hablar con el directorio", e)
        throw WifiNumberException(WifiNumberError.NO_CONNECTION, e)
    }

    private fun newPeerId(): String {
        val random = SecureRandom()
        return PEER_PREFIX + (1..PEER_ID_LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
    }

    private fun savedAccount(): WifiNumberAccount? {
        // El número registrado pertenece a la sesión: si la sesión se descartó, el número también
        if (session.wasReset) prefs.edit().remove(KEY_NUMBER).remove(KEY_PEER_ID).apply()
        val number = prefs.getString(KEY_NUMBER, null) ?: return null
        val peerId = prefs.getString(KEY_PEER_ID, null) ?: return null
        return WifiNumberAccount(number, peerId)
    }

    private fun save(account: WifiNumberAccount?) {
        prefs.edit().putString(KEY_NUMBER, account?.phoneNumber).putString(KEY_PEER_ID, account?.peerId).apply()
        _account.value = account
    }

    private companion object {
        const val TAG = "TraduyaDirectory"
        const val PREFS_NAME = "wifi_number"
        const val KEY_NUMBER = "number"
        const val KEY_PEER_ID = "peer_id"
        const val PEER_PREFIX = "traduya-"
        const val PEER_ID_LENGTH = 16
        const val ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
    }
}
