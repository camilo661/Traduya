package com.traduya.core.directory.callers

import com.traduya.core.directory.FirebaseConfig
import com.traduya.core.directory.http.JsonHttp
import javax.inject.Inject

/** Ficha de quien llama tal como la guarda la función de registro. */
internal data class CallerProfile(val fullName: String, val phone: String, val email: String)

/** Colección "callers" de Firestore: una ficha por cuenta registrada en la página web. */
internal class CallersApi @Inject constructor(private val config: FirebaseConfig) {

    /** Devuelve la ficha, o null si la cuenta no terminó el registro. */
    suspend fun profile(uid: String, token: String): CallerProfile? {
        val response = JsonHttp.request("GET", "${config.callersUrl}/$uid", bearerToken = token)
        if (!response.isSuccess) return null
        val fields = response.body.optJSONObject("fields") ?: return null
        fun text(name: String) = fields.optJSONObject(name)?.optString("stringValue").orEmpty()
        return CallerProfile(fullName = text("fullName"), phone = text("phone"), email = text("email"))
            .takeIf { it.fullName.isNotBlank() && it.phone.isNotBlank() }
    }
}
