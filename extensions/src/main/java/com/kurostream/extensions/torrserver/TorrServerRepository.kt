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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrServerRepository @Inject constructor(
    private val api: TorrServerApi,
    private val config: TorrServerConfig
) {
    fun isServerReachable(): Flow<Result<Boolean>> = flow {
        try {
            val response = api.echo()
            emit(Result.success(response.isSuccessful))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun updateSettings(): Result<Map<String, Any>> = try {
        val response = api.updateSettings(config.getSettingsPayload())
        if (response.isSuccessful) {
            Result.success(response.body() ?: emptyMap())
        } else {
            Result.failure(Exception("Failed to update settings: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun listTorrents(): Flow<Result<List<TorrServerTorrent>>> = flow {
        try {
            val response = api.listTorrents()
            if (response.isSuccessful) {
                emit(Result.success(response.body() ?: emptyList()))
            } else {
                emit(Result.failure(Exception("Failed to list torrents: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun addTorrent(magnetLink: String, title: String? = null): Result<TorrServerTorrent> = try {
        val response = api.addTorrent(AddTorrentRequest(link = magnetLink, title = title))
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("Failed to add torrent: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun addTorrentFile(fileData: ByteArray, title: String? = null): Result<TorrServerTorrent> = try {
        val base64Link = "data:application/x-bittorrent;base64," + android.util.Base64.encodeToString(fileData, android.util.Base64.NO_WRAP)
        val response = api.addTorrent(AddTorrentRequest(link = base64Link, title = title))
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("Failed to add torrent file: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun removeTorrent(hash: String): Result<Unit> = try {
        val response = api.removeTorrent(hash)
        if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to remove torrent: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun dropTorrent(hash: String): Result<Unit> = try {
        val response = api.actionTorrent(hash, "drop")
        if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to drop torrent: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getTorrentStat(hash: String): Flow<Result<TorrentStat>> = flow {
        try {
            val response = api.getTorrentStat(hash)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("Failed to get stat: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun buildStreamUrl(hash: String, fileIndex: Int): String {
        return "${config.serverUrl}/stream/$hash/$fileIndex"
    }

    fun buildPlaylistUrl(hash: String, fileIndex: Int): String {
        return "${config.serverUrl}/playlist/$hash/$fileIndex.m3u"
    }

    fun getMediaItems(torrent: TorrServerTorrent): List<TorrServerMediaItem> {
        return torrent.fileStats?.map { file ->
            TorrServerMediaItem(
                id = "${torrent.hash}_${file.id}",
                title = file.path.substringAfterLast("/"),
                fullPath = file.path,
                size = file.length,
                fileIndex = file.id,
                torrentHash = torrent.hash,
                torrentTitle = torrent.title,
                poster = torrent.poster
            )
        } ?: emptyList()
    }
}

data class TorrServerMediaItem(
    val id: String,
    val title: String,
    val fullPath: String,
    val size: Long,
    val fileIndex: Int,
    val torrentHash: String,
    val torrentTitle: String,
    val poster: String?
)
