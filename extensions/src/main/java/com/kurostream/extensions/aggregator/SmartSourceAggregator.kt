package com.kurostream.extensions.aggregator

import com.kurostream.domain.extension.*
import com.kurostream.domain.entity.MediaItem
import com.kurostream.domain.entity.VideoSource
import com.kurostream.extensions.cloudstream.CloudStreamAdapter
import com.kurostream.extensions.cloudstream.CloudStreamPlugin
import com.kurostream.extensions.jellyfin.JellyfinAdapter
import com.kurostream.extensions.kodi.KodiAdapter
import com.kurostream.extensions.plex.PlexAdapter
import com.kurostream.extensions.stremio.StremioAdapter
import com.kurostream.extensions.stremio.StremioManifest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartSourceAggregator @Inject constructor(
    private val extensionRepository: com.kurostream.domain.extension.ExtensionRepository,
    private val debridManager: com.kurostream.domain.debrid.DebridManager,
    private val healthMonitor: ExtensionHealthMonitor,
    private val stremioAdapter: StremioAdapter,
    private val cloudStreamAdapter: CloudStreamAdapter,
    private val jellyfinAdapter: JellyfinAdapter,
    private val kodiAdapter: KodiAdapter,
    private val plexAdapter: PlexAdapter,
) : SourceAggregator {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

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
        val disabled = disabledQueries[query]
        if (disabled != null && disabled.contains(ext.id)) return emptyList()

        return when (ext.sourceFormat) {
            ExtensionSourceFormat.STREMIO_ADDON -> searchStremio(ext, query, types)
            ExtensionSourceFormat.CLOUDSTREAM_REPO -> searchCloudStream(ext, query, types)
            ExtensionSourceFormat.JELLYFIN_SERVER -> searchJellyfin(ext, query, types)
            ExtensionSourceFormat.KODI_REPOSITORY -> searchKodi(ext, query, types)
            ExtensionSourceFormat.PLEX_SERVER -> searchPlex(ext, query, types)
            else -> emptyList()
        }
    }

    private suspend fun getStreamsFromExtension(ext: UnifiedExtension, mediaId: String, type: ContentType): List<StreamAggregateResult> {
        val disabled = disabledQueries[mediaId]
        if (disabled != null && disabled.contains(ext.id)) return emptyList()

        return when (ext.sourceFormat) {
            ExtensionSourceFormat.STREMIO_ADDON -> streamsFromStremio(ext, mediaId, type)
            ExtensionSourceFormat.JELLYFIN_SERVER -> streamsFromJellyfin(ext, mediaId)
            ExtensionSourceFormat.PLEX_SERVER -> streamsFromPlex(ext, mediaId)
            ExtensionSourceFormat.KODI_REPOSITORY -> streamsFromKodi(ext, mediaId)
            ExtensionSourceFormat.CLOUDSTREAM_REPO -> streamsFromCloudStream(ext, mediaId, type)
            else -> emptyList()
        }
    }

    private suspend fun getHomeRowsFromExtension(ext: UnifiedExtension): List<HomeRowResult> {
        return when (ext.sourceFormat) {
            ExtensionSourceFormat.STREMIO_ADDON -> homeRowsFromStremio(ext)
            ExtensionSourceFormat.JELLYFIN_SERVER -> homeRowsFromJellyfin(ext)
            ExtensionSourceFormat.PLEX_SERVER -> homeRowsFromPlex(ext)
            ExtensionSourceFormat.CLOUDSTREAM_REPO -> emptyList() // CloudStream repos don't expose home rows
            ExtensionSourceFormat.KODI_REPOSITORY -> emptyList()
            else -> emptyList()
        }
    }

    // ── Stremio ───────────────────────────────────────────────────────────────

    private suspend fun searchStremio(ext: UnifiedExtension, query: String, types: Set<ContentType>): List<MediaSearchResult> {
        return try {
            val manifest = json.decodeFromString<StremioManifest>(ext.rawManifest)
            val results = mutableListOf<MediaSearchResult>()
            for (type in types) {
                stremioAdapter.getCatalog(manifest, type.toStremioType(), mapOf("search" to query))
                    .getOrNull()?.let { results.addAll(it) }
            }
            results
        } catch (e: Exception) {
            Timber.w(e, "Stremio search failed for ${ext.id}")
            emptyList()
        }
    }

    private suspend fun streamsFromStremio(ext: UnifiedExtension, mediaId: String, type: ContentType): List<StreamAggregateResult> {
        return try {
            val manifest = json.decodeFromString<StremioManifest>(ext.rawManifest)
            stremioAdapter.getStreams(manifest, mediaId, type.toStremioType()).getOrElse { emptyList() }
        } catch (e: Exception) {
            Timber.w(e, "Stremio getStreams failed for ${ext.id}")
            emptyList()
        }
    }

    private suspend fun homeRowsFromStremio(ext: UnifiedExtension): List<HomeRowResult> {
        return try {
            val manifest = json.decodeFromString<StremioManifest>(ext.rawManifest)
            val rows = mutableListOf<HomeRowResult>()
            manifest.catalogs?.forEach { catalog ->
                stremioAdapter.getCatalog(manifest, catalog.type).getOrNull()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { items ->
                        rows.add(HomeRowResult(
                            title = "${manifest.name} — ${catalog.name ?: catalog.type.replaceFirstChar { it.uppercase() }}",
                            items = items.map { it.media },
                            sourceExtensionId = ext.id,
                        ))
                    }
            }
            rows
        } catch (e: Exception) {
            Timber.w(e, "Stremio getHomeRows failed for ${ext.id}")
            emptyList()
        }
    }

    // ── CloudStream ───────────────────────────────────────────────────────────

    private suspend fun searchCloudStream(ext: UnifiedExtension, query: String, types: Set<ContentType>): List<MediaSearchResult> {
        return try {
            val plugin = json.decodeFromString<CloudStreamPlugin>(ext.rawManifest)
            // CloudStream plugins expose a /search endpoint via their plugin URL
            val searchUrl = "${plugin.url}/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
            cloudStreamAdapter.fetchRepository(searchUrl)
                .map { repo ->
                    repo.plugins.map { p ->
                        MediaSearchResult(
                            media = MediaItem(
                                id = "cs_${ext.id}_${p.name.hashCode()}",
                                title = p.name,
                                thumbnailUrl = p.icon ?: "",
                                mediaType = types.firstOrNull()?.name ?: "MOVIE",
                            ),
                            confidence = 0.7f,
                            sourceExtensionId = ext.id,
                        )
                    }
                }.getOrElse { emptyList() }
        } catch (e: Exception) {
            Timber.w(e, "CloudStream search failed for ${ext.id}")
            emptyList()
        }
    }

    private suspend fun streamsFromCloudStream(ext: UnifiedExtension, mediaId: String, type: ContentType): List<StreamAggregateResult> {
        return try {
            val plugin = json.decodeFromString<CloudStreamPlugin>(ext.rawManifest)
            val streamsUrl = "${plugin.url}/streams/${type.toStremioType()}/$mediaId.json"
            // Fetch stream list from CloudStream plugin endpoint
            val results = cloudStreamAdapter.fetchRepository(streamsUrl).getOrNull()
            results?.plugins?.map { p ->
                StreamAggregateResult(
                    url = p.url,
                    title = p.name,
                    qualityScore = 60f,
                    debridCached = false,
                    sourceExtensionId = ext.id,
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Timber.w(e, "CloudStream getStreams failed for ${ext.id}")
            emptyList()
        }
    }

    // ── Jellyfin ──────────────────────────────────────────────────────────────

    private suspend fun searchJellyfin(ext: UnifiedExtension, query: String, types: Set<ContentType>): List<MediaSearchResult> {
        return try {
            jellyfinAdapter.search(query)
                .getOrElse { emptyList() }
                .map { item ->
                    MediaSearchResult(
                        media = item.toMediaItem(jellyfinAdapter),
                        confidence = 0.85f,
                        sourceExtensionId = ext.id,
                    )
                }
        } catch (e: Exception) {
            Timber.w(e, "Jellyfin search failed for ${ext.id}")
            emptyList()
        }
    }

    private suspend fun streamsFromJellyfin(ext: UnifiedExtension, mediaId: String): List<StreamAggregateResult> {
        return try {
            buildList {
                val directUrl = jellyfinAdapter.getDirectPlayUrl(mediaId)
                add(StreamAggregateResult(
                    source = ext,
                    stream = VideoSource(url = directUrl, quality = "Direct Play"),
                    qualityScore = 95f,
                    debridCached = false,
                ))
                val transcodeUrl = jellyfinAdapter.getTranscodedUrl(mediaId)
                add(StreamAggregateResult(
                    source = ext,
                    stream = VideoSource(url = transcodeUrl, quality = "Transcoded", isHls = true),
                    qualityScore = 75f,
                    debridCached = false,
                ))
            }
        } catch (e: Exception) {
            Timber.w(e, "Jellyfin getStreams failed for ${ext.id}")
            emptyList()
        }
    }

    private suspend fun homeRowsFromJellyfin(ext: UnifiedExtension): List<HomeRowResult> {
        return try {
            val rows = mutableListOf<HomeRowResult>()

            jellyfinAdapter.getMovies(limit = 20).getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { rows.add(HomeRowResult("Latest Movies", it.map { i -> i.toMediaItem(jellyfinAdapter) }, ext.id)) }

            jellyfinAdapter.getSeries(limit = 20).getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { rows.add(HomeRowResult("Latest Series", it.map { i -> i.toMediaItem(jellyfinAdapter) }, ext.id)) }

            jellyfinAdapter.getResumeItems().getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { rows.add(HomeRowResult("Continue Watching", it.map { i -> i.toMediaItem(jellyfinAdapter) }, ext.id)) }

            rows
        } catch (e: Exception) {
            Timber.w(e, "Jellyfin getHomeRows failed for ${ext.id}")
            emptyList()
        }
    }

    // ── Kodi ──────────────────────────────────────────────────────────────────
    // KodiAdapter exposes repository discovery; individual stream resolution
    // is delegated to Kodi's JSON-RPC API via the extension's configured URL.

    private suspend fun searchKodi(ext: UnifiedExtension, query: String, types: Set<ContentType>): List<MediaSearchResult> {
        // Kodi repository addons don't expose a search endpoint in the
        // standard repo format; the search URL is addon-specific.
        // Return empty and let other engines handle search for now.
        Timber.d("Kodi search not supported for ${ext.id} (repo-level addon)")
        return emptyList()
    }

    private suspend fun streamsFromKodi(ext: UnifiedExtension, mediaId: String): List<StreamAggregateResult> {
        // Kodi addons expose streams via their plugin:// URI scheme.
        // Construct a best-effort plugin URL from the stored manifest.
        return try {
            val addon = json.decodeFromString<com.kurostream.extensions.kodi.KodiAddon>(ext.rawManifest)
            val pluginUrl = "plugin://${addon.id}/stream?id=$mediaId"
            listOf(StreamAggregateResult(
                source = ext,
                stream = VideoSource(url = pluginUrl, quality = "Kodi Plugin"),
                qualityScore = 60f,
                debridCached = false,
            ))
        } catch (e: Exception) {
            Timber.w(e, "Kodi streamsFromKodi failed for ${ext.id}")
            emptyList()
        }
    }

    // ── Plex ──────────────────────────────────────────────────────────────────

    private suspend fun searchPlex(ext: UnifiedExtension, query: String, types: Set<ContentType>): List<MediaSearchResult> {
        return try {
            plexAdapter.search(query)
                .getOrElse { emptyList() }
                .map { item ->
                    MediaSearchResult(
                        media = item.toMediaItem(plexAdapter),
                        confidence = 0.90f,
                        sourceExtensionId = ext.id,
                    )
                }
        } catch (e: Exception) {
            Timber.w(e, "Plex search failed for ${ext.id}")
            emptyList()
        }
    }

    private suspend fun streamsFromPlex(ext: UnifiedExtension, mediaId: String): List<StreamAggregateResult> {
        return try {
            // Look up the item to get the partKey needed for direct-play URL
            val items = plexAdapter.search(mediaId).getOrElse { emptyList() }
            val item  = items.firstOrNull()
            val partKey = item?.firstPart

            buildList {
                if (partKey != null) {
                    val directUrl = plexAdapter.getDirectPlayUrl(mediaId, partKey)
                    add(StreamAggregateResult(
                        source = ext,
                        stream = VideoSource(url = directUrl, quality = "Direct Play"),
                        qualityScore = 95f,
                        debridCached = false,
                    ))
                }
                val sessionId = java.util.UUID.randomUUID().toString()
                val transUrl = plexAdapter.getTranscodeUrl(mediaId, sessionId)
                add(StreamAggregateResult(
                    source = ext,
                    stream = VideoSource(url = transUrl, quality = "Transcoded 1080p", isHls = true),
                    qualityScore = 78f,
                    debridCached = false,
                ))
            }
        } catch (e: Exception) {
            Timber.w(e, "Plex getStreams failed for ${ext.id}")
            emptyList()
        }
    }

    private suspend fun homeRowsFromPlex(ext: UnifiedExtension): List<HomeRowResult> {
        return try {
            val rows = mutableListOf<HomeRowResult>()

            plexAdapter.getOnDeck().getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { rows.add(HomeRowResult("On Deck", it.map { i -> i.toMediaItem(plexAdapter) }, ext.id)) }

            plexAdapter.getRecentlyAdded().getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { rows.add(HomeRowResult("Recently Added", it.map { i -> i.toMediaItem(plexAdapter) }, ext.id)) }

            rows
        } catch (e: Exception) {
            Timber.w(e, "Plex getHomeRows failed for ${ext.id}")
            emptyList()
        }
    }

    // ── Type helpers ──────────────────────────────────────────────────────────

    private fun ContentType.toStremioType(): String = when (this) {
        ContentType.MOVIE -> "movie"
        ContentType.TV -> "series"
        ContentType.ANIME -> "anime"
        ContentType.LIVE_TV -> "live"
        ContentType.DOCUMENTARY -> "movie"
        ContentType.SPORTS -> "live"
    }

    // ── Model mappers ─────────────────────────────────────────────────────────

    private fun com.kurostream.extensions.jellyfin.JellyfinItem.toMediaItem(
        adapter: JellyfinAdapter,
    ): com.kurostream.domain.entity.MediaItem {
        return com.kurostream.domain.entity.MediaItem(
            id          = Id,
            title       = Name,
            description = Overview ?: "",
            posterUrl   = if (hasPoster) adapter.getImageUrl(Id) else "",
            backdropUrl = if (hasBackdrop) adapter.getImageUrl(Id, com.kurostream.extensions.jellyfin.ImageType.Backdrop) else "",
            genre       = Genres,
            rating      = CommunityRating ?: 0f,
            year        = ProductionYear ?: 0,
            duration    = (durationMs / 1_000).toInt(),
            source      = "jellyfin",
            watchProgress = (UserData?.PlaybackPositionTicks ?: 0L) / 10_000,
        )
    }

    private fun com.kurostream.extensions.plex.PlexItem.toMediaItem(
        adapter: PlexAdapter,
    ): com.kurostream.domain.entity.MediaItem {
        return com.kurostream.domain.entity.MediaItem(
            id          = ratingKey,
            title       = title,
            description = summary ?: "",
            posterUrl   = thumb?.let { adapter.getThumbUrl(it) } ?: "",
            backdropUrl = art?.let { adapter.getArtUrl(it) } ?: "",
            genre       = emptyList(),
            rating      = rating ?: 0f,
            year        = year ?: 0,
            duration    = ((duration ?: 0L) / 1_000).toInt(),
            source      = "plex",
            watchProgress = viewOffset ?: 0L,
        )
    }
}
