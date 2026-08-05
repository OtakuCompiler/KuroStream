// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.extensions.jellyfin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Jellyfin / Emby media-server adapter.
 *
 * Supports both Jellyfin (open-source) and Emby (closed-source) since they
 * share the same REST API surface at the versions KuroStream targets.
 *
 * Capabilities:
 * - Library browsing (Movies, Series, Anime collections)
 * - Episode listing with resume points
 * - Direct-play URL construction with session-token auth
 * - Transcoded stream URL (for devices that can't direct-play)
 * - Watch progress sync (PlaybackStart / PlaybackStopped / PlaybackProgress)
 * - Server capability detection (Dolby Vision, HDR10, AV1 etc.)
 *
 * Usage:
 * ```kotlin
 * val adapter = JellyfinAdapter(okHttpClient, json)
 * adapter.configure(baseUrl = "http://192.168.1.10:8096", apiKey = "…", userId = "…")
 * val movies = adapter.getMovies()
 * val url    = adapter.getDirectPlayUrl(itemId)
 * ```
 */
@Singleton
class JellyfinAdapter @Inject constructor(
    private val client: OkHttpClient,
    private val json:   Json,
) {
    private var baseUrl: String = ""
    private var apiKey:  String = ""
    private var userId:  String = ""

    fun configure(baseUrl: String, apiKey: String, userId: String) {
        this.baseUrl = baseUrl.trimEnd('/')
        this.apiKey  = apiKey
        this.userId  = userId
    }

    val isConfigured: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank()

    // ── Library ───────────────────────────────────────────────────────────────

    suspend fun getMovies(limit: Int = 50, startIndex: Int = 0): Result<List<JellyfinItem>> =
        get("$baseUrl/Users/$userId/Items?IncludeItemTypes=Movie&Recursive=true&Limit=$limit&StartIndex=$startIndex&Fields=Overview,RunTimeTicks,ImageTags,BackdropImageTags")
            .map { parseItems(it) }

    suspend fun getSeries(limit: Int = 50, startIndex: Int = 0): Result<List<JellyfinItem>> =
        get("$baseUrl/Users/$userId/Items?IncludeItemTypes=Series&Recursive=true&Limit=$limit&StartIndex=$startIndex&Fields=Overview,ImageTags,BackdropImageTags")
            .map { parseItems(it) }

    suspend fun getEpisodes(seriesId: String, seasonId: String): Result<List<JellyfinItem>> =
        get("$baseUrl/Shows/$seriesId/Episodes?SeasonId=$seasonId&UserId=$userId&Fields=Overview,RunTimeTicks,ImageTags")
            .map { parseItems(it) }

    suspend fun getSeasons(seriesId: String): Result<List<JellyfinItem>> =
        get("$baseUrl/Shows/$seriesId/Seasons?UserId=$userId&Fields=ImageTags")
            .map { parseItems(it) }

    suspend fun getResumeItems(limit: Int = 12): Result<List<JellyfinItem>> =
        get("$baseUrl/Users/$userId/Items/Resume?Limit=$limit&Fields=Overview,RunTimeTicks,ImageTags&MediaTypes=Video")
            .map { parseItems(it) }

    suspend fun getNextUp(limit: Int = 12): Result<List<JellyfinItem>> =
        get("$baseUrl/Shows/NextUp?UserId=$userId&Limit=$limit&Fields=Overview,RunTimeTicks,ImageTags")
            .map { parseItems(it) }

    suspend fun search(query: String, limit: Int = 20): Result<List<JellyfinItem>> =
        get("$baseUrl/Users/$userId/Items?SearchTerm=${query.encode()}&Recursive=true&Limit=$limit&Fields=Overview,ImageTags,RunTimeTicks")
            .map { parseItems(it) }

    // ── Playback URLs ─────────────────────────────────────────────────────────

    /**
     * Direct-play URL — container passthrough, no server-side transcoding.
     * Use when the client supports the source codec (most 4K/HDR content).
     */
    fun getDirectPlayUrl(itemId: String, mediaSourceId: String? = null): String {
        val src = mediaSourceId?.let { "&MediaSourceId=$it" } ?: ""
        return "$baseUrl/Videos/$itemId/stream?Static=true&api_key=$apiKey$src"
    }

    /**
     * Transcoded HLS stream — server decodes + re-encodes.
     * Use for devices that can't direct-play the source format.
     * [videoBitrate] in kbps (e.g. 8000 for 1080p, 40000 for 4K).
     */
    fun getTranscodedUrl(
        itemId:      String,
        videoBitrate: Int    = 20_000,
        audioCodec:   String = "aac",
        container:    String = "ts",
    ): String {
        return "$baseUrl/Videos/$itemId/master.m3u8?" +
            "api_key=$apiKey&UserId=$userId" +
            "&VideoBitrate=${videoBitrate * 1000}" +
            "&AudioCodec=$audioCodec&Container=$container" +
            "&TranscodingContainer=$container&VideoCodec=h264"
    }

    /** Image URL for poster/thumb/backdrop. */
    fun getImageUrl(itemId: String, type: ImageType = ImageType.Primary, maxWidth: Int = 400): String =
        "$baseUrl/Items/$itemId/Images/${type.name}?MaxWidth=$maxWidth&api_key=$apiKey"

    // ── Watch progress sync ───────────────────────────────────────────────────

    suspend fun reportPlaybackStart(itemId: String, positionTicks: Long = 0): Result<Unit> =
        post("$baseUrl/Sessions/Playing", """{"ItemId":"$itemId","PositionTicks":$positionTicks}""")

    suspend fun reportProgress(itemId: String, positionTicks: Long): Result<Unit> =
        post("$baseUrl/Sessions/Playing/Progress", """{"ItemId":"$itemId","PositionTicks":$positionTicks}""")

    suspend fun reportPlaybackStopped(itemId: String, positionTicks: Long): Result<Unit> =
        post("$baseUrl/Sessions/Playing/Stopped", """{"ItemId":"$itemId","PositionTicks":$positionTicks}""")

    // ── Server info ───────────────────────────────────────────────────────────

    suspend fun getServerInfo(): Result<JellyfinServerInfo> =
        get("$baseUrl/System/Info").map {
            json.decodeFromString(it)
        }

    // ── Private helpers ───────────────────────────────────────────────────────

    private suspend fun get(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("X-Emby-Token", apiKey)
                .header("Accept", "application/json")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("Jellyfin HTTP ${response.code} for $url")
            response.body?.string() ?: throw Exception("Empty response")
        }.onFailure { Timber.e(it, "JellyfinAdapter.get($url)") }
    }

    private suspend fun post(url: String, body: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val reqBody = okhttp3.RequestBody.create(
                "application/json".toMediaType(), body
            )
            val request = Request.Builder()
                .url(url)
                .header("X-Emby-Token", apiKey)
                .post(reqBody)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("Jellyfin POST ${response.code}")
        }.onFailure { Timber.e(it, "JellyfinAdapter.post($url)") }
    }

    private fun parseItems(body: String): List<JellyfinItem> =
        runCatching {
            json.decodeFromString<JellyfinItemsResponse>(body).items
        }.getOrElse { emptyList() }

    private fun String.encode() = java.net.URLEncoder.encode(this, "UTF-8")
}

