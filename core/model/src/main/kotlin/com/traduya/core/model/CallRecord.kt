package com.traduya.core.model

/** Una llamada guardada en el historial. */
data class CallRecord(
    val id: Long,
    val number: String,
    val direction: CallDirection,
    val startedAtMillis: Long,
    val durationSeconds: Long,
    val detectedLanguage: Language?,
    val lineCount: Int,
    val recordingPath: String? = null,
)
