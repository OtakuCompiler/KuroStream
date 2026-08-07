package com.kurostream.extensions.consumet

import com.kurostream.domain.extension.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsumetAdapter @Inject constructor(
    private val client: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun fetchAnime(baseUrl: String, animeId: String): Result<ConsumetAnime> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$baseUrl/meta/anime/$animeId"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val anime = json.decodeFromString<ConsumetAnime>(body)
                Result.success(anime)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch Consumet anime: $baseUrl/meta/anime/$animeId")
                Result.failure(e)
            }
        }
    }

    suspend fun fetchEpisodes(baseUrl: String, animeId: String): Result<List<ConsumetEpisode>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$baseUrl/meta/anime/$animeId/watch"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val watchResponse = json.decodeFromString<ConsumetWatchResponse>(body)
                Result.success(watchResponse.episodes)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch Consumet episodes: $baseUrl/meta/anime/$animeId/watch")
                Result.failure(e)
            }
        }
    }

    suspend fun fetchStreams(baseUrl: String, episodeId: String): Result<ConsumetStreamResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$baseUrl/meta/anime/$episodeId"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val streamResponse = json.decodeFromString<ConsumetStreamResponse>(body)
                Result.success(streamResponse)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch Consumet streams: $baseUrl/meta/anime/$episodeId")
                Result.failure(e)
            }
        }
    }

    fun toUnifiedExtension(
        name: String,
        baseUrl: String,
        description: String = "",
        iconUrl: String = "",
        author: String = "",
    ): UnifiedExtension {
        return UnifiedExtension(
            id = "consumet_${baseUrl.hashCode().toString(16)}_${name.lowercase().replace(" ", "_")}",
            name = name,
            description = description,
            version = "1.0",
            type = ExtensionType.SOURCE,
            originUrl = baseUrl,
            iconUrl = iconUrl,
            author = author,
            capabilities = setOf(
                ExtensionCapability.STREAM_RESOLUTION,
                ExtensionCapability.SEARCH,
                ExtensionCapability.CATALOG,
            ),
            supportedTypes = setOf(ContentType.MOVIE, ContentType.TV, ContentType.ANIME),
            supportedLanguages = emptyList(),
            sourceFormat = ExtensionSourceFormat.CONSUMET_ADDON,
            rawManifest = json.encodeToString(
                mapOf(
                    "name" to name,
                    "baseUrl" to baseUrl,
                    "type" to "consumet",
                )
            ),
        )
    }
}
