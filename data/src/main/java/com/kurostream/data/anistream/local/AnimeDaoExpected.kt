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

package com.kurostream.data.anistream.local

import androidx.room.Dao
import androidx.room.Query
import com.kurostream.data.anistream.model.AnimeItem
import kotlinx.coroutines.flow.Flow

/**
 * Expected methods from Phase 1-40 AnimeDao that SearchRepository depends on.
 * These should already exist in your project from earlier phases.
 */
interface AnimeDaoExpectedMethods {
    @Query("SELECT * FROM anime WHERE title LIKE :pattern")
    suspend fun searchByTitle(pattern: String): List<AnimeItem>

    @Query("SELECT * FROM anime")
    suspend fun getAllAnime(): List<AnimeItem>

    @Query("SELECT * FROM anime WHERE title LIKE :pattern LIMIT :limit")
    suspend fun getTitleSuggestions(pattern: String, limit: Int): List<AnimeSuggestion>
}

data class AnimeSuggestion(
    val title: String
)
