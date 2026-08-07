package com.kurostream.data.remote.dto.ann

import kotlinx.serialization.Serializable

@Serializable
data class AnnAnimeResponse(
    val id: String,
    val title: String? = null,
    val titleEnglish: String? = null,
    val titleJapanese: String? = null,
    val synonyms: List<String> = emptyList(),
    val description: String? = null,
    val image: String? = null,
    val type: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val year: Int? = null,
    val season: String? = null,
    val score: Double? = null,
    val popularity: Int? = null,
    val rating: String? = null,
    val banner: String? = null,
    val trailer: String? = null,
)

@Serializable
data class AnnSearchResponse(
    val results: List<AnnAnimeResponse> = emptyList(),
    val totalResults: Int = 0,
)
