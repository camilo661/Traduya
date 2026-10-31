package com.traduya.core.callcapture;

/** Servicio que corre con permisos de shell (vía Shizuku) y graba el audio de la llamada. */
interface ICallAudioService {

    /** Código reservado por Shizuku para cerrar el proceso. */
    void destroy() = 16777114;

    /**
     * Empieza a grabar la fuente indicada (MediaRecorder.AudioSource) a 16 kHz junto con
     * VOICE_UPLINK (el usuario). Con muteUser, la fuente mezcla ambos lados y se silencia
     * mientras habla el usuario.
     * Devuelve el extremo de lectura de una tubería con PCM 16 bits little-endian en dos canales:
     * [voz de la otra persona, llamada completa].
     */
    ParcelFileDescriptor start(int source, boolean muteUser) = 1;

    void stop() = 2;
}
