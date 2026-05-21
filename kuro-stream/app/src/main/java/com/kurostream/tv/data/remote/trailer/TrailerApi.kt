package com.kurostream.tv.data.remote.trailer

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * YouTube Data API for fetching anime trailers.
 * Used to search and retrieve trailer information.
 */
interface YouTubeApi {
    
    companion object {
        const val BASE_URL = "https://www.googleapis.com/youtube/v3/"
    }
    
    /**
     * Search for videos on YouTube.
     */
    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 5,
        @Query("videoCategoryId") categoryId: String? = null,
        @Query("key") apiKey: String
    ): Response<YouTubeSearchResponse>
    
    /**
     * Get video details by ID.
     */
    @GET("videos")
    suspend fun getVideoDetails(
        @Query("part") part: String = "snippet,contentDetails,statistics",
        @Query("id") videoId: String,
        @Query("key") apiKey: String
    ): Response<YouTubeVideoResponse>
}

/**
 * YouTube search response.
 */
data class YouTubeSearchResponse(
    val kind: String?,
    val etag: String?,
    val nextPageToken: String?,
    val prevPageToken: String?,
    val regionCode: String?,
    val pageInfo: YouTubePageInfo?,
    val items: List<YouTubeSearchItem>?
)

data class YouTubePageInfo(
    val totalResults: Int?,
    val resultsPerPage: Int?
)

data class YouTubeSearchItem(
    val kind: String?,
    val etag: String?,
    val id: YouTubeVideoId?,
    val snippet: YouTubeSnippet?
)

data class YouTubeVideoId(
    val kind: String?,
    val videoId: String?,
    val channelId: String?,
    val playlistId: String?
)

data class YouTubeSnippet(
    val publishedAt: String?,
    val channelId: String?,
    val title: String?,
    val description: String?,
    val thumbnails: YouTubeThumbnails?,
    val channelTitle: String?,
    val liveBroadcastContent: String?,
    val publishTime: String?
)

data class YouTubeThumbnails(
    val default: YouTubeThumbnail?,
    val medium: YouTubeThumbnail?,
    val high: YouTubeThumbnail?,
    val standard: YouTubeThumbnail?,
    val maxres: YouTubeThumbnail?
)

data class YouTubeThumbnail(
    val url: String?,
    val width: Int?,
    val height: Int?
)

/**
 * YouTube video details response.
 */
data class YouTubeVideoResponse(
    val kind: String?,
    val etag: String?,
    val items: List<YouTubeVideoItem>?
)

data class YouTubeVideoItem(
    val kind: String?,
    val etag: String?,
    val id: String?,
    val snippet: YouTubeSnippet?,
    val contentDetails: YouTubeContentDetails?,
    val statistics: YouTubeStatistics?
)

data class YouTubeContentDetails(
    val duration: String?, // ISO 8601 duration format (e.g., PT1H30M)
    val dimension: String?,
    val definition: String?,
    val caption: String?,
    val licensedContent: Boolean?,
    val contentRating: YouTubeContentRating?,
    val projection: String?
)

data class YouTubeContentRating(
    val ytRating: String?
)

data class YouTubeStatistics(
    val viewCount: String?,
    val likeCount: String?,
    val favoriteCount: String?,
    val commentCount: String?
)

/**
 * Trailer Repository for fetching and managing anime trailers.
 */
interface TrailerRepository {
    
    /**
     * Search for anime trailers.
     */
    suspend fun searchTrailer(animeName: String, animeNameJp: String? = null): Result<TrailerInfo?>
    
    /**
     * Get trailer by YouTube video ID.
     */
    suspend fun getTrailerById(videoId: String): Result<TrailerInfo?>
    
    /**
     * Get multiple trailers for an anime (PV, CM, etc.).
     */
    suspend fun getAllTrailers(animeName: String): Result<List<TrailerInfo>>
    
    /**
     * Build embeddable YouTube URL.
     */
    fun buildEmbedUrl(videoId: String): String
    
    /**
     * Build YouTube watch URL.
     */
    fun buildWatchUrl(videoId: String): String
}

/**
 * Unified trailer information model.
 */
data class TrailerInfo(
    val videoId: String,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val channelName: String?,
    val publishedAt: String?,
    val durationSeconds: Int?,
    val viewCount: Long?,
    val embedUrl: String,
    val watchUrl: String,
    val trailerType: TrailerType
)

/**
 * Types of anime trailers.
 */
enum class TrailerType {
    PV,          // Promotional Video
    CM,          // Commercial
    TEASER,      // Teaser trailer
    FULL_TRAILER,// Full trailer
    OPENING,     // Opening theme
    ENDING,      // Ending theme
    MUSIC_VIDEO, // Music video
    OTHER        // Other/Unknown
}

/**
 * Default implementation of TrailerRepository.
 */
