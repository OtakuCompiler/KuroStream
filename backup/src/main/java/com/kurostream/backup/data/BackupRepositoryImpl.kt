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

package com.kurostream.backup.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.kurostream.backup.domain.*
import com.kurostream.common.result.Result
import com.kurostream.data.anistream.addons.AddonDao
import com.kurostream.data.anistream.downloads.DownloadDao
import com.kurostream.data.anistream.profile.ProfileDao
import com.kurostream.data.anistream.search.RecentSearchDao
import com.kurostream.data.anistream.settings.SettingsDao
import com.kurostream.data.anistream.sync.SyncProviderDao
import com.kurostream.data.anistream.introskip.IntroSkipDao
import com.kurostream.data.local.dao.FavoriteDao
import com.kurostream.data.local.dao.MediaItemDao
import com.kurostream.data.local.dao.WatchHistoryDao
import com.kurostream.data.local.entity.FavoriteEntity
import com.kurostream.data.local.entity.WatchHistoryEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: GitHubApiService,
    private val profileDao: ProfileDao,
    private val downloadDao: DownloadDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val favoriteDao: FavoriteDao,
    private val settingsDao: SettingsDao,
    private val sourceLockDao: SourceLockDao,
    private val homeRowDao: HomeRowDao,
    private val bookmarkDao: BookmarkDao,
    private val addonDao: AddonDao,
    private val settingsDataStore: SettingsDataStore,
) : BackupRepository {

    private val _authState = MutableStateFlow(GitHubAuthState())
    override val authState: Flow<GitHubAuthState> = _authState.distinctUntilChanged()

    private val _backupConfig = MutableStateFlow(BackupConfig())
    override val backupConfig: Flow<BackupConfig> = _backupConfig.distinctUntilChanged()

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private val masterKey: MasterKey by lazy { createMasterKey() }
    private val encryptedPrefs by lazy { createEncryptedPrefs() }

    private val GITHUB_CLIENT_ID = "YOUR_GITHUB_CLIENT_ID"
    private val GITHUB_SCOPES = "gist,repo"

    init {
        loadAuthState()
        loadBackupConfig()
    }

    private fun createMasterKey(): MasterKey {
        val spec = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return spec
    }

    private fun createEncryptedPrefs() = EncryptedSharedPreferences.create(
        "github_auth",
        masterKey,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun loadAuthState() {
        val token = encryptedPrefs.getString("access_token", null)
        val username = encryptedPrefs.getString("username", null)
        val userId = encryptedPrefs.getLong("user_id", -1L)
        val avatarUrl = encryptedPrefs.getString("avatar_url", null)
        val expiresAt = encryptedPrefs.getLong("expires_at", -1L)

        if (token != null && username != null) {
            _authState.value = GitHubAuthState(
                isAuthenticated = true,
                accessToken = token,
                username = username,
                userId = if (userId > 0) userId else null,
                avatarUrl = avatarUrl,
                expiresAt = if (expiresAt > 0) expiresAt else null,
            )
        }
    }

    private fun loadBackupConfig() {
        _backupConfig.value = BackupConfig(
            backupType = BackupType.valueOf(settingsDataStore.getString("backup_type", "GIST")),
            gistId = settingsDataStore.getString("backup_gist_id", null),
            repoOwner = settingsDataStore.getString("backup_repo_owner", null),
            repoName = settingsDataStore.getString("backup_repo_name", null),
            repoBranch = settingsDataStore.getString("backup_repo_branch", "main"),
            repoPath = settingsDataStore.getString("backup_repo_path", "kurostream_backup.json.enc"),
            autoBackupEnabled = settingsDataStore.getBoolean("backup_auto_enabled", false),
            autoBackupIntervalDays = settingsDataStore.getInt("backup_auto_interval_days", 7),
            lastBackupTimestamp = settingsDataStore.getLong("backup_last_timestamp", 0),
            encryptBackups = settingsDataStore.getBoolean("backup_encrypt", true),
            compressionEnabled = settingsDataStore.getBoolean("backup_compress", true),
        )
    }

    override suspend fun authenticate(): Result<GitHubAuthState> = withContext(Dispatchers.IO) {
        try {
            // Use GitHub Device Flow for TV apps
            val deviceCodeResponse = requestDeviceCode()
            if (deviceCodeResponse is Result.Failure) return@withContext deviceCodeResponse

            val deviceCode = deviceCodeResponse.data
            val userCode = deviceCode.userCode

            // Poll for token
            val tokenResult = pollForToken(deviceCode.deviceCode, deviceCode.interval)
            if (tokenResult is Result.Failure) return@withContext tokenResult

            val accessToken = tokenResult.data.accessToken
            val userResult = apiService.getUser("Bearer $accessToken")
            if (userResult is Result.Failure) return@withContext userResult

            val user = userResult.data
            val authState = GitHubAuthState(
                isAuthenticated = true,
                accessToken = accessToken,
                username = user.login,
                userId = user.id,
                avatarUrl = user.avatarUrl,
            )

            saveAuthState(authState)
            _authState.value = authState
            Result.Success(authState)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    private suspend fun requestDeviceCode(): Result<DeviceCodeResponse> = withContext(Dispatchers.IO) {
        // In production, use a proper HTTP client for device flow
        // This is a simplified implementation
        TODO("Implement device code request")
    }

    private suspend fun pollForToken(deviceCode: String, interval: Int): Result<TokenResponse> = withContext(Dispatchers.IO) {
        TODO("Implement token polling")
    }

    override suspend fun checkAuth(): Result<GitHubAuthState> = withContext(Dispatchers.IO) {
        val token = encryptedPrefs.getString("access_token", null) ?: return@withContext Result.Failure(Exception("No token"))
        val userResult = apiService.getUser("Bearer $token")
        if (userResult is Result.Failure) {
            logout()
            return@withContext userResult
        }
        val authState = _authState.value
        Result.Success(authState)
    }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().clear().apply()
        _authState.value = GitHubAuthState()
        Result.Success(Unit)
    }

    override fun observeAuthState(): Flow<GitHubAuthState> = authState

    override suspend fun createBackup(config: BackupConfig, password: String?): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            val backupData = collectBackupData()
            val jsonString = json.encodeToString(backupData)
            val encryptedData = if (config.encryptBackups && password != null) {
                encrypt(jsonString.toByteArray(), password)
            } else {
                jsonString.toByteArray()
            }

            val base64Data = android.util.Base64.encodeToString(encryptedData, android.util.Base64.NO_WRAP)
            val timestamp = System.currentTimeMillis()
            val fileName = "kurostream_backup_${timestamp}.json${if (config.encryptBackups) ".enc" else ""}"

            val metadata = when (config.backupType) {
                BackupType.GIST -> {
                    val description = "KuroStream Backup - ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(timestamp))}"
                    val request = GistRequest(
                        description = description,
                        public = false,
                        files = mapOf(fileName to GistFile(content = base64Data))
                    )
                    val authHeader = "Bearer ${_authState.value.accessToken}"
                    val result = apiService.createGist(authHeader, request)
                    if (result is Result.Failure) return@withContext result

                    val gist = result.data
                    updateBackupConfig(config.copy(gistId = gist.id, lastBackupTimestamp = timestamp))

                    BackupMetadata(
                        id = gist.id,
                        type = BackupType.GIST,
                        timestamp = timestamp,
                        size = encryptedData.size.toLong(),
                        description = description,
                        gistUrl = gist.htmlUrl,
                        profileCount = backupData.profiles.size,
                        settingsCount = backupData.settings.size,
                        fileCount = backupData.downloads.size,
                    )
                }
                BackupType.REPOSITORY -> {
                    val authHeader = "Bearer ${_authState.value.accessToken}"
                    val request = RepoContentRequest(
                        message = "KuroStream Backup - ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(timestamp))}",
                        content = base64Data,
                    )
                    val result = apiService.createOrUpdateFile(
                        authHeader,
                        config.repoOwner!!,
                        config.repoName!!,
                        config.repoPath,
                        request
                    )
                    if (result is Result.Failure) return@withContext result

                    updateBackupConfig(config.copy(lastBackupTimestamp = timestamp))

                    BackupMetadata(
                        id = result.data.content?.sha ?: "repo_$timestamp",
                        type = BackupType.REPOSITORY,
                        timestamp = timestamp,
                        size = encryptedData.size.toLong(),
                        description = "Repository backup",
                        repoUrl = "https://github.com/${config.repoOwner}/${config.repoName}",
                        profileCount = backupData.profiles.size,
                        settingsCount = backupData.settings.size,
                        fileCount = backupData.downloads.size,
                    )
                }
            }

            Result.Success(metadata)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun listBackups(config: BackupConfig): Result<List<BackupMetadata>> = withContext(Dispatchers.IO) {
        try {
            val authHeader = "Bearer ${_authState.value.accessToken}"
            when (config.backupType) {
                BackupType.GIST -> {
                    config.gistId?.let { gistId ->
                        val result = apiService.getGist(authHeader, gistId)
                        if (result is Result.Success) {
                            val gist = result.data
                            val metadata = BackupMetadata(
                                id = gist.id,
                                type = BackupType.GIST,
                                timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                                    .parse(gist.createdAt)?.time ?: 0,
                                size = gist.files.values.sumOf { it.size },
                                description = gist.description,
                                gistUrl = gist.htmlUrl,
                            )
                            return@withContext Result.Success(listOf(metadata))
                        }
                    }
                    Result.Success(emptyList())
                }
                BackupType.REPOSITORY -> {
                    val result = apiService.getFile(authHeader, config.repoOwner!!, config.repoName!!, config.repoPath, config.repoBranch)
                    if (result is Result.Success && result.data != null) {
                        val file = result.data!!
                        val metadata = BackupMetadata(
                            id = file.sha,
                            type = BackupType.REPOSITORY,
                            timestamp = 0, // Would need commit history
                            size = file.size,
                            repoUrl = "https://github.com/${config.repoOwner}/${config.repoName}",
                        )
                        Result.Success(listOf(metadata))
                    } else {
                        Result.Success(emptyList())
                    }
                }
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun restoreBackup(config: BackupConfig, metadata: BackupMetadata, password: String?): Result<RestoreResult> = withContext(Dispatchers.IO) {
        try {
            val authHeader = "Bearer ${_authState.value.accessToken}"
            val base64Data = when (config.backupType) {
                BackupType.GIST -> {
                    val result = apiService.getGist(authHeader, metadata.id)
                    if (result is Result.Failure) return@withContext result
                    result.data.files.values.first().content ?: return@withContext Result.Failure(Exception("Empty gist content"))
                }
                BackupType.REPOSITORY -> {
                    val result = apiService.getFile(authHeader, config.repoOwner!!, config.repoName!!, config.repoPath, config.repoBranch)
                    if (result is Result.Failure) return@withContext result
                    result.data?.content ?: return@withContext Result.Failure(Exception("Empty file content"))
                }
            }

            val encryptedData = android.util.Base64.decode(base64Data, android.util.Base64.NO_WRAP)
            val decryptedData = if (config.encryptBackups && password != null) {
                decrypt(encryptedData, password)
            } else {
                encryptedData
            }

            val jsonString = String(decryptedData, Charsets.UTF_8)
            val backupData = json.decodeFromString<BackupData>(jsonString)

            val result = restoreBackupData(backupData)
            Result.Success(result)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun deleteBackup(config: BackupConfig, metadata: BackupMetadata): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val authHeader = "Bearer ${_authState.value.accessToken}"
            when (config.backupType) {
                BackupType.GIST -> {
                    apiService.deleteGist(authHeader, metadata.id)
                }
                BackupType.REPOSITORY -> {
                    val result = apiService.getFile(authHeader, config.repoOwner!!, config.repoName!!, config.repoPath, config.repoBranch)
                    if (result is Result.Success && result.data != null) {
                        apiService.deleteFile(authHeader, config.repoOwner!!, config.repoName!!, config.repoPath,
                            RepoContentRequest(message = "Delete backup", content = "", sha = result.data!!.sha)
                        )
                    } else {
                        Result.Success(Unit)
                    }
                }
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override fun observeBackupConfig(): Flow<BackupConfig> = backupConfig

    override suspend fun updateBackupConfig(config: BackupConfig) = withContext(Dispatchers.IO) {
        settingsDataStore.setString("backup_type", config.backupType.name)
        config.gistId?.let { settingsDataStore.setString("backup_gist_id", it) }
        config.repoOwner?.let { settingsDataStore.setString("backup_repo_owner", it) }
        config.repoName?.let { settingsDataStore.setString("backup_repo_name", it) }
        settingsDataStore.setString("backup_repo_branch", config.repoBranch)
        settingsDataStore.setString("backup_repo_path", config.repoPath)
        settingsDataStore.setBoolean("backup_auto_enabled", config.autoBackupEnabled)
        settingsDataStore.setInt("backup_auto_interval_days", config.autoBackupIntervalDays)
        settingsDataStore.setLong("backup_last_timestamp", config.lastBackupTimestamp)
        settingsDataStore.setBoolean("backup_encrypt", config.encryptBackups)
        settingsDataStore.setBoolean("backup_compress", config.compressionEnabled)
        _backupConfig.value = config
    }

    override suspend fun getAuthUrl(): String = withContext(Dispatchers.IO) {
        "https://github.com/login/device?scope=${GITHUB_SCOPES}&client_id=${GITHUB_CLIENT_ID}"
    }

    private suspend fun collectBackupData(): BackupData = withContext(Dispatchers.IO) {
        BackupData(
            profiles = profileDao.getAll().map { ProfileBackup(
                id = it.id, name = it.name, avatarUrl = it.avatarUrl, isDefault = it.isDefault,
                settings = it.settings, createdAt = it.createdAt, lastUsedAt = it.lastUsedAt
            ) },
            settings = settingsDao.getAll().map { SettingBackup(key = it.key, value = it.value) },
            downloads = downloadDao.getAll().map { DownloadBackup(
                id = it.id, mediaId = it.mediaId, episodeId = it.episodeId, title = it.title,
                filePath = it.filePath, fileSize = it.fileSize, progress = it.progress,
                status = it.status, quality = it.quality, addedAt = it.addedAt, completedAt = it.completedAt
            ) },
            watchHistory = watchHistoryDao.getAll().map { WatchHistoryBackup(
                id = it.id, profileId = it.profileId, mediaId = it.mediaId, episodeId = it.episodeId,
                progress = it.progress, currentTime = it.currentTime, totalTime = it.totalTime, watchedAt = it.watchedAt
            ) },
            favorites = favoriteDao.getAll().map { FavoriteBackup(
                id = it.id, profileId = it.profileId, mediaId = it.mediaId, mediaType = it.mediaType, addedAt = it.addedAt
            ) },
            sourceLocks = sourceLockDao.getAll().map { SourceLockBackup(
                id = it.id, profileId = it.profileId, mediaId = it.mediaId, extensionId = it.extensionId,
                sourceUrl = it.sourceUrl, fallbackSourceUrl = it.fallbackSourceUrl,
                createdAt = it.createdAt, lastUsedAt = it.lastUsedAt
            ) },
            customHomeRows = homeRowDao.getAll().map { HomeRowBackup(
                id = it.id, profileId = it.profileId, title = it.title, rowType = it.rowType,
                sourceExtensionId = it.sourceExtensionId, query = it.query, order = it.order,
                isVisible = it.isVisible, createdAt = it.createdAt
            ) },
            bookmarks = bookmarkDao.getAll().map { BookmarkBackup(
                id = it.id, profileId = it.profileId, mediaId = it.mediaId, episodeId = it.episodeId,
                timestamp = it.timestamp, note = it.note
            ) },
            addonConfigs = addonDao.getAll().map { AddonConfigBackup(
                extensionId = it.extensionId, config = it.config, isEnabled = it.isEnabled, installedAt = it.installedAt
            ) }
        )
    }

    private suspend fun restoreBackupData(data: BackupData): RestoreResult = withContext(Dispatchers.IO) {
        var profilesRestored = 0
        var settingsRestored = 0
        var downloadsRestored = 0
        var watchHistoryRestored = 0
        var favoritesRestored = 0
        var sourceLocksRestored = 0
        var homeRowsRestored = 0
        var bookmarksRestored = 0
        var addonConfigsRestored = 0

        data.profiles.forEach {
            profileDao.insert(/* convert to entity */)
            profilesRestored++
        }
        data.settings.forEach {
            settingsDao.insert(/* convert to entity */)
            settingsRestored++
        }
        data.downloads.forEach {
            downloadDao.insert(/* convert to entity */)
            downloadsRestored++
        }
        data.watchHistory.forEach {
            watchHistoryDao.insert(/* convert to entity */)
            watchHistoryRestored++
        }
        data.favorites.forEach {
            favoriteDao.insert(/* convert to entity */)
            favoritesRestored++
        }
        data.sourceLocks.forEach {
            sourceLockDao.insert(/* convert to entity */)
            sourceLocksRestored++
        }
        data.customHomeRows.forEach {
            homeRowDao.insert(/* convert to entity */)
            homeRowsRestored++
        }
        data.bookmarks.forEach {
            bookmarkDao.insert(/* convert to entity */)
            bookmarksRestored++
        }
        data.addonConfigs.forEach {
            addonDao.insert(/* convert to entity */)
            addonConfigsRestored++
        }

        RestoreResult(
            profilesRestored = profilesRestored,
            settingsRestored = settingsRestored,
            downloadsRestored = downloadsRestored,
            watchHistoryRestored = watchHistoryRestored,
            favoritesRestored = favoritesRestored,
            sourceLocksRestored = sourceLocksRestored,
            homeRowsRestored = homeRowsRestored,
            bookmarksRestored = bookmarksRestored,
            addonConfigsRestored = addonConfigsRestored,
        )
    }

    private fun saveAuthState(state: GitHubAuthState) {
        encryptedPrefs.edit()
            .putString("access_token", state.accessToken ?: "")
            .putString("username", state.username ?: "")
            .putLong("user_id", state.userId ?: -1)
            .putString("avatar_url", state.avatarUrl ?: "")
            .putLong("expires_at", state.expiresAt ?: -1)
            .apply()
    }

    private fun encrypt(data: ByteArray, password: String): ByteArray {
        val key = deriveKey(password)
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    private fun decrypt(data: ByteArray, password: String): ByteArray {
        val key = deriveKey(password)
        val iv = data.copyOfRange(0, 12)
        val encrypted = data.copyOfRange(12, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(encrypted)
    }

    private fun deriveKey(password: String): SecretKeySpec {
        val passwordBytes = password.toByteArray(Charsets.UTF_8)
        val keyBytes = ByteArray(32)
        System.arraycopy(passwordBytes, 0, keyBytes, 0, minOf(passwordBytes.size, 32))
        if (passwordBytes.size < 32) {
            val hash = java.security.MessageDigest.getInstance("SHA-256").digest(passwordBytes)
            System.arraycopy(hash, 0, keyBytes, passwordBytes.size, 32 - passwordBytes.size)
        }
        return SecretKeySpec(keyBytes, "AES")
    }

    // DAO interfaces needed
    interface SourceLockDao { suspend fun getAll(): List<SourceLockEntity>; suspend fun insert(entity: SourceLockEntity) }
    interface HomeRowDao { suspend fun getAll(): List<HomeRowEntity>; suspend fun insert(entity: HomeRowEntity) }
    interface BookmarkDao { suspend fun getAll(): List<BookmarkEntity>; suspend fun insert(entity: BookmarkEntity) }
    interface AddonDao { suspend fun getAll(): List<AddonConfigEntity>; suspend fun insert(entity: AddonConfigEntity) }
    interface SettingsDataStore {
        suspend fun getString(key: String, default: String): String
        suspend fun setString(key: String, value: String)
        suspend fun getBoolean(key: String, default: Boolean): Boolean
        suspend fun setBoolean(key: String, value: Boolean)
        suspend fun getInt(key: String, default: Int): Int
        suspend fun setInt(key: String, value: Int)
        suspend fun getLong(key: String, default: Long): Long
        suspend fun setLong(key: String, value: Long)
    }
}

@Serializable data class DeviceCodeResponse(val deviceCode: String, val userCode: String, val verificationUri: String, val interval: Int, val expiresIn: Int)
@Serializable data class TokenResponse(val accessToken: String, val tokenType: String, val scope: String)