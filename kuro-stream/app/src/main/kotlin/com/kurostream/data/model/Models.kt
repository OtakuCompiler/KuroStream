package com.kurostream.data.model

import com.kurostream.core.plugin.PluginType
import kotlinx.serialization.Serializable

@Serializable
data class ContentItem(
    val id: String,
    val title: String,
    val type: String, // "movie" | "series" | "anime"
    val poster: String? = null,
    val backdrop: String? = null,
    val year: Int? = null,
    val rating: Double? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val runtime: Int? = null,
    val seasons: Int? = null,
    val sourcePlugin: String = ""
)

@Serializable
data class Plugin(
    val id: String,
    val name: String,
    val type: PluginType,
    val baseUrl: String,
    val manifestUrl: String,
    val version: String,
    val description: String,
    val isEnabled: Boolean,
    val logoUrl: String? = null,
    val supportedTypes: List<String> = emptyList()
)

@Serializable
data class StreamSource(
    val url: String,
    val title: String,
    val pluginName: String,
    val qualityScore: Int = 50,
    val type: StreamType = StreamType.HTTP,
    val resolution: String? = null,
    val size: String? = null
)

@Serializable
enum class StreamType {
    HTTP, HLS, DASH, TORRENT
}

@Serializable
data class WatchHistoryEntry(
    val id: String,
    val contentId: String,
    val contentTitle: String,
    val poster: String? = null,
    val watchedAt: Long,
    val progressMs: Long = 0,
    val durationMs: Long = 0
) {
    val progressPercent: Float
        get() = if (durationMs > 0) (progressMs.toFloat() / durationMs) else 0f
}

@Serializable
data class EpisodeItem(
    val id: String,
    val seriesId: String,
    val season: Int,
    val episode: Int,
    val title: String,
    val thumbnail: String? = null,
    val description: String? = null,
    val airDate: String? = null
)
