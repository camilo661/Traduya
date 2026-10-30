package com.traduya.core.callcapture;

/** Servicio que corre con permisos de shell (vía Shizuku) y graba el audio de la llamada. */
interface ICallAudioService {

    /** Código reservado por Shizuku para cerrar el proceso. */
    void destroy() = 16777114;

    /**
     * Empieza a grabar la fuente indicada (MediaRecorder.AudioSource) a 16 kHz mono.
     * Con muteUser, graba también VOICE_UPLINK y silencia la fuente mientras habla el usuario,
     * para fuentes que mezclan ambos lados de la llamada.
     * Devuelve el extremo de lectura de una tubería con PCM 16 bits little-endian.
     */
    ParcelFileDescriptor start(int source, boolean muteUser) = 1;

    void stop() = 2;
}
