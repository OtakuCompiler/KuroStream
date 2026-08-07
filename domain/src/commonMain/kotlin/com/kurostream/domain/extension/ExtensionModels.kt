package com.kurostream.domain.extension

import kotlinx.serialization.Serializable

@Serializable
data class UnifiedExtension(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val type: ExtensionType,
    val originUrl: String,
    val iconUrl: String = "",
    val author: String = "",
    val capabilities: Set<ExtensionCapability> = emptySet(),
    val supportedTypes: Set<ContentType> = emptySet(),
    val supportedLanguages: List<String> = emptyList(),
    val sourceFormat: ExtensionSourceFormat,
    val rawManifest: String = "",
    val configSchema: List<ConfigField> = emptyList(),
    val healthScore: Float = 1f,
    val isOfficial: Boolean = false,
    val isEnabled: Boolean = true,
    val isInstalled: Boolean = false,
)

@Serializable
enum class ExtensionType {
    SOURCE,
    METADATA,
    SUBTITLE,
    DEBRID,
    UTILITY,
}

@Serializable
enum class ExtensionCapability {
    STREAM_RESOLUTION,
    SEARCH,
    CATALOG,
    SUBTITLE,
    DEBRID_CACHE_CHECK,
    DEBRID_STREAM_CONVERSION,
    DOWNLOAD,
    TRAILER,
    METADATA_ENRICHMENT,
}

@Serializable
enum class ContentType {
    MOVIE,
    TV,
    ANIME,
    LIVE_TV,
    DOCUMENTARY,
    SPORTS,
}

@Serializable
enum class ExtensionSourceFormat {
    STREMIO_ADDON,
    CLOUDSTREAM_REPO,
    KODI_REPOSITORY,
    KUROSTREAM_NATIVE,
    JELLYFIN_SERVER,
    PLEX_SERVER,
    CONSUMET_ADDON,
}

@Serializable
data class ConfigField(
    val key: String,
    val label: String,
    val type: ConfigFieldType,
    val defaultValue: String? = null,
    val options: List<String>? = null,
    val helpText: String? = null,
    val required: Boolean = false,
)

@Serializable
enum class ConfigFieldType {
    STRING,
    PASSWORD,
    NUMBER,
    BOOLEAN,
    SELECT,
    URL,
}

@Serializable
data class ExtensionHealth(
    val extensionId: String,
    val isHealthy: Boolean,
    val lastCheck: Long,
    val successRate: Float,
    val consecutiveFailures: Int,
    val lastError: String? = null,
    val latencyMs: Long = 0,
)

data class HealthIssue(
    val extensionId: String,
    val severity: IssueSeverity,
    val message: String,
    val timestamp: Long,
)

enum class IssueSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

@Serializable
data class MarketplaceItem(
    val extension: UnifiedExtension,
    val reviews: List<MarketplaceReview> = emptyList(),
    val avgRating: Float = 0f,
    val installCount: Long = 0,
    val isInstalled: Boolean = false,
    val hasUpdate: Boolean = false,
)

@Serializable
data class MarketplaceReview(
    val user: String,
    val rating: Float,
    val comment: String,
    val timestamp: Long,
)

@Serializable
data class MarketplaceCategory(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val itemCount: Int,
)

@Serializable
data class MarketplaceFilters(
    val query: String = "",
    val types: List<ExtensionType> = emptyList(),
    val languages: List<String> = emptyList(),
    val onlyOfficial: Boolean = false,
    val minRating: Float = 0f,
)
