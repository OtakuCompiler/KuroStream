package com.kurostream.backup.data

import com.kurostream.backup.domain.BackupData
import com.kurostream.backup.domain.*
import com.kurostream.data.local.dao.AddonDao
import com.kurostream.data.local.dao.BookmarkDao
import com.kurostream.data.local.dao.DownloadItemDao
import com.kurostream.data.local.dao.FavoriteDao
import com.kurostream.data.local.dao.HomeRowDao
import com.kurostream.data.local.dao.ProfileDao
import com.kurostream.data.local.dao.SourceLockDao
import com.kurostream.data.local.dao.WatchHistoryDao
import com.kurostream.data.local.preferences.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BackupDataCollector @Inject constructor(
    private val profileDao: ProfileDao,
    private val downloadDao: DownloadItemDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val favoriteDao: FavoriteDao,
    private val settingsDataStore: SettingsDataStore,
    private val sourceLockDao: SourceLockDao,
    private val homeRowDao: HomeRowDao,
    private val bookmarkDao: BookmarkDao,
    private val addonDao: AddonDao,
) {
    suspend fun collect(): BackupData = withContext(Dispatchers.IO) {
        BackupData(
            profiles = profileDao.getAll().map {
                ProfileBackup(
                    id = it.id, name = it.name, avatarUrl = it.avatarUrl,
                    isDefault = it.isActive,
                    settings = emptyMap(), // ProfileEntity uses preferencesJson (String?) instead of Map
                    createdAt = it.createdAt, lastUsedAt = it.createdAt
                )
            },
            settings = settingsDataStore.data.first().asMap().map { (key, value) ->
                SettingBackup(key = key.name, value = value?.toString() ?: "")
            },
            downloads = downloadDao.getAll().map {
                DownloadBackup(
                    id = it.id, mediaId = it.mediaItemId, episodeId = "",
                    title = "", filePath = it.localPath, fileSize = it.totalBytes,
                    progress = it.progress, status = it.status, quality = null,
                    addedAt = it.startedAt, completedAt = it.completedAt
                )
            },
            watchHistory = watchHistoryDao.getAll().map {
                WatchHistoryBackup(
                    id = it.id, profileId = it.profileId, mediaId = it.mediaItemId,
                    episodeId = it.episodeNumber?.toString() ?: "", progress = it.completionPercent,
                    currentTime = it.position, totalTime = it.duration,
                    watchedAt = it.watchedAt
                )
            },
            favorites = favoriteDao.getAll().map {
                FavoriteBackup(
                    id = it.id, profileId = it.profileId, mediaId = it.mediaItemId,
                    mediaType = it.category, addedAt = it.addedAt
                )
            },
            sourceLocks = sourceLockDao.getAll().map {
                SourceLockBackup(
                    id = it.seriesId, profileId = "", mediaId = "",
                    extensionId = it.providerId, sourceUrl = it.sourceUrl,
                    fallbackSourceUrl = null, createdAt = it.createdAt,
                    lastUsedAt = it.lastUsedAt
                )
            },
            customHomeRows = homeRowDao.getAll().map {
                HomeRowBackup(
                    id = it.id, profileId = it.profileId, title = it.title,
                    rowType = it.rowType, sourceExtensionId = it.sourceExtensionId,
                    query = it.query, order = it.orderIndex, isVisible = it.isVisible,
                    createdAt = it.createdAt
                )
            },
            bookmarks = bookmarkDao.getAll().map {
                BookmarkBackup(
                    id = it.id, profileId = it.profileId, mediaId = it.mediaId,
                    episodeId = it.episodeId, timestamp = it.timestamp, note = it.note
                )
            },
            addonConfigs = addonDao.getAll().map {
                AddonConfigBackup(
                    extensionId = it.extensionId,
                    config = it.configJson.lines()
                        .filter { line -> line.contains('=') }
                        .associate { line ->
                            val eq = line.indexOf('=')
                            line.substring(0, eq) to line.substring(eq + 1)
                        },
                    isEnabled = it.isEnabled, installedAt = it.installedAt
                )
            }
        )
    }
}
