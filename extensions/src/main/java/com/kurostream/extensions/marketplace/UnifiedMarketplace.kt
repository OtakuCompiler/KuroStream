package com.kurostream.extensions.marketplace

import com.kurostream.domain.extension.*
import com.kurostream.extensions.stremio.StremioAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnifiedMarketplace @Inject constructor(
    private val client: OkHttpClient,
    private val stremioAdapter: StremioAdapter,
) : ExtensionMarketplace {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val stremioCentralUrl = "https://api.strem.io/addonscollection/selected"
    private val cloudStreamMegaRepo = "https://raw.githubusercontent.com/recloudstream/cs-repos/master/repos-db.json"

    override suspend fun search(query: String, filters: MarketplaceFilters): List<MarketplaceItem> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<MarketplaceItem>()
            try {
                val stremio = async { searchStremioCentral(query, filters) }
                val cloudstream = async { searchCloudStreamRepos(query, filters) }
                results.addAll(stremio.await())
                results.addAll(cloudstream.await())
            } catch (e: Exception) {
                Timber.e(e, "Marketplace search failed")
            }
            results.sortedByDescending { it.avgRating }.take(50)
        }
    }

    override suspend fun getFeatured(): List<MarketplaceItem> {
        return withContext(Dispatchers.IO) {
            try {
                searchStremioCentral("", MarketplaceFilters(onlyOfficial = true)).take(10)
            } catch (e: Exception) {
                Timber.e(e, "Featured load failed")
                emptyList()
            }
        }
    }

    override suspend fun getTrending(): List<MarketplaceItem> {
        return withContext(Dispatchers.IO) {
            try {
                searchStremioCentral("", MarketplaceFilters()).sortedByDescending { it.installCount }.take(20)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun getCategories(): List<MarketplaceCategory> {
        return listOf(
            MarketplaceCategory("sources", "Stream Sources", "movie", "Addons that provide video streams", 500),
            MarketplaceCategory("metadata", "Metadata", "info", "Posters, descriptions, ratings", 50),
            MarketplaceCategory("subtitles", "Subtitles", "subtitles", "Subtitle providers", 10),
            MarketplaceCategory("debrid", "Debrid", "cloud", "Debrid service integrations", 5),
            MarketplaceCategory("anime", "Anime", "animation", "Anime-specific sources", 100),
            MarketplaceCategory("live_tv", "Live TV", "live_tv", "IPTV and live channels", 30),
        )
    }

    override suspend fun getItemDetails(itemId: String): MarketplaceItem? {
        return null
    }

    override fun observeInstalledCount(itemId: String): kotlinx.coroutines.flow.Flow<Long> {
        return kotlinx.coroutines.flow.flowOf(0L)
    }

    private suspend fun searchStremioCentral(query: String, filters: MarketplaceFilters): List<MarketplaceItem> {
        val request = Request.Builder().url(stremioCentralUrl).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()
        val body = response.body?.string() ?: return emptyList()
        val collection = json.decodeFromString<StremioCollectionResponse>(body)

        return collection.addons.filter { addon ->
            val matchesQuery = query.isBlank() ||
                addon.manifest.name.contains(query, ignoreCase = true) ||
                (addon.manifest.description?.contains(query, ignoreCase = true) ?: false)
            val matchesOfficial = !filters.onlyOfficial || addon.isOfficial
            val matchesType = filters.types.isEmpty() || filters.types.contains(detectType(addon.manifest))
            matchesQuery && matchesOfficial && matchesType
        }.map { addon ->
            val ext = stremioAdapter.toUnifiedExtension(addon.manifest, addon.manifestUrl)
            MarketplaceItem(
                extension = ext,
                reviews = emptyList(),
                avgRating = addon.manifest.behaviorHints?.rating ?: 0f,
                installCount = addon.installCount ?: 0,
                isInstalled = false,
                hasUpdate = false,
            )
        }
    }

    private suspend fun searchCloudStreamRepos(query: String, filters: MarketplaceFilters): List<MarketplaceItem> {
        val request = Request.Builder().url(cloudStreamMegaRepo).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()
        val body = response.body?.string() ?: return emptyList()
        val repos = json.decodeFromString<List<CloudStreamMegaRepo>>(body)

        val allPlugins = repos.flatMap { repo ->
            repo.plugins.filter { plugin ->
                val matchesQuery = query.isBlank() ||
                    plugin.name.contains(query, ignoreCase = true)
                val matchesLang = filters.languages.isEmpty() || filters.languages.contains(plugin.language ?: "en")
                matchesQuery && matchesLang
            }.map { plugin ->
                val ext = UnifiedExtension(
                    id = "cloudstream_${plugin.name.lowercase().replace(" ", "_")}",
                    name = plugin.name,
                    description = plugin.description ?: "",
                    version = plugin.versionName ?: "1.0",
                    type = ExtensionType.SOURCE,
                    originUrl = plugin.url,
                    iconUrl = plugin.iconUrl ?: "",
                    author = plugin.author ?: "unknown",
                    capabilities = setOf(ExtensionCapability.STREAM_RESOLUTION, ExtensionCapability.SEARCH),
                    supportedTypes = setOf(ContentType.MOVIE, ContentType.TV, ContentType.ANIME),
                    supportedLanguages = listOf(plugin.language ?: "en"),
                    sourceFormat = ExtensionSourceFormat.CLOUDSTREAM_REPO,
                    rawManifest = json.encodeToString(plugin),
                )
                MarketplaceItem(
                    extension = ext,
                    reviews = emptyList(),
                    avgRating = 0f,
                    installCount = 0,
                    isInstalled = false,
                    hasUpdate = false,
                )
            }
        }
        return allPlugins
    }

    private fun detectType(manifest: com.kurostream.extensions.stremio.StremioManifest): ExtensionType {
        val hasStream = manifest.resources.any { it.name == "stream" }
        val hasCatalog = manifest.resources.any { it.name == "catalog" }
        return when {
            hasStream -> ExtensionType.SOURCE
            hasCatalog && !hasStream -> ExtensionType.METADATA
            else -> ExtensionType.UTILITY
        }
    }
}

@kotlinx.serialization.Serializable
data class StremioCollectionResponse(
    val addons: List<StremioCollectionAddon> = emptyList(),
)

@kotlinx.serialization.Serializable
data class StremioCollectionAddon(
    @kotlinx.serialization.SerialName("manifest_url") val manifestUrl: String,
    val manifest: com.kurostream.extensions.stremio.StremioManifest,
    @kotlinx.serialization.SerialName("install_count") val installCount: Long? = null,
    @kotlinx.serialization.SerialName("is_official") val isOfficial: Boolean = false,
)

@kotlinx.serialization.Serializable
data class CloudStreamMegaRepo(
    val name: String,
    val url: String,
    val plugins: List<CloudStreamMegaPlugin> = emptyList(),
)

@kotlinx.serialization.Serializable
data class CloudStreamMegaPlugin(
    val name: String,
    val url: String,
    val versionName: String? = null,
    val description: String? = null,
    val author: String? = null,
    val language: String? = "en",
    @kotlinx.serialization.SerialName("iconUrl") val iconUrl: String? = null,
)
