package com.traduya.core.directory.appusers

import com.traduya.core.directory.FirebaseConfig
import com.traduya.core.directory.http.JsonHttp
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject

/**
 * Colección "appUsers" de Firestore: el número de cada teléfono con la app.
 * Le permite buscar números para llamar y que quien recibe en la web sepa quién llama.
 */
internal class AppUsersApi @Inject constructor(private val config: FirebaseConfig) {

    suspend fun save(uid: String, phoneNumber: String, token: String) {
        val body = JSONObject().put("fields", JSONObject().put("phone", JSONObject().put("stringValue", phoneNumber)))
        val response = JsonHttp.request("PATCH", "${config.appUsersUrl}/$uid", body, token)
        if (!response.isSuccess) throw IOException("No se pudo guardar el número de la app (${response.code})")
    }

    /** El número de otro teléfono con la app, o null si no existe. */
    suspend fun phoneOf(uid: String, token: String): String? {
        val response = JsonHttp.request("GET", "${config.appUsersUrl}/$uid", bearerToken = token)
        if (!response.isSuccess) return null
        return response.body.optJSONObject("fields")?.optJSONObject("phone")?.optString("stringValue")
            ?.takeIf { it.isNotBlank() }
    }

    suspend fun delete(uid: String, token: String) {
        JsonHttp.request("DELETE", "${config.appUsersUrl}/$uid", bearerToken = token)
    }
}
