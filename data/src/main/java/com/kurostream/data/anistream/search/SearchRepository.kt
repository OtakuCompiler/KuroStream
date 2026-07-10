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

import com.kurostream.data.anistream.local.AnimeDao
import com.kurostream.data.anistream.model.AnimeItem
import com.kurostream.legacyui.anistream.util.FuzzyMatcher
import com.kurostream.legacyui.anistream.util.TypoCorrector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val animeDao: AnimeDao,
    private val apiService: SearchApiService,
    private val fuzzyMatcher: FuzzyMatcher,
    private val typoCorrector: TypoCorrector
) {

    suspend fun search(
        query: String,
        filters: List<SearchFilter>,
        enableFuzzy: Boolean = true,
        enableTypoCorrection: Boolean = true
    ): List<AnimeItem> = withContext(Dispatchers.IO) {
        val correctedQuery = if (enableTypoCorrection) {
            typoCorrector.correct(query)
        } else query

        var results = animeDao.searchByTitle("%$correctedQuery%")
        results = applyFilters(results, filters)

        if (results.size < 5) {
            try {
                val remoteResults = apiService.search(correctedQuery)
                results = (results + remoteResults).distinctBy { it.id }
            } catch (e: Exception) {
                // Use local only
            }
        }

        if (enableFuzzy) {
            results = results.sortedByDescending {
                fuzzyMatcher.score(it.title, correctedQuery)
            }
        }

        results
    }

    suspend fun fuzzySearch(query: String): List<AnimeItem> = withContext(Dispatchers.IO) {
        val allAnime = animeDao.getAllAnime()
        allAnime.filter {
            fuzzyMatcher.score(it.title, query) > 0.3f
        }.sortedByDescending {
            fuzzyMatcher.score(it.title, query)
        }
    }

    suspend fun getSuggestions(partial: String): List<String> = withContext(Dispatchers.IO) {
        if (partial.length < 2) return@withContext emptyList()
        animeDao.getTitleSuggestions("%$partial%", limit = 5)
            .map { it.title }
    }

    private fun applyFilters(
        items: List<AnimeItem>,
        filters: List<SearchFilter>
    ): List<AnimeItem> {
        if (filters.contains(SearchFilter.ALL)) return items
        return items.filter { item ->
            filters.any { filter ->
                when (filter) {
                    SearchFilter.MOVIES -> item.type == "Movie"
                    SearchFilter.SERIES -> item.type == "TV"
                    SearchFilter.OVA -> item.type == "OVA"
                    SearchFilter.SPECIAL -> item.type == "Special"
                    else -> true
                }
            }
        }
    }
}

enum class SearchFilter {
    ALL, MOVIES, SERIES, OVA, SPECIAL
