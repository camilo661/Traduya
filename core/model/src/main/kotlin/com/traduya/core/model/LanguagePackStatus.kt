package com.traduya.core.model

/** Estado de descarga del paquete de un idioma (voz + traducción). */
sealed interface LanguagePackStatus {
    data object Missing : LanguagePackStatus
    data class Downloading(val progress: Float) : LanguagePackStatus
    data object Ready : LanguagePackStatus
    data object Failed : LanguagePackStatus
}
