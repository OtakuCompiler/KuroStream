package com.kurostream.tv.di

import android.content.Context
import androidx.room.Room
import com.kurostream.tv.data.local.database.CachedAnimeDao
import com.kurostream.tv.data.local.database.FavoriteDao
import com.kurostream.tv.data.local.database.KuroDatabase
import com.kurostream.tv.data.local.database.WatchHistoryDao
import com.kurostream.tv.data.local.database.WatchProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): KuroDatabase {
        return Room.databaseBuilder(
            context,
            KuroDatabase::class.java,
            "kurostream_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideWatchHistoryDao(database: KuroDatabase): WatchHistoryDao {
        return database.watchHistoryDao()
    }
    
    @Provides
    fun provideFavoriteDao(database: KuroDatabase): FavoriteDao {
        return database.favoriteDao()
    }
    
    @Provides
    fun provideCachedAnimeDao(database: KuroDatabase): CachedAnimeDao {
        return database.cachedAnimeDao()
    }
    
    @Provides
    fun provideWatchProgressDao(database: KuroDatabase): WatchProgressDao {
        return database.watchProgressDao()
    }
}
