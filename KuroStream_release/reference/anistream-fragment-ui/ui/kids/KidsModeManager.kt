package com.kurostream.legacyui.anistream.ui.kids

import android.content.Context
import com.kurostream.data.anistream.profile.ProfileRepository
import com.kurostream.data.anistream.model.AnimeItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kids Mode Manager handles content filtering, time limits,
 * and parental controls based on active profile settings.
 */
@Singleton
class KidsModeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository
) {

    // Content rating thresholds for kids mode
    private val blockedGenres = setOf(
        "Hentai", "Ecchi", "Erotica", "Horror", "Thriller",
        "Psychological", "Gore", "Mature"
    )

    private val blockedRatings = setOf(
        "R", "R+", "Rx", "NC-17", "Mature"
    )

    private val maxKidsRating = "PG-13"

    suspend fun isKidsModeActive(): Boolean {
        val profile = profileRepository.activeProfile.first()
        return profile?.isKidsMode == true
    }

    suspend fun getCurrentProfile() = profileRepository.activeProfile.first()

    /**
     * Filter content list for kids mode.
     * Returns only items appropriate for children.
     */
    suspend fun filterContent(items: List<AnimeItem>): List<AnimeItem> {
        if (!isKidsModeActive()) return items

        return items.filter { item ->
            isContentAppropriate(item)
        }
    }

    /**
     * Check if a single item is appropriate for kids mode.
     */
    suspend fun isContentAppropriate(item: AnimeItem): Boolean {
        if (!isKidsModeActive()) return true

        // Check rating
        if (item.rating != null && isRatingBlocked(item.rating)) {
            return false
        }

        // Check genres
        if (item.genres.any { it in blockedGenres }) {
            return false
        }

        // Check content tags
        if (item.tags.any { it in blockedGenres }) {
            return false
        }

        return true
    }

    private fun isRatingBlocked(rating: String): Boolean {
        return rating in blockedRatings ||
               rating.contains("R", ignoreCase = true) ||
               rating.contains("Mature", ignoreCase = true)
    }

    /**
     * Check if the current profile has exceeded daily time limit.
     */
    suspend fun hasExceededTimeLimit(): Boolean {
        val profile = profileRepository.activeProfile.first() ?: return false
        if (!profile.isKidsMode) return false

        return profile.watchTimeMinutesToday >= profile.dailyTimeLimitMinutes
    }

    /**
     * Get remaining watch time in minutes.
     */
    suspend fun getRemainingTimeMinutes(): Int {
        val profile = profileRepository.activeProfile.first() ?: return Int.MAX_VALUE
        if (!profile.isKidsMode) return Int.MAX_VALUE

        val remaining = profile.dailyTimeLimitMinutes - profile.watchTimeMinutesToday
        return maxOf(remaining, 0)
    }

    /**
     * Record watch time. Should be called periodically during playback.
     */
    suspend fun recordWatchTime(minutes: Int) {
        val profile = profileRepository.activeProfile.first() ?: return
        if (!profile.isKidsMode) return

        profileRepository.updateWatchTime(profile.id, minutes)
    }

    /**
     * Get content warning for a specific item.
     */
    fun getContentWarning(item: AnimeItem): String? {
        val warnings = mutableListOf<String>()

        if (item.genres.any { it == "Horror" || it == "Thriller" }) {
            warnings.add("Scary content")
        }
        if (item.genres.any { it == "Action" || it == "Violence" }) {
            warnings.add("Action/Violence")
        }
        if (item.rating == "PG-13") {
            warnings.add("Parental guidance suggested")
        }

        return if (warnings.isNotEmpty()) warnings.joinToString(", ") else null
    }

    /**
     * Get safe search query modifications for kids mode.
     */
    fun getSafeSearchFilters(): List<String> {
        return listOf(
            "-hentai", "-ecchi", "-mature", "-gore",
            "rating:pg", "rating:pg13"
        )
    }
}