class TrailerRepositoryImpl(
    private val youTubeApi: YouTubeApi,
    private val apiKey: String
) : TrailerRepository {
    
    override suspend fun searchTrailer(animeName: String, animeNameJp: String?): Result<TrailerInfo?> {
        return try {
            // Try searching with English name first
            var searchQuery = "$animeName anime trailer PV"
            var response = youTubeApi.searchVideos(
                query = searchQuery,
                maxResults = 1,
                apiKey = apiKey
            )
            
            // If no results, try with Japanese name
            if (response.body()?.items.isNullOrEmpty() && !animeNameJp.isNullOrEmpty()) {
                searchQuery = "$animeNameJp PV アニメ"
                response = youTubeApi.searchVideos(
                    query = searchQuery,
                    maxResults = 1,
                    apiKey = apiKey
                )
            }
            
            val item = response.body()?.items?.firstOrNull()
            if (item != null) {
                val videoId = item.id?.videoId ?: return Result.success(null)
                Result.success(mapToTrailerInfo(item, videoId))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getTrailerById(videoId: String): Result<TrailerInfo?> {
        return try {
            val response = youTubeApi.getVideoDetails(
                videoId = videoId,
                apiKey = apiKey
            )
            
            val item = response.body()?.items?.firstOrNull()
            if (item != null) {
                Result.success(mapVideoItemToTrailerInfo(item))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAllTrailers(animeName: String): Result<List<TrailerInfo>> {
        return try {
            val searchQueries = listOf(
                "$animeName anime PV",
                "$animeName anime trailer",
                "$animeName anime opening",
                "$animeName anime CM"
            )
            
            val trailers = mutableListOf<TrailerInfo>()
            val seenVideoIds = mutableSetOf<String>()
            
            for (query in searchQueries) {
                val response = youTubeApi.searchVideos(
                    query = query,
                    maxResults = 3,
                    apiKey = apiKey
                )
                
                response.body()?.items?.forEach { item ->
                    val videoId = item.id?.videoId
                    if (videoId != null && videoId !in seenVideoIds) {
                        seenVideoIds.add(videoId)
                        trailers.add(mapToTrailerInfo(item, videoId))
                    }
                }
            }
            
            Result.success(trailers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun buildEmbedUrl(videoId: String): String {
        return "https://www.youtube.com/embed/$videoId"
    }
    
    override fun buildWatchUrl(videoId: String): String {
        return "https://www.youtube.com/watch?v=$videoId"
    }
    
    private fun mapToTrailerInfo(item: YouTubeSearchItem, videoId: String): TrailerInfo {
        val title = item.snippet?.title ?: ""
        return TrailerInfo(
            videoId = videoId,
            title = title,
            description = item.snippet?.description,
            thumbnailUrl = item.snippet?.thumbnails?.high?.url
                ?: item.snippet?.thumbnails?.medium?.url
                ?: item.snippet?.thumbnails?.default?.url,
            channelName = item.snippet?.channelTitle,
            publishedAt = item.snippet?.publishedAt,
            durationSeconds = null, // Not available in search results
            viewCount = null, // Not available in search results
            embedUrl = buildEmbedUrl(videoId),
            watchUrl = buildWatchUrl(videoId),
            trailerType = detectTrailerType(title)
        )
    }
    
    private fun mapVideoItemToTrailerInfo(item: YouTubeVideoItem): TrailerInfo {
        val title = item.snippet?.title ?: ""
        val videoId = item.id ?: ""
        return TrailerInfo(
            videoId = videoId,
            title = title,
            description = item.snippet?.description,
            thumbnailUrl = item.snippet?.thumbnails?.high?.url
                ?: item.snippet?.thumbnails?.medium?.url
                ?: item.snippet?.thumbnails?.default?.url,
            channelName = item.snippet?.channelTitle,
            publishedAt = item.snippet?.publishedAt,
            durationSeconds = parseDuration(item.contentDetails?.duration),
            viewCount = item.statistics?.viewCount?.toLongOrNull(),
            embedUrl = buildEmbedUrl(videoId),
            watchUrl = buildWatchUrl(videoId),
            trailerType = detectTrailerType(title)
        )
    }
    
    private fun detectTrailerType(title: String): TrailerType {
        val lowerTitle = title.lowercase()
        return when {
            lowerTitle.contains("pv") || lowerTitle.contains("promotional") -> TrailerType.PV
            lowerTitle.contains("cm") || lowerTitle.contains("commercial") -> TrailerType.CM
            lowerTitle.contains("teaser") -> TrailerType.TEASER
            lowerTitle.contains("trailer") -> TrailerType.FULL_TRAILER
            lowerTitle.contains("opening") || lowerTitle.contains("op ") -> TrailerType.OPENING
            lowerTitle.contains("ending") || lowerTitle.contains("ed ") -> TrailerType.ENDING
            lowerTitle.contains("mv") || lowerTitle.contains("music video") -> TrailerType.MUSIC_VIDEO
            else -> TrailerType.OTHER
        }
    }
    
    /**
     * Parse ISO 8601 duration to seconds.
     * Example: PT1H30M45S -> 5445 seconds
     */
    private fun parseDuration(duration: String?): Int? {
        if (duration == null) return null
        
        var totalSeconds = 0
        val regex = Regex("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
        val match = regex.find(duration) ?: return null
        
        match.groupValues.getOrNull(1)?.toIntOrNull()?.let { hours ->
            totalSeconds += hours * 3600
        }
        match.groupValues.getOrNull(2)?.toIntOrNull()?.let { minutes ->
            totalSeconds += minutes * 60
        }
        match.groupValues.getOrNull(3)?.toIntOrNull()?.let { seconds ->
            totalSeconds += seconds
        }
        
        return if (totalSeconds > 0) totalSeconds else null
    }
}
