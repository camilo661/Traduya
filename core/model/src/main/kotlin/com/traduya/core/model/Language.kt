package com.traduya.core.model

/**
 * Idiomas que Traduya sabe reconocer y traducir.
 * El usuario elige el suyo en Ajustes (por defecto, español); los demás son los posibles de la otra persona.
 */
enum class Language(
    val code: String,
    val localeTag: String,
    val displayName: String,
    val flag: String,
) {
    SPANISH(code = "es", localeTag = "es-ES", displayName = "Español", flag = "🇪🇸"),
    ENGLISH(code = "en", localeTag = "en-US", displayName = "Inglés", flag = "🇺🇸"),
    CHINESE(code = "zh", localeTag = "zh-CN", displayName = "Chino", flag = "🇨🇳");

    companion object {
        /** Idiomas que puede hablar la otra persona si el usuario habla [mine]. */
        fun othersThan(mine: Language): List<Language> = entries - mine

        /** Acepta códigos como "en", "en-US", "zh-Hans" o "zh-Latn". */
        fun fromCode(code: String?): Language? {
            val normalized = code?.trim()?.lowercase() ?: return null
            return entries.firstOrNull { normalized.startsWith(it.code) }
        }
    }
}
