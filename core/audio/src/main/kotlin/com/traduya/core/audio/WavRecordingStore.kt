package com.traduya.core.audio

import android.content.Context
import com.traduya.core.domain.audio.CallRecordingStore
import com.traduya.core.domain.audio.RecordingWriter
import com.traduya.core.model.AudioChunk
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/** Guarda cada llamada como WAV (PCM 16 kHz mono) en el almacenamiento privado de la app. */
internal class WavRecordingStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : CallRecordingStore {

    override fun create(callId: Long): RecordingWriter {
        val dir = File(context.filesDir, "recordings").apply { mkdirs() }
        return WavWriter(File(dir, "llamada_$callId.wav"))
    }
}

internal class WavWriter(private val file: File) : RecordingWriter {

    private val output = BufferedOutputStream(FileOutputStream(file))
    private var dataBytes = 0L
    private var finished = false

    init {
        output.write(ByteArray(HEADER_SIZE)) // se rellena al terminar, cuando se conoce el tamaño
    }

    @Synchronized
    override fun write(samples: ShortArray) {
        if (finished) return
        val bytes = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { bytes.putShort(it) }
        output.write(bytes.array())
        dataBytes += samples.size * 2
    }

    @Synchronized
    override fun finish(): String? {
        if (finished) return file.takeIf { it.exists() }?.absolutePath
        finished = true
        output.close()
        if (dataBytes == 0L) {
            file.delete()
            return null
        }
        RandomAccessFile(file, "rw").use { it.write(header(dataBytes)) }
        return file.absolutePath
    }

    private fun header(dataSize: Long): ByteArray {
        val sampleRate = AudioChunk.SAMPLE_RATE
        val byteRate = sampleRate * CHANNELS * BITS / 8
        return ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt((dataSize + HEADER_SIZE - 8).toInt())
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1) // PCM
            putShort(CHANNELS.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort((CHANNELS * BITS / 8).toShort())
            putShort(BITS.toShort())
            put("data".toByteArray())
            putInt(dataSize.toInt())
        }.array()
    }

    private companion object {
        const val HEADER_SIZE = 44
        const val CHANNELS = 1
        const val BITS = 16
    }
}
