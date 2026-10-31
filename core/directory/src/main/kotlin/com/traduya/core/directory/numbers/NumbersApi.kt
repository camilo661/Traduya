package com.traduya.core.directory.numbers

import com.traduya.core.directory.FirebaseConfig
import com.traduya.core.directory.http.JsonHttp
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

/** El servidor no aceptó el token de la sesión. */
internal class UnauthorizedException : IOException("Sesión no aceptada")

/** Resultado de intentar quedarse con un número del directorio. */
internal enum class ClaimResult { CLAIMED, TAKEN }

/**
 * Colección "numbers" de Firestore: cada documento se llama como el número (E.164)
 * y guarda su dueño y el identificador de la conexión WebRTC.
 */
internal class NumbersApi(private val config: FirebaseConfig) {

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

    suspend fun release(number: String, token: String) {
        val response = JsonHttp.request("DELETE", documentUrl(number), bearerToken = token)
        if (response.code == HTTP_UNAUTHORIZED) throw UnauthorizedException()
        if (!response.isSuccess && response.code != HTTP_NOT_FOUND) {
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
