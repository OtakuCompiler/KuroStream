package com.kurostream.data.sync

import com.kurostream.domain.result.Result
import com.kurostream.domain.model.WatchProgress
import com.kurostream.domain.sync.CrossDeviceSyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrossDeviceSyncRepositoryImpl @Inject constructor() : CrossDeviceSyncRepository {

    private val progressStore = mutableMapOf<String, MutableMap<String, StoredProgress>>()
    private val deviceStore = mutableMapOf<String, StoredDevice>()

    private data class StoredProgress(
        val mediaId: String,
        val profileId: String,
        val position: Long = 0,
        val duration: Long = 0,
        val completionPercent: Float = 0f,
        val episodeNumber: Int? = null,
        val seasonNumber: Int? = null,
        val watchedAt: Long = System.currentTimeMillis()
    )

    private data class StoredDevice(
        val deviceId: String,
        val profileId: String,
        val deviceName: String,
        val lastActive: Long = System.currentTimeMillis(),
        val appVersion: String = "1.0.0"
    )

    override suspend fun sync(): Result<Unit> = Result.success(Unit)

    override suspend fun pushData(data: Any): Result<Unit> = Result.success(Unit)

    override suspend fun pullData(): Result<Any?> = Result.success(null)

    override suspend fun syncWatchProgress(progress: WatchProgress): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profileProgress = progressStore.getOrPut(progress.profileId) { mutableMapOf() }
            profileProgress[progress.mediaItemId] = StoredProgress(
                mediaId = progress.mediaItemId,
                profileId = progress.profileId,
                position = progress.position,
                duration = progress.duration,
                completionPercent = progress.completionPercent,
                episodeNumber = progress.episodeNumber,
                seasonNumber = null,
                watchedAt = System.currentTimeMillis()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun getRemoteWatchProgress(profileId: String, mediaId: String): Result<WatchProgress?> = withContext(Dispatchers.IO) {
        try {
            val stored = progressStore[profileId]?.get(mediaId)
            if (stored == null) return@withContext Result.success(null)
            Result.success(
                WatchProgress(
                    id = "$profileId:$mediaId",
                    mediaItemId = stored.mediaId,
                    profileId = stored.profileId,
                    position = stored.position,
                    duration = stored.duration,
                    watchedAt = stored.watchedAt,
                    completionPercent = stored.completionPercent,
                    episodeNumber = stored.episodeNumber,
                    seasonNumber = stored.seasonNumber,
                )
            )
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun getAllRemoteWatchProgress(profileId: String): Result<List<WatchProgress>> = withContext(Dispatchers.IO) {
        try {
            val list = progressStore[profileId]?.map { (mediaId, stored) ->
                WatchProgress(
                    id = "$profileId:$mediaId",
                    mediaItemId = stored.mediaId,
                    profileId = stored.profileId,
                    position = stored.position,
                    duration = stored.duration,
                    watchedAt = stored.watchedAt,
                    completionPercent = stored.completionPercent,
                    episodeNumber = stored.episodeNumber,
                    seasonNumber = stored.seasonNumber,
                )
            } ?: emptyList()
            Result.success(list)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun registerDevice(profileId: String, deviceId: String, deviceName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            deviceStore[deviceId] = StoredDevice(
                deviceId = deviceId,
                profileId = profileId,
                deviceName = deviceName,
                lastActive = System.currentTimeMillis()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun updateDeviceHeartbeat(deviceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            deviceStore[deviceId] = deviceStore[deviceId]?.copy(lastActive = System.currentTimeMillis())
                ?: return@withContext Result.success(Unit)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override suspend fun getDevicesForProfile(profileId: String): Result<List<CrossDeviceSyncRepository.DeviceInfo>> = withContext(Dispatchers.IO) {
        try {
            val devices = deviceStore.filter { it.value.profileId == profileId }.map { (_, stored) ->
                CrossDeviceSyncRepository.DeviceInfo(
                    deviceId = stored.deviceId,
                    profileId = stored.profileId,
                    deviceName = stored.deviceName,
                    lastActive = stored.lastActive,
                    appVersion = stored.appVersion,
                )
            }
            Result.success(devices)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override fun observeRemoteWatchProgress(profileId: String, mediaId: String): Flow<Result<WatchProgress?>> {
        return MutableStateFlow(Result.success<WatchProgress?>(null))
    }
}
