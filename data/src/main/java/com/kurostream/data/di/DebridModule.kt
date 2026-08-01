package com.kurostream.data.di

import com.kurostream.data.debrid.RealDebridApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DebridModule {

    @Provides
    @Singleton
    fun provideRealDebridApi(client: OkHttpClient): RealDebridApi {
        return Retrofit.Builder()
            .baseUrl("https://api.real-debrid.com/rest/1.0/")
            .client(client)
            .addConverterFactory(
                kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }.asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(RealDebridApi::class.java)
    }
}
