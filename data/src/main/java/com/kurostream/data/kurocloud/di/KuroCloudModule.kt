package com.kurostream.data.kurocloud.di

import com.kurostream.data.kurocloud.api.KuroApiService
import com.kurostream.data.kurocloud.auth.KuroAuthService
import com.kurostream.data.kurocloud.auth.KuroAuthenticator
import com.kurostream.data.kurocloud.auth.KuroTokenManager
import com.kurostream.data.kurocloud.db.KuroCloudDatabase
import com.kurostream.data.kurocloud.sync.KuroSyncRepository
import com.kurostream.data.security.EncryptedPreferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class KuroCloudBindsModule {
    @Binds
    @Singleton
    abstract fun bindAuthenticator(authenticator: KuroAuthenticator): okhttp3.Authenticator
}

@Module
@InstallIn(SingletonComponent::class)
object KuroCloudProvidesModule {

    private const val API_BASE = "https://kuro-stream-tv.lovable.app"
    private const val AUTH_URL = "https://kklyohtsedcdgmnmameh.supabase.co"
    private const val ANON_KEY = "sb_publishable_x_ZB45-mADfu4479vmZdaw_SGpIE6Kx"

    @Provides
    @Singleton
    @Named("api")
    fun provideApiClient(authenticator: okhttp3.Authenticator): OkHttpClient {
        return OkHttpClient.Builder()
            .authenticator(authenticator)
            .build()
    }

    @Provides
    @Singleton
    @Named("auth")
    fun provideAuthClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(@Named("auth") authClient: OkHttpClient): KuroAuthService {
        return Retrofit.Builder()
            .baseUrl(AUTH_URL)
            .client(authClient)
            .addConverterFactory(
                kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }.asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(KuroAuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideApi(@Named("api") apiClient: OkHttpClient): KuroApiService {
        return Retrofit.Builder()
            .baseUrl(API_BASE)
            .client(apiClient)
            .addConverterFactory(
                kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }.asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(KuroApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTokenManager(encryptedPrefs: EncryptedPreferences): KuroTokenManager {
        return KuroTokenManager(encryptedPrefs)
    }

    @Provides
    @Singleton
    fun provideSyncRepository(
        database: KuroCloudDatabase,
        api: KuroApiService,
        tokenManager: KuroTokenManager,
    ): KuroSyncRepository {
        return KuroSyncRepository(database, api, tokenManager)
    }

    @Provides
    @Singleton
    fun provideKuroCloudDatabase(@dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context): KuroCloudDatabase {
        return com.kurostream.data.kurocloud.db.KuroCloudDatabase.Companion.getDatabase(context)
    }
}
