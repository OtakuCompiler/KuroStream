package com.kurostream.tv.data.remote.skip

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for fetching skip timestamps from AniSkip API
 * Provides intro/outro skip times for anime episodes
 */
@Singleton
class SkipTimestampService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    companion object {
        private const val ANISKIP_BASE_URL = "https://api.aniskip.com/v2"
        private const val TAG = "SkipTimestampService"
        
        // Skip types
        const val SKIP_TYPE_OP = "op"
        const val SKIP_TYPE_ED = "ed"
        const val SKIP_TYPE_RECAP = "recap"
        const val SKIP_TYPE_MIXED_OP = "mixed-op"
        const val SKIP_TYPE_MIXED_ED = "mixed-ed"
    }
    
    /**
     * Fetch skip timestamps for an episode
     * @param malId MyAnimeList ID
     * @param episodeNumber Episode number
     * @param episodeLength Episode length in seconds (for validation)
     */
    suspend fun getSkipTimestamps(
        malId: Long,
        episodeNumber: Int,
        episodeLength: Long? = null
    ): Result<List<SkipTimestamp>> = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = StringBuilder()
                .append(ANISKIP_BASE_URL)
                .append("/skip-times/")
                .append(malId)
                .append("/")
                .append(episodeNumber)
                .append("?types[]=op&types[]=ed&types[]=recap&types[]=mixed-op&types[]=mixed-ed")
            
            if (episodeLength != null) {
                urlBuilder.append("&episodeLength=").append(episodeLength)
            }
            
            val request = Request.Builder()
                .url(urlBuilder.toString())
                .get()
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Timber.tag(TAG).w("Failed to fetch skip timestamps: ${response.code}")
                return@withContext Result.success(emptyList())
            }
            
            val body = response.body?.string() ?: return@withContext Result.success(emptyList())
            val skipResponse = json.decodeFromString<AniSkipResponse>(body)
            
            if (!skipResponse.found) {
                return@withContext Result.success(emptyList())
            }
            
            val timestamps = skipResponse.results.map { result ->
                SkipTimestamp(
                    type = mapSkipType(result.skipType),
                    startTime = result.interval.startTime,
                    endTime = result.interval.endTime,
                    episodeLength = result.episodeLength
                )
            }
            
            Timber.tag(TAG).d("Found ${timestamps.size} skip timestamps for MAL ID $malId episode $episodeNumber")
            Result.success(timestamps)
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error fetching skip timestamps")
            Result.failure(e)
        }
    }
    
    /**
     * Submit a skip timestamp (for community contribution)
     */
    suspend fun submitSkipTimestamp(
        malId: Long,
        episodeNumber: Int,
        skipType: SkipType,
        startTime: Double,
        endTime: Double,
        episodeLength: Long,
        submitterId: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val submitData = SkipSubmitRequest(
                skipType = skipType.apiValue,
                providerName = "kurostream",
                startTime = startTime,
                endTime = endTime,
                episodeLength = episodeLength,
                submitterId = submitterId
            )
            
            val requestBody = json.encodeToString(SkipSubmitRequest.serializer(), submitData)
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$ANISKIP_BASE_URL/skip-times/$malId/$episodeNumber")
                .post(requestBody)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                Timber.tag(TAG).d("Successfully submitted skip timestamp")
                Result.success(true)
            } else {
                Timber.tag(TAG).w("Failed to submit skip timestamp: ${response.code}")
                Result.success(false)
            }
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error submitting skip timestamp")
            Result.failure(e)
        }
    }
    
    /**
     * Vote on a skip timestamp
     */
    suspend fun voteOnSkipTimestamp(
        skipId: String,
        voteType: VoteType
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$ANISKIP_BASE_URL/skip-times/vote/$skipId")
                .post(
                    """{"voteType": "${voteType.apiValue}"}"""
                        .toRequestBody("application/json".toMediaType())
                )
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error voting on skip timestamp")
            Result.failure(e)
        }
    }
    
    private fun mapSkipType(apiType: String): SkipType {
        return when (apiType.lowercase()) {
            SKIP_TYPE_OP, SKIP_TYPE_MIXED_OP -> SkipType.OPENING
            SKIP_TYPE_ED, SKIP_TYPE_MIXED_ED -> SkipType.ENDING
            SKIP_TYPE_RECAP -> SkipType.RECAP
            else -> SkipType.UNKNOWN
        }
    }
}

/**
 * Represents a skip timestamp segment
 */
data class SkipTimestamp(
    val type: SkipType,
    val startTime: Double,
    val endTime: Double,
    val episodeLength: Long? = null
) {
    val durationSeconds: Double get() = endTime - startTime
    
    fun isWithinRange(currentPosition: Double): Boolean {
        return currentPosition >= startTime && currentPosition < endTime
    }
    
    fun shouldShowSkipButton(currentPosition: Double, leadTime: Double = 2.0): Boolean {
        return currentPosition >= (startTime - leadTime) && currentPosition < endTime
    }
}

/**
 * Skip segment types
 */
enum class SkipType(val apiValue: String, val displayName: String) {
    OPENING("op", "Opening"),
    ENDING("ed", "Ending"),
    RECAP("recap", "Recap"),
    UNKNOWN("unknown", "Skip")
}

/**
 * Vote types for skip timestamps
 */
enum class VoteType(val apiValue: String) {
    UPVOTE("upvote"),
    DOWNVOTE("downvote")
}

// API Response Models
@Serializable
data class AniSkipResponse(
    @SerialName("found") val found: Boolean,
    @SerialName("results") val results: List<AniSkipResult> = emptyList(),
    @SerialName("message") val message: String? = null,
    @SerialName("statusCode") val statusCode: Int = 200
)

@Serializable
data class AniSkipResult(
    @SerialName("interval") val interval: SkipInterval,
    @SerialName("skipType") val skipType: String,
    @SerialName("skipId") val skipId: String,
    @SerialName("episodeLength") val episodeLength: Long
)

@Serializable
data class SkipInterval(
    @SerialName("startTime") val startTime: Double,
    @SerialName("endTime") val endTime: Double
)

@Serializable
data class SkipSubmitRequest(
    @SerialName("skipType") val skipType: String,
    @SerialName("providerName") val providerName: String,
    @SerialName("startTime") val startTime: Double,
    @SerialName("endTime") val endTime: Double,
    @SerialName("episodeLength") val episodeLength: Long,
    @SerialName("submitterId") val submitterId: String
)

/**
 * Cached skip timestamps for offline access
 */
data class CachedSkipTimestamps(
    val malId: Long,
    val episodeNumber: Int,
    val timestamps: List<SkipTimestamp>,
    val cachedAt: Long = System.currentTimeMillis()
) {
    companion object {
        private const val CACHE_DURATION_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
    }
    
    fun isExpired(): Boolean {
        return System.currentTimeMillis() - cachedAt > CACHE_DURATION_MS
    }
}
