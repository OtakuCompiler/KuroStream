package com.kurostream.core.plugin

import com.kurostream.data.model.ContentItem
import com.kurostream.data.model.Plugin
import com.kurostream.data.model.StreamSource
import com.kurostream.data.model.StreamType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class StremioManifest(
    val id: String = "",
    val name: String = "",
    val version: String = "1.0.0",
    val description: String = "",
    val resources: List<String> = emptyList(),
    val types: List<String> = emptyList(),
    val catalogs: List<StremioManifestCatalog> = emptyList(),
    val logo: String? = null
)

@Serializable
data class StremioManifestCatalog(
    val type: String = "",
    val id: String = "",
    val name: String = ""
)

@Serializable
data class StremioStream(
    val url: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val name: String? = null,
    val title: String? = null,
    val behaviorHints: StreamBehaviorHints? = null
)

@Serializable
data class StreamBehaviorHints(
    val notWebReady: Boolean? = null,
    val bingeGroup: String? = null
)

@Serializable
data class StremioStreamsResponse(
    val streams: List<StremioStream> = emptyList()
)

@Singleton
class StremioPluginAdapter @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) : PluginAdapter {

    override suspend fun parseManifest(url: String): Plugin = withContext(Dispatchers.IO) {
        val baseUrl = url.removeSuffix("/manifest.json")
        val manifestUrl = if (url.endsWith("manifest.json")) url else "$url/manifest.json"

        val request = Request.Builder().url(manifestUrl).build()
        val response = okHttpClient.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty manifest response")

        val manifest = json.decodeFromString<StremioManifest>(body)

        Plugin(
            id = UUID.randomUUID().toString(),
            name = manifest.name,
            type = PluginType.STREMIO,
            baseUrl = baseUrl,
            manifestUrl = manifestUrl,
            version = manifest.version,
            description = manifest.description,
            isEnabled = true,
            logoUrl = manifest.logo,
            supportedTypes = manifest.types
        )
    }

    override suspend fun getStreams(
        plugin: Plugin,
        contentId: String,
        type: String
    ): List<StreamSource> = withContext(Dispatchers.IO) {
        try {
            val url = "${plugin.baseUrl}/stream/$type/$contentId.json"
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()

            val streamsResponse = json.decodeFromString<StremioStreamsResponse>(body)

            streamsResponse.streams.mapNotNull { stream ->
                stream.url?.let { streamUrl ->
                    StreamSource(
                        url = streamUrl,
                        title = stream.name ?: stream.title ?: "Stream",
                        pluginName = plugin.name,
                        qualityScore = parseQualityScore(stream.name ?: ""),
                        type = StreamType.HTTP
                    )
                } ?: stream.infoHash?.let { hash ->
                    StreamSource(
                        url = "magnet:?xt=urn:btih:$hash",
                        title = stream.name ?: "Torrent Stream",
                        pluginName = plugin.name,
                        qualityScore = parseQualityScore(stream.name ?: ""),
                        type = StreamType.TORRENT
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Stremio getStreams failed for plugin ${plugin.name}")
            emptyList()
        }
    }

    override suspend fun search(plugin: Plugin, query: String): List<ContentItem> {
        return try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "${plugin.baseUrl}/catalog/movie/search=${encoded}.json"
            fetchCatalogItems(plugin, url)
        } catch (e: Exception) {
            Timber.e(e, "Stremio search failed for plugin ${plugin.name}")
            emptyList()
        }
    }

    override suspend fun getCatalog(
        plugin: Plugin,
        type: String,
        page: Int
    ): List<ContentItem> {
        return try {
            val skip = page * 100
            val url = "${plugin.baseUrl}/catalog/$type/top/skip=$skip.json"
            fetchCatalogItems(plugin, url)
        } catch (e: Exception) {
            Timber.e(e, "Stremio getCatalog failed for plugin ${plugin.name}")
            emptyList()
        }
    }

    private suspend fun fetchCatalogItems(plugin: Plugin, url: String): List<ContentItem> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()

            val jsonObj = json.parseToJsonElement(body)
            val metas = jsonObj.let {
                it.toString()
                    .substringAfter("\"metas\":[")
                    .substringBefore("]}")
            }

            emptyList()
        }

    private fun parseQualityScore(name: String): Int {
        val upper = name.uppercase()
        return when {
            upper.contains("4K") || upper.contains("2160") -> 100
            upper.contains("1080") -> 80
            upper.contains("720") -> 60
            upper.contains("480") -> 40
            else -> 20
        }
    }
}
