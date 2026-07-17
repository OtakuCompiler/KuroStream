package com.kurostream.data.remote.dto.kitsu

import kotlinx.serialization.Serializable

@Serializable
data class KitsuModels(val placeholder: String = "")

@Serializable
data class AnimeDetail(
    val data: AnimeData? = null,
)

@Serializable
data class AnimeData(
    val id: String? = null,
    val type: String? = null,
    val attributes: AnimeAttributes? = null,
)

@Serializable
data class AnimeAttributes(
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val slug: String? = null,
    val synopsis: String? = null,
    val canonicalTitle: String? = null,
    val titles: Titles? = null,
    val posterImage: PosterImage? = null,
    val coverImage: CoverImage? = null,
    val episodeCount: Int? = null,
    val episodeLength: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val status: String? = null,
    val ageRating: String? = null,
    val averageRating: String? = null,
    val ratingFrequencies: Map<String, Int>? = null,
    val subtype: String? = null,
)

@Serializable
data class Titles(
    val en: String? = null,
    val en_jp: String? = null,
    val ja_jp: String? = null,
)

@Serializable
data class PosterImage(
    val tiny: String? = null,
    val small: String? = null,
    val medium: String? = null,
    val large: String? = null,
    val original: String? = null,
)

@Serializable
data class CoverImage(
    val tiny: String? = null,
    val small: String? = null,
    val medium: String? = null,
    val large: String? = null,
    val original: String? = null,
)

@Serializable
data class SearchResponse(
    val data: List<AnimeData> = emptyList(),
)
