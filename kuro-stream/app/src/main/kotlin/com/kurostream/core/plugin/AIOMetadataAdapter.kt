package com.kurostream.core.plugin

import com.kurostream.data.model.ContentItem
import com.kurostream.data.model.Plugin
import com.kurostream.data.model.StreamSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AIOMetadata adapter — universal metadata enrichment plugin.
 * Fetches enriched metadata (ratings, cast, trailers, related content)
 * from custom AIOMetadata-compatible endpoints.
 */
@Singleton
class AIOMetadataAdapter @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) : PluginAdapter {

    override suspend fun parseManifest(url: String): Plugin = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$url/meta").build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string()

            Plugin(
                id = UUID.randomUUID().toString(),
                name = "AIOMetadata",
                type = PluginType.AIO_METADATA,
                baseUrl = url,
                manifestUrl = "$url/meta",
                version = "1.0.0",
                description = "Universal metadata enrichment addon",
                isEnabled = true,
                logoUrl = null,
                supportedTypes = listOf("movie", "series", "anime")
            )
        } catch (e: Exception) {
            Timber.e(e, "AIOMetadata parseManifest failed")
            Plugin(
                id = UUID.randomUUID().toString(),
                name = "AIOMetadata",
                type = PluginType.AIO_METADATA,
                baseUrl = url,
                manifestUrl = url,
                version = "1.0.0",
                description = "Universal metadata enrichment addon",
                isEnabled = false,
                logoUrl = null,
                supportedTypes = emptyList()
            )
        }
    }

    override suspend fun getStreams(
        plugin: Plugin,
        contentId: String,
        type: String
    ): List<StreamSource> = withContext(Dispatchers.IO) {
        try {
            val url = "${plugin.baseUrl}/stream/$type/$contentId"
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            parseStreams(body, plugin.name)
        } catch (e: Exception) {
            Timber.e(e, "AIOMetadata getStreams failed")
            emptyList()
        }
    }

    override suspend fun search(plugin: Plugin, query: String): List<ContentItem> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val url = "${plugin.baseUrl}/search?query=$encoded"
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext emptyList()
                parseContentList(body)
            } catch (e: Exception) {
                Timber.e(e, "AIOMetadata search failed")
                emptyList()
            }
        }

    override suspend fun getCatalog(plugin: Plugin, type: String, page: Int): List<ContentItem> =
        withContext(Dispatchers.IO) {
            try {
                val url = "${plugin.baseUrl}/catalog/$type?page=$page"
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext emptyList()
                parseContentList(body)
            } catch (e: Exception) {
                Timber.e(e, "AIOMetadata getCatalog failed")
                emptyList()
            }
        }

    private fun parseStreams(body: String, pluginName: String): List<StreamSource> = emptyList()
    private fun parseContentList(body: String): List<ContentItem> = emptyList()
}
