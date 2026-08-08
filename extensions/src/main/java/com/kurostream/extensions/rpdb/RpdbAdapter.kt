// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.extensions.rpdb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RpdbAdapter @Inject constructor(
    private val client: OkHttpClient,
) {
    private var apiKey: String = ""
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun configure(apiKey: String) {
        this.apiKey = apiKey
    }

    val isConfigured: Boolean get() = apiKey.isNotBlank()

    suspend fun getPoster(tmdbId: String, type: String = "movie"): Result<RpdbPosterResponse> = withContext(Dispatchers.IO) {
        runCatching<RpdbPosterResponse> {
            val body = get("https://ratingposterdb.com/api/poster/$type/$tmdbId?apikey=$apiKey")
            json.decodeFromString(body)
        }.onFailure { Timber.e(it, "RpdbAdapter.getPoster($tmdbId)") }
    }

    suspend fun getPostersByImdb(imdbId: String): Result<List<RpdbPoster>> = withContext(Dispatchers.IO) {
        runCatching<List<RpdbPoster>> {
            val body = get("https://ratingposterdb.com/api/poster/imdb/$imdbId?apikey=$apiKey")
            json.decodeFromString(body)
        }.onFailure { Timber.e(it, "RpdbAdapter.getPostersByImdb($imdbId)") }
    }

    suspend fun getRatings(imdbId: String): Result<RpdbRatings> = withContext(Dispatchers.IO) {
        runCatching<RpdbRatings> {
            val body = get("https://ratingposterdb.com/api/ratings/$imdbId?apikey=$apiKey")
            json.decodeFromString(body)
        }.onFailure { Timber.e(it, "RpdbAdapter.getRatings($imdbId)") }
    }

    private fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("RPDB HTTP ${response.code} for $url")
        return response.body?.string() ?: throw Exception("Empty response")
    }
}

@Serializable
data class RpdbPosterResponse(
    @SerialName("poster_url")
    val posterUrl: String = "",
    val rating: Float = 0f,
    @SerialName("rating_count")
    val ratingCount: Int = 0,
    val source: String = "",
)

@Serializable
data class RpdbPoster(
    @SerialName("poster_url")
    val posterUrl: String = "",
    val rating: Float = 0f,
    val category: String = "default",
    val episode: Int? = null,
)

@Serializable
data class RpdbRatings(
    val imdb: RpdbSourceRating? = null,
    @SerialName("rt_audience")
    val rtAudience: RpdbSourceRating? = null,
    @SerialName("rt_critics")
    val rtCritics: RpdbSourceRating? = null,
    val metacritic: RpdbSourceRating? = null,
    val mal: RpdbSourceRating? = null,
    val anilist: RpdbSourceRating? = null,
)

@Serializable
data class RpdbSourceRating(
    val rating: Float = 0f,
    val votes: Int = 0,
)
