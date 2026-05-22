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

@Singleton
class CloudStreamPluginAdapter @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) : PluginAdapter {

    override suspend fun parseManifest(url: String): Plugin {
        return Plugin(
            id = UUID.randomUUID().toString(),
            name = "CloudStream Plugin",
            type = PluginType.CLOUDSTREAM,
            baseUrl = url,
            manifestUrl = url,
            version = "1.0.0",
            description = "CloudStream compatible plugin",
            isEnabled = true,
            logoUrl = null,
            supportedTypes = listOf("movie", "series")
        )
    }

    override suspend fun getStreams(
        plugin: Plugin,
        contentId: String,
        type: String
    ): List<StreamSource> = withContext(Dispatchers.IO) {
        try {
            val url = "${plugin.baseUrl}/streams?id=$contentId&type=$type"
            val request = Request.Builder()
                .url(url)
                .header("X-Plugin-Type", "CloudStream")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()

            parseCloudStreamStreams(body, plugin.name)
        } catch (e: Exception) {
            Timber.e(e, "CloudStream getStreams failed for plugin ${plugin.name}")
            emptyList()
        }
    }

    override suspend fun search(plugin: Plugin, query: String): List<ContentItem> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val url = "${plugin.baseUrl}/search?q=$encoded"
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext emptyList()
                parseContentItems(body, plugin)
            } catch (e: Exception) {
                Timber.e(e, "CloudStream search failed")
                emptyList()
            }
        }

    override suspend fun getCatalog(plugin: Plugin, type: String, page: Int): List<ContentItem> =
        withContext(Dispatchers.IO) {
            try {
                val url = "${plugin.baseUrl}/catalog?type=$type&page=$page"
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext emptyList()
                parseContentItems(body, plugin)
            } catch (e: Exception) {
                Timber.e(e, "CloudStream getCatalog failed")
                emptyList()
            }
        }

    private fun parseCloudStreamStreams(body: String, pluginName: String): List<StreamSource> {
        return emptyList()
    }

    private fun parseContentItems(body: String, plugin: Plugin): List<ContentItem> {
        return emptyList()
    }
}
