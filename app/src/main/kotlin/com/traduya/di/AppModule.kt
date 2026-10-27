package com.traduya.di

import com.traduya.InCallScreenLauncher
import com.traduya.core.domain.translation.CallTranslationSession
import com.traduya.core.telecom.CallUiLauncher
import com.traduya.session.CallSessionCoordinator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindCallUiLauncher(impl: InCallScreenLauncher): CallUiLauncher

    @Binds
    abstract fun bindCallTranslationSession(impl: CallSessionCoordinator): CallTranslationSession
}
