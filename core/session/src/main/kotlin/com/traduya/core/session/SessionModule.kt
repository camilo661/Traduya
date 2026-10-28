package com.traduya.core.session

import com.traduya.core.domain.translation.CallTranslationSession
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SessionModule {

    @Binds
    abstract fun bindCallTranslationSession(impl: LiveTranslationSession): CallTranslationSession
}
