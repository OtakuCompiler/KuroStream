// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.extensions.mal

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
 * MyAnimeList unofficial JIKAN adapter (v4).
 * Free, no auth needed for reads. JIKAN is a public MAL scraper.
 * Endpoint: https://api.jikan.moe/v4
 *
 * Note: MAL itself deprecated its public API. JIKAN is the community
 * alternative and is good enough for ranking/popular rows.
 */
@Singleton
class MalAdapter @Inject constructor(
    private val client: OkHttpClient,
) {
    private val base = "https://api.jikan.moe/v4"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun topAnime(page: Int = 1, limit: Int = 20): Result<List<MalAnime>> =
        get("/top/anime?page=$page&limit=$limit")

    suspend fun topManga(page: Int = 1, limit: Int = 20): Result<List<MalManga>> =
        getManga("/top/manga?page=$page&limit=$limit")

    private suspend fun getManga(path: String): Result<List<MalManga>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("$base$path")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { response ->
                val body = response.body?.string() ?: throw Exception("Empty body")
                if (!response.isSuccessful) throw Exception("MAL/JIKAN HTTP ${response.code}: $body")
                json.decodeFromString<MalData<List<MalManga>>>(body).data
            }
        }.onFailure { Timber.e(it, "MAL/JIKAN GET $path") }
    }

    suspend fun seasonalNow(page: Int = 1, limit: Int = 20): Result<List<MalAnime>> =
        get("/seasons/now?page=$page&limit=$limit")

    suspend fun seasonalUpcoming(page: Int = 1, limit: Int = 20): Result<List<MalAnime>> =
        get("/seasons/upcoming?page=$page&limit=$limit")

    suspend fun popular(page: Int = 1, limit: Int = 20): Result<List<MalAnime>> =
        get("/top/anime?filter=bypopularity&page=$page&limit=$limit")

    suspend fun search(query: String, page: Int = 1, limit: Int = 20): Result<List<MalAnime>> =
        get("/anime?q=${java.net.URLEncoder.encode(query, "UTF-8")}&page=$page&limit=$limit")

    suspend fun anime(id: Int): Result<MalAnimeFull> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("$base/anime/$id/full")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { response ->
                val body = response.body?.string() ?: throw Exception("Empty body")
                if (!response.isSuccessful) throw Exception("MAL HTTP ${response.code}: $body")
                json.decodeFromString<MalData<MalAnimeFull>>(body).data
            }
        }.onFailure { Timber.e(it, "MAL anime($id)") }
    }

    private suspend fun get(path: String): Result<List<MalAnime>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("$base$path")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { response ->
                val body = response.body?.string() ?: throw Exception("Empty body")
                if (!response.isSuccessful) throw Exception("MAL/JIKAN HTTP ${response.code}: $body")
                json.decodeFromString<MalData<List<MalAnime>>>(body).data
            }
        }.onFailure { Timber.e(it, "MAL/JIKAN GET $path") }
    }
}

@Serializable
data class MalData<T>(
    val data: T,
    val pagination: MalPagination? = null,
)

@Serializable
data class MalPagination(
    @SerialName("last_visible_page") val lastVisiblePage: Int = 0,
    @SerialName("has_next_page") val hasNextPage: Boolean = false,
)

@Serializable
data class MalAnime(
    @SerialName("mal_id") val malId: Int = 0,
    val url: String = "",
    @SerialName("images") val images: MalImages = MalImages(),
    val title: String = "",
    @SerialName("title_english") val titleEnglish: String? = null,
    @SerialName("title_japanese") val titleJapanese: String? = null,
    val type: String? = null,
    val episodes: Int? = null,
    val status: String? = null,
    val score: Float? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val synopsis: String? = null,
    val year: Int? = null,
    val genres: List<MalGenre> = emptyList(),
) {
    val displayTitle: String get() = titleEnglish ?: title
}

@Serializable
data class MalAnimeFull(
    @SerialName("mal_id") val malId: Int = 0,
    val url: String = "",
    @SerialName("images") val images: MalImages = MalImages(),
    val title: String = "",
    @SerialName("title_english") val titleEnglish: String? = null,
    @SerialName("title_japanese") val titleJapanese: String? = null,
    val type: String? = null,
    val episodes: Int? = null,
    val status: String? = null,
    val duration: String? = null,
    val rating: String? = null,
    val score: Float? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val synopsis: String? = null,
    val background: String? = null,
    val year: Int? = null,
    val genres: List<MalGenre> = emptyList(),
    val studios: List<MalStudio> = emptyList(),
    val aired: MalAired? = null,
)

@Serializable
data class MalManga(
    @SerialName("mal_id") val malId: Int = 0,
    @SerialName("images") val images: MalImages = MalImages(),
    val title: String = "",
    @SerialName("title_english") val titleEnglish: String? = null,
    val type: String? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    val status: String? = null,
    val score: Float? = null,
    val synopsis: String? = null,
    val genres: List<MalGenre> = emptyList(),
)

@Serializable
data class MalImages(
    val jpg: MalImageVariant = MalImageVariant(),
    val webp: MalImageVariant = MalImageVariant(),
)

@Serializable
data class MalImageVariant(
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("small_image_url") val smallImageUrl: String? = null,
    @SerialName("large_image_url") val largeImageUrl: String? = null,
)

@Serializable
data class MalGenre(@SerialName("mal_id") val id: Int = 0, val name: String = "")

@Serializable
data class MalStudio(@SerialName("mal_id") val id: Int = 0, val name: String = "", val type: String? = null)

@Serializable
data class MalAired(val from: String? = null, val to: String? = null, val string: String? = null)
