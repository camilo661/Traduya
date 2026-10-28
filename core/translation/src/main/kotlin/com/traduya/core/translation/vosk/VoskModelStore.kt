package com.traduya.core.translation.vosk

import android.content.Context
import com.traduya.core.model.Language
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Descarga y carga en memoria los modelos de voz de Vosk (uno por idioma, ~40 MB cada uno).
 * Los modelos cargados se reutilizan entre llamadas para empezar a escuchar al instante.
 */
@Singleton
internal class VoskModelStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val root = File(context.filesDir, "vosk")
    private val loaded = mutableMapOf<Language, Model>()
    private val mutex = Mutex()

    init {
        LibVosk.setLogLevel(LogLevel.WARNINGS)
    }

    fun isInstalled(language: Language): Boolean = File(directory(language), READY_MARKER).exists()

    suspend fun load(language: Language): Model? = mutex.withLock {
        loaded[language]?.let { return it }
        if (!isInstalled(language)) return null
        withContext(Dispatchers.IO) { Model(directory(language).absolutePath) }
            .also { loaded[language] = it }
    }

    /** Descarga el ZIP del modelo y lo descomprime. [onProgress] va de 0 a 1. */
    suspend fun download(language: Language, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        if (isInstalled(language)) return@withContext onProgress(1f)
        val zip = File(context.cacheDir, "vosk-${language.code}.zip")
        val connection = URL(MODEL_URLS.getValue(language)).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            val total = connection.contentLengthLong.coerceAtLeast(1)
            connection.inputStream.use { input ->
                zip.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        onProgress((copied.toFloat() / total).coerceAtMost(0.99f))
                    }
                }
            }
            unzip(zip, directory(language))
            onProgress(1f)
        } finally {
            connection.disconnect()
            zip.delete()
        }
    }

    /** El ZIP trae una carpeta raíz con el nombre del modelo: se quita para tener rutas fijas. */
    private fun unzip(zip: File, target: File) {
        target.deleteRecursively()
        target.mkdirs()
        ZipInputStream(zip.inputStream().buffered()).use { stream ->
            generateSequence { stream.nextEntry }.forEach { entry ->
                val relative = entry.name.substringAfter('/', missingDelimiterValue = "")
                if (relative.isEmpty()) return@forEach
                val out = File(target, relative)
                require(out.canonicalPath.startsWith(target.canonicalPath)) { "Ruta inválida en el ZIP" }
                if (entry.isDirectory) out.mkdirs() else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { stream.copyTo(it) }
                }
            }
        }
        File(target, READY_MARKER).createNewFile()
    }

    private fun directory(language: Language) = File(root, language.code)

    private companion object {
        const val READY_MARKER = ".ready"
        const val BASE_URL = "https://alphacephei.com/vosk/models"
        val MODEL_URLS = mapOf(
            Language.ENGLISH to "$BASE_URL/vosk-model-small-en-us-0.15.zip",
            Language.CHINESE to "$BASE_URL/vosk-model-small-cn-0.22.zip",
            Language.SPANISH to "$BASE_URL/vosk-model-small-es-0.42.zip",
        )
    }
}
