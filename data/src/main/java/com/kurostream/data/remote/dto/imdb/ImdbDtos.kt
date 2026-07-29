package com.kurostream.data.remote.dto.imdb

data class TitleResponse(val data: Title? = null)

data class SearchResponse(val data: List<Title> = emptyList())

data class EpisodesResponse(val data: List<Episode> = emptyList())

data class Title(
    val id: String = "",
    val titleText: TitleText? = null,
    val originalTitleText: TitleText? = null,
    val alternateTitles: List<AlternateTitle>? = null,
    val plot: Plot? = null,
    val primaryImage: ImageData? = null,
    val titleType: String? = null,
    val releaseYear: Int? = null,
    val genres: GenresData? = null,
    val productionCompany: ProductionCompanyData? = null,
    val ratingsSummary: RatingsSummary? = null,
    val certificates: CertificateData? = null,
    val runtime: RuntimeData? = null,
    val episodes: EpisodeData? = null,
    val primaryVideos: VideoData? = null,
)

data class TitleText(val text: String? = null)

data class AlternateTitle(val titleText: TitleText? = null)

data class Plot(val plotText: PlainText? = null)

data class PlainText(val plainText: String? = null)

data class ImageData(val url: String? = null)

data class GenresData(val genres: List<GenreItem>? = null)

data class GenreItem(val text: String? = null)

data class ProductionCompanyData(val edges: List<ProductionEdge>? = null)

data class ProductionEdge(val node: ProductionNode? = null)

data class ProductionNode(val name: String? = null)

data class RatingsSummary(
    val aggregateRating: Double? = null,
    val voteCount: Int? = null,
    val popularityScore: Int? = null,
)

data class CertificateData(val edges: List<CertificateEdge>? = null)

data class CertificateEdge(val node: CertificateNode? = null)

data class CertificateNode(val rating: String? = null)

data class RuntimeData(val seconds: Int? = null)

data class EpisodeData(val episodes: List<EpisodeItem>? = null)

data class EpisodeItem(val totalEpisodes: Int? = null)

data class VideoData(val edges: List<VideoEdge>? = null)

data class VideoEdge(val node: VideoNode? = null)

data class VideoNode(val playbackURLs: List<PlaybackURL>? = null)

data class PlaybackURL(val url: String? = null)

data class Episode(
    val id: String = "",
    val titleText: TitleText? = null,
    val image: ImageData? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val releaseDate: String? = null,
)
