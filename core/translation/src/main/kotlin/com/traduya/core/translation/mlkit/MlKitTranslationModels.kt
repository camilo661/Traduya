package com.traduya.core.translation.mlkit

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.traduya.core.domain.translation.TranslationModels
import com.traduya.core.model.Language
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class MlKitTranslationModels @Inject constructor() : TranslationModels {

    private val modelManager = RemoteModelManager.getInstance()

    override suspend fun downloadedLanguages(): Set<Language> =
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java).await()
            .mapNotNull { Language.fromCode(it.language) }
            .toSet()

    override suspend fun download(language: Language) {
        val model = TranslateRemoteModel.Builder(language.mlKitCode).build()
        modelManager.download(model, DownloadConditions.Builder().build()).await()
    }
}
