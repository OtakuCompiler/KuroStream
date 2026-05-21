package com.kurostream.tv.core.plugin

import com.kurostream.tv.data.adapter.cloudstream.CloudStreamPluginLoader
import com.kurostream.tv.data.adapter.stremio.StremioAdapter
import com.kurostream.tv.data.adapter.stremio.StremioAddon
import com.kurostream.tv.domain.provider.AnimeProvider
import com.kurostream.tv.domain.provider.ProviderAnimeDetails
import com.kurostream.tv.domain.provider.ProviderEpisode
import com.kurostream.tv.domain.provider.ProviderSearchResult
import com.kurostream.tv.domain.provider.ProviderStream
import com.kurostream.tv.domain.provider.ProviderType
import com.kurostream.tv.domain.provider.StreamQuality
import com.kurostream.tv.domain.provider.StreamType
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified plugin interface that all content sources must implement.
 *
 * Abstracts Stremio addons, CloudStream plugins, and future plugin types
 * behind a single contract so the rest of the app never needs to know
 * which protocol it is talking to.
 */
interface UniversalPlugin {
    val id: String
    val name: String
    val type: UniversalPluginType
    val isHealthy: Boolean
    val capabilities: Set<PluginCapability>

    fun asAnimeProvider(): AnimeProvider
    suspend fun ping(): Boolean
    fun release()
}

enum class UniversalPluginType {
    STREMIO,
    CLOUDSTREAM,
    NUVIO_COMPAT,
    INTERNAL
}

enum class PluginCapability {
    SEARCH,
    CATALOG,
    STREAM,
    METADATA,
    SUBTITLES,
    SKIP_TIMESTAMPS
}

// ─── Plugin lifecycle ─────────────────────────────────────────────────────────

data class PluginInfo(
    val plugin: UniversalPlugin,
    val status: UniversalPluginStatus,
    val installTime: Long = System.currentTimeMillis(),
    val lastHealthCheck: Long = 0L,
    val errorCount: Int = 0,
    val errorMessage: String? = null
)

enum class UniversalPluginStatus {
    LOADING,
    ACTIVE,
    DEGRADED,
    FAILED,
    DISABLED
}

// ─── PluginManager ────────────────────────────────────────────────────────────

/**
 * Central plugin registry and lifecycle manager.
 *
 * Wraps Stremio addons (via [StremioAdapter]) and CloudStream plugins (via
 * [CloudStreamPluginLoader]) in a unified [UniversalPlugin] abstraction,
 * runs health checks, and exposes the active provider list to [ProviderAggregator].
 */
