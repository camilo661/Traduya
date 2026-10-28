package com.traduya.core.data.database

import com.traduya.core.model.CallDirection
import com.traduya.core.model.CallRecord
import com.traduya.core.model.Language
import com.traduya.core.model.Speaker
import com.traduya.core.model.TranscriptLine

internal fun CallWithLineCount.toModel() = CallRecord(
    id = call.id,
    number = call.number,
    direction = CallDirection.valueOf(call.direction),
    startedAtMillis = call.startedAt,
    durationSeconds = call.durationSeconds,
    detectedLanguage = call.detectedLanguage?.let(Language::fromCode),
    lineCount = lineCount,
    recordingPath = call.recordingPath,
)

internal fun TranscriptLineEntity.toModel() = TranscriptLine(
    id = id,
    callId = callId,
    timestampMillis = timestamp,
    speaker = Speaker.valueOf(speaker),
    sourceLanguage = Language.fromCode(sourceLanguage) ?: Language.ENGLISH,
    targetLanguage = Language.fromCode(targetLanguage) ?: Language.SPANISH,
    originalText = originalText,
    translatedText = translatedText,
)

internal fun TranscriptLine.toEntity() = TranscriptLineEntity(
    callId = callId,
    timestamp = timestampMillis,
    speaker = speaker.name,
    sourceLanguage = sourceLanguage.code,
    targetLanguage = targetLanguage.code,
    originalText = originalText,
    translatedText = translatedText,
)
