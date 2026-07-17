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

package com.kurostream.data.remote.dto.tvdb

import kotlinx.serialization.Serializable

@Serializable
data class TvdbDtos(val placeholder: String = "")

@Serializable
data class Series(
    val id: Int,
    val name: String,
    val slug: String? = null,
    val overview: String? = null,
    val status: String? = null,
    val imageUrl: String? = null,
    val bannerUrl: String? = null,
    val firstAired: String? = null,
    val lastAired: String? = null,
    val network: String? = null,
    val scoreCount: Int? = null,
    val siteRating: Double? = null,
    val runtime: Int? = null,
    val episodeCount: Int? = null,
    val contentRating: String? = null,
    val type: String? = null,
    val genres: List<Genre>? = null,
    val aliases: List<Alias>? = null,
)

@Serializable
data class Genre(
    val id: Int,
    val name: String,
)

@Serializable
data class Alias(
    val language: String? = null,
    val name: String,
)

@Serializable
data class SeriesResponse(
    val data: Series? = null,
)

@Serializable
data class SearchResponse(
    val data: List<Series> = emptyList(),
)

@Serializable
data class EpisodesResponse(
    val data: List<Episode> = emptyList(),
)

@Serializable
data class Episode(
    val id: Int,
    val name: String? = null,
    val episodeNumber: Int? = null,
    val seasonNumber: Int? = null,
    val overview: String? = null,
)
