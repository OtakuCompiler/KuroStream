package com.kurostream.domain.extension

import kotlinx.coroutines.flow.Flow

interface ExtensionRepository {
    fun observeAll(): Flow<List<UnifiedExtension>>
    suspend fun getExtension(id: String): UnifiedExtension?
    suspend fun install(extension: UnifiedExtension)
    suspend fun uninstall(id: String)
    suspend fun enable(id: String)
    suspend fun disable(id: String)
    suspend fun getConfig(id: String): Map<String, String>
    suspend fun setConfig(id: String, values: Map<String, String>)
    suspend fun checkForUpdates(): List<UnifiedExtension>
}

interface ExtensionMarketplace {
    suspend fun search(query: String, filters: MarketplaceFilters): List<MarketplaceItem>
    suspend fun getFeatured(): List<MarketplaceItem>
    suspend fun getTrending(): List<MarketplaceItem>
    suspend fun getCategories(): List<MarketplaceCategory>
    suspend fun getItemDetails(itemId: String): MarketplaceItem?
    fun observeInstalledCount(itemId: String): Flow<Long>
}

interface ExtensionHealthMonitor {
    fun observeHealth(extensionId: String): Flow<ExtensionHealth>
    fun observeGlobalHealth(): Flow<Map<String, ExtensionHealth>>
    suspend fun checkHealth(extensionId: String): ExtensionHealth
    suspend fun runDiagnostics(): List<HealthIssue>
    suspend fun autoDisableBroken()
    suspend fun autoEnableFixed()
}

interface SourceAggregator {
    suspend fun searchAll(query: String, types: Set<ContentType>): List<MediaSearchResult>
    suspend fun getStreams(mediaId: String, type: ContentType): List<StreamAggregateResult>
    suspend fun getHomeRows(): List<HomeRowResult>
    fun toggleSourceForQuery(sourceId: String, query: String)
    fun setDebridPriority(priority: List<String>)
}

data class MediaSearchResult(
    val media: com.kurostream.domain.entity.MediaItem,
    val sourceExtensionId: String,
    val confidence: Float,
)

data class StreamAggregateResult(
    val source: UnifiedExtension,
    val stream: com.kurostream.domain.entity.VideoSource,
    val qualityScore: Float,
    val debridCached: Boolean = false,
)

data class HomeRowResult(
    val title: String,
    val items: List<com.kurostream.domain.entity.MediaItem>,
    val sourceExtensionId: String,
)
