package com.traduya.core.data.repository

import com.traduya.core.data.database.CallDao
import com.traduya.core.data.database.CallEntity
import com.traduya.core.data.database.toEntity
import com.traduya.core.data.database.toModel
import com.traduya.core.domain.repository.HistoryRepository
import com.traduya.core.model.CallDirection
import com.traduya.core.model.CallRecord
import com.traduya.core.model.Language
import com.traduya.core.model.TranscriptLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

internal class RoomHistoryRepository @Inject constructor(
    private val dao: CallDao,
) : HistoryRepository {

    override fun observeCalls(): Flow<List<CallRecord>> =
        dao.observeCalls().map { calls -> calls.map { it.toModel() } }

    override fun observeCall(callId: Long): Flow<CallRecord?> =
        dao.observeCall(callId).map { it?.toModel() }

    override fun observeTranscript(callId: Long): Flow<List<TranscriptLine>> =
        dao.observeTranscript(callId).map { lines -> lines.map { it.toModel() } }

    override suspend fun startCall(number: String, direction: CallDirection, startedAtMillis: Long): Long =
        dao.insertCall(CallEntity(number = number, direction = direction.name, startedAt = startedAtMillis))

    override suspend fun finishCall(
        callId: Long,
        durationSeconds: Long,
        detectedLanguage: Language?,
        recordingPath: String?,
    ) = dao.finishCall(callId, durationSeconds, detectedLanguage?.code, recordingPath)

    override suspend fun addLine(line: TranscriptLine) = dao.insertLine(line.toEntity())

    override suspend fun deleteCall(callId: Long) {
        deleteFiles(listOfNotNull(dao.recordingPath(callId)))
        dao.deleteCall(callId)
    }

    override suspend fun clearAll() {
        deleteFiles(dao.allRecordingPaths())
        dao.deleteAll()
    }

    private suspend fun deleteFiles(paths: List<String>) = withContext(Dispatchers.IO) {
        paths.forEach { File(it).delete() }
    }
}
