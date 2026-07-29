package com.kurostream.domain.entity

import com.kurostream.domain.metadata.AiringStatus
import com.kurostream.domain.metadata.MediaType
import com.kurostream.domain.metadata.Season
import kotlinx.serialization.Serializable

@Serializable
enum class ContentRating { G, PG, R13, R17, UNRATED, ADULT }

@Serializable
data class MediaItem(
    val id: String,
    val title: String,
    val originalTitle: String? = null,
    val synopsis: String? = null,
    val coverImageUrl: String? = null,
    val bannerImageUrl: String? = null,
    val type: MediaType = MediaType.UNKNOWN,
    val status: AiringStatus = AiringStatus.UNKNOWN,
    val episodeNumber: Int? = null,
    val totalEpisodes: Int? = null,
    val durationMinutes: Int? = null,
    val seasonYear: Int? = null,
    val seasonQuarter: Season? = null,
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val rating: ContentRating = ContentRating.UNRATED,
    val score: Double? = null,
    val sourceExtensionId: String = "",
    val deepLink: String? = null,
    val lastUpdated: Long = 0L,
    // App-level compatibility fields (populated at UI layer)
    val isFavorite: Boolean = false,
    val posterUrl: String? = coverImageUrl,
    val backdropUrl: String? = bannerImageUrl,
)
