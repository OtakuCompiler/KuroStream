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

package com.kurostream.domain.entity

import kotlinx.serialization.Serializable

@Serializable
data class SubtitleCandidate(
    val id: String,
    val mediaId: String,
    val languageCode: String,
    val languageName: String,
    val label: String? = null,
    val format: SubtitleFormat,
    val sourceUrl: String? = null,
    val isDefault: Boolean = false,
    val isHearingImpaired: Boolean = false,
    val providerId: String
)

enum class SubtitleFormat { SRT, ASS, VTT, TTML, PGS, UNKNOWN }
