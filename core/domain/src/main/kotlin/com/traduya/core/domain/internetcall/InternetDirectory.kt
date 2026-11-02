package com.traduya.core.domain.internetcall

import com.traduya.core.model.DirectoryEntry

/**
 * Directorio de números para las llamadas por internet, desde la app:
 * buscar a quién llamar, identificarse ante quien recibe y avisar que este teléfono está activo.
 * Para usarlo, este teléfono debe tener su número registrado (Ajustes › Llamadas por WiFi).
 */
interface InternetDirectory {
    /** El número en el directorio, o null si no está en Traduya. Lanza IOException si no hay conexión. */
    suspend fun lookup(phoneNumber: String): DirectoryEntry?

    /** Token de la sesión de este teléfono: viaja con la llamada para que quien recibe sepa quién llama. */
    suspend fun sessionToken(): String

    /** Renueva la marca de "activo" del número de este teléfono (luz verde para quien llama). */
    suspend fun markActive()
}
