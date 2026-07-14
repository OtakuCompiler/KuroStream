package com.kurostream.backup.data

import com.kurostream.backup.domain.*
import com.kurostream.data.anistream.addons.AddonDao
import com.kurostream.data.anistream.downloads.DownloadDao
import com.kurostream.data.anistream.profile.ProfileDao
import com.kurostream.data.anistream.settings.SettingsDao
import com.kurostream.data.local.dao.FavoriteDao
import com.kurostream.data.local.dao.WatchHistoryDao
import com.kurostream.data.local.entity.AddonConfigEntity
import com.kurostream.data.local.entity.BookmarkEntity
import com.kurostream.data.local.entity.FavoriteEntity
import com.kurostream.data.local.entity.HomeRowEntity
import com.kurostream.data.local.entity.SourceLockEntity
import com.kurostream.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BackupDataRestorer @Inject constructor(
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
    suspend fun restore(data: BackupData): RestoreResult = withContext(Dispatchers.IO) {
        var profilesRestored = 0
        var settingsRestored = 0
        var downloadsRestored = 0
        var watchHistoryRestored = 0
        var favoritesRestored = 0
        var sourceLocksRestored = 0
        var homeRowsRestored = 0
        var bookmarksRestored = 0
        var addonConfigsRestored = 0

        data.profiles.forEach { backup ->
            profileDao.insert(
                com.kurostream.data.anistream.profile.ProfileEntity(
                    id = backup.id, name = backup.name, avatarUrl = backup.avatarUrl ?: "",
                    isDefault = backup.isDefault, settings = backup.settings,
                    createdAt = backup.createdAt, lastUsedAt = backup.lastUsedAt
                )
            )
            profilesRestored++
        }
        data.settings.forEach { backup ->
            settingsDao.insert(
                com.kurostream.data.anistream.settings.SettingsEntity(
                    key = backup.key, value = backup.value
                )
            )
            settingsRestored++
        }
        data.downloads.forEach { backup ->
            downloadDao.insert(
                com.kurostream.data.anistream.downloads.DownloadEntity(
                    id = backup.id, mediaId = backup.mediaId, episodeId = backup.episodeId,
                    title = backup.title, filePath = backup.filePath, fileSize = backup.fileSize,
                    progress = backup.progress, status = backup.status, quality = backup.quality ?: "",
                    addedAt = backup.addedAt, completedAt = backup.completedAt ?: 0
                )
            )
            downloadsRestored++
        }
        data.watchHistory.forEach { backup ->
            watchHistoryDao.insert(
                WatchHistoryEntity(
                    id = backup.id, profileId = backup.profileId, mediaId = backup.mediaId,
                    episodeId = backup.episodeId, progress = backup.progress,
                    currentTime = backup.currentTime, totalTime = backup.totalTime,
                    watchedAt = backup.watchedAt
                )
            )
            watchHistoryRestored++
        }
        data.favorites.forEach { backup ->
            favoriteDao.insert(
                FavoriteEntity(
                    id = backup.id, profileId = backup.profileId, mediaId = backup.mediaId,
                    mediaType = backup.mediaType, addedAt = backup.addedAt
                )
            )
            favoritesRestored++
        }
        data.sourceLocks.forEach { backup ->
            sourceLockDao.insert(
                SourceLockEntity(
                    seriesId = backup.id,
                    providerId = backup.extensionId,
                    sourceQuality = "",
                    sourceUrl = backup.sourceUrl,
                    createdAt = backup.createdAt,
                    lastUsedAt = backup.lastUsedAt,
                    episodeCount = 0,
                    fallbackCount = 0,
                    isActive = true
                )
            )
            sourceLocksRestored++
        }
        data.customHomeRows.forEach { backup ->
            homeRowDao.insert(
                HomeRowEntity(
                    id = backup.id, profileId = backup.profileId, title = backup.title,
                    rowType = backup.rowType, sourceExtensionId = backup.sourceExtensionId,
                    query = backup.query, orderIndex = backup.order,
                    isVisible = backup.isVisible, createdAt = backup.createdAt
                )
            )
            homeRowsRestored++
        }
        data.bookmarks.forEach { backup ->
            bookmarkDao.insert(
                BookmarkEntity(
                    id = backup.id, profileId = backup.profileId, mediaId = backup.mediaId,
                    episodeId = backup.episodeId, timestamp = backup.timestamp, note = backup.note
                )
            )
            bookmarksRestored++
        }
        data.addonConfigs.forEach { backup ->
            addonDao.insert(
                AddonConfigEntity(
                    extensionId = backup.extensionId,
                    configJson = backup.config.entries.joinToString("\n") { "${it.key}=${it.value}" },
                    isEnabled = backup.isEnabled, installedAt = backup.installedAt
                )
            )
            addonConfigsRestored++
        }

        RestoreResult(
            profilesRestored = profilesRestored, settingsRestored = settingsRestored,
            downloadsRestored = downloadsRestored, watchHistoryRestored = watchHistoryRestored,
            favoritesRestored = favoritesRestored, sourceLocksRestored = sourceLocksRestored,
            homeRowsRestored = homeRowsRestored, bookmarksRestored = bookmarksRestored,
            addonConfigsRestored = addonConfigsRestored,
        )
    }
}
