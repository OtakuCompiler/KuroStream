package com.kurostream.tv.di

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.kurostream.tv.core.perf.MemoryAwareQualitySelector
import com.kurostream.tv.core.player.datasource.TorrentDataSourceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.IOException
import javax.inject.Singleton

/**
 * Hilt module for Media3 / ExoPlayer dependencies.
 *
 * Resolution caps and buffer sizes are driven by [MemoryAwareQualitySelector]
 * so they adapt automatically to the device's available RAM:
 *
 * | RAM tier  | Max resolution | Max bitrate  | Buffer window |
 * |-----------|----------------|--------------|---------------|
 * | ≤ 1.1 GB  | 1080p (default)| 8 Mbps       | 6 s – 15 s    |
 * | 1–2.1 GB  | 1080p          | 12 Mbps      | 8 s – 20 s    |
 * | > 2.1 GB  | 4K if HW dec   | 25 Mbps      | 10 s – 30 s   |
 *
 * On 1 GB Fire TV devices the profile resolves to 1080p / 8 Mbps by default.
 * If available RAM drops below 150 MB at runtime the [PlayerViewModel] queries
 * [MemoryAwareQualitySelector.runtimeQualityConstraint] and may temporarily
 * request a lower-quality stream without changing the ExoPlayer track selector.
 *
 * Torrent / magnet URIs are transparently routed to [TorrentDataSourceFactory]
 * by the [TorrentRoutingDataSourceFactory] that wraps the normal cache factory.
 * ExoPlayer itself never needs to know the difference.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    // Fixed cache size — keep media cache small to leave heap for the player
    private const val CACHE_SIZE_BYTES = 50L * 1024 * 1024  // 50 MB

    /**
     * Provides [LoadControl] tuned to the device's RAM tier via [QualityProfile].
     *
     * Lower-RAM devices use shorter buffer windows to keep heap usage minimal.
     * Higher-RAM devices buffer more aggressively to reduce rebuffering on 4K streams.
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideLoadControl(
        qualitySelector: MemoryAwareQualitySelector
    ): LoadControl {
        val profile = qualitySelector.profile
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                profile.minBufferMs,          // min buffer before playback
                profile.maxBufferMs,          // max buffer to keep in memory
                profile.playbackBufferMs,     // min for playback to start/resume
                profile.rebufferMs            // min to recover from stall
            )
            .setTargetBufferBytes(C.LENGTH_UNSET)
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(
                /* backBufferDurationMs = */ 5_000,
                /* retainBackBufferFromKeyframe = */ false
            )
            .build()
    }

    /**
     * Provides [DefaultTrackSelector] with resolution and bitrate constraints
     * derived from [MemoryAwareQualitySelector].
     *
     * Key changes vs previous implementation:
     * - Default max is now **1080p** on 1 GB devices (was 720p).
     * - 4K (3840×2160) is unlocked when hardware decoding is confirmed available.
     * - `setExceedVideoConstraintsIfNecessary(true)` means ExoPlayer can exceed
     *   the cap when no lower-quality track is available, preventing black-screen
     *   on streams that only publish 1080p+.
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideTrackSelector(
        @ApplicationContext context: Context,
        qualitySelector: MemoryAwareQualitySelector
    ): DefaultTrackSelector {
        val profile = qualitySelector.profile
        return DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSize(profile.maxWidth, profile.maxHeight)
                    .setMaxVideoBitrate(profile.maxBitrateBps)
                    // Prefer highest supported bitrate on mid/high-RAM devices
                    .setForceHighestSupportedBitrate(profile.preferHighestBitrate)
                    // Allow exceeding cap if stream has no lower-quality track
                    .setExceedVideoConstraintsIfNecessary(true)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoNonSeamlessAdaptiveness(true)
                    .setPreferredAudioLanguage("ja")   // Default: Japanese audio
                    .setPreferredTextLanguage("en")    // Default: English subtitles
            )
        }
    }

    /**
     * Provides [SimpleCache] for video segment caching.
     *
     * 50 MB LRU cache shared across all stream types.  Cache writing is
     * disabled in [CacheDataSource.Factory] during normal playback to prevent
     * large heap allocations; only metadata/thumbnails are actually written.
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideSimpleCache(
        @ApplicationContext context: Context
    ): SimpleCache {
        val cacheDir = File(context.cacheDir, "media_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES)
        return SimpleCache(cacheDir, evictor)
    }

    /**
     * Provides [DefaultBandwidthMeter] for ABR decisions.
     *
     * Resets estimated bandwidth when network type changes (e.g. Wi-Fi → ETH
     * via HDMI adapter) to avoid overshoot on 4K ABR selection.
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideBandwidthMeter(
        @ApplicationContext context: Context
    ): DefaultBandwidthMeter {
        return DefaultBandwidthMeter.Builder(context)
            .setResetOnNetworkTypeChange(true)
            .build()
    }

    /** [AudioAttributes] configured for movie-style media content. */
    @Provides
    @Singleton
    fun provideAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
    }

    /**
     * Provides [DefaultHttpDataSource.Factory] for network media requests.
     *
     * User-agent identifies the app + player version to content servers that
     * serve different manifests based on client type.
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideHttpDataSourceFactory(): DefaultHttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setUserAgent("KuroStream/1.0 (ExoPlayer/Media3)")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
    }

    /**
     * Provides [CacheDataSource.Factory].
     *
     * Cache *writing* is disabled (`setCacheWriteDataSinkFactory(null)`) to
     * avoid writing large video chunks to disk during playback; only upstream
     * reads (pre-cached segments) are served from cache.  This keeps heap
     * pressure low on 1 GB devices while still benefiting from previously
     * cached segments.
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideCacheDataSourceFactory(
        @ApplicationContext context: Context,
        simpleCache: SimpleCache,
        httpDataSourceFactory: DefaultHttpDataSource.Factory
    ): CacheDataSource.Factory {
        val upstream = DefaultDataSource.Factory(context, httpDataSourceFactory)
        return CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(upstream)
            .setCacheWriteDataSinkFactory(null)  // Disable cache writes during playback
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Provides [DefaultMediaSourceFactory] backed by a [TorrentRoutingDataSourceFactory].
     *
     * The routing factory transparently dispatches to:
     *  - [TorrentDataSourceFactory] for `magnet:` and `torrent-proxy:` URIs
     *  - [CacheDataSource.Factory] (HTTP/HTTPS/HLS/DASH) for everything else
     *
     * ExoPlayer itself doesn't need scheme-specific logic; all routing is
     * handled at the [DataSource] level before data reaches the player.
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideMediaSourceFactory(
        @ApplicationContext context: Context,
        cacheDataSourceFactory: CacheDataSource.Factory,
        torrentDataSourceFactory: TorrentDataSourceFactory
    ): DefaultMediaSourceFactory {
        val routingFactory = TorrentRoutingDataSourceFactory(
            httpFactory     = cacheDataSourceFactory,
            torrentFactory  = torrentDataSourceFactory
        )
        return DefaultMediaSourceFactory(context)
            .setDataSourceFactory(routingFactory)
    }

    // ─── ExoPlayer factory ────────────────────────────────────────────────────

    /**
     * Factory interface for creating [ExoPlayer] instances.
     *
     * Using a factory (rather than a singleton player) allows [PlayerViewModel]
     * to create a fresh player on each screen entry and release it cleanly on
     * exit — avoiding stale state between episodes.
     */
    interface ExoPlayerFactory {
        fun create(): ExoPlayer
    }

    /**
     * Provides [ExoPlayerFactory] configured with all optimised dependencies.
     *
     * Player is **not** a singleton — each call to [ExoPlayerFactory.create]
     * returns a new instance sharing the singleton [LoadControl], [DefaultTrackSelector],
     * and [DefaultMediaSourceFactory].
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideExoPlayerFactory(
        @ApplicationContext context: Context,
        loadControl: LoadControl,
        trackSelector: DefaultTrackSelector,
        audioAttributes: AudioAttributes,
        bandwidthMeter: DefaultBandwidthMeter,
        mediaSourceFactory: DefaultMediaSourceFactory
    ): ExoPlayerFactory {
        return object : ExoPlayerFactory {
            override fun create(): ExoPlayer {
                return ExoPlayer.Builder(context)
                    .setLoadControl(loadControl)
                    .setTrackSelector(trackSelector)
                    .setBandwidthMeter(bandwidthMeter)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .setSeekBackIncrementMs(10_000)
                    .setSeekForwardIncrementMs(10_000)
                    .setHandleAudioBecomingNoisy(true)
                    .setWakeMode(C.WAKE_MODE_LOCAL)
                    .build()
                    .apply {
                        setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
                        pauseAtEndOfMediaItems = false
                    }
            }
        }
    }
}

// ─── Torrent routing data source ──────────────────────────────────────────────

/**
 * [DataSource.Factory] that routes requests to [torrentFactory] when the URI
 * scheme is `magnet` or `torrent-proxy`, and to [httpFactory] for all other
 * schemes (HTTP/HTTPS/HLS/DASH/file/…).
 *
 * This is the single integration point between ExoPlayer's media pipeline and
 * [TorrentProxyDataSource]; no other component in the player stack needs to be
 * aware of torrent URIs.
 */
