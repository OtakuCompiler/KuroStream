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

package com.kurostream.data.anistream.subtitle

import java.io.File

interface SubtitleProvider {
    val name: String
    suspend fun searchSubtitles(query: SubtitleSearchQuery): Result<List<SubtitleResult>>
    suspend fun downloadSubtitle(subtitleId: String): Result<File>
}

data class SubtitleSearchQuery(
    val query: String? = null,
    val imdbId: String? = null,
    val tmdbId: Int? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val languages: List<String> = listOf("en"),
    val movieHash: String? = null,
    val movieByteSize: Long? = null
)

data class SubtitleResult(
    val id: String,
    val language: String,
    val languageName: String,
    val filename: String,
    val format: String,
    val downloadCount: Int,
    val isHearingImpaired: Boolean = false,
    val rating: Float = 0f,
    val uploadDate: String? = null
)
