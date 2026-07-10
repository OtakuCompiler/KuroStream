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

package com.kurostream.extensions.torrserver

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TorrServerTorrent(
    @Json(name = "title") val title: String,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "data") val data: String? = null,
    @Json(name = "hash") val hash: String,
    @Json(name = "torrent_size") val torrentSize: Long? = null,
    @Json(name = "file_stats") val fileStats: List<FileStat>? = null,
    @Json(name = "stat") val stat: Int? = null,
    @Json(name = "stat_string") val statString: String? = null,
    @Json(name = "active_peers") val activePeers: Int? = null,
    @Json(name = "total_peers") val totalPeers: Int? = null,
    @Json(name = "connected_seeders") val connectedSeeders: Int? = null,
    @Json(name = "preload_size") val preloadSize: Long? = null,
    @Json(name = "torrent_speed") val torrentSpeed: Long? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "save_to_db") val saveToDb: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class FileStat(
    @Json(name = "id") val id: Int,
    @Json(name = "path") val path: String,
    @Json(name = "length") val length: Long,
    @Json(name = "viewed") val viewed: Boolean = false
)

@JsonClass(generateAdapter = true)
data class TorrentStat(
    @Json(name = "hash") val hash: String,
    @Json(name = "torrent_size") val torrentSize: Long? = null,
    @Json(name = "preloaded_bytes") val preloadedBytes: Long? = null,
    @Json(name = "preloaded") val preloaded: Long? = null,
    @Json(name = "size") val size: Long? = null,
    @Json(name = "active_peers") val activePeers: Int? = null,
    @Json(name = "total_peers") val totalPeers: Int? = null,
    @Json(name = "connected_seeders") val connectedSeeders: Int? = null,
    @Json(name = "bytes_written") val bytesWritten: Long? = null,
    @Json(name = "bytes_read") val bytesRead: Long? = null,
    @Json(name = "bytes_read_data") val bytesReadData: Long? = null,
    @Json(name = "bytes_read_useful_data") val bytesReadUsefulData: Long? = null,
    @Json(name = "download_speed") val downloadSpeed: Double? = null,
    @Json(name = "upload_speed") val uploadSpeed: Double? = null,
    @Json(name = "has_incoming_connections") val hasIncomingConnections: Boolean? = null,
    @Json(name = "state") val state: Int? = null,
    @Json(name = "state_string") val stateString: String? = null
)
