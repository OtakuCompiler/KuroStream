// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.extensions.plex

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plex Media Server adapter.
 *
 * Communicates with a local or remote Plex server via the Plex HTTP API.
 * Authentication uses an X-Plex-Token (obtained from plex.tv or locally).
 *
 * Features:
 * - Library section discovery and browsing (Movies, TV Shows, Anime)
 * - Metadata fetching (poster, backdrop, description, rating)
 * - Continue Watching / On Deck items
 * - Direct-play stream URL with token auth
 * - Transcoded stream (Universal Transcoder)
 * - Watch-state syncing (scrobble / unscrobble)
 * - Session reporting for Tautulli compatibility
 */
@Singleton
class PlexAdapter @Inject constructor(
    private val client: OkHttpClient,
    private val json:   Json,
) {
    private var baseUrl:    String = ""
    private var token:      String = ""
    private var clientId:   String = "kurostream-android"
    private var clientName: String = "KuroStream"

    fun configure(baseUrl: String, token: String, clientId: String = "kurostream-android") {
        this.baseUrl   = baseUrl.trimEnd('/')
        this.token     = token
        this.clientId  = clientId
    }

    val isConfigured: Boolean get() = baseUrl.isNotBlank() && token.isNotBlank()

    // ── Libraries ─────────────────────────────────────────────────────────────

    suspend fun getLibrarySections(): Result<List<PlexLibrarySection>> = withContext(Dispatchers.IO) {
        get("$baseUrl/library/sections").map { body ->
            runCatching {
                // Plex returns XML by default; we request JSON via Accept header
                json.decodeFromString<PlexSectionsResponse>(body).MediaContainer.Directory
            }.getOrElse { emptyList() }
        }
    }

    suspend fun getLibraryItems(sectionKey: String, limit: Int = 50, offset: Int = 0): Result<List<PlexItem>> =
        get("$baseUrl/library/sections/$sectionKey/all?X-Plex-Container-Size=$limit&X-Plex-Container-Start=$offset")
            .map { parsePlexItems(it) }

    suspend fun getOnDeck(limit: Int = 12): Result<List<PlexItem>> =
        get("$baseUrl/library/onDeck?X-Plex-Container-Size=$limit").map { parsePlexItems(it) }

    suspend fun getRecentlyAdded(limit: Int = 20): Result<List<PlexItem>> =
        get("$baseUrl/library/recentlyAdded?X-Plex-Container-Size=$limit").map { parsePlexItems(it) }

    suspend fun getShowSeasons(showRatingKey: String): Result<List<PlexItem>> =
        get("$baseUrl/library/metadata/$showRatingKey/children").map { parsePlexItems(it) }

    suspend fun getSeasonEpisodes(seasonRatingKey: String): Result<List<PlexItem>> =
        get("$baseUrl/library/metadata/$seasonRatingKey/children").map { parsePlexItems(it) }

    suspend fun search(query: String): Result<List<PlexItem>> =
        get("$baseUrl/library/search?query=${query.encode()}&limit=20").map { parsePlexItems(it) }

    // ── Playback ──────────────────────────────────────────────────────────────

    /** Direct-play URL — no server-side processing. Best for local network. */
    fun getDirectPlayUrl(ratingKey: String, partKey: String): String =
        "$baseUrl$partKey?X-Plex-Token=$token"

    /**
     * Universal transcoder URL (HLS).
     * [videoBitrate] in kbps. [videoResolution] e.g. "1920x1080".
     */
    fun getTranscodeUrl(
        ratingKey:       String,
        sessionId:       String,
        videoBitrate:    Int    = 20_000,
        videoResolution: String = "1920x1080",
        audioBoost:      Int    = 100,
    ): String {
        return "$baseUrl/video/:/transcode/universal/start.m3u8" +
            "?X-Plex-Token=$token" +
            "&X-Plex-Client-Identifier=$clientId" +
            "&X-Plex-Platform=Android" +
            "&X-Plex-Device=KuroStream" +
            "&session=$sessionId" +
            "&mediaIndex=0&partIndex=0" +
            "&protocol=hls" +
            "&videoBitrate=$videoBitrate" +
            "&videoResolution=$videoResolution" +
            "&audioBoost=$audioBoost" +
            "&directPlay=0&directStream=1" +
            "&subtitles=burn" +
            "&location=lan"
    }

    /** Poster/art image URL with width constraint. */
    fun getThumbUrl(thumbPath: String, width: Int = 400): String =
        "$baseUrl/photo/:/transcode?url=${thumbPath.encode()}&width=$width&height=${(width * 1.5).toInt()}&X-Plex-Token=$token"

    fun getArtUrl(artPath: String, width: Int = 1920): String =
        "$baseUrl/photo/:/transcode?url=${artPath.encode()}&width=$width&X-Plex-Token=$token"

    // ── Watch state ───────────────────────────────────────────────────────────

    suspend fun markWatched(ratingKey: String): Result<Unit> =
        get("$baseUrl/:/scrobble?key=$ratingKey&identifier=com.plexapp.plugins.library").map {}

    suspend fun markUnwatched(ratingKey: String): Result<Unit> =
        get("$baseUrl/:/unscrobble?key=$ratingKey&identifier=com.plexapp.plugins.library").map {}

    suspend fun reportProgress(
        ratingKey: String,
        timeMs:    Long,
        duration:  Long,
        state:     String = "playing",  // playing | paused | stopped
    ): Result<Unit> =
        get("$baseUrl/:/progress?key=$ratingKey&identifier=com.plexapp.plugins.library" +
            "&time=$timeMs&duration=$duration&state=$state").map {}

    // ── Server ────────────────────────────────────────────────────────────────

    suspend fun getServerInfo(): Result<PlexServerInfo> =
        get("$baseUrl/").map { body ->
            runCatching { json.decodeFromString<PlexRootResponse>(body).MediaContainer.let {
                PlexServerInfo(it.friendlyName ?: "", it.version ?: "", it.machineIdentifier ?: "")
            } }.getOrDefault(PlexServerInfo())
        }

    // ── Private ───────────────────────────────────────────────────────────────

    private suspend fun get(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val reqUrl = if (url.contains("X-Plex-Token")) url
                         else "$url${if ('?' in url) '&' else '?'}X-Plex-Token=$token"
            val request = Request.Builder()
                .url(reqUrl)
                .header("Accept", "application/json")
                .header("X-Plex-Client-Identifier", clientId)
                .header("X-Plex-Product", clientName)
                .header("X-Plex-Platform", "Android")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("Plex HTTP ${response.code}")
            response.body?.string() ?: ""
        }.onFailure { Timber.e(it, "PlexAdapter.get($url)") }
    }

    private fun parsePlexItems(body: String): List<PlexItem> =
        runCatching {
            json.decodeFromString<PlexItemsResponse>(body).MediaContainer.items
        }.getOrElse { emptyList() }

    private fun String.encode() = java.net.URLEncoder.encode(this, "UTF-8")
}

