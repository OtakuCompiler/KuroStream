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

import com.kurostream.data.BuildConfig
import com.kurostream.data.network.GsonConverterFactory
import com.kurostream.data.remote.api.AniListApi
import com.kurostream.data.remote.api.KitsuApi
import com.kurostream.data.remote.api.MalApi
import com.kurostream.data.remote.api.OpenSubtitlesApi
import com.kurostream.data.remote.api.TmdbApi
import com.kurostream.data.remote.api.YouTubeApi
import com.kurostream.data.network.security.CertificatePinningConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.brotli.BrotliInterceptor
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val ANILIST_BASE_URL = "https://graphql.anilist.co/"
    private const val MAL_BASE_URL = "https://api.myanimelist.net/v2/"
    private const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
    private const val KITSU_BASE_URL = "https://kitsu.io/api/edge/"
    private const val OPENSUBTITLES_BASE_URL = "https://api.opensubtitles.com/"
    private const val YOUTUBE_BASE_URL = "https://www.googleapis.com/youtube/v3/"


    @Provides
    @Singleton
    fun provideCacheDir(context: android.content.Context): File {
        return File(context.cacheDir, "http_cache").apply { mkdirs() }
    }

    @Provides
    @Singleton
    fun provideHttpCache(cacheDir: File): Cache {
        // 50MB HTTP response cache with ETag support
        return Cache(cacheDir, 50L * 1024 * 1024)
    }

    @Provides
    @Singleton
    fun provideConnectionPool(): ConnectionPool {
        // Keep connections alive for 5 minutes, max 10 idle connections
        return ConnectionPool(10, 5, TimeUnit.MINUTES)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cache: Cache,
        connectionPool: ConnectionPool,
        certificatePinningConfig: CertificatePinningConfig,
        context: android.content.Context
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            if (BuildConfig.DEBUG) Timber.tag("OkHttp").d(message)
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return certificatePinningConfig.applyPinning(OkHttpClient.Builder())
            .cache(cache)
            .connectionPool(connectionPool)
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .addInterceptor(loggingInterceptor)
            .addInterceptor(BrotliInterceptor) // Brotli compression
            .addNetworkInterceptor { chain ->
                // Add cache headers for immutable resources
                val request = chain.request()
                val response = chain.proceed(request)
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=300")
                    .build()
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .pingInterval(30, TimeUnit.SECONDS) // HTTP/2 keepalive
            .build()
    }

    @Provides
    @Singleton
    fun provideAniListApi(client: OkHttpClient): AniListApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(ANILIST_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(AniListApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMalApi(client: OkHttpClient): MalApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(MAL_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(MalApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTmdbApi(client: OkHttpClient): TmdbApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(TMDB_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(TmdbApi::class.java)
    }

    @Provides
    @Singleton
    fun provideKitsuApi(client: OkHttpClient): KitsuApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(KITSU_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(KitsuApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenSubtitlesApi(client: OkHttpClient): OpenSubtitlesApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(OPENSUBTITLES_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(OpenSubtitlesApi::class.java)
    }

    @Provides
    @Singleton
    fun provideYouTubeApi(client: OkHttpClient): YouTubeApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(YOUTUBE_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(YouTubeApi::class.java)
    }
}
