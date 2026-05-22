package com.kurostream.core.di

import android.content.Context
import androidx.room.Room
import com.kurostream.data.source.local.KuroDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KuroDatabase {
        return Room.databaseBuilder(
            context,
            KuroDatabase::class.java,
            "kuro_stream.db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideContentDao(db: KuroDatabase) = db.contentDao()

    @Provides
    fun providePluginDao(db: KuroDatabase) = db.pluginDao()

    @Provides
    fun provideHistoryDao(db: KuroDatabase) = db.historyDao()
}