// ── Models ────────────────────────────────────────────────────────────────────

@Serializable
data class PlexSectionsResponse(val MediaContainer: PlexSectionsContainer = PlexSectionsContainer())

@Serializable
data class PlexSectionsContainer(val Directory: List<PlexLibrarySection> = emptyList())

@Serializable
data class PlexLibrarySection(
    val key:   String = "",
    val title: String = "",
    val type:  String = "",   // movie | show
    val art:   String? = null,
    val thumb: String? = null,
)

@Serializable
data class PlexItemsResponse(val MediaContainer: PlexItemsContainer = PlexItemsContainer())

@Serializable
data class PlexItemsContainer(
    val Metadata:    List<PlexItem> = emptyList(),
    val totalSize:   Int            = 0,
    val offset:      Int            = 0,
) {
    val items: List<PlexItem> get() = Metadata
}

@Serializable
data class PlexItem(
    val ratingKey:         String  = "",
    val title:             String  = "",
    val summary:           String? = null,
    val type:              String  = "",  // movie | show | season | episode
    val grandparentTitle:  String? = null,   // Show name (when type=episode)
    val parentTitle:       String? = null,   // Season name
    val index:             Int?    = null,   // Episode number
    val parentIndex:       Int?    = null,   // Season number
    val duration:          Long?   = null,   // ms
    val viewOffset:        Long?   = null,   // resume position ms
    val rating:            Float?  = null,
    val year:              Int?    = null,
    val thumb:             String? = null,
    val art:               String? = null,
    val banner:            String? = null,
    val viewCount:         Int?    = null,
    val Media:             List<PlexMedia> = emptyList(),
) {
    val isWatched:  Boolean get() = (viewCount ?: 0) > 0
    val hasResume:  Boolean get() = (viewOffset ?: 0L) > 0L
    val firstPart:  String? get() = Media.firstOrNull()?.Part?.firstOrNull()?.key
}

@Serializable
data class PlexMedia(
    val Part:           List<PlexPart> = emptyList(),
    val videoResolution: String?       = null,
    val bitrate:         Int?          = null,
    val videoCodec:      String?       = null,
    val audioCodec:      String?       = null,
)

@Serializable
data class PlexPart(val key: String = "", val duration: Long? = null, val size: Long? = null)

@Serializable
data class PlexRootResponse(val MediaContainer: PlexRootContainer = PlexRootContainer())

@Serializable
data class PlexRootContainer(
    val friendlyName:      String? = null,
    val version:           String? = null,
    val machineIdentifier: String? = null,
)

data class PlexServerInfo(
    val name:    String = "",
    val version: String = "",
    val id:      String = "",
)
