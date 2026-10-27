package com.traduya.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.traduya.core.data.database.CallDao
import com.traduya.core.data.database.TraduyaDatabase
import com.traduya.core.data.repository.DataStoreSettingsRepository
import com.traduya.core.data.repository.RoomHistoryRepository
import com.traduya.core.domain.repository.HistoryRepository
import com.traduya.core.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: RoomHistoryRepository): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): TraduyaDatabase =
            Room.databaseBuilder(context, TraduyaDatabase::class.java, TraduyaDatabase.NAME).build()

        @Provides
        fun provideCallDao(database: TraduyaDatabase): CallDao = database.callDao()

        @Provides
        @Singleton
        fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.settingsDataStore
    }
}
