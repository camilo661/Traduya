package com.traduya.core.domain.internetcall

import com.traduya.core.model.VerifiedCaller

/**
 * Comprueba quién llama por internet: la página web envía el token de su sesión
 * y aquí se valida con Firebase. Sin una sesión válida la llamada se rechaza.
 */
interface CallerVerifier {
    /** Devuelve quién llama, o null si el token no es válido o el correo no está verificado. */
    suspend fun verify(idToken: String): VerifiedCaller?
}
