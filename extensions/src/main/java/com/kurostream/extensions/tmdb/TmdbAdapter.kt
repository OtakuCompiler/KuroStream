// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.extensions.tmdb

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

/**
 * TMDB v3 adapter — movies and TV.
 * Free tier: 50 requests / second per IP without API key for poster/image
 * endpoints; full data endpoints require an API key (v3 auth).
 */
@Singleton
class TmdbAdapter @Inject constructor(
    private val client: OkHttpClient,
) {
    private val imageBase = "https://image.tmdb.org/t/p/"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var apiKey: String = ""

    fun configure(apiKey: String) { this.apiKey = apiKey }
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    fun posterUrl(path: String?, size: String = "w500"): String? =
        if (path.isNullOrBlank()) null else "$imageBase$size$path"

    fun backdropUrl(path: String?, size: String = "w1280"): String? =
        if (path.isNullOrBlank()) null else "$imageBase$size$path"

    suspend fun trending(mediaType: String = "all", window: String = "week"): Result<List<TmdbResult>> =
        get("/trending/$mediaType/$window")

    suspend fun popularMovies(page: Int = 1): Result<TmdbPage> =
        get("/movie/popular?page=$page", asPage = true)

    suspend fun popularTv(page: Int = 1): Result<TmdbPage> =
        get("/tv/popular?page=$page", asPage = true)

    suspend fun topRatedMovies(page: Int = 1): Result<TmdbPage> =
        get("/movie/top_rated?page=$page", asPage = true)

    suspend fun topRatedTv(page: Int = 1): Result<TmdbPage> =
        get("/tv/top_rated?page=$page", asPage = true)

    suspend fun nowPlayingMovies(page: Int = 1): Result<TmdbPage> =
        get("/movie/now_playing?page=$page", asPage = true)

    suspend fun airingTodayTv(page: Int = 1): Result<TmdbPage> =
        get("/tv/airing_today?page=$page", asPage = true)

    suspend fun search(query: String, page: Int = 1): Result<TmdbPage> =
        get("/search/multi?query=${java.net.URLEncoder.encode(query, "UTF-8")}&page=$page", asPage = true)

    suspend fun movieDetails(id: Int): Result<TmdbMovie> = get("/movie/$id", asPage = false)
    suspend fun tvDetails(id: Int): Result<TmdbTv> = get("/tv/$id", asPage = false)

    suspend fun movieGenres(): Result<List<TmdbGenre>> = withContext(Dispatchers.IO) {
        runCatching {
            val body = get("/genre/movie/list", asPage = false).getOrThrow()
            json.decodeFromString<TmdbGenreList>(body).genres
        }.onFailure { Timber.e(it, "TMDB movieGenres") }
    }

    suspend fun tvGenres(): Result<List<TmdbGenre>> = withContext(Dispatchers.IO) {
        runCatching {
            val body = get("/genre/tv/list", asPage = false).getOrThrow()
            json.decodeFromString<TmdbGenreList>(body).genres
        }.onFailure { Timber.e(it, "TMDB tvGenres") }
    }

    private suspend fun get(path: String, asPage: Boolean = false): Result<TmdbPage> = withContext(Dispatchers.IO) {
        runCatching {
            val sep = if (path.contains("?")) "&" else "?"
            val url = "https://api.themoviedb.org/3$path${sep}api_key=$apiKey"
            val req = Request.Builder().url(url).header("Accept", "application/json").build()
            client.newCall(req).execute().use { response ->
                val body = response.body?.string() ?: throw Exception("Empty body")
                if (!response.isSuccessful) throw Exception("TMDB HTTP ${response.code}: $body")
                if (asPage) json.decodeFromString<TmdbPage>(body)
                else TmdbPage(results = listOf(json.decodeFromString<TmdbResult>(body)))
            }
        }.onFailure { Timber.e(it, "TMDB GET $path") }
    }
}

@Serializable
data class TmdbPage(
    val page: Int = 1,
    val results: List<TmdbResult> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 0,
    @SerialName("total_results") val totalResults: Int = 0,
)

@Serializable
data class TmdbResult(
    val id: Int = 0,
    @SerialName("media_type") val mediaType: String? = null,
    val title: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Float = 0f,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    val popularity: Float = 0f,
) {
    val displayTitle: String get() = title ?: name ?: originalTitle ?: originalName ?: "Unknown"
    val displayDate: String get() = releaseDate ?: firstAirDate ?: ""
}

@Serializable
data class TmdbMovie(
    val id: Int = 0,
    val title: String = "",
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Float = 0f,
    val runtime: Int? = null,
    val genres: List<TmdbGenre> = emptyList(),
    @SerialName("release_date") val releaseDate: String? = null,
)

@Serializable
data class TmdbTv(
    val id: Int = 0,
    val name: String = "",
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Float = 0f,
    @SerialName("episode_run_time") val episodeRuntime: List<Int> = emptyList(),
    val genres: List<TmdbGenre> = emptyList(),
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("number_of_episodes") val episodes: Int? = null,
    @SerialName("number_of_seasons") val seasons: Int? = null,
    val status: String? = null,
)

@Serializable
data class TmdbGenre(val id: Int = 0, val name: String = "")

@Serializable
data class TmdbGenreList(val genres: List<TmdbGenre> = emptyList())
