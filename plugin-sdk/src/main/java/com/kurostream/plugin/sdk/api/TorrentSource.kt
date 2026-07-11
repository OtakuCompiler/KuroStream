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

import com.kurostream.common.result.Result
import com.kurostream.domain.entity.MediaItem

interface TorrentSource : ExtensionApi {
    override val extensionId: String = "torrent_source"

    override suspend fun getVideoSources(episodeId: String): Result<List<VideoSource>> {
        return Result.failure(UnsupportedOperationException("Torrent sources handled by TorrentService"))
    }

    override fun getCapabilities(): Set<ExtensionCapability> {
        return setOf(ExtensionCapability.STREAMING, ExtensionCapability.TORRENT_STREAMING)
    }

    suspend fun addMagnetLink(magnetUri: String, mediaId: String, episodeId: String): Result<TorrentStreamInfo>
    suspend fun addTorrentFile(filePath: String, mediaId: String, episodeId: String): Result<TorrentStreamInfo>
    suspend fun getTorrentStatus(infoHash: String): Result<TorrentStatusInfo>
    suspend fun setFilePriority(infoHash: String, fileIndex: Int, priority: FilePriority): Result<Unit>
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

@Serializable
data class TorrentStatusInfo(
    val infoHash: String,
    val name: String,
    val status: TorrentStatus,
    val progress: Float,
    val downloadSpeed: Long,
    val uploadSpeed: Long,
    val peers: Int,
    val seeds: Int,
    val eta: Long,
    val fileProgress: Map<Int, Float>,
)

enum class TorrentStatus {
    QUEUED,
    CHECKING,
    DOWNLOADING_METADATA,
    DOWNLOADING,
    SEEDING,
    PAUSED,
    ERROR,
    FINISHED,
}

enum class FilePriority { DONT_DOWNLOAD, LOW, NORMAL, HIGH }