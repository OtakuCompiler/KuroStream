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
import retrofit2.converter.gson.GsonConverterFactory
import com.kurostream.data.remote.api.AniListApi
import com.kurostream.data.remote.api.AniDbApi
import com.kurostream.data.remote.api.AnnApi
import com.kurostream.data.remote.api.KitsuApi
import com.kurostream.data.remote.api.MalApi
import com.kurostream.data.remote.api.OpenSubtitlesApi
import com.kurostream.data.remote.api.TmdbApi
import com.kurostream.data.remote.api.TvdbApi
import com.kurostream.data.remote.api.ImdbApi
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
    private const val TVDB_BASE_URL = "https://api.thetvdb.com/"
    private const val IMDB_BASE_URL = "https://imdb-api.com/"
    private const val ANIDB_BASE_URL = "https://api.anidb.net/"
    private const val ANN_BASE_URL = "https://cdn.animenewsnetwork.com/"


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
    fun provideYouTubeApiKey(): String {
        return com.kurostream.data.BuildConfig.YOUTUBE_API_KEY
            .ifBlank { System.getenv("YOUTUBE_API_KEY").orEmpty() }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cache: Cache,
        connectionPool: ConnectionPool,
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

        return OkHttpClient.Builder()
            .cache(cache)
            .connectionPool(connectionPool)
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .addInterceptor(loggingInterceptor)
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                if (request.method == "GET" && request.header("Authorization").isNullOrBlank()) {
                    response.newBuilder()
                        .header("Cache-Control", "public, max-age=300")
                        .build()
                } else {
                    response
                }
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .apply {
                try {
                    addInterceptor(okhttp3.brotli.BrotliInterceptor)
                } catch (e: NoClassDefFoundError) {
                    Timber.w("BrotliInterceptor not available")
                }
            }
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

    @Provides
    @Singleton
    fun provideTvdbApi(client: OkHttpClient): TvdbApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(TVDB_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(TvdbApi::class.java)
    }

    @Provides
    @Singleton
    fun provideImdbApi(client: OkHttpClient): ImdbApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(IMDB_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(ImdbApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAniDbApi(client: OkHttpClient): AniDbApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(ANIDB_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(AniDbApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAnnApi(client: OkHttpClient): AnnApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(ANN_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(AnnApi::class.java)
    }
}
