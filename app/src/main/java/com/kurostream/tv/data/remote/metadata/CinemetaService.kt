package com.kurostream.tv.data.remote.metadata

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cinemeta metadata service.
 *
 * Wraps the Cinemeta Stremio addon REST API to retrieve anime/series metadata.
 * Uses raw OkHttp (not Retrofit) to match the app's existing network layer.
 */
@Singleton
class CinemetaService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "CinemetaService"
        private const val BASE_URL = "https://v3-cinemeta.strem.io"
    }

    /**
     * Fetch metadata for a series by its IMDB ID (e.g. "tt0388629").
     */
    suspend fun getSeriesMetadata(imdbId: String): AnimeMetadata? {
        return try {
            val url = "$BASE_URL/meta/series/$imdbId.json"
            val response = okHttpClient.newCall(Request.Builder().url(url).build()).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val meta = JSONObject(body).optJSONObject("meta") ?: return null
            parseMeta(meta)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to fetch Cinemeta metadata for $imdbId")
            null
        }
    }

    /**
     * Search for series by query string via the catalog search endpoint.
     */
    suspend fun searchSeries(query: String): List<AnimeMetadata> {
        return try {
            val encodedQuery = query.trim().replace(" ", "%20")
            val url = "$BASE_URL/catalog/series/top/search=$encodedQuery.json"
            val response = okHttpClient.newCall(Request.Builder().url(url).build()).execute()
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val metas = JSONObject(body).optJSONArray("metas") ?: return emptyList()
            buildList {
                for (i in 0 until metas.length()) {
                    parseMeta(metas.getJSONObject(i))?.let { add(it) }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Cinemeta search failed for '$query'")
            emptyList()
        }
    }

    private fun parseMeta(json: JSONObject): AnimeMetadata? {
        val id = json.optString("id").takeIf { it.isNotBlank() } ?: return null
        return AnimeMetadata(
            kitsuId = null,
            imdbId = id.takeIf { it.startsWith("tt") },
            malId = null,
            title = json.optString("name").ifBlank { return null },
            titleJapanese = null,
            titleEnglish = json.optString("name").takeIf { it.isNotBlank() },
            synopsis = json.optString("description").takeIf { it.isNotBlank() },
            posterUrl = json.optString("poster").takeIf { it.isNotBlank() },
            coverUrl = json.optString("background").takeIf { it.isNotBlank() },
            trailerUrl = null,
            averageRating = json.optString("imdbRating").toFloatOrNull(),
            popularityRank = null,
            ratingRank = null,
            status = null,
            ageRating = null,
            subtype = json.optString("type").takeIf { it.isNotBlank() },
            episodeCount = json.optJSONArray("videos")?.length()?.takeIf { it > 0 },
            episodeLength = null,
            startDate = json.optString("releaseInfo").takeIf { it.isNotBlank() },
            endDate = null,
            genres = buildList {
                val genres = json.optJSONArray("genres")
                if (genres != null) {
                    for (i in 0 until genres.length()) add(genres.getString(i))
                }
            },
            studios = emptyList(),
            isNsfw = false
        )
    }
}
