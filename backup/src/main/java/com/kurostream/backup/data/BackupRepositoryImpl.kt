package com.kurostream.backup.data

import android.content.Context
import com.kurostream.backup.domain.*
import com.kurostream.core.common.result.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class DeviceCodeResponse(
    val deviceCode: String,
    val interval: Int
)

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: GitHubApiService,
    private val authManager: GitHubAuthManager,
    private val dataCollector: BackupDataCollector,
    private val dataRestorer: BackupDataRestorer,
    private val cryptoService: BackupCryptoService,
) : BackupRepository {

    private val _authState = MutableStateFlow(authManager.loadAuthState())
    private val authState: Flow<GitHubAuthState> = _authState.asStateFlow()

    private val _backupConfig = MutableStateFlow(BackupConfig())
    private val backupConfig: Flow<BackupConfig> = _backupConfig.asStateFlow()

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    private val GITHUB_CLIENT_ID = "YOUR_GITHUB_CLIENT_ID"
    private val GITHUB_SCOPES = "gist,repo"

    init {
        loadBackupConfig()
    }

    private fun loadBackupConfig() {
        val prefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        _backupConfig.value = BackupConfig(
            backupType = BackupType.valueOf(prefs.getString("backup_type", "GIST") ?: "GIST"),
            gistId = prefs.getString("backup_gist_id", null),
            repoOwner = prefs.getString("backup_repo_owner", null),
            repoName = prefs.getString("backup_repo_name", null),
            repoBranch = prefs.getString("backup_repo_branch", "main") ?: "main",
            repoPath = prefs.getString("backup_repo_path", "kurostream_backup.json.enc") ?: "kurostream_backup.json.enc",
            autoBackupEnabled = prefs.getBoolean("backup_auto_enabled", false),
            autoBackupIntervalDays = prefs.getInt("backup_auto_interval_days", 7),
            lastBackupTimestamp = prefs.getLong("backup_last_timestamp", 0),
            encryptBackups = prefs.getBoolean("backup_encrypt", true),
            compressionEnabled = prefs.getBoolean("backup_compress", true),
        )
    }

    override suspend fun authenticate(): Result<GitHubAuthState> = withContext(Dispatchers.IO) {
        try {
            val deviceCodeResponse = requestDeviceCode()
            if (deviceCodeResponse is Result.Error) return@withContext deviceCodeResponse

            val responseData = (deviceCodeResponse as Result.Success).data
            authManager.authenticate(responseData.deviceCode, responseData.interval) { state ->
                _authState.value = state
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun requestDeviceCode(): Result<DeviceCodeResponse> = withContext(Dispatchers.IO) {
        TODO("Implement device code request")
    }

    override suspend fun checkAuth(): Result<GitHubAuthState> = withContext(Dispatchers.IO) {
        val result = authManager.checkAuth()
        if (result is Result.Success) {
            _authState.value = result.data
        }
        result
    }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        authManager.logout().also { _authState.value = GitHubAuthState() }
    }

    override fun observeAuthState(): Flow<GitHubAuthState> = authState

    override suspend fun createBackup(config: BackupConfig, password: String?): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            val backupData = dataCollector.collect()
            val jsonString = json.encodeToString(backupData)
            val encryptedData = if (config.encryptBackups && password != null) {
                cryptoService.encrypt(jsonString.toByteArray(), password)
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
                        description = description, public = false,
                        files = mapOf(fileName to GistFile(content = base64Data))
                    )
                    val authHeader = "Bearer ${_authState.value.accessToken}"
                    val result = apiService.createGist(authHeader, request)
                    if (result is Result.Error) return@withContext result

                    val gist = (result as Result.Success).data
                    updateBackupConfig(config.copy(gistId = gist.id, lastBackupTimestamp = timestamp))

                    BackupMetadata(
                        id = gist.id, type = BackupType.GIST, timestamp = timestamp,
                        size = encryptedData.size.toLong(), description = description,
                        gistUrl = gist.htmlUrl, profileCount = backupData.profiles.size,
                        settingsCount = backupData.settings.size, fileCount = backupData.downloads.size,
                    )
                }
                BackupType.REPOSITORY -> {
                    val authHeader = "Bearer ${_authState.value.accessToken}"
                    val request = RepoContentRequest(
                        message = "KuroStream Backup - ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(timestamp))}",
                        content = base64Data,
                    )
                    val result = apiService.createOrUpdateFile(
                        authHeader, config.repoOwner!!, config.repoName!!, config.repoPath, request
                    )
                    if (result is Result.Error) return@withContext result

                    updateBackupConfig(config.copy(lastBackupTimestamp = timestamp))

                    val responseData = (result as Result.Success).data
                    BackupMetadata(
                        id = responseData.content?.sha ?: "repo_$timestamp",
                        type = BackupType.REPOSITORY, timestamp = timestamp,
                        size = encryptedData.size.toLong(), description = "Repository backup",
                        repoUrl = "https://github.com/${config.repoOwner}/${config.repoName}",
                        profileCount = backupData.profiles.size,
                        settingsCount = backupData.settings.size, fileCount = backupData.downloads.size,
                    )
                }
            }

            Result.Success(metadata)
        } catch (e: Exception) {
            Result.Error(e)
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
                                id = gist.id, type = BackupType.GIST,
                                timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                                    .parse(gist.createdAt)?.time ?: 0,
                                size = gist.files.values.sumOf { it.size },
                                description = gist.description, gistUrl = gist.htmlUrl,
                            )
                            return@withContext Result.Success(listOf(metadata))
                        }
                    }
                    Result.Success(emptyList())
                }
                BackupType.REPOSITORY -> {
                    val result = apiService.getFile(authHeader, config.repoOwner!!, config.repoName!!, config.repoPath, config.repoBranch)
                    if (result is Result.Success && result.data != null) {
                        val file = (result as Result.Success).data!!
                        Result.Success(listOf(BackupMetadata(
                            id = file.sha, type = BackupType.REPOSITORY, timestamp = 0, size = file.size,
                            repoUrl = "https://github.com/${config.repoOwner}/${config.repoName}",
                        )))
                    } else {
                        Result.Success(emptyList())
                    }
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun restoreBackup(config: BackupConfig, metadata: BackupMetadata, password: String?): Result<RestoreResult> = withContext(Dispatchers.IO) {
        try {
            val authHeader = "Bearer ${_authState.value.accessToken}"
            val base64Data = when (config.backupType) {
                BackupType.GIST -> {
                    val result = apiService.getGist(authHeader, metadata.id)
                    if (result is Result.Error) return@withContext result
                    (result as Result.Success).data.files.values.first().content ?: return@withContext Result.Error(Exception("Empty gist content"))
                }
                BackupType.REPOSITORY -> {
                    val result = apiService.getFile(authHeader, config.repoOwner!!, config.repoName!!, config.repoPath, config.repoBranch)
                    if (result is Result.Error) return@withContext result
                    (result as Result.Success).data?.content ?: return@withContext Result.Error(Exception("Empty file content"))
                }
            }

            val encryptedData = android.util.Base64.decode(base64Data, android.util.Base64.NO_WRAP)
            val decryptedData = if (config.encryptBackups && password != null) {
                cryptoService.decrypt(encryptedData, password)
            } else {
                encryptedData
            }

            val jsonString = String(decryptedData, Charsets.UTF_8)
            val backupData = json.decodeFromString<BackupData>(jsonString)

            val result = dataRestorer.restore(backupData)
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e)
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
                        val file = (result as Result.Success).data!!
                        val deleteResult = apiService.deleteFile(authHeader, config.repoOwner!!, config.repoName!!, config.repoPath,
                            RepoContentRequest(message = "Delete backup", content = "", sha = file.sha)
                        )
                        if (deleteResult is Result.Error) {
                            Result.Error(deleteResult.exception)
                        } else {
                            Result.Success(Unit)
                        }
                    } else {
                        Result.Success(Unit)
                    }
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun observeBackupConfig(): Flow<BackupConfig> = backupConfig

    override suspend fun updateBackupConfig(config: BackupConfig) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE).edit()
        prefs.putString("backup_type", config.backupType.name)
        config.gistId?.let { prefs.putString("backup_gist_id", it) }
        config.repoOwner?.let { prefs.putString("backup_repo_owner", it) }
        config.repoName?.let { prefs.putString("backup_repo_name", it) }
        prefs.putString("backup_repo_branch", config.repoBranch)
        prefs.putString("backup_repo_path", config.repoPath)
        prefs.putBoolean("backup_auto_enabled", config.autoBackupEnabled)
        prefs.putInt("backup_auto_interval_days", config.autoBackupIntervalDays)
        prefs.putLong("backup_last_timestamp", config.lastBackupTimestamp)
        prefs.putBoolean("backup_encrypt", config.encryptBackups)
        prefs.putBoolean("backup_compress", config.compressionEnabled)
        prefs.apply()
        _backupConfig.value = config
    }

    override suspend fun getAuthUrl(): String = withContext(Dispatchers.IO) {
        "https://github.com/login/device?scope=${GITHUB_SCOPES}&client_id=${GITHUB_CLIENT_ID}"
    }
}
