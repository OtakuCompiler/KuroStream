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

package com.kurostream.torrent.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class TorrentInfo(
    val infoHash: String,
    val name: String,
    val totalSize: Long,
    val downloadDir: String,
    val status: TorrentStatus = TorrentStatus.QUEUED,
    val progress: Float = 0f,
    val downloadSpeed: Long = 0L,
    val uploadSpeed: Long = 0L,
    val peers: Int = 0,
    val seeds: Int = 0,
    val eta: Long = 0L,
    val priority: Int = 0,
    val isSequentialDownload: Boolean = false,
    val seedRatioLimit: Float = 2.0f,
    val seedTimeLimitMinutes: Long = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val magnetUri: String? = null,
    val torrentFilePath: String? = null,
    val error: String? = null,
    val files: List<TorrentFile> = emptyList(),
    val savePath: String = "",
    val downloadLimit: Long = -1,
    val uploadLimit: Long = -1,
    val ratio: Float = 0f,
    val totalDownloaded: Long = 0L,
    val totalUploaded: Long = 0L,
    val activeTime: Long = 0L,
    val seedingTime: Long = 0L,
    val numPieces: Int = 0,
    val pieceSize: Int = 0,
    val peersConnected: Int = 0,
    val trackerCount: Int = 0,
    val swarmHealthScore: Int = 0,
    val dhtNodes: Int = 0,
    val utpEnabled: Boolean = true,
    val connectionQuality: Float = 1.0f,
)

@Serializable
data class SwarmHealth(
    val seedCount: Int = 0,
    val peerCount: Int = 0,
    val dhtNodeCount: Int = 0,
    val avgPeerSpeedBps: Long = 0,
    val pieceAvailability: Map<Int, Int> = emptyMap(),
    val healthScore: Int = 0,
    val connectedSince: Long = 0L,
)

@Serializable
data class TorrentFile(
    val path: String,
    val size: Long,
    val priority: FilePriority = FilePriority.NORMAL,
    val progress: Float = 0f,
    val isMediaFile: Boolean = false,
    val index: Int = -1,
) {
    val fileName: String = path.substringAfterLast("/")
}

enum class FilePriority { LOW, NORMAL, HIGH, DONT_DOWNLOAD }

enum class TorrentStatus {
    QUEUED,
    CHECKING,
    DOWNLOADING_METADATA,
    DOWNLOADING,
    SEEDING,
    PAUSED,
    ERROR,
    FINISHED,
    MOVING,
    CHECKING_RESUME_DATA,
}

sealed interface TorrentResult {
    data class Success(val info: TorrentInfo) : TorrentResult
    data class Failure(val message: String) : TorrentResult
}

interface TorrentRepository {
    suspend fun addTorrent(magnetUri: String, downloadDir: String, sequentialDownload: Boolean = false): TorrentResult
    suspend fun addTorrentFile(filePath: String, downloadDir: String, sequentialDownload: Boolean = false): TorrentResult
    suspend fun removeTorrent(infoHash: String, deleteFiles: Boolean)
    suspend fun pauseTorrent(infoHash: String)
    suspend fun resumeTorrent(infoHash: String)
    suspend fun setTorrentPriority(infoHash: String, priority: Int)
    suspend fun setFilePriorities(infoHash: String, priorities: Map<String, FilePriority>)
    suspend fun setSequentialDownload(infoHash: String, enabled: Boolean)
    suspend fun setSeedLimits(infoHash: String, ratioLimit: Float, timeLimitMinutes: Long)
    fun observeTorrents(): Flow<List<TorrentInfo>>
    fun observeTorrent(infoHash: String): Flow<TorrentInfo?>
    suspend fun getTorrent(infoHash: String): TorrentInfo?
    suspend fun getAllTorrents(): List<TorrentInfo>
    suspend fun pauseAll()
    suspend fun resumeAll()
    suspend fun removeAll(deleteFiles: Boolean)
    suspend fun setGlobalSpeedLimits(downloadLimitKbps: Long, uploadLimitKbps: Long)
    suspend fun setGlobalSeedLimits(ratioLimit: Float, timeLimitMinutes: Long)
    fun observeGlobalStats(): Flow<GlobalStats>
    suspend fun getSessionStats(): GlobalStats
    suspend fun moveTorrentData(infoHash: String, newPath: String): TorrentResult
    suspend fun reannounceTorrent(infoHash: String)
    suspend fun scrapeTracker(infoHash: String)
    suspend fun getTorrentStreamUrl(infoHash: String, fileIndex: Int): String?
    suspend fun setSessionSettings(settings: TorrentSessionSettings)
    suspend fun getSessionSettings(): TorrentSessionSettings
    suspend fun getSwarmHealth(infoHash: String): SwarmHealth
}

