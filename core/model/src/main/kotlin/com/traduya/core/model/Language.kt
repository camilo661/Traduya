package com.traduya.core.model

/**
 * Idiomas que Traduya sabe reconocer y traducir.
 * El español es siempre el idioma del usuario; los demás son los de la otra persona.
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
        val foreignLanguages: List<Language> = listOf(ENGLISH, CHINESE)

        /** Acepta códigos como "en", "en-US", "zh-Hans" o "zh-Latn". */
        fun fromCode(code: String?): Language? {
            val normalized = code?.trim()?.lowercase() ?: return null
            return entries.firstOrNull { normalized.startsWith(it.code) }
        }
    }
}
