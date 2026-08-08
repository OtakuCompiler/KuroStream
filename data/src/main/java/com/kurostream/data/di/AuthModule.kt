// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.di

import com.kurostream.data.network.security.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(): AuthInterceptor {
        val interceptor = AuthInterceptor()

        val tmdbToken = System.getenv("TMDB_API_KEY").orEmpty()
        if (tmdbToken.isNotBlank()) {
            interceptor.addRule("api.themoviedb.org", "Authorization", "Bearer $tmdbToken")
        }

        val malClientId = System.getenv("MAL_CLIENT_ID").orEmpty()
        if (malClientId.isNotBlank()) {
            interceptor.addRule("api.myanimelist.net", "X-MAL-CLIENT-ID", malClientId)
        }

        val opensubApiKey = System.getenv("OPENSUBTITLES_API_KEY").orEmpty()
        if (opensubApiKey.isNotBlank()) {
            interceptor.addRule("api.opensubtitles.com", "Api-Key", opensubApiKey)
        }

        val tvdbToken = System.getenv("TVDB_API_KEY").orEmpty()
        if (tvdbToken.isNotBlank()) {
            interceptor.addRule("api.thetvdb.com", "Authorization", "Bearer $tvdbToken")
        }

        Timber.d("AuthInterceptor configured for 4 hosts")
        return interceptor
    }
}
