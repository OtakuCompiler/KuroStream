package com.kurostream.extensions.stremio

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
class StremioAdapter @Inject constructor(
    private val client: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun fetchManifest(manifestUrl: String): Result<StremioManifest> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(manifestUrl).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val manifest = json.decodeFromString<StremioManifest>(body)
                Result.success(manifest)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch Stremio manifest: $manifestUrl")
                Result.failure(e)
            }
        }
    }

    fun toUnifiedExtension(manifest: StremioManifest, manifestUrl: String): UnifiedExtension {
        val capabilities = mutableSetOf<ExtensionCapability>()
        val supportedTypes = mutableSetOf<ContentType>()
        manifest.resources.forEach { resource ->
            when (resource.name) {
                "stream" -> {
                    capabilities.add(ExtensionCapability.STREAM_RESOLUTION)
                    capabilities.add(ExtensionCapability.SEARCH)
                    manifest.catalogs?.forEach { catalog ->
                        supportedTypes.addAll(catalog.type.split(",").mapNotNull { parseContentType(it.trim()) })
                    }
                }
                "catalog" -> {
                    capabilities.add(ExtensionCapability.CATALOG)
                    supportedTypes.addAll(manifest.catalogs?.flatMap { it.type.split(",").mapNotNull { type -> parseContentType(type.trim()) } } ?: emptyList())
                }
                "subtitles" -> capabilities.add(ExtensionCapability.SUBTITLE)
            }
        }
        return UnifiedExtension(
            id = "stremio_${manifest.id}",
            name = manifest.name,
            description = manifest.description ?: "",
            version = manifest.version ?: "1.0",
            type = if (capabilities.contains(ExtensionCapability.STREAM_RESOLUTION)) ExtensionType.SOURCE else ExtensionType.METADATA,
            originUrl = manifestUrl,
            iconUrl = manifest.logo ?: "",
            author = manifest.name,
            capabilities = capabilities,
            supportedTypes = supportedTypes.ifEmpty { setOf(ContentType.MOVIE, ContentType.TV) },
            supportedLanguages = manifest.languages ?: emptyList(),
            sourceFormat = ExtensionSourceFormat.STREMIO_ADDON,
            rawManifest = json.encodeToString<StremioManifest>(manifest),
            isOfficial = manifest.name.contains("official", ignoreCase = true) || manifest.id.contains("stremio"),
        )
    }

    suspend fun getCatalog(manifest: StremioManifest, type: String, extra: Map<String, String> = emptyMap()): Result<List<MediaSearchResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val catalog = manifest.catalogs?.firstOrNull { it.type.equals(type, ignoreCase = true) }
                    ?: return@withContext Result.success(emptyList())
                val url = "${catalog.id}?${extra.entries.joinToString("&") { "${it.key}=${it.value}" }}"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext Result.success(emptyList())
                val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                val meta = json.decodeFromString<StremioCatalogResponse>(body)
                val results = meta.metas.map { preview ->
                    MediaSearchResult(
                        media = com.kurostream.domain.entity.MediaItem(
                            id = preview.id,
                            title = preview.name,
                            description = preview.description ?: "",
                            posterUrl = preview.poster ?: "",
                            year = preview.year ?: 0,
                            rating = preview.rating?.toFloat() ?: 0f,
                            source = "stremio",
                        ),
                        sourceExtensionId = "stremio_${manifest.id}",
                        confidence = 0.9f,
                    )
                }
                Result.success(results)
            } catch (e: Exception) {
                Timber.e(e, "Failed to get Stremio catalog")
                Result.failure(e)
            }
        }
    }

    suspend fun getStreams(manifest: StremioManifest, id: String, type: String): Result<List<StreamAggregateResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "${manifest.id}/stream/${type}/${id}.json"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext Result.success(emptyList())
                val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                val streamResponse = json.decodeFromString<StremioStreamResponse>(body)
                val results = streamResponse.streams.map { stream ->
                    StreamAggregateResult(
                        source = UnifiedExtension(
                            id = "stremio_${manifest.id}",
                            name = manifest.name,
                            description = manifest.description ?: "",
                            version = manifest.version ?: "1.0",
                            type = ExtensionType.SOURCE,
                            originUrl = manifest.id,
                            iconUrl = manifest.logo ?: "",
                            author = manifest.name,
                            capabilities = setOf(ExtensionCapability.STREAM_RESOLUTION, ExtensionCapability.SEARCH),
                            supportedTypes = setOf(parseContentType(type)),
                            supportedLanguages = manifest.languages ?: emptyList(),
                            sourceFormat = ExtensionSourceFormat.STREMIO_ADDON,
                            rawManifest = json.encodeToString<StremioManifest>(manifest),
                        ),
                        stream = com.kurostream.domain.entity.VideoSource(
                            url = stream.url,
                            quality = stream.quality ?: "Unknown",
                            headers = stream.headers ?: emptyMap(),
                        ),
                        qualityScore = parseQualityScore(stream.quality),
                    )
                }
                Result.success(results)
            } catch (e: Exception) {
                Timber.e(e, "Failed to get Stremio streams")
                Result.failure(e)
            }
        }
    }

    private fun parseContentType(type: String): ContentType = when (type.lowercase()) {
        "movie" -> ContentType.MOVIE
        "series", "tv" -> ContentType.TV
        "anime" -> ContentType.ANIME
        "live", "iptv" -> ContentType.LIVE_TV
        else -> ContentType.MOVIE
    }

    private fun parseQualityScore(quality: String?): Float = when (quality?.lowercase()) {
        "4k", "uhd" -> 1.0f
        "1080p", "fhd" -> 0.9f
        "720p", "hd" -> 0.7f
        "480p", "sd" -> 0.5f
        else -> 0.3f
    }

    /**
     * Encode an extension as a config JSON for persistence in the Room DB.
     * Used by the Addons screen to save the manifest alongside the install row.
     */
    fun encodeConfig(extension: UnifiedExtension): String {
        val config = buildString {
            append("{\"manifestUrl\":")
            append(json.encodeToString(kotlinx.serialization.serializer<String>(), extension.originUrl))
            append(",\"name\":")
            append(json.encodeToString(kotlinx.serialization.serializer<String>(), extension.name))
            append(",\"version\":")
            append(json.encodeToString(kotlinx.serialization.serializer<String>(), extension.version))
            append(",\"icon\":")
            append(json.encodeToString(kotlinx.serialization.serializer<String>(), extension.iconUrl))
            append("}")
        }
        return config
    }
}
