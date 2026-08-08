// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.extensions.simkl

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
class SimklAdapter @Inject constructor(
    private val client: OkHttpClient,
) {
    private var clientId: String = ""
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun configure(clientId: String) {
        this.clientId = clientId
    }

    val isConfigured: Boolean get() = clientId.isNotBlank()

    suspend fun search(query: String): Result<List<SimklSearchResult>> = withContext(Dispatchers.IO) {
        runCatching {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val body = get("https://api.simkl.com/search?q=$encoded&type=movie,show")
            json.decodeFromString<SimklSearchResponse>(body).results
        }.onFailure { Timber.e(it, "SimklAdapter.search($query)") }
    }

    suspend fun getShowInfo(imdbId: String): Result<SimklShow> = withContext(Dispatchers.IO) {
        runCatching {
            val body = get("https://api.simkl.com/tv/$imdbId?extended=full")
            json.decodeFromString(body)
        }.onFailure { Timber.e(it, "SimklAdapter.getShowInfo($imdbId)") }
    }

    suspend fun getProgress(imdbId: String): Result<List<SimklProgressItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val body = get("https://api.simkl.com/sync/activities/history/$imdbId")
            json.decodeFromString<List<SimklProgressItem>>(body)
        }.onFailure { Timber.e(it, "SimklAdapter.getProgress($imdbId)") }
    }

    suspend fun getWatchlist(): Result<List<SimklListItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val body = get("https://api.simkl.com/users/me/lists/watchlist")
            json.decodeFromString(body)
        }.onFailure { Timber.e(it, "SimklAdapter.getWatchlist()") }
    }

    private fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Client-ID $clientId")
            .header("Content-Type", "application/json")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Simkl HTTP ${response.code} for $url")
        return response.body?.string() ?: throw Exception("Empty response")
    }
}

@Serializable
data class SimklSearchResponse(
    @SerialName("results")
    val results: List<SimklSearchResult> = emptyList(),
)

@Serializable
data class SimklSearchResult(
    val title: String = "",
    val year: Int? = null,
    val type: String = "",
    @SerialName("imdb_id")
    val imdbId: String? = null,
    val poster: String? = null,
    val fanart: String? = null,
    val ratings: SimklRatings? = null,
)

@Serializable
data class SimklRatings(
    val simkl: SimklRating? = null,
)

@Serializable
data class SimklRating(
    val rating: Float = 0f,
    val votes: Int = 0,
)

@Serializable
data class SimklShow(
    val title: String = "",
    val year: Int? = null,
    val overview: String? = null,
    val poster: String? = null,
    val ratings: SimklRatings? = null,
    val genres: List<String> = emptyList(),
    @SerialName("total_episodes")
    val episodesCount: Int = 0,
)

@Serializable
data class SimklProgressItem(
    @SerialName("watched_at")
    val watchedAt: String = "",
    val type: String = "",
    @SerialName("imdb_id")
    val imdbId: String? = null,
    val title: String = "",
)

@Serializable
data class SimklListItem(
    val title: String = "",
    val year: Int? = null,
    val type: String = "",
    @SerialName("imdb_id")
    val imdbId: String? = null,
    val poster: String? = null,
)
