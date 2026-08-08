// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.extensions.mdblist

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
class MdblistAdapter @Inject constructor(
    private val client: OkHttpClient,
) {
    private var apiKey: String = ""
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun configure(apiKey: String) {
        this.apiKey = apiKey
    }

    val isConfigured: Boolean get() = apiKey.isNotBlank()

    suspend fun searchLists(query: String): Result<List<MdblistList>> = withContext(Dispatchers.IO) {
        runCatching {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val body = get("https://api.mdblist.com/lists/search/$encoded")
            json.decodeFromString(body)
        }.onFailure { Timber.e(it, "MdblistAdapter.searchLists($query)") }
    }

    suspend fun getList(listId: Int): Result<MdblistListDetail> = withContext(Dispatchers.IO) {
        runCatching {
            val body = get("https://api.mdblist.com/list/$listId?apikey=$apiKey")
            json.decodeFromString(body)
        }.onFailure { Timber.e(it, "MdblistAdapter.getList($listId)") }
    }

    suspend fun getUserLists(): Result<List<MdblistList>> = withContext(Dispatchers.IO) {
        runCatching {
            val body = get("https://api.mdblist.com/user/lists?apikey=$apiKey")
            json.decodeFromString(body)
        }.onFailure { Timber.e(it, "MdblistAdapter.getUserLists()") }
    }

    suspend fun searchItem(query: String): Result<List<MdblistSearchResult>> = withContext(Dispatchers.IO) {
        runCatching {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val body = get("https://api.mdblist.com/search/$encoded?apikey=$apiKey")
            json.decodeFromString<MdblistSearchResponse>(body).results
        }.onFailure { Timber.e(it, "MdblistAdapter.searchItem($query)") }
    }

    private fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Mdblist HTTP ${response.code} for $url")
        return response.body?.string() ?: throw Exception("Empty response")
    }
}

@Serializable
data class MdblistList(
    val id: Int = 0,
    val name: String = "",
    val description: String? = null,
    @SerialName("items_count")
    val itemsCount: Int = 0,
    val username: String = "",
    val likes: Int = 0,
)

@Serializable
data class MdblistListDetail(
    val id: Int = 0,
    val name: String = "",
    val items: List<MdblistItem> = emptyList(),
)

@Serializable
data class MdblistItem(
    val id: Int = 0,
    val type: String = "",
    val title: String = "",
    val year: Int? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
    val rating: Float = 0f,
    val overview: String? = null,
)

@Serializable
data class MdblistSearchResponse(
    val results: List<MdblistSearchResult> = emptyList(),
)

@Serializable
data class MdblistSearchResult(
    val id: Int = 0,
    val type: String = "",
    val title: String = "",
    val year: Int? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
)
