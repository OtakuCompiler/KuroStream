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

package com.kurostream.plugin.sdk.api

import com.kurostream.core.common.result.Result
import com.kurostream.domain.entity.AnimeDetails
import com.kurostream.domain.entity.ExtensionCapability
import com.kurostream.domain.entity.ExtensionInfo
import com.kurostream.domain.entity.HomeRow
import com.kurostream.domain.entity.MediaItem
import com.kurostream.domain.entity.SubtitleCandidate
import com.kurostream.domain.entity.VideoSource
import kotlinx.serialization.Serializable

interface ExtensionApi {
    val extensionId: String
    val info: ExtensionInfo
    suspend fun onCreate(config: ExtensionConfig)
    suspend fun onEnable()
    suspend fun onDisable()
    suspend fun onDestroy()
    fun getCapabilities(): Set<ExtensionCapability>
    suspend fun getHomeRows(): Result<List<HomeRow>>
    suspend fun search(query: String, page: Int, limit: Int): Result<List<MediaItem>>
    suspend fun getAnimeDetails(mediaId: String): Result<AnimeDetails>
    suspend fun getVideoSources(episodeId: String): Result<List<VideoSource>>
    suspend fun getSubtitleCandidates(episodeId: String): Result<List<SubtitleCandidate>>
    suspend fun reportProgress(mediaId: String, episodeNumber: Int, progressPercent: Float): Result<Unit>
    suspend fun syncWatchlist(): Result<List<MediaItem>>

    // Torrent streaming support (optional)
    suspend fun getTorrentStream(episodeId: String): Result<TorrentStreamInfo?> = Result.success(null)
    suspend fun onTorrentProgress(infoHash: String, progress: TorrentProgressCallback): Result<Unit> = Result.success(Unit)
}

interface TorrentProgressCallback {
    fun onProgress(infoHash: String, progress: Float, downloadSpeed: Long, uploadSpeed: Long, peers: Int, seeds: Int)
    fun onReadyToPlay(infoHash: String, fileIndex: Int, streamUrl: String)
    fun onError(infoHash: String, error: String)
}

@Serializable
data class TorrentStreamInfo(
    val infoHash: String,
    val fileIndex: Int,
    val streamUrl: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val isSeekable: Boolean,
    val downloadProgress: Float,
    val bufferProgress: Float,
    val isReadyToPlay: Boolean,
)

data class ExtensionConfig(
    val baseCacheDir: String,
    val baseDataDir: String,
    val preferredLanguage: String,
    val networkTimeoutMs: Long = 30_000L,
    val userAgent: String = "KuroStream/1.0",
    val logLevel: ExtensionLogLevel = ExtensionLogLevel.WARN
)

enum class ExtensionLogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR, NONE }