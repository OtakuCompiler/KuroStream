package com.kurostream.common.di

import com.kurostream.core.common.dispatcher.DefaultDispatcherProvider
import com.kurostream.core.common.dispatcher.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommonModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(defaultDispatcherProvider: DefaultDispatcherProvider): DispatcherProvider {
        return defaultDispatcherProvider
    }
}