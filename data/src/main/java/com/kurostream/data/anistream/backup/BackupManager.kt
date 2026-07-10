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

package com.kurostream.data.anistream.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.kurostream.data.anistream.downloads.DownloadDao
import com.kurostream.data.anistream.profile.ProfileDao
import com.kurostream.data.anistream.search.RecentSearchDao
import com.kurostream.data.anistream.settings.SettingsDao
import com.kurostream.data.anistream.sync.SyncProviderDao
import com.kurostream.data.anistream.introskip.IntroSkipDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backup/Restore manager using Storage Access Framework (SAF).
 * Exports all user data as encrypted JSON.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileDao: ProfileDao,
    private val downloadDao: DownloadDao,
    private val recentSearchDao: RecentSearchDao,
    private val settingsDao: SettingsDao,
    private val syncProviderDao: SyncProviderDao,
    private val introSkipDao: IntroSkipDao
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val BACKUP_VERSION = 1
        private const val BACKUP_FILENAME = "anistream_backup_%s.json.enc"
    }

    /**
     * Export all user data to a SAF URI with password-based encryption.
     */
    suspend fun exportBackup(
        destinationUri: Uri,
        password: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val backupData = collectBackupData()
            val jsonString = json.encodeToString(backupData)
            val encryptedData = encrypt(jsonString.toByteArray(Charsets.UTF_8), password)

            val docFile = DocumentFile.fromTreeUri(context, destinationUri)
                ?: return@withContext Result.failure(Exception("Invalid destination"))

            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                .format(java.util.Date())
            val fileName = BACKUP_FILENAME.format(timestamp)

            val backupFile = docFile.createFile("application/octet-stream", fileName)
                ?: return@withContext Result.failure(Exception("Failed to create backup file"))

            context.contentResolver.openOutputStream(backupFile.uri)?.use { outputStream ->
                outputStream.write(encryptedData)
            } ?: return@withContext Result.failure(Exception("Failed to write backup"))

            Result.success(backupFile.uri.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import user data from a SAF URI with password decryption.
     */
    suspend fun importBackup(
        sourceUri: Uri,
        password: String
    ): Result<BackupSummary> = withContext(Dispatchers.IO) {
        try {
            val encryptedData = context.contentResolver.openInputStream(sourceUri)?.use {
                it.readBytes()
            } ?: return@withContext Result.failure(Exception("Failed to read backup file"))

            val decryptedBytes = decrypt(encryptedData, password)
            val jsonString = decryptedBytes.toString(Charsets.UTF_8)
            val backupData = json.decodeFromString<BackupData>(jsonString)

            // Validate backup version
            if (backupData.version > BACKUP_VERSION) {
                return@withContext Result.failure(
                    Exception("Backup version ${backupData.version} not supported. Max: $BACKUP_VERSION")
                )
            }

            val summary = restoreBackupData(backupData)
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun collectBackupData(): BackupData {
        return BackupData(
            version = BACKUP_VERSION,
            exportedAt = System.currentTimeMillis(),
            profiles = profileDao.getAll(),
            downloads = downloadDao.getAll(),
            recentSearches = recentSearchDao.getRecentSearches(100),
            settings = settingsDao.getAllSettings(),
            syncProviders = syncProviderDao.getAll(),
            introSkips = introSkipDao.getAll()
        )
    }

    private suspend fun restoreBackupData(data: BackupData): BackupSummary {
        var profilesRestored = 0
        var downloadsRestored = 0
        var searchesRestored = 0
        var settingsRestored = 0
        var syncProvidersRestored = 0
        var introSkipsRestored = 0

        data.profiles?.forEach { profile ->
            profileDao.insert(profile)
            profilesRestored++
        }

        data.downloads?.forEach { download ->
            downloadDao.insert(download)
            downloadsRestored++
        }

        data.recentSearches?.forEach { search ->
            recentSearchDao.insert(search)
            searchesRestored++
        }

        data.settings?.forEach { setting ->
            settingsDao.insert(setting)
            settingsRestored++
        }

        data.syncProviders?.forEach { provider ->
            syncProviderDao.insert(provider)
            syncProvidersRestored++
        }

        data.introSkips?.forEach { skip ->
            introSkipDao.insert(skip)
            introSkipsRestored++
        }

        return BackupSummary(
            profilesRestored = profilesRestored,
            downloadsRestored = downloadsRestored,
            searchesRestored = searchesRestored,
            settingsRestored = settingsRestored,
            syncProvidersRestored = syncProvidersRestored,
            introSkipsRestored = introSkipsRestored
        )
    }

    private fun encrypt(data: ByteArray, password: String): ByteArray {
        val key = deriveKey(password)
        val iv = ByteArray(GCM_IV_LENGTH).apply {
            SecureRandom().nextBytes(this)
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    private fun decrypt(data: ByteArray, password: String): ByteArray {
        val key = deriveKey(password)
        val iv = data.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = data.copyOfRange(GCM_IV_LENGTH, data.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        return cipher.doFinal(encrypted)
    }

    private fun deriveKey(password: String): SecretKeySpec {
        // Simple PBKDF2-like derivation for demo; use proper PBKDF2 in production
        val passwordBytes = password.toByteArray(Charsets.UTF_8)
        val keyBytes = ByteArray(32)
        System.arraycopy(passwordBytes, 0, keyBytes, 0, minOf(passwordBytes.size, 32))
        // Pad with hash if password is short
        if (passwordBytes.size < 32) {
            val hash = java.security.MessageDigest.getInstance("SHA-256")
                .digest(passwordBytes)
            System.arraycopy(hash, 0, keyBytes, passwordBytes.size, 32 - passwordBytes.size)
        }
        return SecretKeySpec(keyBytes, "AES")
    }
}

@Serializable
data class BackupData(
    val version: Int,
    val exportedAt: Long,
    val profiles: List<com.kurostream.legacyui.anistream.ui.profile.UserProfile>? = null,
    val downloads: List<com.kurostream.data.anistream.downloads.DownloadItem>? = null,
    val recentSearches: List<com.kurostream.data.anistream.search.RecentSearchEntity>? = null,
    val settings: List<com.kurostream.data.anistream.settings.SettingEntity>? = null,
    val syncProviders: List<com.kurostream.data.anistream.sync.SyncProviderEntity>? = null,
    val introSkips: List<com.kurostream.data.anistream.introskip.IntroSkipEntity>? = null
)

data class BackupSummary(
    val profilesRestored: Int,
    val downloadsRestored: Int,
    val searchesRestored: Int,
    val settingsRestored: Int,
    val syncProvidersRestored: Int,
    val introSkipsRestored: Int
)
