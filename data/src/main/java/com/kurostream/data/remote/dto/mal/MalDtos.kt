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

package com.kurostream.data.remote.dto.mal

import kotlinx.serialization.Serializable

@Serializable
data class MalDtos()

@Serializable
data class Anime(
    val id: Int,
    val title: String,
    val mainPicture: MainPicture?,
    val alternativeTitles: AlternativeTitles?,
    val startDate: String?,
    val endDate: String?,
    val synopsis: String?,
    val mean: Double?,
    val rank: Int?,
    val popularity: Int?,
    val numScoringUsers: Int?,
    val numEpisodes: Int?,
    val status: String?,
    val mediaType: String?,
    val startSeason: Season?,
    val broadcast: Broadcast?,
    val averageEpisodeDuration: Int?,
    val rating: String?,
    val source: String?,
    val genres: List<Genre>?,
    val studios: List<Studio>?,
    val pictures: List<Picture>?,
    val background: String?,
    val relatedAnime: List<RelatedAnime>?,
    val relatedManga: List<RelatedManga>?,
    val recommendations: List<Recommendation>?,
    val statistics: Statistics?,
    val numFavorites: Int?,
)

@Serializable
data class MainPicture(val medium: String?, val large: String?)

@Serializable
data class AlternativeTitles(
    val synonyms: List<String>?,
    val en: String?,
    val ja: String?,
    val en_jp: String?,
)

@Serializable
data class Season(val year: Int, val season: String)

@Serializable
data class Broadcast(val dayOfTheWeek: String?, val startTime: String?)

@Serializable
data class Genre(val id: Int, val name: String)

@Serializable
data class Studio(val id: Int, val name: String)

@Serializable
data class Picture(val medium: String?, val large: String?)

@Serializable
data class RelatedAnime(
    val node: RelatedNode,
    val relationType: String,
    val relationTypeFormatted: String,
)

@Serializable
data class RelatedNode(
    val id: Int,
    val title: String,
    val mainPicture: MainPicture?,
    val startDate: String?,
    val endDate: String?,
)

@Serializable
data class RelatedManga(
    val node: MangaNode,
    val relationType: String,
)

@Serializable
data class MangaNode(
    val id: Int,
    val title: String,
    val mainPicture: MainPicture?,
)

@Serializable
data class Recommendation(
    val node: RecommendationNode,
    val numRecommendations: Int,
)

@Serializable
data class RecommendationNode(
    val id: Int,
    val title: String,
    val mainPicture: MainPicture?,
)

@Serializable
data class Statistics(
    val status: StatusStats?,
    val numListUsers: Int?,
)

@Serializable
data class StatusStats(
    val watching: Int?,
    val completed: Int?,
    val on_hold: Int?,
    val dropped: Int?,
    val plan_to_watch: Int?,
)

@Serializable
data class SearchResponse(
    val data: List<SearchResult>?,
    val paging: Paging?,
)

@Serializable
data class SearchResult(
    val node: SearchNode,
)

@Serializable
data class SearchNode(
    val id: Int,
    val title: String,
    val mainPicture: MainPicture?,
    val alternativeTitles: AlternativeTitles?,
    val startDate: String?,
    val endDate: String?,
    val synopsis: String?,
    val mean: Double?,
    val rank: Int?,
    val popularity: Int?,
    val numScoringUsers: Int?,
    val numEpisodes: Int?,
    val status: String?,
    val mediaType: String?,
    val numFavorites: Int?,
)

@Serializable
data class Paging(
    val next: String?,
    val previous: String?,
)

@Serializable
data class SeasonalResponse(
    val data: List<SeasonalNode>?,
    val paging: Paging?,
)

@Serializable
data class SeasonalNode(
    val node: SeasonalAnime,
)

@Serializable
data class SeasonalAnime(
    val id: Int,
    val title: String,
    val mainPicture: MainPicture?,
    val alternativeTitles: AlternativeTitles?,
    val startDate: String?,
    val endDate: String?,
    val synopsis: String?,
    val mean: Double?,
    val rank: Int?,
    val popularity: Int?,
    val numScoringUsers: Int?,
    val numEpisodes: Int?,
    val status: String?,
    val mediaType: String?,
    val numFavorites: Int?,
)

@Serializable
data class TopAnimeResponse(
    val data: List<TopAnimeNode>?,
    val paging: Paging?,
)

@Serializable
data class TopAnimeNode(
    val node: TopAnime,
    val ranking: Ranking,
)

@Serializable
data class TopAnime(
    val id: Int,
    val title: String,
    val mainPicture: MainPicture?,
    val alternativeTitles: AlternativeTitles?,
    val startDate: String?,
    val endDate: String?,
    val synopsis: String?,
    val mean: Double?,
    val rank: Int?,
    val popularity: Int?,
    val numScoringUsers: Int?,
    val numEpisodes: Int?,
    val status: String?,
    val mediaType: String?,
    val numFavorites: Int?,
)

@Serializable
data class Ranking(
    val rank: Int,
)

@Serializable
data class Trailer(
    val youtubeId: String?,
    val url: String?,
    val embedUrl: String?,
    val images: TrailerImages?,
)

@Serializable
data class TrailerImages(
    val imageUrl: String?,
    val smallImageUrl: String?,
    val mediumImageUrl: String?,
    val largeImageUrl: String?,
    val maximumImageUrl: String?,
)