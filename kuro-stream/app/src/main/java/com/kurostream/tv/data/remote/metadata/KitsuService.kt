package com.kurostream.tv.data.remote.metadata

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kitsu anime metadata service.
 *
 * Accesses the Kitsu REST API (https://kitsu.io/api/edge/) to retrieve
 * anime titles, ratings, cover images, episodes, and seasonal lists.
 *
 * Uses raw OkHttp to stay consistent with the project's network layer and
 * to avoid introducing a second Retrofit instance with a separate base URL.
 */
@Singleton
class KitsuService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "KitsuService"
        private const val BASE_URL = "https://kitsu.io/api/edge"
        private const val JSON_API_ACCEPT = "application/vnd.api+json"
    }

    private fun buildRequest(url: String): Request =
        Request.Builder()
            .url(url)
            .header("Accept", JSON_API_ACCEPT)
            .build()

    // ── Search ────────────────────────────────────────────────────────────────

    suspend fun searchAnime(query: String, limit: Int = 20): List<AnimeMetadata> {
        return try {
            val encodedQuery = query.trim().replace(" ", "%20")
            val url = "$BASE_URL/anime?filter[text]=$encodedQuery&page[limit]=$limit"
            parseAnimeList(fetchJson(url))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Kitsu search failed for '$query'")
            emptyList()
        }
    }

    // ── Discovery ─────────────────────────────────────────────────────────────

    suspend fun getTrendingAnime(limit: Int = 20): List<AnimeMetadata> {
        return try {
            parseAnimeList(fetchJson("$BASE_URL/trending/anime?limit=$limit"))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Kitsu trending fetch failed")
            emptyList()
        }
    }

    suspend fun getTopRated(limit: Int = 20): List<AnimeMetadata> {
        return try {
            parseAnimeList(fetchJson("$BASE_URL/anime?page[limit]=$limit&sort=-averageRating"))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Kitsu top-rated fetch failed")
            emptyList()
        }
    }

    suspend fun getSeasonalAnime(year: Int, season: String, limit: Int = 20): List<AnimeMetadata> {
        return try {
            val url = "$BASE_URL/anime?filter[seasonYear]=$year&filter[season]=$season" +
                      "&page[limit]=$limit&sort=-averageRating"
            parseAnimeList(fetchJson(url))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Kitsu seasonal fetch failed ($year $season)")
            emptyList()
        }
    }

    // ── Detail ────────────────────────────────────────────────────────────────

    suspend fun getAnime(kitsuId: String): AnimeMetadata? {
        return try {
            val json = fetchJson("$BASE_URL/anime/$kitsuId")
            json?.optJSONObject("data")?.let { parseAnimeData(it) }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Kitsu getAnime failed for id=$kitsuId")
            null
        }
    }

    suspend fun getEpisodes(kitsuId: String, limit: Int = 25, offset: Int = 0): List<EpisodeMetadata> {
        return try {
            val url = "$BASE_URL/anime/$kitsuId/episodes?page[limit]=$limit&page[offset]=$offset"
            val json = fetchJson(url)
            val data = json?.optJSONArray("data") ?: return emptyList()
            buildList {
                for (i in 0 until data.length()) {
                    parseEpisode(data.getJSONObject(i))?.let { add(it) }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Kitsu getEpisodes failed for id=$kitsuId")
            emptyList()
        }
    }

    // ── Parsing ───────────────────────────────────────────────────────────────

    private suspend fun fetchJson(url: String): JSONObject? {
        val response = okHttpClient.newCall(buildRequest(url)).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        return JSONObject(body)
    }

    private fun parseAnimeList(json: JSONObject?): List<AnimeMetadata> {
        val data = json?.optJSONArray("data") ?: return emptyList()
        return buildList {
            for (i in 0 until data.length()) {
                parseAnimeData(data.getJSONObject(i))?.let { add(it) }
            }
        }
    }

    private fun parseAnimeData(obj: JSONObject): AnimeMetadata? {
        val id = obj.optString("id").takeIf { it.isNotBlank() } ?: return null
        val attrs = obj.optJSONObject("attributes") ?: return null
        val titles = attrs.optJSONObject("titles")
        val posterImage = attrs.optJSONObject("posterImage")
        val coverImage = attrs.optJSONObject("coverImage")

        val title = titles?.optString("en_jp")?.takeIf { it.isNotBlank() }
            ?: titles?.optString("en")?.takeIf { it.isNotBlank() }
            ?: attrs.optString("canonicalTitle").takeIf { it.isNotBlank() }
            ?: return null

        return AnimeMetadata(
            kitsuId = id,
            imdbId = null,
            malId = null,
            title = title,
            titleJapanese = titles?.optString("ja_jp")?.takeIf { it.isNotBlank() },
            titleEnglish = titles?.optString("en")?.takeIf { it.isNotBlank() },
            synopsis = attrs.optString("synopsis").takeIf { it.isNotBlank() },
            posterUrl = posterImage?.optString("large")?.takeIf { it.isNotBlank() }
                ?: posterImage?.optString("medium")?.takeIf { it.isNotBlank() },
            coverUrl = coverImage?.optString("large")?.takeIf { it.isNotBlank() }
                ?: coverImage?.optString("original")?.takeIf { it.isNotBlank() },
            trailerUrl = attrs.optString("youtubeVideoId").takeIf { it.isNotBlank() }
                ?.let { "https://www.youtube.com/watch?v=$it" },
            averageRating = attrs.optString("averageRating").toFloatOrNull(),
            popularityRank = attrs.optInt("popularityRank").takeIf { it > 0 },
            ratingRank = attrs.optInt("ratingRank").takeIf { it > 0 },
            status = attrs.optString("status").takeIf { it.isNotBlank() },
            ageRating = attrs.optString("ageRating").takeIf { it.isNotBlank() },
            subtype = attrs.optString("subtype").takeIf { it.isNotBlank() },
            episodeCount = attrs.optInt("episodeCount").takeIf { it > 0 },
            episodeLength = attrs.optInt("episodeLength").takeIf { it > 0 },
            startDate = attrs.optString("startDate").takeIf { it.isNotBlank() },
            endDate = attrs.optString("endDate").takeIf { it.isNotBlank() },
            genres = emptyList(),
            studios = emptyList(),
            isNsfw = attrs.optBoolean("nsfw", false)
        )
    }

    private fun parseEpisode(obj: JSONObject): EpisodeMetadata? {
        val id = obj.optString("id").takeIf { it.isNotBlank() } ?: return null
        val attrs = obj.optJSONObject("attributes") ?: return null
        val titles = attrs.optJSONObject("titles")
        val thumbnail = attrs.optJSONObject("thumbnail")

        return EpisodeMetadata(
            id = id,
            number = attrs.optInt("number", 0).takeIf { it > 0 } ?: return null,
            seasonNumber = attrs.optInt("seasonNumber").takeIf { it > 0 },
            title = titles?.optString("en_jp")?.takeIf { it.isNotBlank() }
                ?: attrs.optString("canonicalTitle").takeIf { it.isNotBlank() },
            synopsis = attrs.optString("synopsis").takeIf { it.isNotBlank() },
            thumbnailUrl = thumbnail?.optString("original")?.takeIf { it.isNotBlank() },
            airDate = attrs.optString("airdate").takeIf { it.isNotBlank() },
            durationMinutes = attrs.optInt("length").takeIf { it > 0 }
        )
    }
}
