package com.kurostream.data.debrid

import com.kurostream.data.local.Constants
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

object RealDebridApiClient {
    fun create(apiKey: String): RealDebridApi {
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(Constants.REAL_DEBRID_BASE_URL)
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
