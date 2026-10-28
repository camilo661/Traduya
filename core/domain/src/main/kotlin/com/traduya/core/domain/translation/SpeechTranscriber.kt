package com.traduya.core.domain.translation

import com.traduya.core.model.AudioChunk
import com.traduya.core.model.Language
import com.traduya.core.model.SpeechEvent
import com.traduya.core.model.TranscriptionConfig
import kotlinx.coroutines.flow.Flow

/** Convierte en texto, de forma continua, el audio de la llamada. */
interface SpeechTranscriber {

    /** Carga los modelos con antelación para que la primera frase salga sin espera. */
    suspend fun prepare(languages: List<Language>)

    fun transcribe(audio: Flow<AudioChunk>, config: TranscriptionConfig): Flow<SpeechEvent>
}