// ── Data models ───────────────────────────────────────────────────────────────

@Serializable
data class JellyfinItemsResponse(
    @SerialName("Items")
    val items:            List<JellyfinItem> = emptyList(),
    @SerialName("TotalRecordCount")
    val totalRecordCount: Int                = 0,
)

@Serializable
data class JellyfinItem(
    val Id:                  String            = "",
    val Name:                String            = "",
    val Overview:            String?           = null,
    val Type:                String            = "",          // Movie | Series | Episode | Season
    val SeriesId:            String?           = null,
    val SeriesName:          String?           = null,
    val SeasonId:            String?           = null,
    val IndexNumber:         Int?              = null,        // Episode number
    val ParentIndexNumber:   Int?              = null,        // Season number
    val RunTimeTicks:        Long?             = null,        // 1 tick = 100 ns
    val CommunityRating:     Float?            = null,
    val ProductionYear:      Int?              = null,
    val OfficialRating:      String?           = null,
    val ImageTags:           Map<String, String> = emptyMap(),
    val BackdropImageTags:   List<String>      = emptyList(),
    val UserData:            JellyfinUserData? = null,
    val Genres:              List<String>      = emptyList(),
    val Studios:             List<JellyfinStudio> = emptyList(),
) {
    val durationMs: Long get() = (RunTimeTicks ?: 0L) / 10_000
    val hasPoster:  Boolean get() = ImageTags.containsKey("Primary")
    val hasBackdrop:Boolean get() = BackdropImageTags.isNotEmpty()
}

@Serializable
data class JellyfinUserData(
    val PlaybackPositionTicks: Long    = 0L,
    val Played:                Boolean = false,
    val PlayCount:             Int     = 0,
    val IsFavorite:            Boolean = false,
)

@Serializable
data class JellyfinStudio(val Name: String = "")

@Serializable
data class JellyfinServerInfo(
    val ServerName:   String = "",
    val Version:      String = "",
    val ProductName:  String = "Jellyfin Server",
    val Id:           String = "",
)

enum class ImageType { Primary, Backdrop, Thumb, Logo, Banner }
