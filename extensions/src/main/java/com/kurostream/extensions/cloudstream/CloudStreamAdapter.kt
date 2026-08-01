package com.kurostream.extensions.cloudstream

import com.kurostream.domain.extension.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudStreamAdapter @Inject constructor(
    private val client: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun fetchRepository(repoUrl: String): Result<CloudStreamRepository> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(repoUrl).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val repo = json.decodeFromString<CloudStreamRepository>(body)
                Result.success(repo)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch CloudStream repo: $repoUrl")
                Result.failure(e)
            }
        }
    }

    fun toUnifiedExtensions(repo: CloudStreamRepository, repoUrl: String): List<UnifiedExtension> {
        return repo.plugins.map { plugin ->
            UnifiedExtension(
                id = "cloudstream_${repo.name}_${plugin.name.lowercase().replace(" ", "_")}",
                name = plugin.name,
                description = plugin.description ?: "",
                version = plugin.version ?: "1.0",
                type = ExtensionType.SOURCE,
                originUrl = plugin.url,
                iconUrl = plugin.icon ?: "",
                author = plugin.author ?: repo.name,
                capabilities = setOf(ExtensionCapability.STREAM_RESOLUTION, ExtensionCapability.SEARCH),
                supportedTypes = setOf(ContentType.MOVIE, ContentType.TV, ContentType.ANIME),
                supportedLanguages = listOf(plugin.language ?: "en"),
                sourceFormat = ExtensionSourceFormat.CLOUDSTREAM_REPO,
                rawManifest = json.encodeToString(plugin),
            )
        }
    }
}

@kotlinx.serialization.Serializable
data class CloudStreamRepository(
    val name: String,
    val url: String,
    val plugins: List<CloudStreamPlugin> = emptyList(),
)

@kotlinx.serialization.Serializable
data class CloudStreamPlugin(
    val name: String,
    val url: String,
    val version: String? = null,
    val description: String? = null,
    val author: String? = null,
    val language: String? = "en",
    val icon: String? = null,
    val types: List<String> = emptyList(),
)
