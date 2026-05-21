package com.kurostream.tv.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain model representing an Anime series.
 * 
 * This is the core domain model used throughout the application.
 * Data layer models should be mapped to this domain model.
 */
@Serializable
data class Anime(
    val id: String,
    val title: String,
    val titleEnglish: String? = null,
    val titleJapanese: String? = null,
    val titleRomaji: String? = null,
    val synopsis: String? = null,
    val coverImage: String? = null,
    val bannerImage: String? = null,
    val trailerUrl: String? = null,
    val status: AnimeStatus = AnimeStatus.UNKNOWN,
    val type: AnimeType = AnimeType.TV,
    val year: Int? = null,
    val season: AnimeSeason? = null,
    val rating: Float? = null,
    val popularity: Int? = null,
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val episodes: List<Episode> = emptyList(),
    val totalEpisodes: Int? = null,
    val averageEpisodeDuration: Int? = null, // in minutes
    val startDate: String? = null, // ISO date format
    val endDate: String? = null,
    val anilistId: Int? = null,
    val malId: Int? = null,
    val kitsuId: String? = null,
    val imdbId: String? = null,
    val isAdult: Boolean = false,
    val source: AnimeSource = AnimeSource.UNKNOWN,
    val isFavorite: Boolean = false,
    val watchProgress: WatchProgress? = null
) {
    /**
     * Returns the best available title based on user preference.
     */
    fun getDisplayTitle(preferEnglish: Boolean = true): String {
        return if (preferEnglish) {
            titleEnglish ?: titleRomaji ?: title
        } else {
            titleRomaji ?: titleEnglish ?: title
        }
    }

    /**
     * Returns true if the anime has aired all episodes.
     */
    val isCompleted: Boolean
        get() = status == AnimeStatus.FINISHED

    /**
     * Returns true if the anime is currently airing.
     */
    val isAiring: Boolean
        get() = status == AnimeStatus.RELEASING

    /**
     * Returns the next unwatched episode, if any.
     */
    val nextEpisode: Episode?
        get() = episodes.firstOrNull { !it.isWatched }

    /**
     * Returns the overall watch progress percentage.
     */
    val watchProgressPercentage: Float
        get() {
            val watchedCount = episodes.count { it.isWatched }
            val total = episodes.size.takeIf { it > 0 } ?: totalEpisodes ?: 1
            return (watchedCount.toFloat() / total.toFloat()) * 100f
        }
}

/**
 * Anime airing status.
 */
@Serializable
enum class AnimeStatus {
    RELEASING,
    FINISHED,
    NOT_YET_RELEASED,
    CANCELLED,
    HIATUS,
    UNKNOWN
}

/**
 * Anime format type.
 */
@Serializable
enum class AnimeType {
    TV,
    TV_SHORT,
    MOVIE,
    SPECIAL,
    OVA,
    ONA,
    MUSIC,
    UNKNOWN
}

/**
 * Anime season.
 */
@Serializable
enum class AnimeSeason {
    WINTER, // January - March
    SPRING, // April - June
    SUMMER, // July - September
    FALL    // October - December
}

/**
 * Source material for the anime.
 */
@Serializable
enum class AnimeSource {
    ORIGINAL,
    MANGA,
    LIGHT_NOVEL,
    VISUAL_NOVEL,
    VIDEO_GAME,
    NOVEL,
    DOUJINSHI,
    ANIME,
    WEB_NOVEL,
    LIVE_ACTION,
    GAME,
    COMIC,
    MULTIMEDIA_PROJECT,
    PICTURE_BOOK,
    OTHER,
    UNKNOWN
}

/**
 * User's watch progress for an anime.
 */
@Serializable
data class WatchProgress(
    val animeId: String,
    val currentEpisode: Int = 0,
    val currentPosition: Long = 0L, // playback position in milliseconds
    val totalEpisodes: Int = 0,
    val lastWatchedAt: Long = 0L, // timestamp
    val status: WatchStatus = WatchStatus.PLAN_TO_WATCH,
    val score: Int? = null // user's rating 1-10
)

/**
 * User's watch status for an anime.
 */
@Serializable
enum class WatchStatus {
    WATCHING,
    COMPLETED,
    ON_HOLD,
    DROPPED,
    PLAN_TO_WATCH
}

/**
 * Anime list entry for user's library.
 */
@Serializable
data class AnimeListEntry(
    val anime: Anime,
    val progress: WatchProgress,
    val addedAt: Long,
    val updatedAt: Long
)

/**
 * Anime recommendation with similarity score.
 */
@Serializable
data class AnimeRecommendation(
    val anime: Anime,
    val score: Float, // 0-100 similarity score
    val reason: String? = null
)

/**
 * Seasonal anime collection.
 */
@Serializable
data class SeasonalAnime(
    val year: Int,
    val season: AnimeSeason,
    val anime: List<Anime>
)
