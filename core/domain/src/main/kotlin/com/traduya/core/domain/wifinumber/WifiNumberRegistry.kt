package com.traduya.core.domain.wifinumber

import com.traduya.core.model.WifiNumberAccount
import com.traduya.core.model.WifiNumberError
import kotlinx.coroutines.flow.StateFlow

/**
 * Directorio de números para llamadas por WiFi: quien escribe tu número en la página
 * de Traduya encuentra tu teléfono y te llama.
 *
 * El registro es voluntario. Solo el teléfono que registró un número puede cambiarlo o borrarlo.
 */
interface WifiNumberRegistry {
    /** false mientras el servidor del directorio no esté configurado en esta versión. */
    val isConfigured: Boolean

    /** Número registrado en este teléfono, o null si no hay ninguno. */
    val account: StateFlow<WifiNumberAccount?>

    /** Registra un número ya normalizado (E.164). Si falla, lanza [WifiNumberException]. */
    suspend fun register(phoneNumber: String): WifiNumberAccount

    /** Borra el número del directorio. Si falla, lanza [WifiNumberException]. */
    suspend fun unregister()
}

class WifiNumberException(val error: WifiNumberError, cause: Throwable? = null) :
    Exception(error.name, cause)
