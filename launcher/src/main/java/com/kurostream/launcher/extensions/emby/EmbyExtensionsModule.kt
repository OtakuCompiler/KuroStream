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

package com.kurostream.launcher.extensions.emby

import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EmbyExtensionsModule {

    @Provides
    @Singleton
    fun provideEmbyAuthManager(
        context: android.content.Context,
        prefs: SharedPreferences,
        okHttpClient: OkHttpClient
    ): EmbyAuthManager = EmbyAuthManager(context, prefs, okHttpClient)

    @Provides
    @Singleton
    fun provideEmbyLibrarySync(
        authManager: EmbyAuthManager,
        libraryDao: com.kurostream.launcher.data.local.LibraryDao
    ): EmbyLibrarySync = EmbyLibrarySync(authManager, libraryDao)

    @Provides
    @Singleton
    fun provideEmbyStreamProxy(
        authManager: EmbyAuthManager
    ): EmbyStreamProxy = EmbyStreamProxy(authManager)
}
