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

package com.kurostream.app.repository

import com.kurostream.data.repository.MediaRepositoryImpl
import com.kurostream.data.repository.ProfileRepositoryImpl
import com.kurostream.data.repository.SettingsRepositoryImpl
import com.kurostream.data.repository.SourceLockRepositoryImpl
import com.kurostream.data.repository.WatchProgressRepositoryImpl
import com.kurostream.domain.repository.MediaRepository as CanonicalMediaRepository
import com.kurostream.domain.repository.ProfileRepository as CanonicalProfileRepository
import com.kurostream.domain.repository.SettingsRepository as CanonicalSettingsRepository
import com.kurostream.domain.repository.SourceLockRepository as CanonicalSourceLockRepository
import com.kurostream.domain.repository.WatchProgressRepository as CanonicalWatchProgressRepository
import com.kurostream.app.repository.TvRepositories.MediaRepository
import com.kurostream.app.repository.TvRepositories.WatchProgressRepository
import com.kurostream.app.repository.TvRepositories.FavoritesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TvRepositoryModule {

    @Provides
    @Singleton
    fun provideMediaRepository(impl: MediaRepositoryAdapter): MediaRepository = impl

    @Provides
    @Singleton
    fun provideWatchProgressRepository(impl: WatchProgressRepositoryAdapter): WatchProgressRepository = impl

    @Provides
    @Singleton
    fun provideFavoritesRepository(impl: FavoritesRepositoryAdapter): FavoritesRepository = impl

    @Provides
    @Singleton
    fun provideSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository = impl

    @Provides
    @Singleton
    fun provideSourceLockRepository(impl: SourceLockRepositoryImpl): SourceLockRepository = impl

    @Provides
    @Singleton
    fun provideCanonicalMediaRepository(impl: MediaRepositoryImpl): CanonicalMediaRepository = impl

    @Provides
    @Singleton
    fun provideCanonicalProfileRepository(impl: ProfileRepositoryImpl): CanonicalProfileRepository = impl

    @Provides
    @Singleton
    fun provideCanonicalSettingsRepository(impl: SettingsRepositoryImpl): CanonicalSettingsRepository = impl

    @Provides
    @Singleton
    fun provideCanonicalSourceLockRepository(impl: SourceLockRepositoryImpl): CanonicalSourceLockRepository = impl

    @Provides
    @Singleton
    fun provideCanonicalWatchProgressRepository(impl: WatchProgressRepositoryImpl): CanonicalWatchProgressRepository = impl
}