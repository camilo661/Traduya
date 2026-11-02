package com.traduya.core.directory.numbers

import com.traduya.core.directory.FirebaseConfig
import com.traduya.core.directory.http.JsonHttp
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.time.Instant
import javax.inject.Inject

/** El servidor no aceptó el token de la sesión. */
internal class UnauthorizedException : IOException("Sesión no aceptada")

/** Un número leído del directorio: su conexión y la última vez que avisó que estaba activo. */
internal data class NumberEntry(val peerId: String, val lastSeen: Instant?)

/** Resultado de intentar quedarse con un número del directorio. */
internal enum class ClaimResult { CLAIMED, TAKEN }

/**
 * Colección "numbers" de Firestore: cada documento se llama como el número (E.164)
 * y guarda su dueño y el identificador de la conexión WebRTC.
 */
internal class NumbersApi @Inject constructor(private val config: FirebaseConfig) {

    suspend fun claim(number: String, peerId: String, ownerUid: String, token: String): ClaimResult {
        val created = JsonHttp.request(
            method = "POST",
            url = "${config.numbersUrl}?documentId=${encode(number)}",
            body = entry(ownerUid, peerId),
            bearerToken = token,
        )
        return when {
            created.isSuccess -> ClaimResult.CLAIMED
            created.code == HTTP_UNAUTHORIZED -> throw UnauthorizedException()
            created.code == HTTP_CONFLICT -> updateIfOwned(number, peerId, ownerUid, token)
            else -> throw IOException("No se pudo registrar el número (${created.code})")
        }
    }

    /** El número, o null si no está en el directorio. */
    suspend fun lookup(number: String, token: String): NumberEntry? {
        val response = JsonHttp.request("GET", documentUrl(number), bearerToken = token)
        if (response.code == HTTP_NOT_FOUND) return null
        if (response.code == HTTP_UNAUTHORIZED) throw UnauthorizedException()
        if (!response.isSuccess) throw IOException("No se pudo buscar el número (${response.code})")
        val fields = response.body.optJSONObject("fields") ?: return null
        val peerId = fields.optJSONObject("peerId")?.optString("stringValue").orEmpty()
        val lastSeen = fields.optJSONObject("lastSeen")?.optString("timestampValue")
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
        return NumberEntry(peerId, lastSeen).takeIf { peerId.isNotBlank() }
    }

    /** Renueva "lastSeen" de un número propio (solo cambia ese campo). */
    suspend fun touch(number: String, token: String) {
        val body = JSONObject().put(
            "fields",
            JSONObject().put("lastSeen", JSONObject().put("timestampValue", Instant.now().toString())),
        )
        val response = JsonHttp.request("PATCH", "${documentUrl(number)}?updateMask.fieldPaths=lastSeen", body, token)
        if (response.code == HTTP_UNAUTHORIZED) throw UnauthorizedException()
        if (!response.isSuccess) throw IOException("No se pudo marcar como activo (${response.code})")
    }

    suspend fun release(number: String, token: String) {
        val response = JsonHttp.request("DELETE", documentUrl(number), bearerToken = token)
        if (response.code == HTTP_UNAUTHORIZED) throw UnauthorizedException()
        // 404: ya no existe. 403: ya no es nuestro (las reglas solo dejan borrar al dueño). En ambos casos está liberado.
        if (!response.isSuccess && response.code != HTTP_NOT_FOUND && response.code != HTTP_FORBIDDEN) {
            throw IOException("No se pudo borrar el número (${response.code})")
        }
    }

    /** El número ya existe: si es nuestro (por ejemplo, de un registro anterior) lo actualizamos. */
    private suspend fun updateIfOwned(number: String, peerId: String, ownerUid: String, token: String): ClaimResult {
        val updated = JsonHttp.request("PATCH", documentUrl(number), entry(ownerUid, peerId), token)
        return when {
            updated.isSuccess -> ClaimResult.CLAIMED
            updated.code == HTTP_UNAUTHORIZED -> throw UnauthorizedException()
            updated.code == HTTP_FORBIDDEN -> ClaimResult.TAKEN
            else -> throw IOException("No se pudo registrar el número (${updated.code})")
        }
    }

    private fun documentUrl(number: String) = "${config.numbersUrl}/${encode(number)}"

    private fun encode(number: String): String = URLEncoder.encode(number, "UTF-8")

    private fun entry(ownerUid: String, peerId: String) = JSONObject().put(
        "fields",
        JSONObject()
            .put("owner", JSONObject().put("stringValue", ownerUid))
            .put("peerId", JSONObject().put("stringValue", peerId)),
    )

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_CONFLICT = 409
    }
}
