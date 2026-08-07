package com.kurostream.extensions.di

import com.kurostream.domain.debrid.DebridManager
import com.kurostream.domain.extension.*
import com.kurostream.extensions.aggregator.SmartSourceAggregator
import com.kurostream.extensions.cloudstream.CloudStreamAdapter
import com.kurostream.extensions.cloudstream.CloudStreamImporter
import com.kurostream.extensions.consumet.ConsumetAdapter
import com.kurostream.extensions.consumet.ConsumetImporter
import com.kurostream.extensions.debrid.DebridManagerImpl
import com.kurostream.extensions.health.ExtensionHealthMonitorImpl
import com.kurostream.extensions.kodi.KodiAdapter
import com.kurostream.extensions.kodi.KodiImporter
import com.kurostream.extensions.marketplace.UnifiedMarketplace
import com.kurostream.extensions.stremio.StremioAdapter
import com.kurostream.extensions.stremio.StremioImporter
import dagger.Binds
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
abstract class ExtensionsBindsModule {
    @Binds
    @Singleton
    abstract fun bindSourceAggregator(impl: SmartSourceAggregator): SourceAggregator

    @Binds
    @Singleton
    abstract fun bindExtensionHealthMonitor(impl: ExtensionHealthMonitorImpl): ExtensionHealthMonitor

    @Binds
    @Singleton
    abstract fun bindExtensionMarketplace(impl: UnifiedMarketplace): ExtensionMarketplace

    @Binds
    @Singleton
    abstract fun bindDebridManager(impl: DebridManagerImpl): DebridManager
}

@Module
@InstallIn(SingletonComponent::class)
object ExtensionsProvidesModule {

    @Provides
    @Singleton
    fun provideStremioAdapter(client: OkHttpClient): StremioAdapter = StremioAdapter(client)

    @Provides
    @Singleton
    fun provideStremioImporter(adapter: StremioAdapter): StremioImporter = StremioImporter(adapter)

    @Provides
    @Singleton
    fun provideCloudStreamAdapter(client: OkHttpClient): CloudStreamAdapter = CloudStreamAdapter(client)

    @Provides
    @Singleton
    fun provideCloudStreamImporter(adapter: CloudStreamAdapter): CloudStreamImporter = CloudStreamImporter(adapter)

    @Provides
    @Singleton
    fun provideConsumetAdapter(client: OkHttpClient): ConsumetAdapter = ConsumetAdapter(client)

    @Provides
    @Singleton
    fun provideConsumetImporter(adapter: ConsumetAdapter): ConsumetImporter = ConsumetImporter(adapter)

    @Provides
    @Singleton
    fun provideKodiAdapter(client: OkHttpClient): KodiAdapter = KodiAdapter(client)

    @Provides
    @Singleton
    fun provideKodiImporter(adapter: KodiAdapter): KodiImporter = KodiImporter(adapter)

    @Provides
    @Singleton
    fun provideRealDebridApi(client: OkHttpClient): com.kurostream.data.debrid.RealDebridApi {
        return Retrofit.Builder()
            .baseUrl("https://api.real-debrid.com/")
            .client(client)
            .addConverterFactory(
                kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }.asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(com.kurostream.data.debrid.RealDebridApi::class.java)
    }
}
