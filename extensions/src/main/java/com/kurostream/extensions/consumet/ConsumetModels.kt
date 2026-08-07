package com.kurostream.extensions.consumet

import kotlinx.serialization.Serializable

@Serializable
data class ConsumetAnime(
    val id: String,
    val title: String,
    val description: String? = null,
    val image: String? = null,
    val type: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val season: String? = null,
    val seasonYear: Int? = null,
    val score: Double? = null,
    val popularity: Int? = null,
    val duration: Int? = null,
    val rating: String? = null,
    val banner: String? = null,
    val synonyms: List<String> = emptyList(),
    val trailer: String? = null,
)

@Serializable
data class ConsumetEpisode(
    val id: String,
    val title: String? = null,
    val number: Int? = null,
    val image: String? = null,
    val description: String? = null,
    val airDate: String? = null,
)

@Serializable
data class ConsumetWatchResponse(
    val episodes: List<ConsumetEpisode> = emptyList(),
)

@Serializable
data class ConsumetStreamResponse(
    val sources: List<ConsumetStreamSource> = emptyList(),
    val subtitles: List<ConsumetSubtitle> = emptyList(),
)

@Serializable
data class ConsumetStreamSource(
    val url: String,
    val quality: String? = null,
    val isM3U8: Boolean = false,
    val headers: Map<String, String> = emptyMap(),
)

@Serializable
data class ConsumetSubtitle(
    val url: String,
    val lang: String,
)
