package com.kurostream.players.skip

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntroSkipManager @Inject constructor() {

    private val _skipSegments = MutableStateFlow<List<SkipSegment>>(emptyList())
    val skipSegments: StateFlow<List<SkipSegment>> = _skipSegments.asStateFlow()

    private val _isSkipPending = MutableStateFlow(false)
    val isSkipPending: StateFlow<Boolean> = _isSkipPending.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val aniskipEndpoint = System.getenv("ANISKIP_ENDPOINT") ?: "https://api.aniskip.com/v2/skip-times/"

    suspend fun fetchSkipSegments(
        animeId: String?,
        episodeNumber: Int?,
        episodeDuration: Long
    ): Result<List<SkipSegment>> {
        if (animeId == null || episodeNumber == null) {
            return Result.failure(IllegalArgumentException("Anime ID and episode number required"))
        }

        return try {
            val url = "$aniskipEndpoint$animeId/$episodeNumber?types[]=ed&types[]=op&types[]=mixed-op&types[]=mixed-ed&types[]=recap"
            val response = client.newCall(
                Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .build()
            ).execute()

            if (!response.isSuccessful) {
                Timber.w("AniSkip request failed: ${response.code}")
                return Result.success(emptyList())
            }

            val json = JSONObject(response.body?.string() ?: "{}")
            if (json.optInt("statusCode", 0) != 200) {
                return Result.success(emptyList())
            }

            val segments = json.optJSONArray("results")?.let { results ->
                List(results.length()) { i ->
                    results.getJSONObject(i).let { segmentJson ->
                        SkipSegment(
                            startTimeMs = (segmentJson.optDouble("startTime", 0.0) * 1000).toLong(),
                            endTimeMs = (segmentJson.optDouble("endTime", 0.0) * 1000).toLong(),
                            type = SkipType.fromString(segmentJson.optString("skipType", "unknown")),
                            source = SkipSource.AniSkip,
                        )
                    }
                }
            } ?: emptyList()

            _skipSegments.value = segments
            Timber.d("Fetched ${segments.size} skip segments from AniSkip")
            Result.success(segments)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch skip segments from AniSkip")
            Result.failure(e)
        }
    }

    fun shouldSkipAt(positionMs: Long): SkipSegment? =
        _skipSegments.value.find { positionMs in it.startTimeMs until it.endTimeMs }

    fun clearSegments() {
        _skipSegments.value = emptyList()
    }
}

data class SkipSegment(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val type: SkipType,
    val source: SkipSource,
    val confidence: Float = 1.0f,
)

enum class SkipType {
    OPENING,
    ENDING,
    RECAP,
    MIXED_OPENING,
    MIXED_ENDING,
    PREVIEW,
    UNKNOWN;

    companion object {
        fun fromString(str: String): SkipType {
            return when (str.lowercase()) {
                "op", "opening" -> OPENING
                "ed", "ending" -> ENDING
                "recap" -> RECAP
                "mixed-op" -> MIXED_OPENING
                "mixed-ed" -> MIXED_ENDING
                "preview" -> PREVIEW
                else -> UNKNOWN
            }
        }
    }
}

enum class SkipSource {
    AniSkip,
    LocalFingerprint,
    Manual,
}