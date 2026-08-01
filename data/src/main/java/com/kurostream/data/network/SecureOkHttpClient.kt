package com.kurostream.data.network

import android.content.Context
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import java.util.Collections
import java.util.concurrent.TimeUnit

class SecureOkHttpClient(private val context: Context) {

    fun create(isDebug: Boolean): OkHttpClient {
        val spec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
            .build()

        val builder = OkHttpClient.Builder()
            .connectionSpecs(Collections.singletonList(spec))
            .certificatePinner(CertificatePinnerFactory.create())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        if (isDebug) {
            builder.addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
        }

        return builder.build()
    }
}