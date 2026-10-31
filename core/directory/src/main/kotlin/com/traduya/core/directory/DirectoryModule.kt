package com.traduya.core.directory

import com.traduya.core.directory.callers.FirebaseCallerVerifier
import com.traduya.core.domain.internetcall.CallerVerifier
import com.traduya.core.domain.wifinumber.WifiNumberRegistry
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DirectoryModule {

    @Binds
    abstract fun bindWifiNumberRegistry(impl: FirebaseWifiNumberRegistry): WifiNumberRegistry

    @Binds
    abstract fun bindCallerVerifier(impl: FirebaseCallerVerifier): CallerVerifier
}
