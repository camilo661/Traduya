package com.traduya.core.domain.repository

import com.traduya.core.model.CallDirection
import com.traduya.core.model.CallRecord
import com.traduya.core.model.Language
import com.traduya.core.model.TranscriptLine
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeCalls(): Flow<List<CallRecord>>
    fun observeCall(callId: Long): Flow<CallRecord?>
    fun observeTranscript(callId: Long): Flow<List<TranscriptLine>>

    /** Crea el registro de la llamada y devuelve su id. */
    suspend fun startCall(number: String, direction: CallDirection, startedAtMillis: Long): Long
    suspend fun finishCall(
        callId: Long,
        durationSeconds: Long,
        detectedLanguage: Language?,
        recordingPath: String?,
    )
    suspend fun addLine(line: TranscriptLine)
    /** Borra la llamada, su transcripción y su grabación. */
    suspend fun deleteCall(callId: Long)
    suspend fun clearAll()
}