@UnstableApi
internal class TorrentRoutingDataSourceFactory(
    private val httpFactory: DataSource.Factory,
    private val torrentFactory: DataSource.Factory
) : DataSource.Factory {

    companion object {
        private val TORRENT_SCHEMES = setOf("magnet", "torrent-proxy")
    }

    override fun createDataSource(): DataSource =
        TorrentRoutingDataSource(httpFactory, torrentFactory)
}

/**
 * [DataSource] that defers factory selection until [open] is called, at which
 * point the URI scheme determines which delegate factory to use.
 *
 * Transfer listeners queued before [open] (e.g. by [DefaultBandwidthMeter]) are
 * forwarded to the chosen delegate immediately after it is created so bandwidth
 * estimation works correctly for both stream types.
 */
@UnstableApi
private class TorrentRoutingDataSource(
    private val httpFactory: DataSource.Factory,
    private val torrentFactory: DataSource.Factory
) : DataSource {

    companion object {
        private val TORRENT_SCHEMES = setOf("magnet", "torrent-proxy")
    }

    private var delegate: DataSource? = null
    private val pendingListeners = mutableListOf<TransferListener>()

    // ─── DataSource ───────────────────────────────────────────────────────────

    /**
     * Queue [transferListener] if the delegate hasn't been created yet; otherwise
     * forward immediately so [DefaultBandwidthMeter] receives all transfer events.
     */
    override fun addTransferListener(transferListener: TransferListener) {
        val d = delegate
        if (d != null) {
            d.addTransferListener(transferListener)
        } else {
            pendingListeners += transferListener
        }
    }

    /**
     * Select the delegate factory based on the URI scheme, drain any queued
     * [TransferListener]s, then delegate [open] to the chosen data source.
     */
    override fun open(dataSpec: DataSpec): Long {
        val isTorrent = dataSpec.uri.scheme in TORRENT_SCHEMES
        val chosen = (if (isTorrent) torrentFactory else httpFactory).createDataSource()
        delegate = chosen

        // Forward any listeners registered before open() was called
        pendingListeners.forEach { chosen.addTransferListener(it) }
        pendingListeners.clear()

        return chosen.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        delegate?.read(buffer, offset, length)
            ?: throw IOException("TorrentRoutingDataSource: read() called before open()")

    override fun getUri(): Uri? = delegate?.uri

    override fun close() {
        delegate?.close()
        delegate = null
    }
}
