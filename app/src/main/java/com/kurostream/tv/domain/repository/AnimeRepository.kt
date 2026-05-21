package com.kurostream.tv.domain.repository

import com.kurostream.tv.domain.model.Anime
import com.kurostream.tv.domain.model.AnimeListEntry
import com.kurostream.tv.domain.model.AnimeSeason
import com.kurostream.tv.domain.model.AnimeStatus
import com.kurostream.tv.domain.model.Episode
import com.kurostream.tv.domain.model.StreamSource
import com.kurostream.tv.domain.model.WatchProgress
import com.kurostream.tv.domain.model.WatchStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for anime-related operations.
 * 
 * This interface defines all data operations for anime content.
 * Implementations should handle both remote and local data sources.
 */
interface AnimeRepository {

    // ==================== Discovery & Search ====================

    /**
     * Search for anime by query string.
     * 
     * @param query Search query
     * @param page Page number for pagination
     * @param perPage Results per page
     * @return Flow of search results
     */
    fun searchAnime(
        query: String,
        page: Int = 1,
        perPage: Int = 20
    ): Flow<Result<List<Anime>>>

    /**
     * Get trending anime.
     * 
     * @param page Page number
     * @param perPage Results per page
     * @return Flow of trending anime
     */
    fun getTrendingAnime(
        page: Int = 1,
        perPage: Int = 20
    ): Flow<Result<List<Anime>>>

    /**
     * Get popular anime of all time.
     * 
     * @param page Page number
     * @param perPage Results per page
     * @return Flow of popular anime
     */
    fun getPopularAnime(
        page: Int = 1,
        perPage: Int = 20
    ): Flow<Result<List<Anime>>>

    /**
     * Get seasonal anime.
     * 
     * @param year Year
     * @param season Season
     * @param page Page number
     * @param perPage Results per page
     * @return Flow of seasonal anime
     */
    fun getSeasonalAnime(
        year: Int,
        season: AnimeSeason,
        page: Int = 1,
        perPage: Int = 20
    ): Flow<Result<List<Anime>>>

    /**
     * Get recently updated anime (new episodes).
     * 
     * @param page Page number
     * @param perPage Results per page
     * @return Flow of recently updated anime
     */
    fun getRecentlyUpdated(
        page: Int = 1,
        perPage: Int = 20
    ): Flow<Result<List<Anime>>>

    /**
     * Get anime recommendations based on an anime.
     * 
     * @param animeId Anime ID to get recommendations for
     * @param limit Maximum number of recommendations
     * @return Flow of recommended anime
     */
    fun getRecommendations(
        animeId: String,
        limit: Int = 10
    ): Flow<Result<List<Anime>>>

    // ==================== Anime Details ====================

    /**
     * Get anime details by ID.
     * 
     * @param id Anime ID
     * @return Flow of anime details
     */
    fun getAnimeById(id: String): Flow<Result<Anime>>

    /**
     * Get anime by AniList ID.
     * 
     * @param anilistId AniList ID
     * @return Flow of anime details
     */
    fun getAnimeByAnilistId(anilistId: Int): Flow<Result<Anime>>

    /**
     * Get anime by MAL ID.
     * 
     * @param malId MyAnimeList ID
     * @return Flow of anime details
     */
    fun getAnimeByMalId(malId: Int): Flow<Result<Anime>>

    // ==================== Episodes ====================

    /**
     * Get episodes for an anime.
     * 
     * @param animeId Anime ID
     * @return Flow of episodes
     */
    fun getEpisodes(animeId: String): Flow<Result<List<Episode>>>

    /**
     * Get episode details.
     * 
     * @param animeId Anime ID
     * @param episodeNumber Episode number
     * @return Flow of episode details
     */
    fun getEpisode(
        animeId: String,
        episodeNumber: Int
    ): Flow<Result<Episode>>

    /**
     * Get stream sources for an episode.
     * 
     * @param animeId Anime ID
     * @param episodeNumber Episode number
     * @return Flow of stream sources
     */
    fun getStreamSources(
        animeId: String,
        episodeNumber: Int
    ): Flow<Result<List<StreamSource>>>

    // ==================== User Library ====================

    /**
     * Get user's anime list.
     * 
     * @param status Filter by watch status (null for all)
     * @return Flow of anime list entries
     */
    fun getMyList(status: WatchStatus? = null): Flow<Result<List<AnimeListEntry>>>

    /**
     * Add anime to user's list.
     * 
     * @param animeId Anime ID
     * @param status Initial watch status
     * @return Result of the operation
     */
    suspend fun addToMyList(
        animeId: String,
        status: WatchStatus = WatchStatus.PLAN_TO_WATCH
    ): Result<Unit>

    /**
     * Remove anime from user's list.
     * 
     * @param animeId Anime ID
     * @return Result of the operation
     */
    suspend fun removeFromMyList(animeId: String): Result<Unit>

    /**
     * Update watch status for an anime.
     * 
     * @param animeId Anime ID
     * @param status New watch status
     * @return Result of the operation
     */
    suspend fun updateWatchStatus(
        animeId: String,
        status: WatchStatus
    ): Result<Unit>

    // ==================== Watch Progress ====================

    /**
     * Get watch progress for an anime.
     * 
     * @param animeId Anime ID
     * @return Flow of watch progress
     */
    fun getWatchProgress(animeId: String): Flow<Result<WatchProgress?>>

    /**
     * Update watch progress for an episode.
     * 
     * @param animeId Anime ID
     * @param episodeNumber Episode number
     * @param position Playback position in milliseconds
     * @param duration Total duration in milliseconds
     * @return Result of the operation
     */
    suspend fun updateWatchProgress(
        animeId: String,
        episodeNumber: Int,
        position: Long,
        duration: Long
    ): Result<Unit>

    /**
     * Mark episode as watched.
     * 
     * @param animeId Anime ID
     * @param episodeNumber Episode number
     * @return Result of the operation
     */
    suspend fun markEpisodeWatched(
        animeId: String,
        episodeNumber: Int
    ): Result<Unit>

    /**
     * Mark episode as unwatched.
     * 
     * @param animeId Anime ID
     * @param episodeNumber Episode number
     * @return Result of the operation
     */
    suspend fun markEpisodeUnwatched(
        animeId: String,
        episodeNumber: Int
    ): Result<Unit>

    /**
     * Get continue watching list.
     * 
     * @param limit Maximum items to return
     * @return Flow of partially watched anime
     */
    fun getContinueWatching(limit: Int = 10): Flow<Result<List<AnimeListEntry>>>

    // ==================== Favorites ====================

    /**
     * Get user's favorite anime.
     * 
     * @return Flow of favorite anime
     */
    fun getFavorites(): Flow<Result<List<Anime>>>

    /**
     * Add anime to favorites.
     * 
     * @param animeId Anime ID
     * @return Result of the operation
     */
    suspend fun addToFavorites(animeId: String): Result<Unit>

    /**
     * Remove anime from favorites.
     * 
     * @param animeId Anime ID
     * @return Result of the operation
     */
    suspend fun removeFromFavorites(animeId: String): Result<Unit>

    /**
     * Check if anime is in favorites.
     * 
     * @param animeId Anime ID
     * @return Flow of favorite status
     */
    fun isFavorite(animeId: String): Flow<Boolean>

    // ==================== Caching ====================

    /**
     * Clear anime cache.
     */
    suspend fun clearCache()

    /**
     * Refresh anime data from remote.
     * 
     * @param animeId Anime ID
     * @return Result of the operation
     */
    suspend fun refreshAnime(animeId: String): Result<Anime>
}
