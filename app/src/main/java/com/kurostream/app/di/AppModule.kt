package com.kurostream.app.di

import android.content.Context
import com.kurostream.app.repository.TvRepositories
import com.kurostream.app.repository.MediaRepositoryBridge
import com.kurostream.app.repository.WatchProgressRepositoryBridge
import com.kurostream.app.repository.FavoritesRepositoryBridge
import com.kurostream.app.repository.SettingsRepositoryAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideMediaRepository(
        impl: MediaRepositoryBridge
    ): TvRepositories.MediaRepository = impl

    @Provides
    @Singleton
    fun provideWatchProgressRepository(
        impl: WatchProgressRepositoryBridge
    ): TvRepositories.WatchProgressRepository = impl
    @Provides
    @Singleton
    fun provideSettingsRepositoryAdapter(
        impl: SettingsRepositoryAdapter
    ): SettingsRepositoryAdapter = impl

    @Provides
    @Singleton
    fun provideFavoritesRepository(
        impl: FavoritesRepositoryBridge
    ): TvRepositories.FavoritesRepository = impl
}
