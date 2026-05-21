package com.kurostream.tv.domain.provider

import kotlinx.coroutines.flow.Flow

/**
 * Unified interface for all anime providers (Stremio, CloudStream, etc.).
 * Abstracts the differences between various streaming source types.
 */
interface AnimeProvider {
    
    /**
     * Unique identifier for this provider.
     */
    val providerId: String
    
    /**
     * Display name of the provider.
     */
    val name: String
    
    /**
     * Provider type (Stremio, CloudStream, Direct, etc.).
     */
    val type: ProviderType
    
    /**
     * Language code (e.g., "en", "ja").
     */
    val language: String
    
    /**
     * Whether this provider is currently enabled.
     */
    val isEnabled: Boolean
    
    /**
     * Priority order (lower = higher priority).
     */
    val priority: Int
    
    /**
     * Whether this provider supports searching.
     */
    val supportsSearch: Boolean
    
    /**
     * Search for anime.
     */
    suspend fun search(query: String, page: Int = 1): Result<List<ProviderSearchResult>>
    
    /**
     * Get anime details.
     */
    suspend fun getAnimeDetails(animeId: String): Result<ProviderAnimeDetails?>
    
    /**
     * Get episodes for an anime.
     */
    suspend fun getEpisodes(animeId: String): Result<List<ProviderEpisode>>
    
    /**
     * Get stream sources for an episode.
     */
    suspend fun getStreams(
        animeId: String,
        episodeId: String
    ): Result<List<ProviderStream>>
    
    /**
     * Check if provider is available/healthy.
     */
    suspend fun checkAvailability(): Boolean
}

/**
 * Provider types.
 */
enum class ProviderType {
    STREMIO,       // Stremio addon
    CLOUDSTREAM,   // CloudStream plugin
    DIRECT,        // Direct streaming source
    TORRENT,       // Torrent-based provider
    SCRAPER        // Web scraper provider
}

/**
 * Search result from a provider.
 */
data class ProviderSearchResult(
    val providerId: String,
    val id: String,
    val title: String,
    val alternativeTitles: List<String> = emptyList(),
    val posterUrl: String?,
    val year: Int?,
    val type: String?,
    val score: Float? = null
)

/**
 * Anime details from a provider.
 */
data class ProviderAnimeDetails(
    val providerId: String,
    val id: String,
    val title: String,
    val alternativeTitles: List<String> = emptyList(),
    val description: String?,
    val posterUrl: String?,
    val bannerUrl: String?,
    val year: Int?,
    val status: String?,
    val genres: List<String> = emptyList(),
    val rating: Float?,
    val episodeCount: Int?,
    val trailerUrl: String?
)

/**
 * Episode from a provider.
 */
data class ProviderEpisode(
    val providerId: String,
    val id: String,
    val animeId: String,
    val number: Int,
    val season: Int? = null,
    val title: String?,
    val description: String?,
    val thumbnailUrl: String?,
    val airDate: String?,
    val durationMinutes: Int?
)

/**
 * Stream source from a provider.
 */
data class ProviderStream(
    val providerId: String,
    val providerName: String,
    val url: String,
    val quality: StreamQuality,
    val qualityLabel: String,
    val type: StreamType,
    val language: String,
    val subtitles: List<ProviderSubtitle> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
    val referer: String? = null,
    val isWorking: Boolean = true,
    val metadata: StreamMetadata? = null
)

/**
 * Stream quality levels.
 */
enum class StreamQuality(val pixels: Int) {
    UNKNOWN(0),
    SD_360(360),
    SD_480(480),
    HD_720(720),
    HD_1080(1080),
    UHD_4K(2160);
    
    companion object {
        fun fromLabel(label: String): StreamQuality {
            return when {
                label.contains("4k", ignoreCase = true) || label.contains("2160") -> UHD_4K
                label.contains("1080") -> HD_1080
                label.contains("720") -> HD_720
                label.contains("480") -> SD_480
                label.contains("360") -> SD_360
                else -> UNKNOWN
            }
        }
        
        fun fromPixels(pixels: Int): StreamQuality {
            return values().findLast { it.pixels <= pixels } ?: UNKNOWN
        }
    }
}

/**
 * Stream types.
 */
enum class StreamType {
    DIRECT,    // Direct MP4/MKV link
    HLS,       // M3U8 stream
    DASH,      // DASH stream
    TORRENT,   // Magnet/torrent link
    EXTERNAL   // External player required
}

/**
 * Subtitle track.
 */
data class ProviderSubtitle(
    val url: String,
    val language: String,
    val label: String,
    val format: SubtitleFormat = SubtitleFormat.SRT,
    val isDefault: Boolean = false
)

/**
 * Subtitle formats.
 */
enum class SubtitleFormat {
    SRT,
    VTT,
    ASS,
    SSA
}

/**
 * Additional stream metadata.
 */
data class StreamMetadata(
    val seeders: Int? = null,
    val fileSize: Long? = null,
    val fileName: String? = null,
    val codec: String? = null,
    val audioChannels: String? = null
)

/**
 * Provider configuration.
 */
data class ProviderConfig(
    val providerId: String,
    val isEnabled: Boolean = true,
    val priority: Int = 100,
    val customSettings: Map<String, Any> = emptyMap()
)
