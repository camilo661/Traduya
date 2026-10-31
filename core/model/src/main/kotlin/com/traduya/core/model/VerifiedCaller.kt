package com.traduya.core.model

/**
 * Persona que llama desde la página web, con su sesión comprobada en Firebase.
 *
 * @param email correo verificado (Google o enlace por correo).
 * @param displayName nombre de la cuenta de Google, si lo tiene.
 */
data class VerifiedCaller(
    val email: String,
    val displayName: String? = null,
) {
    /** Lo que se muestra en la pantalla de llamada y en el historial. */
    val label: String get() = displayName?.takeIf { it.isNotBlank() } ?: email
}
