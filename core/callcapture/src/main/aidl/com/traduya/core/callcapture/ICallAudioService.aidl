package com.traduya.core.callcapture;

/** Servicio que corre con permisos de shell (vía Shizuku) y graba el audio de la llamada. */
interface ICallAudioService {

    /** Código reservado por Shizuku para cerrar el proceso. */
    void destroy() = 16777114;

    /**
     * Empieza a grabar la fuente indicada (MediaRecorder.AudioSource) a 16 kHz mono.
     * Devuelve el extremo de lectura de una tubería con PCM 16 bits little-endian.
     */
    ParcelFileDescriptor start(int source) = 1;

    void stop() = 2;
}
