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

package com.kurostream.backup.di

import android.app.Application
import android.content.Context
import com.kurostream.backup.data.BackupRepositoryImpl
import com.kurostream.backup.domain.BackupRepository
import com.kurostream.backup.domain.GitHubApiService
import com.kurostream.data.anistream.addons.AddonDao
import com.kurostream.data.anistream.downloads.DownloadDao
import com.kurostream.data.anistream.profile.ProfileDao
import com.kurostream.data.anistream.search.RecentSearchDao
import com.kurostream.data.anistream.settings.SettingsDao
import com.kurostream.data.anistream.sync.SyncProviderDao
import com.kurostream.data.anistream.introskip.IntroSkipDao
import com.kurostream.data.local.dao.BookmarkDao
import com.kurostream.data.local.dao.FavoriteDao
import com.kurostream.data.local.dao.HomeRowDao
import com.kurostream.data.local.dao.SourceLockDao
import com.kurostream.data.local.dao.WatchHistoryDao
import com.kurostream.data.local.database.KuroStreamDatabase
import com.kurostream.data.local.preferences.SettingsDataStore
import com.kurostream.data.local.preferences.SettingsDataStoreImpl
import dagger.hilt.InstallIn
import dagger.hilt.android.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

@InstallIn(SingletonComponent::class)
@Module
object BackupModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideGitHubApiService(retrofit: Retrofit): GitHubApiService {
        return retrofit.create(GitHubApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideBackupRepository(
        @ApplicationContext context: Context,
        apiService: GitHubApiService,
        profileDao: ProfileDao,
        downloadDao: DownloadDao,
        watchHistoryDao: WatchHistoryDao,
        favoriteDao: FavoriteDao,
        settingsDao: SettingsDao,
        sourceLockDao: SourceLockDao,
        homeRowDao: HomeRowDao,
        bookmarkDao: BookmarkDao,
        addonDao: AddonDao,
        settingsDataStore: SettingsDataStore,
    ): BackupRepository {
        return BackupRepositoryImpl(
            context, apiService, profileDao, downloadDao, watchHistoryDao, favoriteDao,
            settingsDao, sourceLockDao, homeRowDao, bookmarkDao, addonDao, settingsDataStore
        )
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStoreImpl(context)
    }
}