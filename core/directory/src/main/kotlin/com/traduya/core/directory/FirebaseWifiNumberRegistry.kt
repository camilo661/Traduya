package com.traduya.core.directory

import android.content.Context
import com.traduya.core.directory.auth.AnonymousSession
import com.traduya.core.directory.numbers.ClaimResult
import com.traduya.core.directory.numbers.NumbersApi
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
        val result = withConnection {
            val token = session.idToken()
            val uid = session.uid ?: throw IOException("Sesión sin usuario")
            numbers.claim(phoneNumber, peerId, uid, token)
        }
        if (result == ClaimResult.TAKEN) throw WifiNumberException(WifiNumberError.NUMBER_TAKEN)
        if (previous != null && previous.phoneNumber != phoneNumber) {
            runCatching { withConnection { numbers.release(previous.phoneNumber, session.idToken()) } }
        }
        return WifiNumberAccount(phoneNumber, peerId).also(::save)
    }

    override suspend fun unregister() {
        val current = _account.value ?: return
        withConnection { numbers.release(current.phoneNumber, session.idToken()) }
        save(null)
    }

    private suspend fun <T> withConnection(block: suspend () -> T): T = try {
        block()
    } catch (e: IOException) {
        throw WifiNumberException(WifiNumberError.NO_CONNECTION, e)
    }

    private fun newPeerId(): String {
        val random = SecureRandom()
        return PEER_PREFIX + (1..PEER_ID_LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
    }

    private fun savedAccount(): WifiNumberAccount? {
        val number = prefs.getString(KEY_NUMBER, null) ?: return null
        val peerId = prefs.getString(KEY_PEER_ID, null) ?: return null
        return WifiNumberAccount(number, peerId)
    }

    private fun save(account: WifiNumberAccount?) {
        prefs.edit().putString(KEY_NUMBER, account?.phoneNumber).putString(KEY_PEER_ID, account?.peerId).apply()
        _account.value = account
    }

    private companion object {
        const val PREFS_NAME = "wifi_number"
        const val KEY_NUMBER = "number"
        const val KEY_PEER_ID = "peer_id"
        const val PEER_PREFIX = "traduya-"
        const val PEER_ID_LENGTH = 16
        const val ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
    }
}
