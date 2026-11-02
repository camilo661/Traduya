package com.traduya.core.model

/**
 * Un número del directorio de Traduya, visto por quien quiere llamarlo.
 *
 * @param peerId identificador de la conexión WebRTC de ese número.
 * @param isActive tiene la pantalla de espera (app) o la página (web) abierta ahora mismo.
 */
data class DirectoryEntry(
    val peerId: String,
    val isActive: Boolean,
)
