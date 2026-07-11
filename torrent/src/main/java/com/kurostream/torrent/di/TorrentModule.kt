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

package com.kurostream.torrent.di

import android.app.Application
import com.kurostream.common.dispatcher.DispatcherProvider
import com.kurostream.torrent.engine.TorrentEngine
import com.kurostream.torrent.repository.TorrentRepository
import com.kurostream.torrent.repository.TorrentRepositoryImpl
import com.kurostream.torrent.service.TorrentService
import dagger.hilt.InstallIn
import dagger.hilt.android.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object TorrentModule {

    @Provides
    @Singleton
    fun provideTorrentEngine(
        @ApplicationContext context: Application,
        dispatcherProvider: DispatcherProvider,
    ): TorrentEngine {
        return TorrentEngine(context, dispatcherProvider)
    }

    @Provides
    @Singleton
    fun provideTorrentRepository(
        engine: TorrentEngine,
        dispatcherProvider: DispatcherProvider,
        @ApplicationContext context: Application,
    ): TorrentRepository {
        return TorrentRepositoryImpl(engine, dispatcherProvider, context)
    }
}