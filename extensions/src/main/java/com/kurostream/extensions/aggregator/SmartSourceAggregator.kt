package com.kurostream.extensions.aggregator

import com.kurostream.domain.extension.*
import com.kurostream.domain.entity.MediaItem
import com.kurostream.domain.entity.VideoSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartSourceAggregator @Inject constructor(
    private val extensionRepository: com.kurostream.domain.extension.ExtensionRepository,
    private val debridManager: com.kurostream.domain.debrid.DebridManager,
    private val healthMonitor: ExtensionHealthMonitor,
) : SourceAggregator {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val disabledQueries = mutableMapOf<String, MutableSet<String>>()
    private val debridPriority = mutableListOf<String>()

    override suspend fun searchAll(query: String, types: Set<ContentType>): List<MediaSearchResult> {
        return withContext(Dispatchers.IO) {
            val extensions = extensionRepository.observeAll().first().filter { it.isEnabled && it.isInstalled }
            val metadataExtensions = extensions.filter { ext ->
                ext.capabilities.contains(ExtensionCapability.CATALOG) || ext.capabilities.contains(ExtensionCapability.SEARCH)
            }

            val deferredResults = metadataExtensions.map { ext ->
                scope.async {
                    try {
                        val health = healthMonitor.checkHealth(ext.id)
                        if (!health.isHealthy) return@async emptyList()
                        searchExtension(ext, query, types)
                    } catch (e: Exception) {
                        Timber.w(e, "Search failed for extension: ${ext.id}")
                        emptyList()
                    }
                }
            }

            val results = deferredResults.awaitAll().flatten()
            results.sortedByDescending { it.confidence }.distinctBy { it.media.id }
        }
    }

    override suspend fun getStreams(mediaId: String, type: ContentType): List<StreamAggregateResult> {
        return withContext(Dispatchers.IO) {
            val extensions = extensionRepository.observeAll().first().filter { it.isEnabled && it.isInstalled }
            val sourceExtensions = extensions.filter { ext ->
                ext.capabilities.contains(ExtensionCapability.STREAM_RESOLUTION) && ext.supportedTypes.contains(type)
            }

            val deferredResults = sourceExtensions.map { ext ->
                scope.async {
                    try {
                        val health = healthMonitor.checkHealth(ext.id)
                        if (!health.isHealthy) return@async emptyList()
                        getStreamsFromExtension(ext, mediaId, type)
                    } catch (e: Exception) {
                        Timber.w(e, "Get streams failed for extension: ${ext.id}")
                        emptyList()
                    }
                }
            }

            val allResults = deferredResults.awaitAll().flatten()
            val debridCached = allResults.filter { it.debridCached }
            val nonDebrid = allResults.filter { !it.debridCached }

            (debridCached + nonDebrid).sortedByDescending { it.qualityScore }
        }
    }

    override suspend fun getHomeRows(): List<HomeRowResult> {
        return withContext(Dispatchers.IO) {
            val extensions = extensionRepository.observeAll().first().filter { it.isEnabled && it.isInstalled }
            val metadataExtensions = extensions.filter { ext ->
                ext.capabilities.contains(ExtensionCapability.CATALOG)
            }

            val deferredResults = metadataExtensions.map { ext ->
                scope.async {
                    try {
                        val health = healthMonitor.checkHealth(ext.id)
                        if (!health.isHealthy) return@async emptyList()
                        getHomeRowsFromExtension(ext)
                    } catch (e: Exception) {
                        Timber.w(e, "Get home rows failed for extension: ${ext.id}")
                        emptyList()
                    }
                }
            }

            deferredResults.awaitAll().flatten().distinctBy { it.title }
        }
    }

    override fun toggleSourceForQuery(sourceId: String, query: String) {
        val disabled = disabledQueries.getOrPut(query) { mutableSetOf() }
        if (disabled.contains(sourceId)) {
            disabled.remove(sourceId)
        } else {
            disabled.add(sourceId)
        }
    }

    override fun setDebridPriority(priority: List<String>) {
        debridPriority.clear()
        debridPriority.addAll(priority)
    }

    private suspend fun searchExtension(ext: UnifiedExtension, query: String, types: Set<ContentType>): List<MediaSearchResult> {
        val disabled = disabledQueries[query] ?: return emptyList()
        if (disabled.contains(ext.id)) return emptyList()
        return emptyList()
    }

    private suspend fun getStreamsFromExtension(ext: UnifiedExtension, mediaId: String, type: ContentType): List<StreamAggregateResult> {
        val disabled = disabledQueries[mediaId] ?: return emptyList()
        if (disabled.contains(ext.id)) return emptyList()
        return emptyList()
    }

    private suspend fun getHomeRowsFromExtension(ext: UnifiedExtension): List<HomeRowResult> {
        return emptyList()
    }
}
