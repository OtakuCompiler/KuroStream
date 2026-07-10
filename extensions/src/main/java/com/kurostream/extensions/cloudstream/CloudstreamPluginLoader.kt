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
import android.util.Log
import dalvik.system.DexClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudstreamPluginLoader @Inject constructor(
    private val context: Context
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

    suspend fun loadPluginFromApk(apkFile: File): Result<CloudstreamManifest> = withContext(Dispatchers.IO) {
        try {
            val manifest = extractManifest(apkFile)
                ?: return@withContext Result.failure(Exception("No manifest found in APK"))

            val optimizedDir = File(pluginDir, "optimized").apply { mkdirs() }
            val classLoader = DexClassLoader(
                apkFile.absolutePath,
                optimizedDir.absolutePath,
                null,
                context.classLoader
            )

            val plugin = LoadedPlugin(
                manifest = manifest,
                classLoader = classLoader,
                apkFile = apkFile
            )

            loadedPlugins[manifest.id] = plugin
            Log.i(TAG, "Loaded plugin: ${manifest.name} v${manifest.version}")
            Result.success(manifest)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load plugin", e)
            Result.failure(e)
        }
    }

    suspend fun loadPluginFromUrl(url: String): Result<CloudstreamManifest> = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(pluginDir, "temp_${System.currentTimeMillis()}.apk")
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Download failed: ${response.code}"))
            }

            response.body?.byteStream()?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            loadPluginFromApk(tempFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractManifest(apkFile: File): CloudstreamManifest? {
        return try {
            val zip = java.util.zip.ZipFile(apkFile)
            val entry = zip.getEntry(MANIFEST_NAME) ?: return null
            val json = zip.getInputStream(entry).bufferedReader().use { it.readText() }
            parseManifest(JSONObject(json))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract manifest", e)
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
    val classLoader: DexClassLoader,
    val apkFile: File
)
