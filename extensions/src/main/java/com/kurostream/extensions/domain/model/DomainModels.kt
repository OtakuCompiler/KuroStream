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

package com.kurostream.extensions.domain.model

data class CatalogItem(
    val id: String,
    val title: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val type: String = "unknown",
    val year: String? = null,
    val rating: Float? = null,
    val genres: List<String> = emptyList(),
    val source: String = "unknown"
)

data class MediaDetail(
    val id: String,
    val title: String,
    val description: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val type: String = "unknown",
    val rating: Float? = null,
    val genres: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val director: List<String> = emptyList(),
    val year: String? = null,
    val runtime: String? = null,
    val episodeCount: Int? = null,
    val status: String? = null,
    val ageRating: String? = null,
    val characters: List<CharacterInfo>? = null,
    val staff: List<StaffInfo>? = null,
    val episodes: List<Episode> = emptyList()
)

data class CharacterInfo(val id: String, val name: String, val imageUrl: String? = null, val role: String? = null)

data class StaffInfo(val id: String, val name: String, val imageUrl: String? = null, val role: String? = null)

data class Episode(val id: String, val title: String, val episodeNumber: Int = 0, val seasonNumber: Int = 0, val thumbnail: String? = null, val overview: String? = null, val released: String? = null)

data class StreamSource(val url: String? = null, val name: String = "Unknown", val quality: String = "Unknown", val headers: Map<String, String> = emptyMap(), val isHls: Boolean = false, val isDash: Boolean = false, val subtitles: List<SubtitleTrack> = emptyList())

data class SubtitleTrack(val url: String, val language: String, val label: String? = null)
