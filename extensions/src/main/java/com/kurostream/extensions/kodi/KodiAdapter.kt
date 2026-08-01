package com.kurostream.extensions.kodi

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
class KodiAdapter @Inject constructor(
    private val client: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun fetchRepository(repoUrl: String): Result<List<UnifiedExtension>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(repoUrl).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val addons = json.decodeFromString<List<KodiAddon>>(body)
                val extensions = addons.map { addon ->
                    UnifiedExtension(
                        id = "kodi_${addon.id}",
                        name = addon.name,
                        description = addon.description ?: "",
                        version = addon.version ?: "1.0",
                        type = ExtensionType.SOURCE,
                        originUrl = repoUrl,
                        iconUrl = addon.icon ?: "",
                        author = addon.provider ?: "unknown",
                        capabilities = setOf(ExtensionCapability.STREAM_RESOLUTION, ExtensionCapability.SEARCH),
                        supportedTypes = setOf(ContentType.MOVIE, ContentType.TV, ContentType.LIVE_TV),
                        supportedLanguages = listOf("en"),
                        sourceFormat = ExtensionSourceFormat.KODI_REPOSITORY,
                        rawManifest = json.encodeToString(addon),
                    )
                }
                Result.success(extensions)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch Kodi repo: $repoUrl")
                Result.failure(e)
            }
        }
    }
}

@kotlinx.serialization.Serializable
data class KodiAddon(
    val id: String,
    val name: String,
    val version: String? = null,
    val description: String? = null,
    val icon: String? = null,
    val provider: String? = null,
    val dependencies: List<String> = emptyList(),
)
