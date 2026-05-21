package com.kurostream.tv.data.adapter.stremio

import com.kurostream.tv.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stremio Addon Adapter.
 *
 * Implements the Stremio addon protocol (manifest + stream + meta + catalog
 * endpoints) over plain OkHttp so no Stremio SDK dependency is required.
 *
 * Default addons (Torrentio + Cinemeta) are installed on [initialize]. Any
 * number of community addons can be added at runtime via [installAddon].
 */
@Singleton
class StremioAdapter @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    companion object {
        private const val TAG = "StremioAdapter"

        private val DEFAULT_ADDONS = listOf(
            "https://torrentio.strem.fun/manifest.json",
            "https://v3-cinemeta.strem.io/manifest.json"
        )
    }

    private val installedAddons = mutableListOf<StremioAddon>()

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    /** Install default addons silently — failures are logged but not thrown. */
    suspend fun initialize() = withContext(ioDispatcher) {
        DEFAULT_ADDONS.forEach { url ->
            installAddon(url).onFailure { e ->
                Timber.tag(TAG).e(e, "Failed to install default addon: $url")
            }
        }
    }

    // ─── Addon management ─────────────────────────────────────────────────────

    /**
     * Install a Stremio addon by fetching and parsing its manifest.json.
     *
     * An existing addon with the same `id` is replaced.
     */
    suspend fun installAddon(manifestUrl: String): Result<StremioAddon> =
        withContext(ioDispatcher) {
            runCatching {
                val body = fetchString(manifestUrl)
                    ?: error("Empty manifest response from $manifestUrl")
                val manifest = JSONObject(body)
                val addon = StremioAddon(
                    id          = manifest.getString("id"),
                    name        = manifest.getString("name"),
                    version     = manifest.getString("version"),
                    description = manifest.optString("description").ifEmpty { null },
                    manifestUrl = manifestUrl,
                    baseUrl     = manifestUrl.substringBeforeLast("/"),
                    resources   = parseResources(manifest.optJSONArray("resources")),
                    types       = parseStringArray(manifest.optJSONArray("types")),
                    catalogs    = parseCatalogs(manifest.optJSONArray("catalogs")),
                    idPrefixes  = parseStringArray(manifest.optJSONArray("idPrefixes")),
                    behaviorHints = parseBehaviorHints(manifest.optJSONObject("behaviorHints"))
                )
                installedAddons.removeAll { it.id == addon.id }
                installedAddons.add(addon)
                Timber.tag(TAG).i("Installed addon: ${addon.name} (${addon.id})")
                addon
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "Failed to install addon: $manifestUrl")
            }
        }

    fun removeAddon(addonId: String) {
        installedAddons.removeAll { it.id == addonId }
    }

    fun getInstalledAddons(): List<StremioAddon> = installedAddons.toList()

    fun getAddonsForResource(resource: String): List<StremioAddon> =
        installedAddons.filter { addon -> addon.resources.any { it.name == resource } }

    fun getAnimeAddons(): List<StremioAddon> =
        installedAddons.filter { addon ->
            addon.types.contains("series") || addon.types.contains("anime")
        }

    // ─── Stream / meta / catalog ──────────────────────────────────────────────

    /**
     * Fetch streams from all installed addons that declare the "stream"
     * resource for [contentType].
     */
    suspend fun getStreams(
        contentType: String,
        contentId: String
    ): List<StremioStream> = withContext(ioDispatcher) {
        installedAddons
            .filter { addon -> addon.resources.any { it.name == "stream" && it.types.contains(contentType) } }
            .flatMap { addon ->
                runCatching { fetchStreamsFromAddon(addon, contentType, contentId) }
                    .onFailure { e -> Timber.tag(TAG).e(e, "Streams failed for ${addon.name}") }
                    .getOrDefault(emptyList())
            }
    }

    /**
     * Fetch metadata from the first addon that returns a non-null result.
     */
    suspend fun getMetadata(
        contentType: String,
        contentId: String
    ): StremioMeta? = withContext(ioDispatcher) {
        installedAddons
            .filter { addon -> addon.resources.any { it.name == "meta" && it.types.contains(contentType) } }
            .firstNotNullOfOrNull { addon ->
                runCatching { fetchMetaFromAddon(addon, contentType, contentId) }
                    .onFailure { e -> Timber.tag(TAG).e(e, "Meta failed for ${addon.name}") }
                    .getOrNull()
            }
    }

    /**
     * Search a specific addon's catalog by query string.
     */
    suspend fun searchCatalog(
        addonId: String,
        catalogId: String,
        contentType: String,
        query: String
    ): List<StremioMeta> = withContext(ioDispatcher) {
        val addon = installedAddons.find { it.id == addonId } ?: return@withContext emptyList()
        val encodedQuery = query.replace(" ", "%20")
        val url = "${addon.baseUrl}/catalog/$contentType/$catalogId/search=$encodedQuery.json"
        runCatching {
            val body = fetchString(url) ?: return@runCatching emptyList<StremioMeta>()
            val json = JSONObject(body)
            val metasArray = json.optJSONArray("metas") ?: return@runCatching emptyList<StremioMeta>()
            (0 until metasArray.length()).map { parseMeta(metasArray.getJSONObject(it)) }
        }.onFailure { e ->
            Timber.tag(TAG).e(e, "Catalog search failed for addon $addonId")
        }.getOrDefault(emptyList())
    }

    // ─── Private network helpers ──────────────────────────────────────────────

    private fun fetchString(url: String): String? {
        val response = okHttpClient.newCall(Request.Builder().url(url).build()).execute()
        return if (response.isSuccessful) response.body?.string() else null
    }

    private fun fetchStreamsFromAddon(
        addon: StremioAddon,
        contentType: String,
        contentId: String
    ): List<StremioStream> {
        val url = "${addon.baseUrl}/stream/$contentType/$contentId.json"
        val body = fetchString(url) ?: return emptyList()
        val streamsArray = JSONObject(body).optJSONArray("streams") ?: return emptyList()
        return (0 until streamsArray.length()).map { parseStream(streamsArray.getJSONObject(it), addon) }
    }

    private fun fetchMetaFromAddon(
        addon: StremioAddon,
        contentType: String,
        contentId: String
    ): StremioMeta? {
        val url = "${addon.baseUrl}/meta/$contentType/$contentId.json"
        val body = fetchString(url) ?: return null
        return JSONObject(body).optJSONObject("meta")?.let { parseMeta(it) }
    }

    // ─── JSON parsing ─────────────────────────────────────────────────────────

    private fun parseResources(array: JSONArray?): List<StremioResource> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            when (val item = array.get(i)) {
                is String -> StremioResource(name = item, types = emptyList(), idPrefixes = emptyList())
                is JSONObject -> StremioResource(
                    name       = item.getString("name"),
                    types      = parseStringArray(item.optJSONArray("types")),
                    idPrefixes = parseStringArray(item.optJSONArray("idPrefixes"))
                )
                else -> null
            }
        }
    }

    private fun parseCatalogs(array: JSONArray?): List<StremioCatalog> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            StremioCatalog(
                id    = obj.getString("id"),
                type  = obj.getString("type"),
                name  = obj.optString("name").ifEmpty { null },
                extra = parseExtra(obj.optJSONArray("extra"))
            )
        }
    }

    private fun parseExtra(array: JSONArray?): List<StremioCatalogExtra> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            StremioCatalogExtra(
                name       = obj.getString("name"),
                isRequired = obj.optBoolean("isRequired", false),
                options    = parseStringArray(obj.optJSONArray("options"))
            )
        }
    }

    private fun parseBehaviorHints(obj: JSONObject?): StremioBehaviorHints =
        StremioBehaviorHints(
            adult                 = obj?.optBoolean("adult", false) ?: false,
            p2p                   = obj?.optBoolean("p2p", false) ?: false,
            configurable          = obj?.optBoolean("configurable", false) ?: false,
            configurationRequired = obj?.optBoolean("configurationRequired", false) ?: false
        )

    private fun parseStream(json: JSONObject, addon: StremioAddon): StremioStream =
        StremioStream(
            addonId      = addon.id,
            addonName    = addon.name,
            name         = json.optString("name").ifEmpty { null },
            title        = json.optString("title").ifEmpty { null },
            url          = json.optString("url").ifEmpty { null },
            infoHash     = json.optString("infoHash").ifEmpty { null },
            fileIdx      = json.optInt("fileIdx", -1).takeIf { it >= 0 },
            externalUrl  = json.optString("externalUrl").ifEmpty { null },
            behaviorHints = parseStreamBehaviorHints(json.optJSONObject("behaviorHints"))
        )

    private fun parseStreamBehaviorHints(obj: JSONObject?): StremioStreamBehaviorHints =
        StremioStreamBehaviorHints(
            notWebReady  = obj?.optBoolean("notWebReady", false) ?: false,
            bingeGroup   = obj?.optString("bingeGroup"),
            proxyHeaders = parseProxyHeaders(obj?.optJSONObject("proxyHeaders")),
            filename     = obj?.optString("filename")
        )

    private fun parseProxyHeaders(obj: JSONObject?): Map<String, String> {
        if (obj == null) return emptyMap()
        val requestObj = obj.optJSONObject("request") ?: return emptyMap()
        return requestObj.keys().asSequence()
            .associateWith { key -> requestObj.getString(key) }
    }

    private fun parseMeta(json: JSONObject): StremioMeta =
        StremioMeta(
            id          = json.getString("id"),
            type        = json.getString("type"),
            name        = json.getString("name"),
            poster      = json.optString("poster").ifEmpty { null },
            background  = json.optString("background").ifEmpty { null },
            logo        = json.optString("logo").ifEmpty { null },
            description = json.optString("description").ifEmpty { null },
            releaseInfo = json.optString("releaseInfo").ifEmpty { null },
            imdbRating  = json.optString("imdbRating").ifEmpty { null },
            runtime     = json.optString("runtime").ifEmpty { null },
            genres      = parseStringArray(json.optJSONArray("genres")),
            videos      = parseVideos(json.optJSONArray("videos"))
        )

    private fun parseVideos(array: JSONArray?): List<StremioVideo> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            StremioVideo(
                id        = obj.getString("id"),
                title     = obj.optString("title").ifEmpty { null },
                season    = obj.optInt("season", 0).takeIf { it > 0 },
                episode   = obj.optInt("episode", 0).takeIf { it > 0 },
                released  = obj.optString("released").ifEmpty { null },
                thumbnail = obj.optString("thumbnail").ifEmpty { null },
                overview  = obj.optString("overview").ifEmpty { null }
            )
        }
    }

    private fun parseStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { array.getString(it) }
    }
}