@Serializable
data class GlobalStats(
    val totalDownloadSpeed: Long,
    val totalUploadSpeed: Long,
    val activeTorrents: Int,
    val pausedTorrents: Int,
    val totalTorrents: Int,
    val dhtNodes: Int,
    val totalDownload: Long = 0L,
    val totalUpload: Long = 0L,
    val sessionTime: Long = 0L,
    val trackerCount: Int = 0,
    val totalPeersConnected: Int = 0,
    val portMapped: Boolean = false,
)

@Serializable
data class TorrentSessionSettings(
    val listenPort: Int = 6881,
    val enableDht: Boolean = true,
    val enableLsd: Boolean = true,
    val enableUpnp: Boolean = true,
    val enableNatpmp: Boolean = true,
    val enablePeX: Boolean = true,
    val enableUtp: Boolean = true,
    val encryptionMode: EncryptionMode = EncryptionMode.PREFER_ENCRYPTION,
    val maxConnections: Int = 200,
    val maxUploadSlots: Int = 50,
    val maxHalfOpenConnections: Int = 8,
    val connectionsPer_torrent: Int = 50,
    val maxConnectionsPerTorrent: Int = 50,
    val maxUploadSlotsPerTorrent: Int = 10,
    val alertMask: Long = -1L,
    val downloadRateLimit: Long = -1,
    val uploadRateLimit: Long = -1,
    val seedRatioLimit: Float = 2.0f,
    val seedTimeLimitMinutes: Long = 0,
    val seedIdleLimitMinutes: Long = 30,
    val announceToAllTrackers: Boolean = true,
    val announceToAllTiers: Boolean = true,
    val preferUdpTrackers: Boolean = true,
    val strictEndGameMode: Boolean = true,
    val utpTargetDelayMs: Int = 50,
    val utpGain: Int = 10000,
    val utpLostSeed: Int = 10,
    val announceIntervalSec: Int = 30,
    val minAnnounceIntervalSec: Int = 15,
    val pieceCacheSizeMb: Int = 64,
    val lazyVerificationEnabled: Boolean = true,
)

enum class EncryptionMode {
    NONE,
    PREFER_ENCRYPTION,
    REQUIRE_ENCRYPTION,
    REQUIRE_RC4,
}

@Serializable
data class TorrentAddRequest(
    val magnetUri: String? = null,
    val torrentFilePath: String? = null,
    val savePath: String,
    val sequentialDownload: Boolean = false,
    val paused: Boolean = false,
    val autoManaged: Boolean = true,
    val filePriorities: Map<String, FilePriority> = emptyMap(),
    val seedRatioLimit: Float = 2.0f,
    val seedTimeLimitMinutes: Long = 0,
)

enum class TorrentSourceType {
    MAGNET,
    TORRENT_FILE,
    TORRENT_URL,
    STREMIO_STREAM,
}

@Serializable
data class TorrentStreamInfo(
    val infoHash: String,
    val fileIndex: Int,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val streamUrl: String,
    val isSeekable: Boolean,
    val downloadProgress: Float,
    val bufferProgress: Float,
    val isReadyToPlay: Boolean,
)
