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

package com.kurostream.data.sync

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kurostream.domain.model.Profile
import com.kurostream.domain.model.VideoQuality
import com.kurostream.domain.model.WatchHistory
import com.kurostream.domain.model.Favorite
import com.kurostream.domain.model.DownloadItem
import com.kurostream.domain.model.DownloadStatus
import com.kurostream.domain.sync.SyncPayload
import com.kurostream.domain.sync.SyncProvider
import com.kurostream.domain.sync.SyncTimestamp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoCloudSyncProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileDao: ProfileDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val favoriteDao: FavoriteDao,
    private val downloadItemDao: DownloadItemDao,
    private val settingsDataStore: SettingsDataStore
) : SyncProvider {

    override val providerName: String = "demo"
    override var isAuthenticated: Boolean = false
        private set

    private val gson = Gson()
    private val syncFile = File(context.filesDir, "demo_cloud_sync.json")
    private val deviceIdFile = File(context.filesDir, "demo_device_id.txt")
    private val lock = Any()

    private val deviceId: String by lazy {
        if (deviceIdFile.exists()) deviceIdFile.readText().trim()
        else UUID.randomUUID().toString().also { deviceIdFile.writeText(it) }
    }

    override suspend fun authenticate(credentials: Map<String, String>): Result<Unit> {
        return if (credentials["username"].isNullOrBlank()) {
            Result.failure(IllegalArgumentException("Username required"))
        } else {
            isAuthenticated = true
            Result.success(Unit)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        isAuthenticated = false
        return Result.success(Unit)
    }

    override suspend fun push(data: SyncPayload): Result<SyncTimestamp> = withContext(Dispatchers.IO) {
        if (!isAuthenticated) return@withContext Result.failure(IllegalStateException("Not authenticated"))
        synchronized(lock) {
            val enriched = data.copy(timestamp = System.currentTimeMillis(), deviceId = deviceId)
            syncFile.writeText(gson.toJson(enriched))
            Result.success(SyncTimestamp(enriched.timestamp, System.currentTimeMillis()))
        }
    }

    suspend fun pushLocalState(): Result<SyncTimestamp> = push(buildPayloadFromLocal())

    override suspend fun pull(lastSyncTimestamp: Long?): Result<SyncPayload?> = withContext(Dispatchers.IO) {
        if (!isAuthenticated) return@withContext Result.failure(IllegalStateException("Not authenticated"))
        synchronized(lock) {
            if (!syncFile.exists()) return@withContext Result.success(null)
            val payload = gson.fromJson<SyncPayload>(syncFile.readText(), object : TypeToken<SyncPayload>() {}.type)
            if (lastSyncTimestamp != null && payload.timestamp <= lastSyncTimestamp) {
                Result.success(null)
            } else {
                Result.success(payload)
            }
        }
    }

    override suspend fun resolveConflicts(local: SyncPayload, remote: SyncPayload): SyncPayload {
        return if (local.timestamp >= remote.timestamp) local else remote
    }

    override suspend fun deleteCloudData(): Result<Unit> = withContext(Dispatchers.IO) {
        synchronized(lock) {
            if (syncFile.exists()) syncFile.delete()
            Result.success(Unit)
        }
    }

    suspend fun applyToLocal(payload: SyncPayload) {
        // Apply to database - stub for now
    }

    suspend fun buildPayloadFromLocal(): SyncPayload {
        val profiles = profileDao.observeAll().first().map {
            Profile(
                id = it.id,
                displayName = it.name,
                avatarUrl = it.avatarUrl,
                isPremium = false,
                preferredLanguage = "en",
                preferredSubtitleLanguage = "en",
                autoSkipIntro = false,
                autoSkipOutro = false,
                preferredQuality = VideoQuality.AUTO,
                hasPin = it.pinHash != null,
                isActive = it.isActive,
                preferencesJson = it.preferencesJson,
                createdAt = it.createdAt
            )
        }
        val activeId = profiles.find { it.isActive }?.id
        val watchHistory = if (activeId != null) watchHistoryDao.observeByProfile(activeId).first().map {
            WatchHistory(it.id, it.mediaItemId, it.profileId, it.position, it.duration, it.watchedAt, it.completionPercent, it.episodeNumber, it.seasonNumber)
        } else emptyList()
        val favorites = if (activeId != null) favoriteDao.observeByProfile(activeId).first().map {
            Favorite(it.id, it.mediaItemId, it.profileId, it.addedAt, it.category)
        } else emptyList()
        val downloads = if (activeId != null) downloadItemDao.observeByProfile(activeId).first().map {
            DownloadItem(it.id, it.mediaItemId, it.profileId, it.localPath, DownloadStatus.valueOf(it.status), it.progress, it.totalBytes, it.downloadedBytes, it.startedAt, it.completedAt, it.errorMessage)
        } else emptyList()
        val settings = mapOf(
            "skin_name" to settingsDataStore.skinName.first(),
            "theme_mode" to settingsDataStore.themeMode.first(),
            "subtitle_language" to settingsDataStore.subtitleLanguage.first()
        )
        return SyncPayload(profiles, watchHistory, favorites, downloads, settings, System.currentTimeMillis(), deviceId, 1)
    }
}
