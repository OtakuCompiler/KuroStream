package com.kurostream.domain.debrid

interface DebridManager {
    suspend fun checkCache(hashes: List<String>): Map<String, Boolean>
    suspend fun addMagnet(magnet: String): DebridResult<String>
    suspend fun addTorrentFile(fileBytes: ByteArray): DebridResult<String>
    suspend fun getDownloadLink(torrentId: String): DebridResult<String>
    suspend fun getUserInfo(): DebridResult<RdUser>
    suspend fun getTransfers(): DebridResult<List<RdTorrentInfo>>
    fun observeTransfers(): kotlinx.coroutines.flow.Flow<List<RdTorrentInfo>>
}

sealed class DebridResult<T> {
    data class Success<T>(val data: T) : DebridResult<T>()
    data class Error<T>(val exception: Exception) : DebridResult<T>()
}