@Singleton
class PluginManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stremioAdapter: StremioAdapter,
    private val cloudStreamPluginLoader: CloudStreamPluginLoader
) {
    companion object {
        private const val TAG = "PluginManager"
        private const val MAX_ERROR_COUNT = 3
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _plugins = MutableStateFlow<Map<String, PluginInfo>>(emptyMap())
    val plugins: StateFlow<Map<String, PluginInfo>> = _plugins.asStateFlow()

    val activeProviders: List<AnimeProvider>
        get() = _plugins.value.values
            .filter { it.status == UniversalPluginStatus.ACTIVE || it.status == UniversalPluginStatus.DEGRADED }
            .map { it.plugin.asAnimeProvider() }

    // ─── Registration ─────────────────────────────────────────────────────────

    fun register(plugin: UniversalPlugin) {
        val info = PluginInfo(plugin = plugin, status = UniversalPluginStatus.LOADING)
        updatePlugin(plugin.id, info)

        scope.launch {
            val healthy = runCatching { plugin.ping() }.getOrDefault(false)
            updatePlugin(
                plugin.id,
                info.copy(
                    status = if (healthy) UniversalPluginStatus.ACTIVE else UniversalPluginStatus.FAILED,
                    lastHealthCheck = System.currentTimeMillis(),
                    errorMessage = if (!healthy) "Initial health check failed" else null
                )
            )
            Timber.tag(TAG).d("Registered ${plugin.type} plugin ${plugin.id}: healthy=$healthy")
        }
    }

    /**
     * Install a Stremio addon by manifest URL. Delegates to [StremioAdapter.installAddon]
     * and wraps the result as a [StremioPlugin].
     */
    suspend fun installStremioAddon(manifestUrl: String): Result<UniversalPlugin> =
        runCatching {
            val addon = stremioAdapter.installAddon(manifestUrl).getOrThrow()
            val plugin = StremioPlugin(addon = addon, adapter = stremioAdapter)
            register(plugin)
            Timber.tag(TAG).i("Installed Stremio addon: ${addon.name}")
            plugin as UniversalPlugin
        }

    /**
     * Install a CloudStream plugin from a .cs3 / .jar file path.
     * Wraps [CloudStreamPluginLoader.loadPlugin] output as a [CloudStreamUniversalPlugin].
     */
    suspend fun installCloudStreamPlugin(pluginPath: String): Result<UniversalPlugin> =
        runCatching {
            val file = File(pluginPath)
            val csPlugin = cloudStreamPluginLoader.loadPlugin(file).getOrThrow()
            val providers = cloudStreamPluginLoader.getProvidersFromPlugin(csPlugin.id)
            val plugin = CloudStreamUniversalPlugin(
                pluginId = "cs3:${csPlugin.id}",
                pluginName = csPlugin.name,
                providers = providers
            )
            register(plugin)
            Timber.tag(TAG).i("Installed CloudStream plugin: ${csPlugin.name}")
            plugin as UniversalPlugin
        }

    /** Wrap an existing [AnimeProvider] (internal / built-in) as a tracked plugin. */
    fun registerProvider(provider: AnimeProvider) {
        register(InternalPlugin(provider))
    }

    fun disable(pluginId: String) = updateStatusIfPresent(pluginId, UniversalPluginStatus.DISABLED)
    fun enable(pluginId: String) = updateStatusIfPresent(pluginId, UniversalPluginStatus.ACTIVE)

    fun unregister(pluginId: String) {
        val current = _plugins.value.toMutableMap()
        current.remove(pluginId)?.plugin?.release()
        _plugins.value = current
    }

    // ─── Health checks ────────────────────────────────────────────────────────

    fun runHealthChecks() {
        scope.launch {
            _plugins.value.values
                .filter { it.status != UniversalPluginStatus.DISABLED }
                .forEach { info -> checkHealth(info) }
        }
    }

    private suspend fun checkHealth(info: PluginInfo) {
        val healthy = runCatching { info.plugin.ping() }.getOrDefault(false)
        val newStatus = when {
            healthy -> UniversalPluginStatus.ACTIVE
            info.errorCount + 1 >= MAX_ERROR_COUNT -> UniversalPluginStatus.FAILED
            else -> UniversalPluginStatus.DEGRADED
        }
        updatePlugin(
            info.plugin.id,
            info.copy(
                status = newStatus,
                lastHealthCheck = System.currentTimeMillis(),
                errorCount = if (healthy) 0 else info.errorCount + 1,
                errorMessage = if (!healthy) "Health check failed" else null
            )
        )
    }

    fun recordError(pluginId: String, message: String) {
        val info = _plugins.value[pluginId] ?: return
        val newCount = info.errorCount + 1
        updatePlugin(
            pluginId,
            info.copy(
                errorCount = newCount,
                status = if (newCount >= MAX_ERROR_COUNT) UniversalPluginStatus.DEGRADED else info.status,
                errorMessage = message
            )
        )
    }

    fun getPlugin(id: String): PluginInfo? = _plugins.value[id]
    fun isInstalled(id: String): Boolean = _plugins.value.containsKey(id)

    fun releaseAll() {
        _plugins.value.values.forEach { it.plugin.release() }
        _plugins.value = emptyMap()
    }

    private fun updatePlugin(id: String, info: PluginInfo) {
        val current = _plugins.value.toMutableMap()
        current[id] = info
        _plugins.value = current
    }

    private fun updateStatusIfPresent(id: String, status: UniversalPluginStatus) {
        val info = _plugins.value[id] ?: return
        updatePlugin(id, info.copy(status = status))
    }
}

// ─── Concrete plugin wrappers ─────────────────────────────────────────────────

/**
 * Wraps a [StremioAddon] (returned by [StremioAdapter.installAddon]) as a
 * [UniversalPlugin]. Stream/search calls delegate to [StremioAdapter].
 */
internal class StremioPlugin(
    val addon: StremioAddon,
    private val adapter: StremioAdapter
) : UniversalPlugin {

    override val id: String = "stremio:${addon.id}"
    override val name: String = addon.name
    override val type: UniversalPluginType = UniversalPluginType.STREMIO
    override var isHealthy: Boolean = true

    override val capabilities: Set<PluginCapability> = buildSet {
        if (addon.resources.any { it.name == "catalog" }) add(PluginCapability.CATALOG)
        if (addon.resources.any { it.name == "stream" }) add(PluginCapability.STREAM)
        if (addon.resources.any { it.name == "meta" }) add(PluginCapability.METADATA)
        if (addon.resources.any { it.name == "subtitles" }) add(PluginCapability.SUBTITLES)
    }

    override fun asAnimeProvider(): AnimeProvider =
        StremioAnimeProviderAdapter(addon, adapter)

    override suspend fun ping(): Boolean =
        adapter.getInstalledAddons().any { it.id == addon.id }

    override fun release() {
        isHealthy = false
        adapter.removeAddon(addon.id)
    }
}

/**
 * Wraps [AnimeSourceProvider]s from a loaded CloudStream plugin.
 * Named [CloudStreamUniversalPlugin] to avoid a name clash with the loader's
 * own `CloudStreamPlugin` data class.
 */
internal class CloudStreamUniversalPlugin(
    override val id: String,
    override val name: String,
    private val providers: List<com.kurostream.tv.data.adapter.cloudstream.AnimeSourceProvider>
) : UniversalPlugin {

    override val type: UniversalPluginType = UniversalPluginType.CLOUDSTREAM
    override var isHealthy: Boolean = providers.isNotEmpty()

    override val capabilities: Set<PluginCapability> = setOf(
        PluginCapability.SEARCH,
        PluginCapability.STREAM,
        PluginCapability.CATALOG
    )

    override fun asAnimeProvider(): AnimeProvider =
        providers.firstOrNull()
            ?.let { CloudStreamProviderAdapter(it) }
            ?: NullAnimeProvider(id)

    override suspend fun ping(): Boolean = isHealthy && providers.isNotEmpty()

    override fun release() { isHealthy = false }
}

/** Wraps a built-in [AnimeProvider] for lifecycle tracking in [PluginManager]. */
internal class InternalPlugin(private val provider: AnimeProvider) : UniversalPlugin {

    override val id: String = "internal:${provider.name.lowercase().replace(" ", "_")}"
    override val name: String = provider.name
    override val type: UniversalPluginType = UniversalPluginType.INTERNAL
    override var isHealthy: Boolean = true
    override val capabilities: Set<PluginCapability> = setOf(
        PluginCapability.SEARCH, PluginCapability.STREAM, PluginCapability.METADATA
    )

    override fun asAnimeProvider(): AnimeProvider = provider
    override suspend fun ping(): Boolean = runCatching { provider.checkAvailability() }.getOrDefault(false)
    override fun release() { isHealthy = false }
}

// ─── AnimeProvider adapters ───────────────────────────────────────────────────

/**
 * Adapts a [StremioAddon] + [StremioAdapter] to the [AnimeProvider] interface
 * so Stremio addons can participate in [ProviderAggregator] alongside native providers.
 */
internal class StremioAnimeProviderAdapter(
    private val addon: StremioAddon,
    private val adapter: StremioAdapter
) : AnimeProvider {

    override val providerId: String = "stremio:${addon.id}"
    override val name: String = addon.name
    override val type: ProviderType = ProviderType.STREMIO
    override val language: String = "en"
    override val isEnabled: Boolean = true
    override val priority: Int = 50
    override val supportsSearch: Boolean = addon.resources.any { it.name == "catalog" }

    override suspend fun search(
        query: String,
        page: Int
    ): Result<List<ProviderSearchResult>> = runCatching {
        val catalog = addon.catalogs.firstOrNull { it.type == "series" || it.type == "anime" }
            ?: return@runCatching emptyList()
        adapter.searchCatalog(addon.id, catalog.id, catalog.type, query)
            .map { meta ->
                ProviderSearchResult(
                    providerId = providerId,
                    id = meta.id,
                    title = meta.name,
                    alternativeTitles = emptyList(),
                    posterUrl = meta.poster,
                    year = meta.releaseInfo?.toIntOrNull(),
                    type = meta.type,
                    score = meta.imdbRating?.toFloatOrNull()
                )
            }
    }

    override suspend fun getAnimeDetails(animeId: String): Result<ProviderAnimeDetails?> =
        runCatching {
            val meta = adapter.getMetadata("series", animeId) ?: return@runCatching null
            ProviderAnimeDetails(
                providerId = providerId,
                id = meta.id,
                title = meta.name,
                description = meta.description,
                posterUrl = meta.poster,
                bannerUrl = meta.background,
                year = meta.releaseInfo?.toIntOrNull(),
                status = null,
                genres = meta.genres,
                rating = meta.imdbRating?.toFloatOrNull(),
                episodeCount = meta.videos.size.takeIf { it > 0 },
                trailerUrl = null
            )
        }

    override suspend fun getEpisodes(animeId: String): Result<List<ProviderEpisode>> =
        runCatching {
            val meta = adapter.getMetadata("series", animeId) ?: return@runCatching emptyList()
            meta.videos.mapIndexed { idx, v ->
                ProviderEpisode(
                    providerId = providerId,
                    id = v.id,
                    animeId = animeId,
                    number = v.episode ?: (idx + 1),
                    season = v.season,
                    title = v.title,
                    description = v.overview,
                    thumbnailUrl = v.thumbnail,
                    airDate = v.released,
                    durationMinutes = null
                )
            }
        }

    override suspend fun getStreams(
        animeId: String,
        episodeId: String
    ): Result<List<ProviderStream>> = runCatching {
        adapter.getStreams("series", "$animeId:$episodeId")
            .mapNotNull { s ->
                val url = s.url ?: s.infoHash?.let { "magnet:?xt=urn:btih:$it" } ?: return@mapNotNull null
                val quality = inferQuality(s.title ?: s.name ?: "")
                ProviderStream(
                    providerId = providerId,
                    providerName = addon.name,
                    url = url,
                    quality = quality,
                    qualityLabel = quality.name,
                    type = if (s.isTorrent) StreamType.TORRENT else StreamType.DIRECT,
                    language = "ja",
                    subtitles = emptyList(),
                    headers = s.behaviorHints.proxyHeaders,
                    referer = null,
                    isWorking = true,
                    metadata = null
                )
            }
    }

    override suspend fun checkAvailability(): Boolean =
        adapter.getInstalledAddons().any { it.id == addon.id }

    private fun inferQuality(title: String): StreamQuality {
        val t = title.uppercase()
        return when {
            "4K" in t || "2160" in t || "UHD" in t -> StreamQuality.UHD_4K
            "1080" in t -> StreamQuality.HD_1080
            "720" in t -> StreamQuality.HD_720
            "480" in t -> StreamQuality.SD_480
            "360" in t -> StreamQuality.SD_360
            else -> StreamQuality.UNKNOWN
        }
    }
}

/**
 * Adapts a CloudStream [AnimeSourceProvider] to the [AnimeProvider] interface.
 */
internal class CloudStreamProviderAdapter(
    private val source: com.kurostream.tv.data.adapter.cloudstream.AnimeSourceProvider
) : AnimeProvider {

    override val providerId: String = "cs3:${source.pluginId}"
    override val name: String = source.name
    override val type: ProviderType = ProviderType.CLOUDSTREAM
    override val language: String = source.language
    override val isEnabled: Boolean = true
    override val priority: Int = 60
    override val supportsSearch: Boolean = true

    override suspend fun search(query: String, page: Int): Result<List<ProviderSearchResult>> =
        runCatching {
            source.search(query).map { r ->
                ProviderSearchResult(
                    providerId = providerId,
                    id = r.url,
                    title = r.name,
                    alternativeTitles = emptyList(),
                    posterUrl = r.posterUrl,
                    year = null,
                    type = "series",
                    score = null
                )
            }
        }

    override suspend fun getAnimeDetails(animeId: String): Result<ProviderAnimeDetails?> =
        runCatching {
            source.getAnimeDetails(animeId)?.let { d ->
                ProviderAnimeDetails(
                    providerId = providerId,
                    id = animeId,
                    title = d.name,
                    description = d.plot,
                    posterUrl = d.posterUrl,
                    bannerUrl = null,
                    year = d.year,
                    status = null,
                    genres = d.tags,
                    rating = d.rating,
                    episodeCount = d.episodes.size.takeIf { it > 0 },
                    trailerUrl = null
                )
            }
        }

    override suspend fun getEpisodes(animeId: String): Result<List<ProviderEpisode>> =
        runCatching {
            source.getEpisodes(animeId).mapIndexed { idx, ep ->
                ProviderEpisode(
                    providerId = providerId,
                    id = ep.url,
                    animeId = animeId,
                    number = ep.episode ?: (idx + 1),
                    season = ep.season,
                    title = ep.name,
                    description = ep.description,
                    thumbnailUrl = ep.posterUrl,
                    airDate = null,
                    durationMinutes = null
                )
            }
        }

    override suspend fun getStreams(
        animeId: String,
        episodeId: String
    ): Result<List<ProviderStream>> = runCatching {
        source.getStreamLinks(episodeId).map { link ->
            val quality = StreamQuality.fromPixels(link.quality ?: 0)
            ProviderStream(
                providerId = providerId,
                providerName = source.name,
                url = link.url,
                quality = quality,
                qualityLabel = link.quality?.let { "${it}p" } ?: quality.name,
                type = when {
                    link.url.startsWith("magnet:") -> StreamType.TORRENT
                    link.isM3u8 -> StreamType.HLS
                    else -> StreamType.DIRECT
                },
                language = source.language,
                subtitles = emptyList(),
                headers = link.headers,
                referer = link.referer,
                isWorking = true,
                metadata = null
            )
        }
    }

    override suspend fun checkAvailability(): Boolean =
        runCatching { source.search("test").isNotEmpty() || true }.getOrDefault(false)
}

/**
 * No-op [AnimeProvider] returned when a plugin fails to load any usable providers.
 */
internal class NullAnimeProvider(override val name: String) : AnimeProvider {
    override val providerId: String = "null:$name"
    override val type: ProviderType = ProviderType.DIRECT
    override val language: String = "en"
    override val isEnabled: Boolean = false
    override val priority: Int = Int.MIN_VALUE
    override val supportsSearch: Boolean = false

    override suspend fun search(query: String, page: Int) =
        Result.success(emptyList<ProviderSearchResult>())

    override suspend fun getAnimeDetails(animeId: String) =
        Result.success<ProviderAnimeDetails?>(null)

    override suspend fun getEpisodes(animeId: String) =
        Result.success(emptyList<ProviderEpisode>())

    override suspend fun getStreams(animeId: String, episodeId: String) =
        Result.success(emptyList<ProviderStream>())

    override suspend fun checkAvailability(): Boolean = false
}
