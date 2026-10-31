package com.traduya.core.directory.callers

import android.util.Log
import com.traduya.core.directory.FirebaseConfig
import com.traduya.core.directory.http.JsonHttp
import com.traduya.core.domain.internetcall.CallerVerifier
import com.traduya.core.model.VerifiedCaller
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject

/** Valida el token de quien llama con la API de Firebase Authentication (accounts:lookup). */
internal class FirebaseCallerVerifier @Inject constructor() : CallerVerifier {

    private val config = FirebaseConfig.fromBuildConfig()

    override suspend fun verify(idToken: String): VerifiedCaller? {
        if (!config.isConfigured || idToken.isBlank()) return null
        val response = try {
            JsonHttp.request(
                method = "POST",
                url = "${config.authUrl}/v1/accounts:lookup?key=${config.apiKey}",
                body = JSONObject().put("idToken", idToken),
            )
        } catch (e: IOException) {
            Log.w(TAG, "No se pudo comprobar quién llama", e)
            return null
        }
        val user = response.body.optJSONArray("users")?.optJSONObject(0)
        if (!response.isSuccess || user == null || !user.optBoolean("emailVerified")) return null
        val email = user.optString("email").takeIf { it.isNotBlank() } ?: return null
        return VerifiedCaller(email = email, displayName = user.optString("displayName").takeIf { it.isNotBlank() })
    }

    private companion object {
        const val TAG = "TraduyaCallers"
    }
}
