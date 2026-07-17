// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.data.remote.dto.imdb

import kotlinx.serialization.Serializable

@Serializable
data class ImdbDtos(val placeholder: String = "")

@Serializable
data class Title(
    val id: String,
    val titleText: TitleText? = null,
    val originalTitleText: TitleText? = null,
    val alternateTitles: List<AlternateTitle>? = null,
    val plot: Plot? = null,
    val primaryImage: PrimaryImage? = null,
    val titleType: String? = null,
    val releaseYear: Int? = null,
    val genres: GenreList? = null,
    val productionCompany: ProductionCompany? = null,
    val ratingsSummary: RatingsSummary? = null,
    val certificates: Certificates? = null,
    val runtime: Runtime? = null,
    val episodes: EpisodesWrapper? = null,
    val primaryVideos: PrimaryVideos? = null,
)

@Serializable
data class TitleText(
    val text: String? = null,
)

@Serializable
data class AlternateTitle(
    val titleText: TitleText? = null,
)

@Serializable
data class Plot(
    val plotText: PlotText? = null,
)

@Serializable
data class PlotText(
    val plainText: String? = null,
)

@Serializable
data class PrimaryImage(
    val url: String? = null,
)

@Serializable
data class GenreList(
    val genres: List<GenreItem> = emptyList(),
)

@Serializable
data class GenreItem(
    val text: String,
)

@Serializable
data class ProductionCompany(
    val edges: List<ProductionEdge> = emptyList(),
)

@Serializable
data class ProductionEdge(
    val node: ProductionNode? = null,
)

@Serializable
data class ProductionNode(
    val name: String? = null,
)

@Serializable
data class RatingsSummary(
    val aggregateRating: Double? = null,
    val voteCount: Int? = null,
    val popularityScore: Int? = null,
)

@Serializable
data class Certificates(
    val edges: List<CertificateEdge> = emptyList(),
)

@Serializable
data class CertificateEdge(
    val node: CertificateNode? = null,
)

@Serializable
data class CertificateNode(
    val rating: String? = null,
)

@Serializable
data class Runtime(
    val seconds: Int? = null,
)

@Serializable
data class EpisodesWrapper(
    val episodes: List<EpisodeData> = emptyList(),
)

@Serializable
data class EpisodeData(
    val totalEpisodes: Int? = null,
)

@Serializable
data class PrimaryVideos(
    val edges: List<VideoEdge> = emptyList(),
)

@Serializable
data class VideoEdge(
    val node: VideoNode? = null,
)

@Serializable
data class VideoNode(
    val playbackURLs: List<PlaybackURL> = emptyList(),
)

@Serializable
data class PlaybackURL(
    val url: String? = null,
)

@Serializable
data class TitleResponse(
    val data: Title? = null,
)

@Serializable
data class SearchResponse(
    val data: List<Title> = emptyList(),
)

@Serializable
data class EpisodesResponse(
    val data: List<EpisodeData> = emptyList(),
)
