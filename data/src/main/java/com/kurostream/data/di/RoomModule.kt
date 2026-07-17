package com.kurostream.data.di

import android.content.Context
import com.kurostream.data.local.dao.*
import com.kurostream.data.local.database.KuroStreamDatabase
import com.kurostream.data.local.preferences.SettingsDataStore
import com.kurostream.data.local.preferences.SettingsDataStoreImpl
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object RoomModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStoreImpl(context)
    }

    @Provides
    @Singleton
    fun provideProfileDao(database: KuroStreamDatabase): ProfileDao {
        return database.profileDao()
    }

    @Provides
    @Singleton
    fun provideWatchHistoryDao(database: KuroStreamDatabase): WatchHistoryDao {
        return database.watchHistoryDao()
    }

    @Provides
    @Singleton
    fun provideFavoriteDao(database: KuroStreamDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    @Singleton
    fun provideDownloadItemDao(database: KuroStreamDatabase): DownloadItemDao {
        return database.downloadItemDao()
    }
}