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

package com.kurostream.app.marketplace

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kurostream.data.local.dao.PurchaseDao
import com.kurostream.data.local.entity.PurchaseEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.skinDataStore: DataStore<Preferences> by preferencesDataStore(name = "skin_preferences")

/**
 * SkinManager handles downloading, verification, installation, and activation of premium skins.
 * 
 * Responsibilities:
 * - Download skin packages from signed URLs
 * - Verify checksums for integrity
 * - Extract and install skins to app storage
 * - Manage active skin selection via DataStore
 * - Handle skin rollback on corruption
 * - Support skin updates
 */
@Singleton
class SkinManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val purchaseDao: PurchaseDao,
    private val httpClient: OkHttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val skinsDir = File(context.filesDir, "skins")
    private val activeSkinKey = stringPreferencesKey("active_skin_id")
    
    private val _activeSkin = MutableStateFlow<String?>(null)
    val activeSkin: StateFlow<String?> = _activeSkin.asStateFlow()
    
    private val _skinState = MutableStateFlow<SkinState>(SkinState.Idle)
    val skinState: StateFlow<SkinState> = _skinState.asStateFlow()
    
    private val _installedSkins = MutableStateFlow<List<InstalledSkin>>(emptyList())
    val installedSkins: StateFlow<List<InstalledSkin>> = _installedSkins.asStateFlow()

    init {
        ensureSkinsDirectory()
        loadActiveSkin()
        scanInstalledSkins()
    }

    private fun ensureSkinsDirectory() {
        if (!skinsDir.exists()) {
            skinsDir.mkdirs()
        }
    }

    /**
     * Load the active skin ID from DataStore
     */
    private fun loadActiveSkin() {
        scope.launch {
            val prefs = context.skinDataStore.data.first()
            _activeSkin.value = prefs[activeSkinKey]
        }
    }

    /**
     * Scan installed skins from the skins directory
     */
    private fun scanInstalledSkins() {
        scope.launch {
            val skins = withContext(Dispatchers.IO) {
                skinsDir.listFiles()
                    ?.filter { it.isDirectory }
                    ?.mapNotNull { dir ->
                        try {
                            val manifest = File(dir, "manifest.json")
                            if (manifest.exists()) {
                                val json = manifest.readText()
                                parseSkinManifest(dir.name, json)
                            } else null
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to parse skin manifest for ${dir.name}")
                            null
                        }
                    }
                    ?: emptyList()
            }
            _installedSkins.value = skins
        }
    }

    /**
     * Install a skin from a purchase
     */
    suspend fun installSkin(purchase: PurchaseEntity): Result<InstalledSkin> {
        if (purchase.productType != "skin") {
            return Result.failure(IllegalArgumentException("Product is not a skin"))
        }

        _skinState.value = SkinState.Installing(purchase.productId)

        return try {
            // Download skin package
            val downloadResult = downloadSkinPackage(purchase)
            if (downloadResult.isFailure) {
                _skinState.value = SkinState.Error(downloadResult.exceptionOrNull()?.message ?: "Download failed")
                return Result.failure(downloadResult.exceptionOrNull() ?: Exception("Download failed"))
            }

            val (tempFile, expectedChecksum) = downloadResult.getOrThrow()
            
            // Verify checksum
            val actualChecksum = calculateChecksum(tempFile)
            if (actualChecksum != expectedChecksum) {
                tempFile.delete()
                _skinState.value = SkinState.Error("Checksum mismatch - file may be corrupted")
                return Result.failure(Exception("Checksum mismatch"))
            }

            // Extract skin to installation directory
            val skinDir = File(skinsDir, purchase.productId)
            if (skinDir.exists()) {
                skinDir.deleteRecursively()
            }
            skinDir.mkdirs()

            extractSkinPackage(tempFile, skinDir)
            tempFile.delete()

            // Verify manifest
            val manifest = File(skinDir, "manifest.json")
            if (!manifest.exists()) {
                skinDir.deleteRecursively()
                _skinState.value = SkinState.Error("Invalid skin package - missing manifest")
                return Result.failure(Exception("Invalid skin package"))
            }

            val manifestJson = manifest.readText()
            val installedSkin = parseSkinManifest(purchase.productId, manifestJson)

            if (installedSkin != null) {
                _installedSkins.value = _installedSkins.value + installedSkin
                _skinState.value = SkinState.Installed(purchase.productId)
                Timber.d("SkinManager: Skin ${purchase.productId} installed successfully")
                Result.success(installedSkin)
            } else {
                skinDir.deleteRecursively()
                _skinState.value = SkinState.Error("Failed to parse skin manifest")
                Result.failure(Exception("Failed to parse skin manifest"))
            }
        } catch (e: Exception) {
            Timber.e(e, "SkinManager: Failed to install skin ${purchase.productId}")
            _skinState.value = SkinState.Error(e.message ?: "Installation failed")
            Result.failure(e)
        }
    }

    /**
     * Download skin package from signed URL
     */
    private suspend fun downloadSkinPackage(purchase: PurchaseEntity): Result<Pair<File, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(purchase.downloadUrl)
                    .build()

                val response = httpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }

                val body = response.body ?: return@withContext Result.failure(Exception("Empty response"))
                
                val tempFile = File.createTempFile("skin_${purchase.productId}", ".zip", context.cacheDir)
                FileOutputStream(tempFile).use { output ->
                    body.byteStream().copyTo(output)
                }

                Result.success(Pair(tempFile, purchase.checksum))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Extract skin package (ZIP) to directory
     */
    private fun extractSkinPackage(zipFile: File, destDir: File) {
        // Simple ZIP extraction - in production, use a proper ZIP library
        java.util.zip.ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val file = File(destDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculate SHA-256 checksum of a file
     */
    private fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Parse skin manifest JSON
     */
    private fun parseSkinManifest(skinId: String, json: String): InstalledSkin? {
        return try {
            // Simple JSON parsing - in production, use Moshi or Gson
            val name = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: skinId
            val version = Regex("\"version\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: "1.0.0"
            val previewUrl = Regex("\"preview_url\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
            
            InstalledSkin(
                id = skinId,
                name = name,
                version = version,
                previewUrl = previewUrl,
                installedAt = System.currentTimeMillis(),
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse skin manifest")
            null
        }
    }

    /**
     * Activate a skin
     */
    suspend fun activateSkin(skinId: String) {
        // Verify skin is installed
        val skin = _installedSkins.value.find { it.id == skinId }
        if (skin == null) {
            Timber.w("SkinManager: Cannot activate non-installed skin: $skinId")
            return
        }

        // Save to DataStore
        context.skinDataStore.edit { prefs ->
            prefs[activeSkinKey] = skinId
        }
        
        _activeSkin.value = skinId
        Timber.d("SkinManager: Activated skin: $skinId")
    }

    /**
     * Deactivate current skin (revert to default)
     */
    suspend fun deactivateSkin() {
        context.skinDataStore.edit { prefs ->
            prefs.remove(activeSkinKey)
        }
        _activeSkin.value = null
        Timber.d("SkinManager: Deactivated skin")
    }

    /**
     * Uninstall a skin
     */
    suspend fun uninstallSkin(skinId: String) {
        val skinDir = File(skinsDir, skinId)
        if (skinDir.exists()) {
            skinDir.deleteRecursively()
        }
        
        _installedSkins.value = _installedSkins.value.filter { it.id != skinId }
        
        // If this was the active skin, deactivate it
        if (_activeSkin.value == skinId) {
            deactivateSkin()
        }
        
        Timber.d("SkinManager: Uninstalled skin: $skinId")
    }

    /**
     * Check if a skin is installed
     */
    fun isSkinInstalled(skinId: String): Boolean {
        return _installedSkins.value.any { it.id == skinId }
    }

    /**
     * Get installed skin by ID
     */
    fun getInstalledSkin(skinId: String): InstalledSkin? {
        return _installedSkins.value.find { it.id == skinId }
    }

    /**
     * Restore skins from purchases (called on startup after sync)
     */
    suspend fun restorePurchasedSkins() {
        val skinPurchases = purchaseDao.getPurchasesByType("skin")
        
        for (purchase in skinPurchases) {
            if (!isSkinInstalled(purchase.productId)) {
                installSkin(purchase)
            }
        }
    }
}

/**
 * Represents an installed skin
 */
data class InstalledSkin(
    val id: String,
    val name: String,
    val version: String,
    val previewUrl: String?,
    val installedAt: Long,
)

/**
 * Skin state sealed class
 */
sealed class SkinState {
    data object Idle : SkinState()
    data class Installing(val skinId: String) : SkinState()
    data class Installed(val skinId: String) : SkinState()
    data class Error(val message: String) : SkinState()
}