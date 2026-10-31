package com.traduya.core.directory.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Respuesta de una petición JSON: el código HTTP y el cuerpo (vacío si no había). */
internal class JsonResponse(val code: Int, val body: JSONObject) {
    val isSuccess: Boolean get() = code in 200..299
}

/** Cliente HTTP mínimo para las APIs REST de Firebase, sin SDKs pesados. */
internal object JsonHttp {

    private const val TIMEOUT_MILLIS = 10_000

    /** Lanza IOException si no hay conexión; los errores HTTP vuelven en [JsonResponse.code]. */
    suspend fun request(
        method: String,
        url: String,
        body: JSONObject? = null,
        bearerToken: String? = null,
    ): JsonResponse = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.setRequestProperty("Content-Type", "application/json")
            bearerToken?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            JsonResponse(code, if (text.isBlank()) JSONObject() else JSONObject(text))
        } finally {
            connection.disconnect()
        }
    }
}
