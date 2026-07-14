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

package com.kurostream.backup.domain

import com.kurostream.core.common.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import retrofit2.http.*

@Serializable
data class BackupConfig(
    val backupType: BackupType = BackupType.GIST,
    val gistId: String? = null,
    val repoOwner: String? = null,
    val repoName: String? = null,
    val repoBranch: String = "main",
    val repoPath: String = "kurostream_backup.json.enc",
    val autoBackupEnabled: Boolean = false,
    val autoBackupIntervalDays: Int = 7,
    val lastBackupTimestamp: Long = 0,
    val encryptBackups: Boolean = true,
    val compressionEnabled: Boolean = true,
)

enum class BackupType {
    GIST,
    REPOSITORY,
}

@Serializable
data class BackupMetadata(
    val id: String,
    val type: BackupType,
    val timestamp: Long,
    val size: Long,
    val version: Int = 1,
    val description: String? = null,
    val gistUrl: String? = null,
    val repoUrl: String? = null,
    val fileCount: Int = 0,
    val profileCount: Int = 0,
    val settingsCount: Int = 0,
)

@Serializable
data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val profiles: List<ProfileBackup> = emptyList(),
    val settings: List<SettingBackup> = emptyList(),
    val downloads: List<DownloadBackup> = emptyList(),
    val watchHistory: List<WatchHistoryBackup> = emptyList(),
    val favorites: List<FavoriteBackup> = emptyList(),
    val sourceLocks: List<SourceLockBackup> = emptyList(),
    val customHomeRows: List<HomeRowBackup> = emptyList(),
    val bookmarks: List<BookmarkBackup> = emptyList(),
    val addonConfigs: List<AddonConfigBackup> = emptyList(),
)

