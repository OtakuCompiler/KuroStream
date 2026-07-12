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

package com.kurostream.torrent.repository

import android.content.Context
import com.kurostream.core.common.dispatcher.DispatcherProvider
import com.kurostream.core.common.result.Result
import com.kurostream.torrent.domain.*
import com.kurostream.torrent.engine.TorrentEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sharingStarted
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentRepositoryImpl @Inject constructor(
    private val engine: TorrentEngine,
    private val dispatcherProvider: DispatcherProvider,
    @androidx.hilt.android.qualifiers.ApplicationContext private val context: Context,
) : TorrentRepository {

    private val defaultSavePath = context.getExternalFilesDir("torrents")?.absolutePath
        ?: context.filesDir.absolutePath + "/torrents"

    override suspend fun addTorrent(
        magnetUri: String,
        downloadDir: String,
        sequentialDownload: Boolean = false
    ): TorrentResult {
        val result = engine.addMagnet(magnetUri, downloadDir, sequentialDownload)
        return result.fold(
            onSuccess = { TorrentResult.Success(it) },
            onFailure = { TorrentResult.Failure(it.message ?: "Failed to add torrent") }
        )
    }

    override suspend fun addTorrentFile(
        filePath: String,
        downloadDir: String,
        sequentialDownload: Boolean = false
    ): TorrentResult {
        val result = engine.addTorrentFile(filePath, downloadDir, sequentialDownload)
        return result.fold(
            onSuccess = { TorrentResult.Success(it) },
            onFailure = { TorrentResult.Failure(it.message ?: "Failed to add torrent file") }
        )
    }

    override suspend fun removeTorrent(infoHash: String, deleteFiles: Boolean) {
        engine.removeTorrent(infoHash, deleteFiles)
    }

    override suspend fun pauseTorrent(infoHash: String) {
        engine.pauseTorrent(infoHash)
    }

    override suspend fun resumeTorrent(infoHash: String) {
        engine.resumeTorrent(infoHash)
    }

    override suspend fun setTorrentPriority(infoHash: String, priority: Int) {
        engine.setTorrentPriority(infoHash, priority)
    }

    override suspend fun setFilePriorities(infoHash: String, priorities: Map<String, FilePriority>) {
        engine.setFilePriorities(infoHash, priorities)
    }

    override suspend fun setSequentialDownload(infoHash: String, enabled: Boolean) {
        engine.setSequentialDownload(infoHash, enabled)
    }

    override suspend fun setSeedLimits(infoHash: String, ratioLimit: Float, timeLimitMinutes: Long) {
        engine.setSeedLimits(infoHash, ratioLimit, timeLimitMinutes)
    }

    override fun observeTorrents(): Flow<List<TorrentInfo>> {
        return engine.observeTorrents()
    }

    override fun observeTorrent(infoHash: String): Flow<TorrentInfo?> {
        return engine.observeTorrent(infoHash)
    }

    override suspend fun getTorrent(infoHash: String): TorrentInfo? {
        return engine.getTorrent(infoHash)
    }

    override suspend fun getAllTorrents(): List<TorrentInfo> {
        return engine.getAllTorrents()
    }

    override suspend fun pauseAll() {
        engine.pauseAll()
    }

    override suspend fun resumeAll() {
        engine.resumeAll()
    }

    override suspend fun removeAll(deleteFiles: Boolean) {
        engine.removeAll(deleteFiles)
    }

    override suspend fun setGlobalSpeedLimits(downloadLimitKbps: Long, uploadLimitKbps: Long) {
        engine.setGlobalSpeedLimits(downloadLimitKbps, uploadLimitKbps)
    }

    override suspend fun setGlobalSeedLimits(ratioLimit: Float, timeLimitMinutes: Long) {
        engine.setGlobalSeedLimits(ratioLimit, timeLimitMinutes)
    }

    override fun observeGlobalStats(): Flow<GlobalStats> {
        return engine.observeGlobalStats()
    }

    override suspend fun getSessionStats(): GlobalStats {
        return engine.getSessionStats()
    }

    override suspend fun moveTorrentData(infoHash: String, newPath: String): TorrentResult {
        return engine.moveTorrentData(infoHash, newPath).fold(
            onSuccess = { TorrentResult.Success(it) },
            onFailure = { TorrentResult.Failure(it.message ?: "Failed to move torrent data") }
        )
    }

    override suspend fun reannounceTorrent(infoHash: String) {
        engine.reannounceTorrent(infoHash)
    }

    override suspend fun scrapeTracker(infoHash: String) {
        engine.scrapeTracker(infoHash)
    }

    override suspend fun getTorrentStreamUrl(infoHash: String, fileIndex: Int): String? {
        return engine.getTorrentStreamUrl(infoHash, fileIndex)
    }

    override suspend fun setSessionSettings(settings: TorrentSessionSettings) {
        engine.setSessionSettings(settings)
    }

    override suspend fun getSessionSettings(): TorrentSessionSettings {
        return engine.getSessionSettings()
    }

    override suspend fun getSwarmHealth(infoHash: String): SwarmHealth {
        val torrent = engine.getTorrent(infoHash) ?: return SwarmHealth()
        return SwarmHealth(
            seedCount = torrent.seeds,
            peerCount = torrent.peers,
            dhtNodeCount = torrent.dhtNodes,
            avgPeerSpeedBps = if (torrent.peers > 0) torrent.downloadSpeed / torrent.peers else 0,
            healthScore = torrent.swarmHealthScore,
        )
    }
}