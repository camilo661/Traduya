package com.traduya.core.model

enum class CallDirection { INCOMING, OUTGOING }

enum class CallStatus { DIALING, RINGING, ACTIVE, HOLDING, DISCONNECTED }

/** Instantánea de la llamada que está ocurriendo ahora mismo. */
data class ActiveCall(
    val id: String,
    val number: String,
    val displayName: String?,
    val direction: CallDirection,
    val status: CallStatus,
    val connectedAtMillis: Long?,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
) {
    val label: String get() = displayName?.takeIf { it.isNotBlank() } ?: number.ifBlank { "Número privado" }
}
