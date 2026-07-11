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

package com.kurostream.data.anistream.search

import com.kurostream.data.anistream.model.AnimeItem
import com.kurostream.legacyui.anistream.ui.kids.KidsModeManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps search results with kids mode content filtering.
 */
@Singleton
class KidsContentFilter @Inject constructor(
    private val kidsModeManager: KidsModeManager
) {

    suspend fun filterSearchResults(results: List<AnimeItem>): List<AnimeItem> {
        return kidsModeManager.filterContent(results)
    }

    suspend fun filterSearchResultsFlow(resultsFlow: Flow<List<AnimeItem>>): Flow<List<AnimeItem>> {
        return resultsFlow.map { results ->
            kidsModeManager.filterContent(results)
        }
    }

    suspend fun canPlayContent(item: AnimeItem): Boolean {
        return kidsModeManager.isContentAppropriate(item)
    }

    fun getBlockedReason(item: AnimeItem): String? {
        val reasons = mutableListOf<String>()

        if (item.genres.any { it in setOf("Hentai", "Ecchi", "Erotica") }) {
            reasons.add("Adult content")
        }
        if (item.genres.any { it in setOf("Horror", "Gore") }) {
            reasons.add("Violent/scary content")
        }
        if (item.rating in setOf("R", "R+", "Rx", "NC-17")) {
            reasons.add("Mature rating (${item.rating})")
        }

        return if (reasons.isNotEmpty()) reasons.joinToString(", ") else null
    }
}
