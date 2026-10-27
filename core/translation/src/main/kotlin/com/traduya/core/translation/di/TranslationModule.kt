package com.traduya.core.translation.di

import com.traduya.core.domain.translation.LanguageDetector
import com.traduya.core.domain.translation.SpeechTranscriber
import com.traduya.core.domain.translation.TranslationEngine
import com.traduya.core.domain.translation.TranslationModels
import com.traduya.core.translation.mlkit.MlKitLanguageDetector
import com.traduya.core.translation.mlkit.MlKitTranslationEngine
import com.traduya.core.translation.mlkit.MlKitTranslationModels
import com.traduya.core.translation.speech.AndroidSpeechTranscriber
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class TranslationModule {

    @Binds
    abstract fun bindTranslationEngine(impl: MlKitTranslationEngine): TranslationEngine

    @Binds
    abstract fun bindLanguageDetector(impl: MlKitLanguageDetector): LanguageDetector

    @Binds
    abstract fun bindTranslationModels(impl: MlKitTranslationModels): TranslationModels

    @Binds
    abstract fun bindSpeechTranscriber(impl: AndroidSpeechTranscriber): SpeechTranscriber
}
