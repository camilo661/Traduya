package com.traduya.core.domain.wifinumber

/** Reglas para los números de teléfono del directorio de llamadas por WiFi. */
object PhoneNumberRules {

    private val E164 = Regex("^\\+[1-9][0-9]{7,14}$")
    private val SEPARATORS = Regex("[\\s().\\-]")

    /** Prefijos de los países más habituales, para rellenar el campo al empezar. */
    private val CALLING_CODES = mapOf(
        "co" to "+57", "mx" to "+52", "us" to "+1", "ca" to "+1", "es" to "+34",
        "ar" to "+54", "cl" to "+56", "pe" to "+51", "ec" to "+593", "ve" to "+58",
        "cn" to "+86", "gb" to "+44",
    )

    /**
     * Convierte lo que escribe el usuario al formato internacional E.164.
     * "+57 300 123-4567" o "0057 3001234567" → "+573001234567". Devuelve null si no es válido.
     */
    fun normalize(raw: String): String? {
        val compact = raw.trim().replace(SEPARATORS, "")
        val international = if (compact.startsWith("00")) "+" + compact.drop(2) else compact
        return international.takeIf { E164.matches(it) }
    }

    /** Prefijo internacional del país de la SIM ("co" → "+57"), o null si no se conoce. */
    fun callingCodeFor(countryIso: String?): String? = countryIso?.lowercase()?.let(CALLING_CODES::get)
}
