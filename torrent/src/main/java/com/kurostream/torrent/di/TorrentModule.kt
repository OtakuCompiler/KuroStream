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

package com.kurostream.torrent.di

import android.app.Application
import com.kurostream.common.dispatcher.DispatcherProvider
import com.kurostream.torrent.cache.TorrentMetadataCache
import com.kurostream.torrent.cache.TorrentPieceCache
import com.kurostream.torrent.engine.AdaptiveLimitsCalculator
import com.kurostream.torrent.engine.LazyVerifier
import com.kurostream.torrent.engine.TorrentEngine
import com.kurostream.torrent.engine.WriteCoalescer
import com.kurostream.torrent.metadata.MetadataFetchManager
import com.kurostream.torrent.network.PortMappingMonitor
import com.kurostream.torrent.network.QuicTorrentProxy
import com.kurostream.torrent.prioritization.BandwidthAwareSelector
import com.kurostream.torrent.prioritization.StreamingPiecePrioritizer
import com.kurostream.torrent.prefetch.PeerWarmupManager
import com.kurostream.torrent.prefetch.PredictivePrefetchManager
import com.kurostream.torrent.repository.TorrentRepository
import com.kurostream.torrent.repository.TorrentRepositoryImpl
import com.kurostream.torrent.tracker.SeederHuntManager
import com.kurostream.torrent.tracker.TrackerListProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object TorrentModule {

    @Provides
    @Singleton
    fun provideTrackerListProvider(): TrackerListProvider {
        return TrackerListProvider()
    }

    @Provides
    @Singleton
    fun provideMetadataFetchManager(): MetadataFetchManager {
        return MetadataFetchManager()
    }

    @Provides
    @Singleton
    fun provideSeederHuntManager(trackerListProvider: TrackerListProvider): SeederHuntManager {
        return SeederHuntManager(trackerListProvider)
    }

    @Provides
    @Singleton
    fun provideStreamingPiecePrioritizer(): StreamingPiecePrioritizer {
        return StreamingPiecePrioritizer()
    }

    @Provides
    @Singleton
    fun providePeerWarmupManager(): PeerWarmupManager {
        return PeerWarmupManager()
    }

    @Provides
    @Singleton
    fun providePortMappingMonitor(): PortMappingMonitor {
        return PortMappingMonitor()
    }

    @Provides
    @Singleton
    fun provideQuicTorrentProxy(): QuicTorrentProxy {
        return QuicTorrentProxy()
    }

    @Provides
    @Singleton
    fun provideBandwidthAwareSelector(): BandwidthAwareSelector {
        return BandwidthAwareSelector()
    }

    @Provides
    @Singleton
    fun providePredictivePrefetchManager(): PredictivePrefetchManager {
        return PredictivePrefetchManager()
    }

    @Provides
    @Singleton
    fun provideWriteCoalescer(): WriteCoalescer {
        return WriteCoalescer()
    }

    @Provides
    @Singleton
    fun provideLazyVerifier(): LazyVerifier {
        return LazyVerifier()
    }

    @Provides
    @Singleton
    fun provideTorrentMetadataCache(@ApplicationContext context: Application): TorrentMetadataCache {
        return TorrentMetadataCache(context)
    }

    @Provides
    @Singleton
    fun provideTorrentPieceCache(): TorrentPieceCache {
        return TorrentPieceCache()
    }

    @Provides
    @Singleton
    fun provideAdaptiveLimitsCalculator(@ApplicationContext context: Application): AdaptiveLimitsCalculator {
        return AdaptiveLimitsCalculator(context)
    }

    @Provides
    @Singleton
    fun provideTorrentEngine(
        @ApplicationContext context: Application,
        dispatcherProvider: DispatcherProvider,
        trackerListProvider: TrackerListProvider,
        metadataFetchManager: MetadataFetchManager,
        seederHuntManager: SeederHuntManager,
        streamingPiecePrioritizer: StreamingPiecePrioritizer,
        peerWarmupManager: PeerWarmupManager,
        portMappingMonitor: PortMappingMonitor,
        quicTorrentProxy: QuicTorrentProxy,
        bandwidthAwareSelector: BandwidthAwareSelector,
        predictivePrefetchManager: PredictivePrefetchManager,
        writeCoalescer: WriteCoalescer,
        lazyVerifier: LazyVerifier,
        torrentMetadataCache: TorrentMetadataCache,
        torrentPieceCache: TorrentPieceCache,
    ): TorrentEngine {
        return TorrentEngine(
            context, dispatcherProvider,
            trackerListProvider, metadataFetchManager, seederHuntManager,
            streamingPiecePrioritizer, peerWarmupManager, portMappingMonitor,
            quicTorrentProxy, bandwidthAwareSelector, predictivePrefetchManager,
            writeCoalescer, lazyVerifier, torrentMetadataCache, torrentPieceCache,
        )
    }

    @Provides
    @Singleton
    fun provideTorrentRepository(
        engine: TorrentEngine,
        dispatcherProvider: DispatcherProvider,
        @ApplicationContext context: Application,
    ): TorrentRepository {
        return TorrentRepositoryImpl(engine, dispatcherProvider, context)
    }
}
