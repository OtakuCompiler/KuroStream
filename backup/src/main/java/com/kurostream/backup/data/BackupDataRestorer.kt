package com.kurostream.backup.data

import com.kurostream.backup.domain.*
import com.kurostream.data.local.dao.AddonDao
import com.kurostream.data.local.dao.BookmarkDao
import com.kurostream.data.local.dao.DownloadItemDao
import com.kurostream.data.local.dao.FavoriteDao
import com.kurostream.data.local.dao.HomeRowDao
import com.kurostream.data.local.dao.ProfileDao
import com.kurostream.data.local.dao.SourceLockDao
import com.kurostream.data.local.dao.WatchHistoryDao
import com.kurostream.data.local.entity.AddonConfigEntity
import com.kurostream.data.local.entity.BookmarkEntity
import com.kurostream.data.local.entity.DownloadItemEntity
import com.kurostream.data.local.entity.FavoriteEntity
import com.kurostream.data.local.entity.HomeRowEntity
import com.kurostream.data.local.entity.ProfileEntity
import com.kurostream.data.local.entity.SourceLockEntity
import com.kurostream.data.local.entity.WatchHistoryEntity
import com.kurostream.data.local.preferences.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BackupDataRestorer @Inject constructor(
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
                ProfileEntity(
                    id = backup.id, name = backup.name, avatarUrl = backup.avatarUrl,
                    isActive = backup.isDefault, createdAt = backup.createdAt,
                    pinHash = null, isPremium = false, preferredLanguage = "en",
                    preferredSubtitleLanguage = "en", autoSkipIntro = false,
                    autoSkipOutro = false, preferredQuality = "AUTO", hasPin = false,
                    preferencesJson = null
                )
            )
            profilesRestored++
        }
        data.settings.forEach { backup ->
            settingsDataStore.editPreferences {
                when {
                    backup.value.toBooleanStrictOrNull() != null ->
                        this[booleanPreferencesKey(backup.key)] = backup.value.toBooleanStrict()
                    backup.value.toIntOrNull() != null ->
                        this[intPreferencesKey(backup.key)] = backup.value.toInt()
                    backup.value.toLongOrNull() != null ->
                        this[longPreferencesKey(backup.key)] = backup.value.toLong()
                    backup.value.toFloatOrNull() != null ->
                        this[floatPreferencesKey(backup.key)] = backup.value.toFloat()
                    else ->
                        this[stringPreferencesKey(backup.key)] = backup.value
                }
            }
            settingsRestored++
        }
        data.downloads.forEach { backup ->
            downloadDao.insert(
                DownloadItemEntity(
                    id = backup.id, mediaItemId = backup.mediaId,
                    profileId = "default", localPath = backup.filePath,
                    status = backup.status, progress = backup.progress,
                    totalBytes = backup.fileSize, downloadedBytes = backup.fileSize,
                    startedAt = backup.addedAt, completedAt = backup.completedAt
                )
            )
            downloadsRestored++
        }
        data.watchHistory.forEach { backup ->
            watchHistoryDao.insert(
                WatchHistoryEntity(
                    id = backup.id, profileId = backup.profileId,
                    mediaItemId = backup.mediaId,
                    position = backup.currentTime,
                    duration = backup.totalTime,
                    watchedAt = backup.watchedAt,
                    completionPercent = backup.progress,
                    episodeNumber = backup.episodeId?.toIntOrNull(),
                    seasonNumber = null
                )
            )
            watchHistoryRestored++
        }
        data.favorites.forEach { backup ->
            favoriteDao.insert(
                FavoriteEntity(
                    id = backup.id, profileId = backup.profileId,
                    mediaItemId = backup.mediaId,
                    addedAt = backup.addedAt, category = backup.mediaType
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
