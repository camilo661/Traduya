package com.traduya.core.domain.audio

/** Guarda la grabación de cada llamada en el teléfono. */
interface CallRecordingStore {
    fun create(callId: Long): RecordingWriter
}

interface RecordingWriter {
    fun write(samples: ShortArray)

    /** Cierra el archivo y devuelve su ruta, o null si no se grabó nada. */
    fun finish(): String?
}
