// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.data.di

import android.content.Context
import com.kurostream.data.cache.CacheManager
import com.kurostream.data.cache.CacheManagerImpl
// import com.kurostream.data.home.CustomHomeRowRepository
// import com.kurostream.data.home.CustomHomeRowRepositoryImpl
import com.kurostream.data.local.dao.*
import com.kurostream.data.local.preferences.SettingsDataStore
import com.kurostream.data.local.preferences.SettingsDataStoreImpl
import com.kurostream.data.metadata.*
import com.kurostream.data.network.NetworkMonitorRepository
import com.kurostream.data.network.NetworkMonitorRepositoryImpl
import com.kurostream.data.repository.*
import com.kurostream.data.subtitle.OfflineTranslator
import com.kurostream.data.subtitle.OfflineTranslatorImpl
import com.kurostream.data.sync.CrossDeviceSyncRepository
import com.kurostream.data.sync.CrossDeviceSyncRepositoryImpl
// import com.kurostream.data.trailer.TrailerRepository
// import com.kurostream.data.trailer.TrailerRepositoryImpl
// import com.kurostream.domain.metadata.MetadataProvider
// import com.kurostream.domain.metadata.UnifiedMetadataRepository
import com.kurostream.domain.network.NetworkMonitorRepository
import com.kurostream.domain.repository.*
import com.kurostream.domain.subtitle.OfflineTranslator
import com.kurostream.domain.sync.CrossDeviceSyncRepository
// import com.kurostream.domain.trailer.TrailerRepository
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DataModule {

    @Provides
    @Singleton
    fun provideMediaRepository(impl: MediaRepositoryImpl): MediaRepository = impl

    @Provides
    @Singleton
    fun provideProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository = impl

    @Provides
    @Singleton
    fun provideSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository = impl

    @Provides
    @Singleton
    fun provideWatchProgressRepository(impl: WatchProgressRepositoryImpl): WatchProgressRepository = impl

    @Provides
    @Singleton
    fun provideDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository = impl

    @Provides
    @Singleton
    fun provideSourceLockRepository(impl: SourceLockRepositoryImpl): SourceLockRepository = impl

    @Provides
    @Singleton
    fun provideCacheManager(impl: CacheManagerImpl): CacheManager = impl

    @Provides
    @Singleton
    fun provideNetworkMonitorRepository(impl: NetworkMonitorRepositoryImpl): NetworkMonitorRepository = impl

    @Provides
    @Singleton
    fun provideCrossDeviceSyncRepository(impl: CrossDeviceSyncRepositoryImpl): CrossDeviceSyncRepository = impl

    @Provides
    @Singleton
    fun provideOfflineTranslator(impl: OfflineTranslatorImpl): OfflineTranslator = impl

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStoreImpl(context)
    }

    @Provides
    @Singleton
    fun provideProfileDao(database: com.kurostream.data.local.database.KuroStreamDatabase): ProfileDao {
        return database.profileDao()
    }

    @Provides
    @Singleton
    fun provideWatchHistoryDao(database: com.kurostream.data.local.database.KuroStreamDatabase): WatchHistoryDao {
        return database.watchHistoryDao()
    }

    @Provides
    @Singleton
    fun provideFavoriteDao(database: com.kurostream.data.local.database.KuroStreamDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    @Singleton
    fun provideDownloadItemDao(database: com.kurostream.data.local.database.KuroStreamDatabase): DownloadItemDao {
        return database.downloadItemDao()
    }

    @Provides
    @Singleton
    fun provideHomeRowDao(database: com.kurostream.data.local.database.KuroStreamDatabase): HomeRowDao {
        return database.homeRowDao()
    }

    @Provides
    @Singleton
    fun provideBookmarkDao(database: com.kurostream.data.local.database.KuroStreamDatabase): BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    @Singleton
    fun provideAddonDao(database: com.kurostream.data.local.database.KuroStreamDatabase): AddonDao {
        return database.addonDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @Singleton
    @Named("IO")
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @Named("Default")
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}