package com.traduya.core.model

/**
 * Persona que llama desde la página web, ya registrada y comprobada en Firebase.
 *
 * @param fullName nombre completo con el que se registró.
 * @param phoneNumber teléfono con código de país (E.164) que escribió al registrarse.
 * @param email su cuenta de Google, donde recibió el código de verificación.
 */
data class VerifiedCaller(
    val fullName: String,
    val phoneNumber: String,
    val email: String,
)
