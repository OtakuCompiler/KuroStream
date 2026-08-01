package com.kurostream.data.di

import android.content.Context
import androidx.room.RoomDatabase
import com.kurostream.data.local.dao.*
import com.kurostream.data.local.database.KuroStreamDatabase
import com.kurostream.data.local.database.MIGRATION_4_5
import com.kurostream.data.local.database.Migration_2_3
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
    fun provideDatabase(@ApplicationContext context: Context): KuroStreamDatabase {
        return androidx.room.Room.databaseBuilder(
            context,
            KuroStreamDatabase::class.java,
            "kurostream.db"
        )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(Migration_2_3, MIGRATION_4_5)
            .build()
    }

    @Provides
    @Singleton
    fun provideMediaItemDao(database: KuroStreamDatabase): MediaItemDao {
        return database.mediaItemDao()
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
    fun provideSourceLockDao(database: KuroStreamDatabase): SourceLockDao {
        return database.sourceLockDao()
    }

    @Provides
    @Singleton
    fun provideHomeRowDao(database: KuroStreamDatabase): HomeRowDao {
        return database.homeRowDao()
    }

    @Provides
    @Singleton
    fun provideBookmarkDao(database: KuroStreamDatabase): BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    @Singleton
    fun provideAddonDao(database: KuroStreamDatabase): AddonDao {
        return database.addonDao()
    }

    @Provides
    @Singleton
    fun providePurchaseDao(database: KuroStreamDatabase): PurchaseDao {
        return database.purchaseDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStoreImpl(context)
    }
}