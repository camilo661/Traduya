package com.traduya.core.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.traduya.core.domain.translation.LanguagePacks
import com.traduya.core.model.Language
import com.traduya.core.translation.mlkit.mlKitCode
import com.traduya.core.translation.vosk.VoskModelStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Un paquete = modelo de voz (Vosk) + modelo de traducción (ML Kit) del mismo idioma. */
internal class LanguagePacksImpl @Inject constructor(
    private val voskModels: VoskModelStore,
) : LanguagePacks {

    private val translationModels = RemoteModelManager.getInstance()

    override suspend fun installed(): Set<Language> {
        val translation = runCatching {
            translationModels.getDownloadedModels(TranslateRemoteModel::class.java).await()
                .mapNotNull { Language.fromCode(it.language) }
                .toSet()
        }.getOrDefault(emptySet())
        return Language.entries.filter { it in translation && voskModels.isInstalled(it) }.toSet()
    }

    override fun install(language: Language): Flow<Float> = callbackFlow {
        val job = launch {
            voskModels.download(language) { trySend(it * VOICE_SHARE) }
            val model = TranslateRemoteModel.Builder(language.mlKitCode).build()
            translationModels.download(model, DownloadConditions.Builder().build()).await()
            send(1f)
            close()
        }
        job.invokeOnCompletion { cause -> if (cause != null) close(cause) }
        awaitClose { job.cancel() }
    }

    private companion object {
        const val VOICE_SHARE = 0.85f
    }
}
