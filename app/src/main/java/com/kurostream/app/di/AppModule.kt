package com.kurostream.app.di

import android.content.Context
import com.kurostream.app.repository.TvRepositories
import com.kurostream.app.repository.FavoritesRepositoryBridge
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
    fun provideFavoritesRepository(
        impl: FavoritesRepositoryBridge
    ): TvRepositories.FavoritesRepository = impl
}
