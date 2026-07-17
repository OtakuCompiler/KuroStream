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

package com.kurostream.data.remote.dto.tmdb

import kotlinx.serialization.Serializable

@Serializable
data class TmdbDtos

@Serializable
data class TvShow(
    val id: Int,
    val name: String,
    val originalName: String,
    val originalLanguage: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val firstAirDate: String?,
    val lastAirDate: String?,
    val numberOfEpisodes: Int?,
    val numberOfSeasons: Int?,
    val episodeRunTime: List<Int> = emptyList(),
    val status: String,
    val genres: List<Genre> = emptyList(),
    val networks: List<Network> = emptyList(),
    val voteAverage: Double?,
    val voteCount: Int?,
    val popularity: Double?,
    val originCountry: List<String> = emptyList(),
    val externalIds: ExternalIds?,
    val videos: Videos?,
    val contentRatings: ContentRatings?,
)

@Serializable
data class Genre(
    val id: Int,
    val name: String,
)

@Serializable
data class Network(
    val id: Int,
    val name: String,
    val logoPath: String?,
    val originCountry: String,
)

@Serializable
data class ExternalIds(
    val imdbId: String?,
    val facebookId: String?,
    val instagramId: String?,
    val twitterId: String?,
    val youtubeId: String?,
    val tiktokId: String?,
)

@Serializable
data class Videos(
    val results: List<Video> = emptyList(),
)

@Serializable
data class Video(
    val id: String,
    val key: String,
    val name: String,
    val site: String,
    val size: Int,
    val type: String,
    val official: Boolean,
    val publishedAt: String,
)

@Serializable
data class ContentRatings(
    val results: List<ContentRating> = emptyList(),
)

@Serializable
data class ContentRating(
    val rating: String,
    val iso3166_1: String,
    val descriptors: List<String> = emptyList(),
)

@Serializable
data class SearchResponse(
    val page: Int,
    val results: List<TvShow> = emptyList(),
    val totalPages: Int,
    val totalResults: Int,
)

@Serializable
data class DiscoverResponse(
    val page: Int,
    val results: List<TvShow> = emptyList(),
    val totalPages: Int,
    val totalResults: Int,
)

@Serializable
data class TrendingResponse(
    val page: Int,
    val results: List<TvShow> = emptyList(),
    val totalPages: Int,
    val totalResults: Int,
)

@Serializable
data class ExternalIdResponse(
    val id: Int?,
    val imdb_id: String?,
    val facebook_id: String?,
    val instagram_id: String?,
    val twitter_id: String?,
    val youtube_id: String?,
    val tiktok_id: String?,
)