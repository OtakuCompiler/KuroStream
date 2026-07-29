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

package com.kurostream.extensions.cloudstream

import android.content.Context
import timber.log.Timber
import com.kurostream.domain.security.SignatureVerifier
import com.kurostream.extensions.sandbox.SandboxClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudstreamPluginLoader @Inject constructor(
    private val context: Context,
    private val signatureVerifier: SignatureVerifier
) {
    companion object {
        private const val TAG = "CloudstreamPlugin"
        private const val PLUGIN_DIR = "cloudstream_plugins"
        private const val MANIFEST_NAME = "manifest.json"
    }

    private val pluginDir: File by lazy {
        File(context.filesDir, PLUGIN_DIR).apply { mkdirs() }
    }

    private val loadedPlugins = mutableMapOf<String, LoadedPlugin>()
    private val httpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun loadPluginFromApk(apkFile: File): Result<CloudstreamManifest> = withContext(Dispatchers.IO) {
        try {
            // Verify signature before loading
            val verificationResult = signatureVerifier.verify(apkFile.absolutePath)
            if (verificationResult.isFailure) {
                return@withContext Result.failure(Exception("Signature verification failed: ${verificationResult.exceptionOrNull()?.message}"))
            }
            
            val manifest = extractManifest(apkFile)
                ?: return@withContext Result.failure(Exception("No manifest found in APK"))

            val optimizedDir = File(pluginDir, "optimized").apply { mkdirs() }
            val classLoader = SandboxClassLoader(
                parent = context.classLoader,
                allowedPackages = setOf(
                    "com.kurostream.extension.api",
                    "kotlin",
                    "kotlinx.coroutines",
                    "java.lang",
                    "java.util"
                ),
                blockedPackages = setOf(
                    "android.os.Process",
                    "java.lang.reflect",
                    "java.lang.invoke",
                    "sun.misc",
                    "dalvik.system"
                )
            )

            val plugin = LoadedPlugin(
                manifest = manifest,
                classLoader = classLoader,
                apkFile = apkFile
            )

            loadedPlugins[manifest.id] = plugin
            Timber.tag(TAG).i(, "Loaded plugin: ${manifest.name} v${manifest.version}")
            Result.success(manifest)
        } catch (e: Exception) {
            Timber.tag(TAG).e(, "Failed to load plugin", e)
            Result.failure(e)
        }
    }

    suspend fun loadPluginFromUrl(url: String): Result<CloudstreamManifest> = withContext(Dispatchers.IO) {
        val tempFile = File(pluginDir, "temp_${System.currentTimeMillis()}.apk")
        try {
            val request = okhttp3.Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                tempFile.delete()
                return@withContext Result.failure(Exception("Download failed: ${response.code}"))
            }

            // Get checksum from headers if available
            val expectedChecksum = response.header("X-Checksum-SHA256")
            
            response.body?.byteStream()?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Verify checksum if provided
            if (expectedChecksum != null) {
                val actualChecksum = calculateFileChecksum(tempFile)
                if (actualChecksum != expectedChecksum) {
                    tempFile.delete()
                    return@withContext Result.failure(Exception("Checksum verification failed"))
                }
            }

            val result = loadPluginFromApk(tempFile)
            tempFile.delete()
            result
        } catch (e: Exception) {
            tempFile.delete()
            Result.failure(e)
        }
    }
    
    private fun calculateFileChecksum(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun extractManifest(apkFile: File): CloudstreamManifest? {
        return try {
            val zip = java.util.zip.ZipFile(apkFile)
            val entry = zip.getEntry(MANIFEST_NAME) ?: throw IllegalStateException("Extension not loaded: configure plugin dependency")
            val json = zip.getInputStream(entry).bufferedReader().use { it.readText() }
            parseManifest(JSONObject(json))
        } catch (e: Exception) {
            Timber.tag(TAG).e(, "Failed to extract manifest", e)
            null
        }
    }

    private fun parseManifest(json: JSONObject): CloudstreamManifest {
        return CloudstreamManifest(
            id = json.getString("id"),
            name = json.getString("name"),
            description = json.optString("description"),
            version = json.getInt("version"),
            versionName = json.optString("versionName"),
            author = json.optString("author"),
            repositoryUrl = json.optString("repositoryUrl"),
            language = json.optString("language"),
            tvTypes = json.optJSONArray("tvTypes")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList(),
            pluginClassName = json.getString("pluginClassName"),
            requiresResources = json.optBoolean("requiresResources", false),
            iconUrl = json.optString("iconUrl"),
            apiVersion = json.optInt("apiVersion", 1)
        )
    }

    fun getLoadedPlugins(): List<LoadedPlugin> = loadedPlugins.values.toList()

    fun getPlugin(id: String): LoadedPlugin? = loadedPlugins[id]

    fun unloadPlugin(id: String) {
        loadedPlugins.remove(id)
    }

    fun unloadAllPlugins() {
        loadedPlugins.clear()
    }
}

data class CloudstreamManifest(
    val id: String,
    val name: String,
    val description: String,
    val version: Int,
    val versionName: String,
    val author: String,
    val repositoryUrl: String,
    val language: String,
    val tvTypes: List<String>,
    val pluginClassName: String,
    val requiresResources: Boolean,
    val iconUrl: String,
    val apiVersion: Int
)

data class LoadedPlugin(
    val manifest: CloudstreamManifest,
    val classLoader: ClassLoader,
    val apkFile: File
)