// ─── Data models ──────────────────────────────────────────────────────────────

data class StremioAddon(
    val id: String,
    val name: String,
    val version: String,
    val description: String?,
    val manifestUrl: String,
    val baseUrl: String,
    val resources: List<StremioResource>,
    val types: List<String>,
    val catalogs: List<StremioCatalog>,
    val idPrefixes: List<String>,
    val behaviorHints: StremioBehaviorHints
)

data class StremioResource(
    val name: String,
    val types: List<String>,
    val idPrefixes: List<String>
)

data class StremioCatalog(
    val id: String,
    val type: String,
    val name: String?,
    val extra: List<StremioCatalogExtra>
)

data class StremioCatalogExtra(
    val name: String,
    val isRequired: Boolean,
    val options: List<String>
)

data class StremioBehaviorHints(
    val adult: Boolean,
    val p2p: Boolean,
    val configurable: Boolean,
    val configurationRequired: Boolean
)

data class StremioStream(
    val addonId: String,
    val addonName: String,
    val name: String?,
    val title: String?,
    val url: String?,
    val infoHash: String?,
    val fileIdx: Int?,
    val externalUrl: String?,
    val behaviorHints: StremioStreamBehaviorHints
) {
    val isTorrent: Boolean get() = infoHash != null
    val isDirectStream: Boolean get() = url != null
    val displayName: String get() = name ?: title ?: "Unknown Stream"
}

data class StremioStreamBehaviorHints(
    val notWebReady: Boolean,
    val bingeGroup: String?,
    val proxyHeaders: Map<String, String>,
    val filename: String?
)

data class StremioMeta(
    val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val logo: String?,
    val description: String?,
    val releaseInfo: String?,
    val imdbRating: String?,
    val runtime: String?,
    val genres: List<String>,
    val videos: List<StremioVideo>
)

data class StremioVideo(
    val id: String,
    val title: String?,
    val season: Int?,
    val episode: Int?,
    val released: String?,
    val thumbnail: String?,
    val overview: String?
)
