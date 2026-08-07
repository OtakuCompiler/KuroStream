package com.kurostream.data.remote.dto.anidb

import kotlinx.serialization.Serializable

@Serializable
data class AniDbAnime(
    val id: Int,
    val title: String? = null,
    val titleEnglish: String? = null,
    val titleJapanese: String? = null,
    val synonyms: String? = null,
    val description: String? = null,
    val picture: String? = null,
    val image: String? = null,
    val type: String? = null,
    val status: String? = null,
    val episodeCount: Int? = null,
    val episodeLength: Int? = null,
    val rating: String? = null,
    val ratingCount: Int? = null,
    val genres: List<String>? = null,
    val studios: List<String>? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val synonymsList: List<String>? = null,
)

@Serializable
data class AniDbSearchResponse(
    val anime: List<AniDbAnime> = emptyList(),
)
