package com.kurostream.desktop.search

import androidx.compose.runtime.Stable
import com.kurostream.desktop.data.DesktopCache
import com.kurostream.desktop.data.DesktopSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * Desktop search. Calls the same TMDB/AniList endpoints the Android app
 * uses through the shared `:domain` repository contracts. Implemented
 * directly with java.net + kotlinx.serialization to avoid pulling in
 * Retrofit/OkHttp on the desktop (smaller binary, less startup time).
 */
@Stable
class DesktopSearchService(
    private val settings: DesktopSettings,
    private val cache: DesktopCache,
) {
    private val recentQueries = MutableStateFlow<List<String>>(emptyList())

    fun observeRecent(): Flow<List<String>> = recentQueries.asStateFlow()

    fun search(query: String): Flow<List<SearchResult>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }
        // Record in history (no-op if duplicate — moves it to top instead)
        rememberQuery(query)

        val url = URL(
            "https://kuro-stream-tv.lovable.app/api/search?q=" +
                java.net.URLEncoder.encode(query, "UTF-8")
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
        }
        try {
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().readText()
                val resp = Json.decodeFromString(SearchResponse.serializer(), body)
                emit(resp.results)
            } else {
                emit(emptyList())
            }
        } finally {
            conn.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun rememberQuery(q: String) {
        val current = recentQueries.value.toMutableList()
        current.remove(q)
        current.add(0, q)
        if (current.size > 30) current.subList(30, current.size).clear()
        recentQueries.value = current
    }
}

@Serializable
data class SearchResult(
    val id: String,
    val title: String,
    val year: Int? = null,
    val type: String = "show", // show | movie
    val posterUrl: String? = null,
    val rating: Double? = null,
)

@Serializable
data class SearchResponse(
    val results: List<SearchResult> = emptyList(),
)
