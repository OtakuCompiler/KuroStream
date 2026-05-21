package com.kurostream.tv.domain.provider

import com.kurostream.tv.data.adapter.cloudstream.AnimeSourceProvider
import com.kurostream.tv.data.adapter.cloudstream.CloudStreamPluginLoader
import com.kurostream.tv.data.adapter.stremio.StremioAdapter
import com.kurostream.tv.data.adapter.stremio.StremioStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates multiple anime providers into a unified interface.
 * Handles provider prioritization, caching, and fallback logic.
 */
@Singleton
class ProviderAggregator @Inject constructor(
    private val stremioAdapter: StremioAdapter,
    private val cloudStreamPluginLoader: CloudStreamPluginLoader,
    private val ioDispatcher: CoroutineDispatcher
) {
    
    companion object {
        private const val TAG = "ProviderAggregator"
        private const val SEARCH_TIMEOUT_MS = 10000L
        private const val STREAM_TIMEOUT_MS = 15000L
    }
    
    private val _providers = MutableStateFlow<List<AnimeProvider>>(emptyList())
    val providers: Flow<List<AnimeProvider>> = _providers.asStateFlow()
    
    private val providerConfigs = mutableMapOf<String, ProviderConfig>()
    
    /**
     * Initialize all providers.
     */
    suspend fun initialize() = withContext(ioDispatcher) {
        val allProviders = mutableListOf<AnimeProvider>()
        
        // Initialize Stremio addons
        try {
            stremioAdapter.initialize()
            val stremioProviders = stremioAdapter.getAnimeAddons().map { addon ->
                StremioAnimeProvider(addon.id, addon.name, stremioAdapter)
            }
            allProviders.addAll(stremioProviders)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to initialize Stremio")
        }
        
        // Initialize CloudStream plugins
        try {
            cloudStreamPluginLoader.loadAllPlugins()
            val cloudStreamProviders = cloudStreamPluginLoader.getAllProviders().map { provider ->
                CloudStreamAnimeProvider(provider)
            }
            allProviders.addAll(cloudStreamProviders)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to initialize CloudStream")
        }
        
        _providers.update { allProviders }
    }
    
    /**
     * Get all enabled providers sorted by priority.
     */
    fun getEnabledProviders(): List<AnimeProvider> {
        return _providers.value
            .filter { provider ->
                val config = providerConfigs[provider.providerId]
                config?.isEnabled ?: provider.isEnabled
            }
            .sortedBy { provider ->
                val config = providerConfigs[provider.providerId]
                config?.priority ?: provider.priority
            }
    }
    
    /**
     * Search across all enabled providers.
     */
    suspend fun search(
        query: String,
        page: Int = 1,
        maxProviders: Int = 5
    ): List<ProviderSearchResult> = coroutineScope {
        val providers = getEnabledProviders().take(maxProviders)
        
        val results = providers.map { provider ->
            async {
                try {
                    withTimeoutOrNull(SEARCH_TIMEOUT_MS) {
                        provider.search(query, page).getOrElse { emptyList() }
                    } ?: emptyList()
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Search failed for ${provider.name}")
                    emptyList()
                }
            }
        }.awaitAll()
        
        // Merge and deduplicate results
        mergeSearchResults(results.flatten())
    }
    
    /**
     * Get anime details from best available provider.
     */
    suspend fun getAnimeDetails(
        animeId: String,
        preferredProviderId: String? = null
    ): ProviderAnimeDetails? = withContext(ioDispatcher) {
        val providers = if (preferredProviderId != null) {
            listOfNotNull(
                _providers.value.find { it.providerId == preferredProviderId }
            ) + getEnabledProviders().filter { it.providerId != preferredProviderId }
        } else {
            getEnabledProviders()
        }
        
        for (provider in providers) {
            try {
                val details = withTimeoutOrNull(SEARCH_TIMEOUT_MS) {
                    provider.getAnimeDetails(animeId).getOrNull()
                }
                if (details != null) {
                    return@withContext details
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Get details failed for ${provider.name}")
            }
        }
        
        null
    }
    
    /**
     * Get episodes from best available provider.
     */
    suspend fun getEpisodes(
        animeId: String,
        preferredProviderId: String? = null
    ): List<ProviderEpisode> = withContext(ioDispatcher) {
        val providers = if (preferredProviderId != null) {
            listOfNotNull(
                _providers.value.find { it.providerId == preferredProviderId }
            ) + getEnabledProviders().filter { it.providerId != preferredProviderId }
        } else {
            getEnabledProviders()
        }
        
        for (provider in providers) {
            try {
                val episodes = withTimeoutOrNull(SEARCH_TIMEOUT_MS) {
                    provider.getEpisodes(animeId).getOrElse { emptyList() }
                }
                if (!episodes.isNullOrEmpty()) {
                    return@withContext episodes
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Get episodes failed for ${provider.name}")
            }
        }
        
        emptyList()
    }
    
    /**
     * Get streams from all available providers.
     */
    suspend fun getStreams(
        animeId: String,
        episodeId: String,
        preferredQuality: StreamQuality = StreamQuality.HD_720
    ): List<ProviderStream> = coroutineScope {
        val providers = getEnabledProviders()
        
        val results = providers.map { provider ->
            async {
                try {
                    withTimeoutOrNull(STREAM_TIMEOUT_MS) {
                        provider.getStreams(animeId, episodeId).getOrElse { emptyList() }
                    } ?: emptyList()
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Get streams failed for ${provider.name}")
                    emptyList()
                }
            }
        }.awaitAll()
        
        // Merge and sort by quality preference
        val allStreams = results.flatten()
        sortStreamsByPreference(allStreams, preferredQuality)
    }
    
    /**
     * Get best stream for an episode.
     */
    suspend fun getBestStream(
        animeId: String,
        episodeId: String,
        maxQuality: StreamQuality = StreamQuality.HD_720,
        preferDirectStreams: Boolean = true
    ): ProviderStream? = withContext(ioDispatcher) {
        val streams = getStreams(animeId, episodeId, maxQuality)
        
        // Filter by max quality
        val filteredStreams = streams.filter { 
            it.quality.pixels <= maxQuality.pixels 
        }
        
        // Prefer direct streams for low-end devices
        if (preferDirectStreams) {
            filteredStreams.firstOrNull { 
                it.type == StreamType.DIRECT || it.type == StreamType.HLS 
            } ?: filteredStreams.firstOrNull()
        } else {
            filteredStreams.firstOrNull()
        }
    }
    
    /**
     * Update provider configuration.
     */
    fun updateProviderConfig(providerId: String, config: ProviderConfig) {
        providerConfigs[providerId] = config
    }
    
    /**
     * Get provider configuration.
     */
    fun getProviderConfig(providerId: String): ProviderConfig? {
        return providerConfigs[providerId]
    }
    
    /**
     * Check which providers have content for an anime.
     */
    suspend fun checkProviderAvailability(
        animeId: String
    ): Map<String, Boolean> = coroutineScope {
        val providers = getEnabledProviders()
        
        providers.associate { provider ->
            val hasContent = async {
                try {
                    withTimeoutOrNull(5000L) {
                        provider.getAnimeDetails(animeId).isSuccess
                    } ?: false
                } catch (e: Exception) {
                    false
                }
            }
            provider.providerId to hasContent
        }.mapValues { it.value.await() }
    }
    
    // Helper methods
    
    private fun mergeSearchResults(results: List<ProviderSearchResult>): List<ProviderSearchResult> {
        // Group by title similarity and take best result from each group
        val grouped = results.groupBy { it.title.lowercase().trim() }
        
        return grouped.map { (_, group) ->
            // Prefer results with more information
            group.maxByOrNull { result ->
                var score = 0
                if (result.posterUrl != null) score += 2
                if (result.year != null) score += 1
                if (result.score != null) score += 1
                score
            } ?: group.first()
        }.distinctBy { it.title.lowercase() }
    }
    
    private fun sortStreamsByPreference(
        streams: List<ProviderStream>,
        preferredQuality: StreamQuality
    ): List<ProviderStream> {
        return streams.sortedWith(compareBy(
            // Prefer working streams
            { !it.isWorking },
            // Prefer quality closest to preferred
            { kotlin.math.abs(it.quality.pixels - preferredQuality.pixels) },
            // Prefer direct streams over torrents
            { if (it.type == StreamType.TORRENT) 1 else 0 },
            // Prefer streams with subtitles
            { -it.subtitles.size }
        ))
    }
}

/**
 * Stremio adapter wrapper implementing AnimeProvider.
 */
private class StremioAnimeProvider(
    override val providerId: String,
    override val name: String,
    private val stremioAdapter: StremioAdapter
) : AnimeProvider {
    
    override val type = ProviderType.STREMIO
    override val language = "en"
    override val isEnabled = true
    override val priority = 50
    override val supportsSearch = true
    
    override suspend fun search(query: String, page: Int): Result<List<ProviderSearchResult>> {
        return try {
            val results = stremioAdapter.searchCatalog(
                addonId = providerId,
                catalogId = "top",
                contentType = "series",
                query = query
            )
            
            Result.success(results.map { meta ->
                ProviderSearchResult(
                    providerId = providerId,
                    id = meta.id,
                    title = meta.name,
                    posterUrl = meta.poster,
                    year = meta.releaseInfo?.toIntOrNull(),
                    type = meta.type
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAnimeDetails(animeId: String): Result<ProviderAnimeDetails?> {
        return try {
            val meta = stremioAdapter.getMetadata("series", animeId)
            
            Result.success(meta?.let {
                ProviderAnimeDetails(
                    providerId = providerId,
                    id = it.id,
                    title = it.name,
                    description = it.description,
                    posterUrl = it.poster,
                    bannerUrl = it.background,
                    year = it.releaseInfo?.toIntOrNull(),
                    status = null,
                    genres = it.genres,
                    rating = it.imdbRating?.toFloatOrNull(),
                    episodeCount = it.videos.size,
                    trailerUrl = null
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getEpisodes(animeId: String): Result<List<ProviderEpisode>> {
        return try {
            val meta = stremioAdapter.getMetadata("series", animeId)
            
            Result.success(meta?.videos?.map { video ->
                ProviderEpisode(
                    providerId = providerId,
                    id = video.id,
                    animeId = animeId,
                    number = video.episode ?: 0,
                    season = video.season,
                    title = video.title,
                    description = video.overview,
                    thumbnailUrl = video.thumbnail,
                    airDate = video.released,
                    durationMinutes = null
                )
            } ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getStreams(animeId: String, episodeId: String): Result<List<ProviderStream>> {
        return try {
            val streams = stremioAdapter.getStreams("series", episodeId)
            
            Result.success(streams.map { stream ->
                ProviderStream(
                    providerId = providerId,
                    providerName = stream.addonName,
                    url = stream.url ?: buildMagnetUrl(stream),
                    quality = StreamQuality.fromLabel(stream.displayName),
                    qualityLabel = extractQualityLabel(stream.displayName),
                    type = if (stream.isTorrent) StreamType.TORRENT else StreamType.DIRECT,
                    language = "en",
                    headers = stream.behaviorHints.proxyHeaders,
                    metadata = if (stream.isTorrent) {
                        StreamMetadata(fileName = stream.behaviorHints.filename)
                    } else null
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun checkAvailability(): Boolean {
        return try {
            stremioAdapter.getInstalledAddons().any { it.id == providerId }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun buildMagnetUrl(stream: StremioStream): String {
        val infoHash = stream.infoHash ?: return ""
        val fileIdx = stream.fileIdx ?: 0
        return "magnet:?xt=urn:btih:$infoHash&dn=${stream.behaviorHints.filename ?: "video"}"
    }
    
    private fun extractQualityLabel(name: String): String {
        val patterns = listOf("4K", "2160p", "1080p", "720p", "480p", "360p")
        return patterns.find { name.contains(it, ignoreCase = true) } ?: "Unknown"
    }
}

/**
 * CloudStream adapter wrapper implementing AnimeProvider.
 */
private class CloudStreamAnimeProvider(
    private val sourceProvider: AnimeSourceProvider
) : AnimeProvider {
    
    override val providerId = "${sourceProvider.pluginId}:${sourceProvider.name}"
    override val name = sourceProvider.name
    override val type = ProviderType.CLOUDSTREAM
    override val language = sourceProvider.language
    override val isEnabled = true
    override val priority = 60
    override val supportsSearch = true
    
    override suspend fun search(query: String, page: Int): Result<List<ProviderSearchResult>> {
        return try {
            val results = sourceProvider.search(query)
            
            Result.success(results.map { result ->
                ProviderSearchResult(
                    providerId = providerId,
                    id = result.url,
                    title = result.name,
                    posterUrl = result.posterUrl,
                    year = result.year,
                    type = result.type
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAnimeDetails(animeId: String): Result<ProviderAnimeDetails?> {
        return try {
            val details = sourceProvider.getAnimeDetails(animeId)
            
            Result.success(details?.let {
                ProviderAnimeDetails(
                    providerId = providerId,
                    id = it.url,
                    title = it.name,
                    description = it.plot,
                    posterUrl = it.posterUrl,
                    bannerUrl = null,
                    year = it.year,
                    status = null,
                    genres = it.tags,
                    rating = it.rating,
                    episodeCount = it.episodes.size,
                    trailerUrl = null
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getEpisodes(animeId: String): Result<List<ProviderEpisode>> {
        return try {
            val episodes = sourceProvider.getEpisodes(animeId)
            
            Result.success(episodes.mapIndexed { index, ep ->
                ProviderEpisode(
                    providerId = providerId,
                    id = ep.url,
                    animeId = animeId,
                    number = ep.episode ?: (index + 1),
                    season = ep.season,
                    title = ep.name,
                    description = ep.description,
                    thumbnailUrl = ep.posterUrl,
                    airDate = null,
                    durationMinutes = null
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getStreams(animeId: String, episodeId: String): Result<List<ProviderStream>> {
        return try {
            val links = sourceProvider.getStreamLinks(episodeId)
            
            Result.success(links.map { link ->
                ProviderStream(
                    providerId = providerId,
                    providerName = name,
                    url = link.url,
                    quality = StreamQuality.fromPixels(link.quality ?: 720),
                    qualityLabel = "${link.quality ?: 720}p",
                    type = if (link.isM3u8) StreamType.HLS else StreamType.DIRECT,
                    language = language,
                    headers = link.headers,
                    referer = link.referer
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun checkAvailability(): Boolean {
        return try {
            sourceProvider.search("test").isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
