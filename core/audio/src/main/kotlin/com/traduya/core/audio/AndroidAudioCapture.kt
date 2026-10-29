package com.traduya.core.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.traduya.core.domain.audio.AudioCapture
import com.traduya.core.model.AudioChunk
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.inject.Inject

/**
 * Lee el micrófono en trozos de 100 ms.
 *
 * Durante una llamada algunos teléfonos silencian ciertas fuentes de audio para otras apps:
 * si una fuente solo entrega ceros, se prueba la siguiente. Si todas fallan,
 * los trozos llegan marcados como [AudioChunk.blocked] para avisar al usuario.
 */
internal class AndroidAudioCapture @Inject constructor(
    @ApplicationContext private val context: Context,
) : AudioCapture {

    // Sin cancelación de eco: queremos oír también la voz que sale por el altavoz
    private val sources = listOf(
        MediaRecorder.AudioSource.MIC,
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        MediaRecorder.AudioSource.CAMCORDER,
        MediaRecorder.AudioSource.UNPROCESSED,
    )

    @SuppressLint("MissingPermission")
    override fun chunks(): Flow<AudioChunk> = flow {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            error("Falta el permiso de micrófono")
        }

        var sourceIndex = 0
        while (currentCoroutineContext().isActive) {
            val record = open(sources[sourceIndex])
            if (record == null) {
                if (sourceIndex == sources.lastIndex) error("No se pudo abrir el micrófono")
                sourceIndex++
                continue
            }
            val isLastSource = sourceIndex == sources.lastIndex
            var silentMillis = 0
            try {
                record.startRecording()
                val buffer = ShortArray(CHUNK_SAMPLES)
                while (currentCoroutineContext().isActive) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read < 0) break
                    if (read == 0) continue
                    val samples = buffer.copyOf(read)
                    silentMillis = if (samples.all { it == ZERO }) silentMillis + CHUNK_MILLIS else 0
                    val blocked = silentMillis >= BLOCKED_AFTER_MILLIS
                    emit(AudioChunk(samples, AudioChunk.levelOf(samples), blocked && isLastSource))
                    if (blocked && !isLastSource) break
                }
            } finally {
                runCatching { record.stop() }
                record.release()
            }
            if (!isLastSource) sourceIndex++
        }
    }.flowOn(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    private fun open(source: Int): AudioRecord? = runCatching {
        val minBuffer = AudioRecord.getMinBufferSize(
            AudioChunk.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        AudioRecord(
            source,
            AudioChunk.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, CHUNK_SAMPLES * 2 * 4),
        ).takeIf { it.state == AudioRecord.STATE_INITIALIZED }
    }.getOrNull()

    private companion object {
        const val CHUNK_MILLIS = 100
        const val CHUNK_SAMPLES = AudioChunk.SAMPLE_RATE * CHUNK_MILLIS / 1000
        const val BLOCKED_AFTER_MILLIS = 3_000
        const val ZERO: Short = 0
    }
}
