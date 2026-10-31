package com.traduya.core.model

/**
 * Número de teléfono que el usuario registró, voluntariamente, para recibir llamadas por WiFi.
 *
 * @param phoneNumber número en formato internacional (E.164), por ejemplo "+573001234567".
 * @param peerId identificador secreto de la conexión WebRTC; solo se obtiene buscando el número.
 */
data class WifiNumberAccount(
    val phoneNumber: String,
    val peerId: String,
)

/** Motivos por los que no se pudo registrar o borrar el número. */
enum class WifiNumberError {
    INVALID_NUMBER,
    NUMBER_TAKEN,
    NO_CONNECTION,
    NOT_CONFIGURED,
}
