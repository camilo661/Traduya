package com.traduya.core.directory

import android.content.Context
import com.traduya.core.directory.auth.AnonymousSession
import com.traduya.core.directory.callers.FirebaseCallerVerifier
import com.traduya.core.directory.internet.FirebaseInternetDirectory
import com.traduya.core.domain.internetcall.CallerVerifier
import com.traduya.core.domain.internetcall.InternetDirectory
import com.traduya.core.domain.wifinumber.WifiNumberRegistry
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DirectoryModule {

    @Binds
    abstract fun bindWifiNumberRegistry(impl: FirebaseWifiNumberRegistry): WifiNumberRegistry

    @Binds
    abstract fun bindCallerVerifier(impl: FirebaseCallerVerifier): CallerVerifier

    @Binds
    abstract fun bindInternetDirectory(impl: FirebaseInternetDirectory): InternetDirectory

    companion object {
        /** Preferencias donde se guardan la sesión anónima y el número registrado. */
        const val PREFS_NAME = "wifi_number"

        @Provides
        @Singleton
        fun provideFirebaseConfig(): FirebaseConfig = FirebaseConfig.fromBuildConfig()

        /** Una sola sesión anónima para todo el teléfono: dueña del número y lectora de fichas. */
        @Provides
        @Singleton
        fun provideAnonymousSession(@ApplicationContext context: Context, config: FirebaseConfig): AnonymousSession =
            AnonymousSession(config, context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    }
}
