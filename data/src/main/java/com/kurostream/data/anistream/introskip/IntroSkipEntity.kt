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

package com.kurostream.data.anistream.introskip

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "intro_skips",
    indices = [Index(value = ["animeId", "episodeNumber"], unique = true)]
)
data class IntroSkipEntity(
    @PrimaryKey
    val id: String = "",
    val animeId: String,
    val episodeNumber: Int,
    val introStartSec: Double? = null,
    val introEndSec: Double? = null,
    val outroStartSec: Double? = null,
    val outroEndSec: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val source: SkipSource = SkipSource.USER_MARKED
) {
    constructor(animeId: String, episodeNumber: Int) : this(
        id = "${animeId}_${episodeNumber}",
        animeId = animeId,
        episodeNumber = episodeNumber
    )
}

enum class SkipSource {
    USER_MARKED, ANISKIP_API, IMPORTED
}
