// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.extensions.kitsu

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
 * Kitsu.io anime + manga adapter (no auth required for public reads).
 * Endpoint: https://kitsu.io/api/edge
 */
@Singleton
class KitsuAdapter @Inject constructor(
    private val client: OkHttpClient,
) {
    private val base = "https://kitsu.io/api/edge"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun trending(limit: Int = 20): Result<List<KitsuAnime>> = list("/trending/anime?limit=$limit")
    suspend fun popular(limit: Int = 20): Result<List<KitsuAnime>> = list("/anime?sort=popularityRank&page[limit]=$limit")
    suspend fun topRated(limit: Int = 20): Result<List<KitsuAnime>> = list("/anime?sort=ratingRank&page[limit]=$limit")
    suspend fun upcoming(limit: Int = 20): Result<List<KitsuAnime>> = list("/anime?filter[status]=upcoming&page[limit]=$limit")
    suspend fun currentSeason(limit: Int = 20): Result<List<KitsuAnime>> = list("/anime?filter[status]=current&page[limit]=$limit")

    suspend fun search(query: String, limit: Int = 20): Result<List<KitsuAnime>> =
        list("/anime?filter[text]=${java.net.URLEncoder.encode(query, "UTF-8")}&page[limit]=$limit")

    private suspend fun list(path: String): Result<List<KitsuAnime>> = withContext(Dispatchers.IO) {
        runCatching {
            val body = get(path).getOrThrow()
            json.decodeFromString<KitsuRoot>(body).data
        }.onFailure { Timber.e(it, "Kitsu GET $path") }
    }

    suspend fun anime(id: String): Result<KitsuAnime> = withContext(Dispatchers.IO) {
        runCatching {
            val body = get("/anime/$id", single = true).getOrThrow()
            json.decodeFromString<KitsuAnimeRoot>(body).data
        }.onFailure { Timber.e(it, "Kitsu anime($id)") }
    }

    private suspend fun get(path: String, single: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("$base$path")
                .header("Accept", "application/vnd.api+json")
                .header("Content-Type", "application/vnd.api+json")
                .build()
            client.newCall(req).execute().use { response ->
                val body = response.body?.string() ?: throw Exception("Empty body")
                if (!response.isSuccessful) throw Exception("Kitsu HTTP ${response.code}: $body")
                body
            }
        }.onFailure { Timber.e(it, "Kitsu GET $path") }
    }
}

@Serializable
data class KitsuRoot(
    val data: List<KitsuAnime> = emptyList(),
    val meta: KitsuMeta? = null,
)

@Serializable
data class KitsuAnimeRoot(
    val data: KitsuAnime = KitsuAnime(),
)

@Serializable
data class KitsuAnime(
    val id: String = "",
    val type: String = "anime",
    val attributes: KitsuAttributes = KitsuAttributes(),
)

@Serializable
data class KitsuAttributes(
    @SerialName("slug") val slug: String = "",
    @SerialName("synopsis") val synopsis: String? = null,
    val description: String? = null,
    @SerialName("canonicalTitle") val canonicalTitle: String = "",
    @SerialName("englishTitle") val englishTitle: String? = null,
    @SerialName("romajiTitle") val romajiTitle: String? = null,
    @SerialName("japaneseTitle") val japaneseTitle: String? = null,
    @SerialName("averageRating") val averageRating: String? = null,
    @SerialName("ratingRank") val ratingRank: Int? = null,
    @SerialName("popularityRank") val popularityRank: Int? = null,
    @SerialName("userCount") val userCount: Int? = null,
    @SerialName("episodeCount") val episodeCount: Int? = null,
    @SerialName("episodeLength") val episodeLength: Int? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("startDate") val startDate: String? = null,
    @SerialName("endDate") val endDate: String? = null,
    @SerialName("posterImage") val posterImage: KitsuImage? = null,
    @SerialName("coverImage") val coverImage: KitsuImage? = null,
) {
    val displayTitle: String get() = englishTitle ?: canonicalTitle ?: romajiTitle ?: japaneseTitle ?: "Unknown"
    val scoreFloat: Float get() = averageRating?.toFloatOrNull()?.div(10f) ?: 0f
}

@Serializable
data class KitsuImage(
    val tiny: String? = null,
    val small: String? = null,
    val medium: String? = null,
    val large: String? = null,
    val original: String? = null,
)

@Serializable
data class KitsuMeta(
    @SerialName("count") val count: Int = 0,
)
