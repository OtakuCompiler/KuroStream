package com.kurostream.core.plugin

import com.kurostream.data.model.ContentItem
import com.kurostream.data.model.Plugin
import com.kurostream.data.model.StreamSource

enum class PluginType {
    STREMIO,
    CLOUDSTREAM,
    AIO_METADATA,
    NUVIO,
    CUSTOM
}

interface PluginAdapter {
    suspend fun parseManifest(url: String): Plugin
    suspend fun getStreams(plugin: Plugin, contentId: String, type: String): List<StreamSource>
    suspend fun search(plugin: Plugin, query: String): List<ContentItem>
    suspend fun getCatalog(plugin: Plugin, type: String, page: Int): List<ContentItem>
}
