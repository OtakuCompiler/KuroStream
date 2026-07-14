package com.kurostream.backup.data

import com.kurostream.backup.domain.BackupData
import com.kurostream.backup.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BackupDataCollector @Inject constructor(
    private val profileDao: ProfileDao,
    private val downloadDao: DownloadDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val favoriteDao: FavoriteDao,
    private val settingsDao: SettingsDao,
    private val sourceLockDao: SourceLockDao,
    private val homeRowDao: HomeRowDao,
    private val bookmarkDao: BookmarkDao,
    private val addonDao: AddonDao,
) {
    suspend fun collect(): BackupData = withContext(Dispatchers.IO) {
        BackupData(
            profiles = profileDao.getAll().map {
                ProfileBackup(id = it.id, name = it.name, avatarUrl = it.avatarUrl,
                    isDefault = it.isDefault, settings = it.settings,
                    createdAt = it.createdAt, lastUsedAt = it.lastUsedAt)
            },
            settings = settingsDao.getAll().map { SettingBackup(key = it.key, value = it.value) },
            downloads = downloadDao.getAll().map {
                DownloadBackup(id = it.id, mediaId = it.mediaId, episodeId = it.episodeId,
                    title = it.title, filePath = it.filePath, fileSize = it.fileSize,
                    progress = it.progress, status = it.status, quality = it.quality,
                    addedAt = it.addedAt, completedAt = it.completedAt)
            },
            watchHistory = watchHistoryDao.getAll().map {
                WatchHistoryBackup(id = it.id, profileId = it.profileId, mediaId = it.mediaId,
                    episodeId = it.episodeId, progress = it.progress, currentTime = it.currentTime,
                    totalTime = it.totalTime, watchedAt = it.watchedAt)
            },
            favorites = favoriteDao.getAll().map {
                FavoriteBackup(id = it.id, profileId = it.profileId, mediaId = it.mediaId,
                    mediaType = it.mediaType, addedAt = it.addedAt)
            },
            sourceLocks = sourceLockDao.getAll().map {
                SourceLockBackup(id = it.id, profileId = it.profileId, mediaId = it.mediaId,
                    extensionId = it.extensionId, sourceUrl = it.sourceUrl,
                    fallbackSourceUrl = it.fallbackSourceUrl, createdAt = it.createdAt,
                    lastUsedAt = it.lastUsedAt)
            },
            customHomeRows = homeRowDao.getAll().map {
                HomeRowBackup(id = it.id, profileId = it.profileId, title = it.title,
                    rowType = it.rowType, sourceExtensionId = it.sourceExtensionId,
                    query = it.query, order = it.order, isVisible = it.isVisible,
                    createdAt = it.createdAt)
            },
            bookmarks = bookmarkDao.getAll().map {
                BookmarkBackup(id = it.id, profileId = it.profileId, mediaId = it.mediaId,
                    episodeId = it.episodeId, timestamp = it.timestamp, note = it.note)
            },
            addonConfigs = addonDao.getAll().map {
                AddonConfigBackup(extensionId = it.extensionId, config = it.config,
                    isEnabled = it.isEnabled, installedAt = it.installedAt)
            }
        )
    }
}
