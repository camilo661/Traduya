package com.traduya.core.directory.callers

import android.util.Log
import com.traduya.core.directory.FirebaseConfig
import com.traduya.core.directory.appusers.AppUsersApi
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
 * 3. Si llama otro teléfono con la app, se identifica con el número que registró.
 */
internal class FirebaseCallerVerifier @Inject constructor(
    private val config: FirebaseConfig,
    private val session: AnonymousSession,
    private val callers: CallersApi,
    private val appUsers: AppUsersApi,
) : CallerVerifier {

    override suspend fun verify(idToken: String): VerifiedCaller? {
        if (!config.isConfigured || idToken.isBlank()) return null
        return try {
            val uid = verifiedUid(idToken) ?: return null
            val token = session.idToken()
            callers.profile(uid, token)
                ?.let { VerifiedCaller(fullName = it.fullName, phoneNumber = it.phone, email = it.email) }
                // Otro teléfono con la app: se identifica con su número
                ?: appUsers.phoneOf(uid, token)?.let { VerifiedCaller(fullName = it, phoneNumber = it, email = "") }
        } catch (e: IOException) {
            Log.w(TAG, "No se pudo comprobar quién llama", e)
            null
        }
    }

    /** El identificador de la cuenta si el token es válido. */
    private suspend fun verifiedUid(idToken: String): String? {
        val response = JsonHttp.request(
            method = "POST",
            url = "${config.authUrl}/v1/accounts:lookup?key=${config.apiKey}",
            body = JSONObject().put("idToken", idToken),
        )
        val user = response.body.optJSONArray("users")?.optJSONObject(0)
        if (!response.isSuccess || user == null) return null
        // Las cuentas de la web deben tener el correo verificado; los teléfonos con la app son anónimos
        val anonymous = user.optString("email").isBlank()
        if (!anonymous && !user.optBoolean("emailVerified")) return null
        return user.optString("localId").takeIf { it.isNotBlank() }
    }

    private companion object {
        const val TAG = "TraduyaCallers"
    }
}
