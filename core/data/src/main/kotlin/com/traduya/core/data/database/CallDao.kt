package com.traduya.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {

    @Query(
        """
        SELECT calls.*, (SELECT COUNT(*) FROM transcript_lines WHERE callId = calls.id) AS lineCount
        FROM calls ORDER BY startedAt DESC
        """,
    )
    fun observeCalls(): Flow<List<CallWithLineCount>>

    @Query(
        """
        SELECT calls.*, (SELECT COUNT(*) FROM transcript_lines WHERE callId = calls.id) AS lineCount
        FROM calls WHERE id = :callId
        """,
    )
    fun observeCall(callId: Long): Flow<CallWithLineCount?>

    @Query("SELECT * FROM transcript_lines WHERE callId = :callId ORDER BY timestamp ASC")
    fun observeTranscript(callId: Long): Flow<List<TranscriptLineEntity>>

    @Insert
    suspend fun insertCall(call: CallEntity): Long

    @Insert
    suspend fun insertLine(line: TranscriptLineEntity)

    @Query("UPDATE calls SET durationSeconds = :durationSeconds, detectedLanguage = :language WHERE id = :callId")
    suspend fun finishCall(callId: Long, durationSeconds: Long, language: String?)

    @Query("DELETE FROM calls WHERE id = :callId")
    suspend fun deleteCall(callId: Long)

    @Query("DELETE FROM calls")
    suspend fun deleteAll()
}
