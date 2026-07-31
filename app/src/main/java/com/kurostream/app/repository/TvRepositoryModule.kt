package com.kurostream.app.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TvRepositoryModule {

    @Binds
    abstract fun bindMediaRepository(
        impl: MediaRepositoryBridge
    ): TvRepositories.MediaRepository

    @Binds
    abstract fun bindWatchProgressRepository(
        impl: WatchProgressRepositoryBridge
    ): TvRepositories.WatchProgressRepository

    @Binds
    abstract fun bindFavoritesRepository(
        impl: FavoritesRepositoryBridge
    ): TvRepositories.FavoritesRepository
}
