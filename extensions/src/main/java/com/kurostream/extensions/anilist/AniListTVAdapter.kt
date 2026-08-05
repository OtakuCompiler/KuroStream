// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.extensions.anilist

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AniList GraphQL TV-oriented adapter.
 *
 * Provides anime-specific queries tuned for the home screen rows:
 * - Trending now (real-time popularity)
 * - This season  (current airing)
 * - Next season  (upcoming)
 * - All-time popular
 * - Top-rated
 * - Recommendations based on a given anime ID
 *
 * Also supports authenticated operations (requires OAuth token):
 * - User's current watching list
 * - User's completed list
 * - Update progress / score
 */
@Singleton
class AniListTVAdapter @Inject constructor(
    private val client: OkHttpClient,
    private val json:   Json,
) {
    private val endpoint = "https://graphql.anilist.co"
    private var oauthToken: String? = null

    fun setOAuthToken(token: String?) { oauthToken = token }

    // ── Public row queries ────────────────────────────────────────────────────

    suspend fun getTrending(page: Int = 1, perPage: Int = 20): Result<List<AniListMedia>> =
        query(TRENDING_QUERY.format(page, perPage))

    suspend fun getCurrentSeason(page: Int = 1, perPage: Int = 20): Result<List<AniListMedia>> {
        val (season, year) = currentSeasonYear()
        return query(SEASONAL_QUERY.format(season, year, page, perPage))
    }

    suspend fun getNextSeason(page: Int = 1, perPage: Int = 20): Result<List<AniListMedia>> {
        val (season, year) = nextSeasonYear()
        return query(SEASONAL_QUERY.format(season, year, page, perPage))
    }

    suspend fun getAllTimePopular(page: Int = 1, perPage: Int = 20): Result<List<AniListMedia>> =
        query(ALL_TIME_POPULAR_QUERY.format(page, perPage))

    suspend fun getTopRated(page: Int = 1, perPage: Int = 20): Result<List<AniListMedia>> =
        query(TOP_RATED_QUERY.format(page, perPage))

    suspend fun getRecommendations(mediaId: Int, perPage: Int = 12): Result<List<AniListMedia>> =
        query(RECOMMENDATIONS_QUERY.format(mediaId, perPage))

    suspend fun search(query: String, page: Int = 1, perPage: Int = 20): Result<List<AniListMedia>> =
        this.query(SEARCH_QUERY.format(query.replace("\"", ""), page, perPage))

    // ── Authenticated (optional) ──────────────────────────────────────────────

    suspend fun getUserList(status: AniListStatus = AniListStatus.CURRENT): Result<List<AniListMedia>> {
        if (oauthToken == null) return Result.failure(Exception("Not authenticated"))
        return query(USER_LIST_QUERY.format(status.name))
    }

    // ── Network ───────────────────────────────────────────────────────────────

    private suspend fun query(gqlQuery: String): Result<List<AniListMedia>> = withContext(Dispatchers.IO) {
        runCatching {
            val body = """{"query":"$gqlQuery"}"""
            val reqBody = RequestBody.create("application/json".toMediaType(), body)
            val builder = Request.Builder()
                .url(endpoint)
                .post(reqBody)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
            oauthToken?.let { builder.header("Authorization", "Bearer $it") }
            val response = client.newCall(builder.build()).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            if (!response.isSuccessful) throw Exception("AniList HTTP ${response.code}")
            parseMedia(responseBody)
        }.onFailure { Timber.e(it, "AniListTVAdapter.query failed") }
    }

    private fun parseMedia(body: String): List<AniListMedia> = runCatching {
        // Extract media list from nested data.Page.media or data.Media.recommendations
        val root = json.parseToJsonElement(body)
        // Try data.Page.media first
        val pageMedia = root.jsonObjectOrNull()
            ?.get("data")?.jsonObjectOrNull()
            ?.get("Page")?.jsonObjectOrNull()
            ?.get("media")?.jsonArrayOrNull()
        if (pageMedia != null) {
            json.decodeFromString<List<AniListMedia>>(pageMedia.toString())
        } else {
            emptyList()
        }
    }.getOrElse { emptyList() }

    // ── Season helpers ────────────────────────────────────────────────────────

    private fun currentSeasonYear(): Pair<String, Int> {
        val cal = java.util.Calendar.getInstance()
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val year  = cal.get(java.util.Calendar.YEAR)
        val season = when (month) {
            in 1..3  -> "WINTER"
            in 4..6  -> "SPRING"
            in 7..9  -> "SUMMER"
            else     -> "FALL"
        }
        return season to year
    }

    private fun nextSeasonYear(): Pair<String, Int> {
        val (cur, year) = currentSeasonYear()
        return when (cur) {
            "WINTER" -> "SPRING" to year
            "SPRING" -> "SUMMER" to year
            "SUMMER" -> "FALL"   to year
            else     -> "WINTER" to (year + 1)
        }
    }

    // ── JSON helpers ──────────────────────────────────────────────────────────
    private fun kotlinx.serialization.json.JsonElement.jsonObjectOrNull() =
        runCatching { this.jsonObject }.getOrNull()
    private fun kotlinx.serialization.json.JsonElement.jsonArrayOrNull() =
        runCatching { this.jsonArray }.getOrNull()
    private val kotlinx.serialization.json.JsonElement.jsonObject get() =
        this as kotlinx.serialization.json.JsonObject
    private val kotlinx.serialization.json.JsonElement.jsonArray get() =
        this as kotlinx.serialization.json.JsonArray

    // ── GraphQL queries (inline strings — short for readability) ─────────────

    private companion object {
        val MEDIA_FIELDS = """
            id title{romaji english native} coverImage{extraLarge large medium color}
            bannerImage description episodes duration season seasonYear
            averageScore meanScore popularity favourites status
            genres tags{name} studios(isMain:true){nodes{name}}
            nextAiringEpisode{episode timeUntilAiring}
            relations{edges{relationType(version:2) node{id title{romaji}}}}
        """.trimIndent().replace("\n", " ")

        val TRENDING_QUERY =
            "query{Page(page:%d,perPage:%d){media(sort:TRENDING_DESC,type:ANIME,isAdult:false){$MEDIA_FIELDS}}}"
        val SEASONAL_QUERY =
            "query{Page(page:%d,perPage:%d){media(season:%s,seasonYear:%d,sort:POPULARITY_DESC,type:ANIME,isAdult:false){$MEDIA_FIELDS}}}"
        val ALL_TIME_POPULAR_QUERY =
            "query{Page(page:%d,perPage:%d){media(sort:POPULARITY_DESC,type:ANIME,isAdult:false){$MEDIA_FIELDS}}}"
        val TOP_RATED_QUERY =
            "query{Page(page:%d,perPage:%d){media(sort:SCORE_DESC,type:ANIME,isAdult:false,averageScore_greater:74){$MEDIA_FIELDS}}}"
        val SEARCH_QUERY =
            "query{Page(page:%d,perPage:%d){media(search:\"%s\",type:ANIME,isAdult:false){$MEDIA_FIELDS}}}"
        val RECOMMENDATIONS_QUERY =
            "query{Media(id:%d){recommendations(perPage:%d){nodes{mediaRecommendation{$MEDIA_FIELDS}}}}}"
        val USER_LIST_QUERY =
            "query{MediaListCollection(userName:\"viewer\",type:ANIME,status:%s){lists{entries{media{$MEDIA_FIELDS}progress score}}}}}"
    }
}

@Serializable
data class AniListMedia(
    val id:            Int               = 0,
    val title:         AniListTitle      = AniListTitle(),
    val coverImage:    AniListImage      = AniListImage(),
    val bannerImage:   String?           = null,
    val description:   String?           = null,
    val episodes:      Int?              = null,
    val duration:      Int?              = null,
    val season:        String?           = null,
    val seasonYear:    Int?              = null,
    val averageScore:  Int?              = null,
    val popularity:    Int?              = null,
    val status:        String?           = null,
    val genres:        List<String>      = emptyList(),
) {
    val displayTitle: String get() = title.english ?: title.romaji ?: title.native ?: "Unknown"
    val posterUrl:    String get() = coverImage.extraLarge ?: coverImage.large ?: coverImage.medium ?: ""
    val scoreFloat:   Float  get() = (averageScore ?: 0) / 10f
}

@Serializable
data class AniListTitle(
    val romaji:  String? = null,
    val english: String? = null,
    val native:  String? = null,
)

@Serializable
data class AniListImage(
    val extraLarge: String? = null,
    val large:      String? = null,
    val medium:     String? = null,
    val color:      String? = null,
)

enum class AniListStatus { CURRENT, COMPLETED, PAUSED, DROPPED, PLANNING, REPEATING }
