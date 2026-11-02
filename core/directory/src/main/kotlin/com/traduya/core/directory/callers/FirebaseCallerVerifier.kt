package com.traduya.core.directory.callers

import android.util.Log
import com.traduya.core.directory.FirebaseConfig
import com.traduya.core.directory.auth.AnonymousSession
import com.traduya.core.directory.http.JsonHttp
import com.traduya.core.domain.internetcall.CallerVerifier
import com.traduya.core.model.VerifiedCaller
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject

/**
 * Comprueba quién llama:
 * 1. Valida su token con Firebase Authentication (accounts:lookup).
 * 2. Lee su ficha (nombre completo y teléfono) con la sesión anónima de este teléfono.
 *    Si no tiene ficha, no terminó el registro (no confirmó el enlace de su Gmail) y se rechaza.
 */
internal class FirebaseCallerVerifier @Inject constructor(
    private val config: FirebaseConfig,
    private val session: AnonymousSession,
    private val callers: CallersApi,
) : CallerVerifier {

    override suspend fun verify(idToken: String): VerifiedCaller? {
        if (!config.isConfigured || idToken.isBlank()) return null
        return try {
            val uid = verifiedUid(idToken) ?: return null
            val profile = callers.profile(uid, session.idToken()) ?: return null
            VerifiedCaller(fullName = profile.fullName, phoneNumber = profile.phone, email = profile.email)
        } catch (e: IOException) {
            Log.w(TAG, "No se pudo comprobar quién llama", e)
            null
        }
    }

    /** El identificador de la cuenta si el token es válido y su correo está verificado. */
    private suspend fun verifiedUid(idToken: String): String? {
        val response = JsonHttp.request(
            method = "POST",
            url = "${config.authUrl}/v1/accounts:lookup?key=${config.apiKey}",
            body = JSONObject().put("idToken", idToken),
        )
        val user = response.body.optJSONArray("users")?.optJSONObject(0)
        if (!response.isSuccess || user == null || !user.optBoolean("emailVerified")) return null
        return user.optString("localId").takeIf { it.isNotBlank() }
    }

    private companion object {
        const val TAG = "TraduyaCallers"
    }
}
