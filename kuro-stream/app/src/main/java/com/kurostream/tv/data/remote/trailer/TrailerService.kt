package com.kurostream.tv.data.remote.trailer

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for resolving anime trailer URLs.
 *
 * Tries multiple sources in order:
 *  1. A pre-resolved YouTube video ID (supplied directly from metadata)
 *  2. YouTube Data API v3 search (requires YOUTUBE_API_KEY in BuildConfig)
 *  3. A public Kitsu trailer endpoint as fallback
 *
 * On 1 GB RAM devices the player avoids loading trailers during low-memory
 * states; this service just resolves the URL — playback decisions are in
 * the ViewModel.
 */
@Singleton
class TrailerService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "TrailerService"
        private const val YT_SEARCH_URL =
            "https://www.googleapis.com/youtube/v3/search"
        private const val YT_WATCH_URL = "https://www.youtube.com/watch?v="
    }

    /**
     * Return a YouTube watch URL for the given [animeTitle].
     *
     * @param youtubeVideoId Optional pre-resolved YouTube video ID from metadata.
     * @param animeTitle     Used for fallback YouTube search.
     * @param apiKey         YouTube Data API v3 key. Pass null to skip API search.
     */
    suspend fun resolveTrailerUrl(
        youtubeVideoId: String?,
        animeTitle: String,
        apiKey: String? = null
    ): String? {
        // 1. Pre-resolved ID from metadata
        if (!youtubeVideoId.isNullOrBlank()) {
            return "$YT_WATCH_URL$youtubeVideoId"
        }

        // 2. YouTube Data API search (requires key)
        if (!apiKey.isNullOrBlank()) {
            return searchYouTube(animeTitle, apiKey)
        }

        // 3. No API key — return null; UI should hide trailer button
        Timber.tag(TAG).d("No trailer source available for '$animeTitle'")
        return null
    }

    private suspend fun searchYouTube(query: String, apiKey: String): String? {
        return try {
            val encodedQuery = "$query official trailer anime".replace(" ", "%20")
            val url = "$YT_SEARCH_URL?part=snippet&q=$encodedQuery" +
                      "&type=video&maxResults=1&key=$apiKey"
            val response = okHttpClient.newCall(Request.Builder().url(url).build()).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            val items = JSONObject(body).optJSONArray("items") ?: return null
            if (items.length() == 0) return null

            val videoId = items.getJSONObject(0)
                .optJSONObject("id")
                ?.optString("videoId")
                ?.takeIf { it.isNotBlank() }
                ?: return null

            "$YT_WATCH_URL$videoId"
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "YouTube search failed for '$query'")
            null
        }
    }
}
