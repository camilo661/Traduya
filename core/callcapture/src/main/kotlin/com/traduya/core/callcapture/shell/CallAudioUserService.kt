package com.traduya.core.callcapture.shell

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.traduya.core.callcapture.ICallAudioService
import java.io.IOException
import java.io.OutputStream
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * Corre en un proceso aparte con el uid de shell, iniciado por Shizuku.
 *
 * Shell tiene el permiso CAPTURE_AUDIO_OUTPUT, así que Android no lo silencia durante
 * las llamadas y puede leer la línea telefónica (VOICE_DOWNLINK = solo la otra persona).
 * La voz del usuario nunca se envía: o la fuente no la contiene, o se silencia con [UplinkGate].
 * El audio viaja a la app por una tubería, en PCM 16 bits little-endian a 16 kHz.
 */
class CallAudioUserService() : ICallAudioService.Stub() {

    private var baseContext: Context? = null
    private var worker: Thread? = null

    @Volatile
    private var running = false

    /** Shizuku 13+ usa este constructor y entrega un contexto del paquete de la app. */
    constructor(context: Context) : this() {
        baseContext = context
    }

    override fun destroy() {
        stop()
        exitProcess(0)
    }

    @Synchronized
    override fun start(source: Int, muteUser: Boolean): ParcelFileDescriptor {
        stop()
        val record = startRecord(source)
        val userLine = if (muteUser) {
            runCatching { startRecord(MediaRecorder.AudioSource.VOICE_UPLINK) }
                .onFailure { record.stop(); record.release() }
                .getOrThrow()
        } else {
            null
        }
        val (readSide, writeSide) = ParcelFileDescriptor.createPipe()
        running = true
        worker = thread(name = "traduya-call-audio") {
            pump(record, userLine, ParcelFileDescriptor.AutoCloseOutputStream(writeSide))
        }
        return readSide
    }

    @Synchronized
    override fun stop() {
        running = false
        worker?.join(STOP_TIMEOUT_MILLIS)
        worker = null
    }

    private fun startRecord(source: Int): AudioRecord {
        val record = open(source) ?: throw IllegalStateException("Fuente de audio $source no disponible")
        record.startRecording()
        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            record.release()
            throw IllegalStateException("La fuente de audio $source no arrancó")
        }
        Log.i(TAG, "Grabando la fuente $source")
        return record
    }

    private fun pump(record: AudioRecord, userLine: AudioRecord?, output: OutputStream) {
        val samples = ShortArray(CHUNK_SAMPLES)
        val userSamples = ShortArray(CHUNK_SAMPLES)
        val bytes = ByteArray(CHUNK_SAMPLES * 2)
        val gate = UplinkGate()
        try {
            output.use {
                while (running) {
                    val read = record.read(samples, 0, samples.size)
                    if (read < 0) break
                    if (userLine != null) {
                        val userRead = userLine.read(userSamples, 0, read)
                        if (userRead < 0) break
                        gate.apply(samples, read, userSamples, userRead)
                    }
                    for (i in 0 until read) {
                        bytes[i * 2] = samples[i].toInt().toByte()
                        bytes[i * 2 + 1] = (samples[i].toInt() shr 8).toByte()
                    }
                    it.write(bytes, 0, read * 2)
                }
            }
        } catch (e: IOException) {
            // La app cerró la tubería: la llamada terminó
        } finally {
            running = false
            listOfNotNull(record, userLine).forEach {
                runCatching { it.stop() }
                it.release()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun open(source: Int): AudioRecord? = runCatching {
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val builder = AudioRecord.Builder()
            .setAudioSource(source)
            .setAudioFormat(format)
            .setBufferSizeInBytes(maxOf(minBuffer, CHUNK_SAMPLES * 2 * 4))
        val context = baseContext ?: systemContext()
        if (Build.VERSION.SDK_INT >= 31 && context != null) builder.setContext(ShellContext(context))
        builder.build().takeIf { it.state == AudioRecord.STATE_INITIALIZED }
    }.onFailure { Log.w(TAG, "No se pudo abrir la fuente $source", it) }.getOrNull()

    /** Respaldo para versiones de Shizuku que no entregan contexto. */
    @SuppressLint("PrivateApi")
    private fun systemContext(): Context? = runCatching {
        val activityThread = Class.forName("android.app.ActivityThread")
        val current = activityThread.getMethod("currentActivityThread").invoke(null)
            ?: activityThread.getMethod("systemMain").invoke(null)
        activityThread.getMethod("getSystemContext").invoke(current) as Context
    }.getOrNull()

    private companion object {
        const val TAG = "TraduyaCallAudio"
        const val SAMPLE_RATE = 16_000
        const val CHUNK_SAMPLES = SAMPLE_RATE / 10
        const val STOP_TIMEOUT_MILLIS = 500L
    }
}
