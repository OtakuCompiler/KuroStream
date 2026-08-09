// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.extensions.tvdb

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
 * TVDB v4 adapter — series + episode metadata.
 * Uses PIN-based auth: subscriber pin + project key.
 * Endpoint: https://api4.thetvdb.com/v4
 */
@Singleton
class TvdbAdapter @Inject constructor(
    private val client: OkHttpClient,
) {
    private val base = "https://api4.thetvdb.com/v4"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var token: String? = null
    private var pin: String = ""

    fun configure(pin: String, apiKey: String = "") {
        this.pin = pin
        // Token is fetched lazily in ensureToken() once a real key is wired in.
        this.token = null
    }

    val isConfigured: Boolean get() = pin.isNotBlank()

    private suspend fun ensureToken() {
        if (token != null) return
        val req = Request.Builder()
            .url("$base/login")
            .post("""{"pin":"$pin"}""".toRequestJson())
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { response ->
            val body = response.body?.string() ?: throw Exception("Empty body")
            if (!response.isSuccessful) throw Exception("TVDB login HTTP ${response.code}")
            token = json.parseToJsonElement(body)
                .jsonObjectOrNull()?.get("data")?.jsonObjectOrNull()
                ?.get("token")?.toString()?.trim('"')
        }
    }

    suspend fun trending(): Result<List<TvdbResult>> = get("/series/0?sort=score.desc&limit=25")
    suspend fun topRated(): Result<List<TvdbResult>> = get("/series/0?sort=score.desc&limit=25")
    suspend fun popular(): Result<List<TvdbResult>> = get("/series/0?sort=popularity.desc&limit=25")

    suspend fun search(query: String): Result<List<TvdbResult>> =
        get("/search?query=${java.net.URLEncoder.encode(query, "UTF-8")}&type=series")

    suspend fun seriesExtended(id: Int): Result<TvdbSeries> = withContext(Dispatchers.IO) {
        runCatching {
            ensureToken()
            val req = Request.Builder()
                .url("$base/series/$id/extended")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer $token")
                .build()
            client.newCall(req).execute().use { response ->
                val body = response.body?.string() ?: throw Exception("Empty body")
                if (!response.isSuccessful) throw Exception("TVDB HTTP ${response.code}: $body")
                json.parseToJsonElement(body).jsonObjectOrNull()?.get("data")?.let {
                    json.decodeFromString<TvdbSeries>(it.toString())
                } ?: throw Exception("No data field")
            }
        }.onFailure { Timber.e(it, "TVDB seriesExtended($id)") }
    }

    private suspend fun get(path: String, asObject: Boolean = false): Result<List<TvdbResult>> = withContext(Dispatchers.IO) {
        runCatching {
            ensureToken()
            val req = Request.Builder()
                .url("$base$path")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer $token")
                .build()
            client.newCall(req).execute().use { response ->
                val body = response.body?.string() ?: throw Exception("Empty body")
                if (!response.isSuccessful) throw Exception("TVDB HTTP ${response.code}: $body")
                val dataArr = json.parseToJsonElement(body)
                    .jsonObjectOrNull()?.get("data")
                when {
                    asObject -> listOf(json.decodeFromString<TvdbResult>(dataArr.toString()))
                    dataArr is kotlinx.serialization.json.JsonArray ->
                        json.decodeFromString(dataArr.toString())
                    else -> emptyList()
                }
            }
        }.onFailure { Timber.e(it, "TVDB GET $path") }
    }

    private fun String.toRequestJson(): okhttp3.RequestBody {
        val mt = "application/json".toMediaTypeOrNull()
        return okhttp3.RequestBody.create(mt, this)
    }

    private fun kotlinx.serialization.json.JsonElement.jsonObjectOrNull() =
        runCatching { this as kotlinx.serialization.json.JsonObject }.getOrNull()
}

@Serializable
data class TvdbResult(
    val id: Int = 0,
    val name: String = "",
    val overview: String? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("score") val score: Float = 0f,
    @SerialName("status") val status: String? = null,
    @SerialName("firstAired") val firstAired: String? = null,
    @SerialName("year") val year: String? = null,
    @SerialName("genres") val genres: List<String> = emptyList(),
    @SerialName("network") val network: String? = null,
)

@Serializable
data class TvdbSeries(
    val id: Int = 0,
    val name: String = "",
    val overview: String? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("score") val score: Float = 0f,
    @SerialName("status") val status: String? = null,
    @SerialName("firstAired") val firstAired: String? = null,
    @SerialName("year") val year: String? = null,
    @SerialName("genres") val genres: List<String> = emptyList(),
    @SerialName("seasons") val seasons: List<TvdbSeason> = emptyList(),
)

@Serializable
data class TvdbSeason(
    val id: Int = 0,
    val number: Int = 0,
    @SerialName("episodeOrder") val episodeOrder: Int = 0,
)
