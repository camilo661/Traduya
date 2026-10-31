package com.traduya.core.directory.auth

import android.content.SharedPreferences
import com.traduya.core.directory.FirebaseConfig
import com.traduya.core.directory.http.JsonHttp
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.IOException

/**
 * Usuario anónimo de Firebase que identifica a este teléfono como dueño de su número.
 * No pide datos personales: Firebase solo entrega un identificador y un token renovable.
 */
internal class AnonymousSession(
    private val config: FirebaseConfig,
    private val prefs: SharedPreferences,
) {
    private val mutex = Mutex()

    /** true si había una sesión de otro servidor (por ejemplo, del emulador de pruebas) y se descartó. */
    val wasReset: Boolean

    init {
        // Una sesión solo vale para el servidor que la creó
        val server = "${config.authUrl}|${config.projectId}"
        wasReset = uid != null && prefs.getString(KEY_SERVER, null) != server
        if (wasReset) {
            prefs.edit().remove(KEY_UID).remove(KEY_ID_TOKEN).remove(KEY_REFRESH_TOKEN).remove(KEY_EXPIRES_AT).apply()
        }
        prefs.edit().putString(KEY_SERVER, server).apply()
    }

    /** Identificador del usuario anónimo, o null si todavía no se creó. */
    val uid: String? get() = prefs.getString(KEY_UID, null)

    /** Token válido para la API de Firestore; crea el usuario o renueva el token si hace falta. */
    suspend fun idToken(): String = mutex.withLock {
        val cached = prefs.getString(KEY_ID_TOKEN, null)
        if (cached != null && System.currentTimeMillis() < prefs.getLong(KEY_EXPIRES_AT, 0)) return cached
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        (refreshToken?.let { refresh(it) }) ?: signUp()
    }

    /** El servidor rechazó el token: el siguiente [idToken] pedirá uno nuevo. */
    fun invalidate() {
        prefs.edit().remove(KEY_ID_TOKEN).remove(KEY_EXPIRES_AT).apply()
    }

    private suspend fun signUp(): String {
        val response = JsonHttp.request(
            method = "POST",
            url = "${config.authUrl}/v1/accounts:signUp?key=${config.apiKey}",
            body = JSONObject().put("returnSecureToken", true),
        )
        if (!response.isSuccess) throw IOException("No se pudo crear la sesión (${response.code})")
        return save(
            uid = response.body.getString("localId"),
            idToken = response.body.getString("idToken"),
            refreshToken = response.body.getString("refreshToken"),
            expiresInSeconds = response.body.optString("expiresIn", "3600").toLong(),
        )
    }

    /** Renueva el token. Devuelve null si el servidor ya no acepta la sesión y hay que crear otra. */
    private suspend fun refresh(refreshToken: String): String? {
        val response = JsonHttp.request(
            method = "POST",
            url = "${config.tokenUrl}/v1/token?key=${config.apiKey}",
            body = JSONObject().put("grant_type", "refresh_token").put("refresh_token", refreshToken),
        )
        if (response.code == HTTP_BAD_REQUEST) return null
        if (!response.isSuccess) throw IOException("No se pudo renovar la sesión (${response.code})")
        return save(
            uid = response.body.getString("user_id"),
            idToken = response.body.getString("id_token"),
            refreshToken = response.body.getString("refresh_token"),
            expiresInSeconds = response.body.optString("expires_in", "3600").toLong(),
        )
    }

    private fun save(uid: String, idToken: String, refreshToken: String, expiresInSeconds: Long): String {
        // Se renueva un minuto antes de que caduque
        val expiresAt = System.currentTimeMillis() + (expiresInSeconds - 60) * 1_000
        prefs.edit()
            .putString(KEY_UID, uid)
            .putString(KEY_ID_TOKEN, idToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
        return idToken
    }

    private companion object {
        const val HTTP_BAD_REQUEST = 400 // sesión revocada, caducada o de otro proyecto
        const val KEY_SERVER = "auth_server"
        const val KEY_UID = "auth_uid"
        const val KEY_ID_TOKEN = "auth_id_token"
        const val KEY_REFRESH_TOKEN = "auth_refresh_token"
        const val KEY_EXPIRES_AT = "auth_expires_at"
    }
}
