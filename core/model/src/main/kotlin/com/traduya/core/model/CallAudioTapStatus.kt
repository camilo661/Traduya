package com.traduya.core.model

/**
 * Estado del acceso directo al audio de las llamadas telefónicas.
 *
 * Android silencia el micrófono de las apps durante una llamada normal. Con Shizuku,
 * Traduya escucha la línea desde un proceso con permisos de sistema y no hace falta el altavoz.
 */
enum class CallAudioTapStatus {
    /** Shizuku no está instalado en el teléfono. */
    NOT_INSTALLED,

    /** Shizuku está instalado pero su servicio no está iniciado (se apaga al reiniciar). */
    NOT_RUNNING,

    /** Falta que el usuario permita a Traduya usar Shizuku. */
    PERMISSION_NEEDED,

    /** Todo listo: las llamadas normales se traducen. */
    READY,
}
