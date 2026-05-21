package com.kurostream.tv.di

import com.kurostream.tv.core.player.SkipOverlayController
import com.kurostream.tv.core.player.datasource.TorrentDataSourceFactory
import com.kurostream.tv.core.player.datasource.TorrentProxyDataSource
import com.kurostream.tv.data.adapter.cloudstream.CloudStreamPluginLoader
import com.kurostream.tv.data.adapter.stremio.StremioAdapter
import com.kurostream.tv.data.remote.metadata.CinemetaService
import com.kurostream.tv.data.remote.metadata.KitsuService
import com.kurostream.tv.data.remote.metadata.MetadataAggregator
import com.kurostream.tv.data.remote.skip.SkipTimestampService
import com.kurostream.tv.data.repository.AnimeRepositoryImpl
import com.kurostream.tv.domain.provider.ProviderAggregator
import com.kurostream.tv.domain.repository.AnimeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Hilt module for repository and service dependencies.
 *
 * Design rules:
 *  - Classes with `@Singleton @Inject constructor` are auto-provided by Hilt; no
 *    manual `@Provides` needed (SmartPlayerRouter, VlcPlayerAdapter, PlaybackErrorHandler,
 *    SubtitleSyncController, StremioAdapter, CloudStreamPluginLoader, TorrentProxyDataSource).
 *  - Explicit `@Provides` is required when:
 *      • The class is a third-party type (no @Inject available).
 *      • Multiple same-type bindings exist that would create ambiguity.
 *      • Scoping needs to be pinned explicitly for clarity (TorrentDataSourceFactory).
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    // ─── Metadata ─────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideMetadataAggregator(
        cinemetaService: CinemetaService,
        kitsuService: KitsuService
    ): MetadataAggregator = MetadataAggregator(cinemetaService, kitsuService)

    // ─── Skip timestamps ──────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideSkipTimestampService(
        okHttpClient: OkHttpClient,
        json: Json
    ): SkipTimestampService = SkipTimestampService(okHttpClient, json)

    @Provides
    @Singleton
    fun provideSkipOverlayController(
        skipTimestampService: SkipTimestampService
    ): SkipOverlayController = SkipOverlayController(skipTimestampService)

    // ─── Providers ────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideProviderAggregator(
        stremioAdapter: StremioAdapter,
        cloudStreamPluginLoader: CloudStreamPluginLoader,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ProviderAggregator = ProviderAggregator(stremioAdapter, cloudStreamPluginLoader, ioDispatcher)

    // ─── Repository binding ───────────────────────────────────────────────────

    /**
     * Binds the concrete [AnimeRepositoryImpl] to the [AnimeRepository] interface.
     * [AnimeRepositoryImpl] is `@Singleton @Inject`, so Hilt constructs it automatically;
     * this function simply exposes it under the interface type.
     */
    @Provides
    @Singleton
    fun provideAnimeRepository(impl: AnimeRepositoryImpl): AnimeRepository = impl

    // ─── Torrent data source ──────────────────────────────────────────────────

    /**
     * Explicitly scope [TorrentDataSourceFactory] as a singleton so the same
     * [TorrentProxyDataSource] instance is shared between the factory and
     * [PlayerModule]'s [TorrentRoutingDataSourceFactory].
     *
     * [TorrentProxyDataSource] is itself `@Singleton @Inject`, so Hilt wires it
     * automatically — we just need to ensure the factory wrapper is also scoped.
     */
    @Provides
    @Singleton
    fun provideTorrentDataSourceFactory(
        torrentProxyDataSource: TorrentProxyDataSource
    ): TorrentDataSourceFactory = TorrentDataSourceFactory(torrentProxyDataSource)
}
