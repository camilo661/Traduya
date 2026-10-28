package com.traduya.core.data.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "calls")
data class CallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val direction: String,
    val startedAt: Long,
    val durationSeconds: Long = 0,
    val detectedLanguage: String? = null,
    val recordingPath: String? = null,
)

@Entity(
    tableName = "transcript_lines",
    foreignKeys = [
        ForeignKey(
            entity = CallEntity::class,
            parentColumns = ["id"],
            childColumns = ["callId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("callId")],
)
data class TranscriptLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callId: Long,
    val timestamp: Long,
    val speaker: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val originalText: String,
    val translatedText: String,
)

/** Llamada junto con cuántas frases se tradujeron. */
data class CallWithLineCount(
    @Embedded val call: CallEntity,
    @ColumnInfo(name = "lineCount") val lineCount: Int,
)
