package com.kurostream.extensions.debrid

import com.kurostream.data.debrid.RealDebridApi
import com.kurostream.data.debrid.RealDebridApiClient
import com.kurostream.data.debrid.RdAvailabilityRequest
import com.kurostream.data.debrid.RdMagnetRequest
import com.kurostream.data.debrid.RdTorrentFileRequest
import com.kurostream.data.debrid.RdUnrestrictRequest
import com.kurostream.domain.debrid.DebridManager
import com.kurostream.domain.debrid.DebridResult
import com.kurostream.domain.debrid.RdTorrentInfo
import com.kurostream.domain.debrid.RdUser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebridManagerImpl @Inject constructor() : DebridManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var api: RealDebridApi? = null
    private val transfersState = MutableStateFlow<List<RdTorrentInfo>>(emptyList())

    fun initialize(apiKey: String) {
        api = RealDebridApiClient.create(apiKey)
        startPolling()
    }

    override suspend fun checkCache(hashes: List<String>): Map<String, Boolean> {
        val api = api ?: return emptyMap()
        return try {
            val response = api.checkInstantAvailability(RdAvailabilityRequest(hashes))
            response.rd.mapValues { it.value.isNotEmpty() }
        } catch (e: Exception) {
            Timber.e(e, "Debrid cache check failed")
            emptyMap()
        }
    }

    override suspend fun addMagnet(magnet: String): DebridResult<String> {
        val api = api ?: return DebridResult.Error(Exception("API not initialized"))
        return try {
            val response = api.addMagnet(RdMagnetRequest(magnet = magnet))
            pollForCompletion(response.id)
            DebridResult.Success(response.id)
        } catch (e: Exception) {
            Timber.e(e, "Add magnet failed")
            DebridResult.Error(e)
        }
    }

    override suspend fun addTorrentFile(fileBytes: ByteArray): DebridResult<String> {
        val api = api ?: return DebridResult.Error(Exception("API not initialized"))
        return try {
            val response = api.addTorrentFile(RdTorrentFileRequest(torrent = fileBytes.encodeBase64()))
            pollForCompletion(response.id)
            DebridResult.Success(response.id)
        } catch (e: Exception) {
            Timber.e(e, "Add torrent file failed")
            DebridResult.Error(e)
        }
    }

    override suspend fun getDownloadLink(torrentId: String): DebridResult<String> {
        val api = api ?: return DebridResult.Error(Exception("API not initialized"))
        return try {
            val info = api.getTorrentInfo(torrentId)
            val link = info.links.firstOrNull()
                ?: return DebridResult.Error(Exception("No download links available"))
            val unrestricted = api.unrestrictLink(RdUnrestrictRequest(link = link))
            DebridResult.Success(unrestricted.download)
        } catch (e: Exception) {
            Timber.e(e, "Get download link failed")
            DebridResult.Error(e)
        }
    }

    override suspend fun getUserInfo(): DebridResult<RdUser> {
        val api = api ?: return DebridResult.Error(Exception("API not initialized"))
        return try {
            val response = api.getUser()
            DebridResult.Success(RdUser(response.id, response.username, response.email, response.points, response.avatar, response.type, response.expiration))
        } catch (e: Exception) {
            Timber.e(e, "Get user info failed")
            DebridResult.Error(e)
        }
    }

    override suspend fun getTransfers(): DebridResult<List<RdTorrentInfo>> {
        val api = api ?: return DebridResult.Error(Exception("API not initialized"))
        return try {
            val transfers = api.getTorrents().map { RdTorrentInfo(it.id, it.filename, it.status, it.progress, it.links) }
            DebridResult.Success(transfers)
        } catch (e: Exception) {
            Timber.e(e, "Get transfers failed")
            DebridResult.Error(e)
        }
    }

    override fun observeTransfers(): Flow<List<RdTorrentInfo>> = transfersState.asStateFlow()

    private suspend fun pollForCompletion(torrentId: String) {
        val api = api ?: return
        repeat(60) {
            delay(2000)
            try {
                val info = api.getTorrentInfo(torrentId)
                if (info.status == "downloaded") {
                    return
                }
            } catch (e: Exception) {
                Timber.w(e, "Polling torrent $torrentId")
            }
        }
    }

    private fun startPolling() {
        scope.launch {
            while (isActive) {
                delay(10000)
                try {
                    val result = getTransfers()
                    if (result is DebridResult.Success) {
                        transfersState.update { result.data }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Polling transfers")
                }
            }
        }
    }

    private fun ByteArray.encodeBase64(): String = java.util.Base64.getEncoder().encodeToString(this)
}