@Serializable
data class ProfileBackup(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val isDefault: Boolean = false,
    val settings: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class SettingBackup(
    val key: String,
    val value: String,
)

@Serializable
data class DownloadBackup(
    val id: String,
    val mediaId: String,
    val episodeId: String,
    val title: String,
    val filePath: String,
    val fileSize: Long,
    val progress: Float,
    val status: String,
    val quality: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
)

@Serializable
data class WatchHistoryBackup(
    val id: String,
    val profileId: String,
    val mediaId: String,
    val episodeId: String,
    val progress: Float,
    val currentTime: Long,
    val totalTime: Long,
    val watchedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class FavoriteBackup(
    val id: String,
    val profileId: String,
    val mediaId: String,
    val mediaType: String,
    val addedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class SourceLockBackup(
    val id: String,
    val profileId: String,
    val mediaId: String,
    val extensionId: String,
    val sourceUrl: String,
    val fallbackSourceUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class HomeRowBackup(
    val id: String,
    val profileId: String,
    val title: String,
    val rowType: String,
    val sourceExtensionId: String? = null,
    val query: String? = null,
    val order: Int = 0,
    val isVisible: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class BookmarkBackup(
    val id: String,
    val profileId: String,
    val mediaId: String,
    val episodeId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null,
)

@Serializable
data class AddonConfigBackup(
    val extensionId: String,
    val config: Map<String, String> = emptyMap(),
    val isEnabled: Boolean = true,
    val installedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class GitHubAuthState(
    val isAuthenticated: Boolean = false,
    val accessToken: String? = null,
    val tokenType: String = "Bearer",
    val scope: String = "",
    val username: String? = null,
    val userId: Long? = null,
    val avatarUrl: String? = null,
    val expiresAt: Long? = null,
)

@Serializable
data class GitHubUser(
    val login: String,
    val id: Long,
    val avatarUrl: String,
    val name: String? = null,
    val email: String? = null,
)

@Serializable
data class GistRequest(
    val description: String,
    val public: Boolean = false,
    val files: Map<String, GistFile>,
)

@Serializable
data class GistFile(
    val content: String,
)

@Serializable
data class GistResponse(
    val id: String,
    val description: String,
    val public: Boolean,
    val files: Map<String, GistFileResponse>,
    val htmlUrl: String,
    val createdAt: String,
    val updatedAt: String,
    val owner: GitHubUser?,
)

@Serializable
data class GistFileResponse(
    val filename: String,
    val type: String,
    val language: String?,
    val rawUrl: String,
    val size: Long,
    val truncated: Boolean,
    val content: String?,
)

interface BackupRepository {
    suspend fun authenticate(): Result<GitHubAuthState>
    suspend fun checkAuth(): Result<GitHubAuthState>
    suspend fun logout(): Result<Unit>
    fun observeAuthState(): Flow<GitHubAuthState>

    suspend fun createBackup(config: BackupConfig, password: String?): Result<BackupMetadata>
    suspend fun listBackups(config: BackupConfig): Result<List<BackupMetadata>>
    suspend fun restoreBackup(config: BackupConfig, metadata: BackupMetadata, password: String?): Result<RestoreResult>
    suspend fun deleteBackup(config: BackupConfig, metadata: BackupMetadata): Result<Unit>

    fun observeBackupConfig(): Flow<BackupConfig>
    suspend fun updateBackupConfig(config: BackupConfig)

    suspend fun getAuthUrl(): String
}

@Serializable
data class RestoreResult(
    val profilesRestored: Int = 0,
    val settingsRestored: Int = 0,
    val downloadsRestored: Int = 0,
    val watchHistoryRestored: Int = 0,
    val favoritesRestored: Int = 0,
    val sourceLocksRestored: Int = 0,
    val homeRowsRestored: Int = 0,
    val bookmarksRestored: Int = 0,
    val addonConfigsRestored: Int = 0,
)

interface GitHubApiService {
    @GET("user")
    suspend fun getUser(@Header("Authorization") authHeader: String): Result<GitHubUser>

    @POST("gists")
    suspend fun createGist(
        @Header("Authorization") authHeader: String,
        @Body request: GistRequest
    ): Result<GistResponse>

    @GET("gists/{id}")
    suspend fun getGist(
        @Header("Authorization") authHeader: String,
        @Path("id") id: String
    ): Result<GistResponse>

    @PATCH("gists/{id}")
    suspend fun updateGist(
        @Header("Authorization") authHeader: String,
        @Path("id") id: String,
        @Body request: GistRequest
    ): Result<GistResponse>

    @DELETE("gists/{id}")
    suspend fun deleteGist(
        @Header("Authorization") authHeader: String,
        @Path("id") id: String
    ): Result<Unit>

    @GET("gists")
    suspend fun listGists(
        @Header("Authorization") authHeader: String
    ): Result<List<GistResponse>>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createOrUpdateFile(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body request: RepoContentRequest
    ): Result<RepoContentResponse>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFile(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Query("ref") ref: String?
    ): Result<RepoFileContent>

    @DELETE("repos/{owner}/{repo}/contents/{path}")
    suspend fun deleteFile(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body request: RepoContentRequest
    ): Result<RepoContentResponse>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun listFiles(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String
    ): Result<List<RepoFileContent>>
}

@Serializable
data class RepoContentRequest(
    val message: String,
    val content: String,
    val sha: String? = null,
    val branch: String? = null,
)

@Serializable
data class RepoContentResponse(
    val content: RepoFileContent?,
    val commit: CommitInfo?,
)

@Serializable
data class RepoFileContent(
    val name: String,
    val path: String,
    val sha: String,
    val size: Long,
    val url: String,
    val htmlUrl: String,
    val gitUrl: String,
    val downloadUrl: String,
    val type: String,
    val content: String? = null,
    val encoding: String? = null,
)

@Serializable
data class CommitInfo(
    val sha: String,
    val nodeId: String,
    val url: String,
    val htmlUrl: String,
    val author: CommitAuthor?,
    val committer: CommitAuthor?,
    val message: String,
    val tree: TreeInfo?,
    val parents: List<CommitParent>? = null,
    val verification: VerificationInfo? = null,
)

@Serializable
data class CommitAuthor(
    val name: String,
    val email: String,
    val date: String,
)

@Serializable
data class TreeInfo(
    val sha: String,
    val url: String,
)

@Serializable
data class CommitParent(
    val sha: String,
    val url: String,
    val htmlUrl: String,
)

@Serializable
data class VerificationInfo(
    val verified: Boolean,
    val reason: String,
    val signature: String? = null,
    val payload: String? = null,
)