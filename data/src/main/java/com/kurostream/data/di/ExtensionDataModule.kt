package com.kurostream.data.di

import com.kurostream.data.local.dao.ExtensionDao
import com.kurostream.data.local.database.KuroStreamDatabase
import com.kurostream.data.repository.ExtensionRepositoryImpl
import com.kurostream.domain.extension.ExtensionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExtensionDataBindsModule {
    @Binds
    @Singleton
    abstract fun bindExtensionRepository(impl: ExtensionRepositoryImpl): ExtensionRepository
}

@Module
@InstallIn(SingletonComponent::class)
object ExtensionDataProvidesModule {
    @Provides
    @Singleton
    fun provideExtensionDao(database: KuroStreamDatabase): ExtensionDao = database.extensionDao()
}
