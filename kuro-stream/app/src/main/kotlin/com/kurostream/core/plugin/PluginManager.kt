package com.kurostream.core.plugin

import com.kurostream.data.model.ContentItem
import com.kurostream.data.model.Plugin
import com.kurostream.data.model.StreamSource
import com.kurostream.data.repository.PluginRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluginManager @Inject constructor(
    private val pluginRepository: PluginRepository,
    private val stremioAdapter: StremioPluginAdapter,
    private val cloudStreamAdapter: CloudStreamPluginAdapter,
    private val aioMetadataAdapter: AIOMetadataAdapter
) {

    private val _activePlugins = MutableStateFlow<List<Plugin>>(emptyList())
    val activePlugins: Flow<List<Plugin>> = _activePlugins.asStateFlow()

    private val adapters: Map<PluginType, PluginAdapter> = mapOf(
        PluginType.STREMIO to stremioAdapter,
        PluginType.CLOUDSTREAM to cloudStreamAdapter,
        PluginType.AIO_METADATA to aioMetadataAdapter
    )

    suspend fun initialize() {
        try {
            val plugins = pluginRepository.getAllPlugins().first()
            _activePlugins.value = plugins.filter { it.isEnabled }
            Timber.d("PluginManager initialized with ${_activePlugins.value.size} active plugins")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize PluginManager")
        }
    }

    suspend fun getStreamsForContent(
        contentId: String,
        type: String
    ): List<StreamSource> {
        val streams = mutableListOf<StreamSource>()

        for (plugin in _activePlugins.value) {
            try {
                val adapter = adapters[plugin.type] ?: continue
                val pluginStreams = adapter.getStreams(plugin, contentId, type)
                streams.addAll(pluginStreams)
            } catch (e: Exception) {
                Timber.e(e, "Plugin ${plugin.name} failed to fetch streams")
            }
        }

        return streams.sortedByDescending { it.qualityScore }
    }

    suspend fun searchContent(query: String): List<ContentItem> {
        val results = mutableListOf<ContentItem>()

        for (plugin in _activePlugins.value) {
            try {
                val adapter = adapters[plugin.type] ?: continue
                results.addAll(adapter.search(plugin, query))
            } catch (e: Exception) {
                Timber.e(e, "Plugin ${plugin.name} failed during search")
            }
        }

        return results.distinctBy { it.id }
    }

    suspend fun addPlugin(manifestUrl: String): Result<Plugin> {
        return try {
            val plugin = detectAndCreatePlugin(manifestUrl)
            pluginRepository.insertPlugin(plugin)
            _activePlugins.value = _activePlugins.value + plugin
            Result.success(plugin)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add plugin from: $manifestUrl")
            Result.failure(e)
        }
    }

    private suspend fun detectAndCreatePlugin(url: String): Plugin {
        return when {
            url.contains("stremio") || url.endsWith("manifest.json") ->
                stremioAdapter.parseManifest(url)
            url.contains("cloudstream") || url.endsWith(".cs3") ->
                cloudStreamAdapter.parseManifest(url)
            else ->
                aioMetadataAdapter.parseManifest(url)
        }
    }

    suspend fun removePlugin(pluginId: String) {
        pluginRepository.deletePlugin(pluginId)
        _activePlugins.value = _activePlugins.value.filter { it.id != pluginId }
    }

    suspend fun togglePlugin(pluginId: String, enabled: Boolean) {
        pluginRepository.updatePluginEnabled(pluginId, enabled)
        _activePlugins.value = _activePlugins.value.map { plugin ->
            if (plugin.id == pluginId) plugin.copy(isEnabled = enabled) else plugin
        }
    }
}
