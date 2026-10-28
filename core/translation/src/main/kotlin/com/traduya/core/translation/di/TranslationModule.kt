package com.traduya.core.translation.di

import com.traduya.core.domain.translation.LanguagePacks
import com.traduya.core.domain.translation.SpeechTranscriber
import com.traduya.core.domain.translation.TranslationEngine
import com.traduya.core.translation.LanguagePacksImpl
import com.traduya.core.translation.mlkit.MlKitTranslationEngine
import com.traduya.core.translation.vosk.VoskSpeechTranscriber
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
    abstract fun bindSpeechTranscriber(impl: VoskSpeechTranscriber): SpeechTranscriber

    @Binds
    abstract fun bindLanguagePacks(impl: LanguagePacksImpl): LanguagePacks
}
