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

package com.kurostream.extensions.search

import com.kurostream.extensions.domain.model.CatalogItem
import com.kurostream.extensions.kitsu.KitsuRepository
import com.kurostream.extensions.stremio.StremioAddonManager
import com.kurostream.extensions.stremio.StremioAddonRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnifiedSearchRepository @Inject constructor(
    private val kitsuRepository: KitsuRepository,
    private val stremioRepository: StremioAddonRepository,
    private val stremioAddonManager: StremioAddonManager,
    private val localSearchSource: LocalSearchSource,
    private val demoSearchSource: DemoSearchSource
) {

    fun searchAll(query: String, includeSources: Set<SearchSource> = SearchSource.ALL): Flow<SearchResult> = flow {
        emit(SearchResult.Loading)

        val results = mutableListOf<SearchSection>()

        coroutineScope {
            val jobs = mutableListOf<kotlinx.coroutines.Deferred<SearchSection?>>()

            if (SearchSource.KITSU in includeSources) {
                jobs.add(async {
                    kitsuRepository.searchAnime(query).fold(
                        onSuccess = { SearchSection("Kitsu", "kitsu", it) },
                        onFailure = { null }
                    )
                })
            }

            if (SearchSource.STREMIO in includeSources) {
                jobs.add(async {
                    val stremioResults = searchStremioAddons(query)
                    if (stremioResults.isNotEmpty()) {
                        SearchSection("Stremio Addons", "stremio", stremioResults)
                    } else null
                })
            }

            if (SearchSource.LOCAL in includeSources) {
                jobs.add(async {
                    localSearchSource.search(query).first().fold(
                        onSuccess = { SearchSection("Local Library", "local", it) },
                        onFailure = { null }
                    )
                })
            }

            if (SearchSource.DEMO in includeSources) {
                jobs.add(async {
                    demoSearchSource.search(query).first().fold(
                        onSuccess = { SearchSection("Demo Content", "demo", it) },
                        onFailure = { null }
                    )
                })
            }

            jobs.awaitAll().filterNotNull().forEach { section ->
                results.add(section)
                emit(SearchResult.Partial(results.toList()))
            }
        }

        emit(SearchResult.Complete(results.toList()))
    }

    private suspend fun searchStremioAddons(query: String): List<CatalogItem> = coroutineScope {
        val enabledAddons = stremioAddonManager.getEnabledAddons()

        enabledAddons.flatMap { addon ->
            listOf(
                async {
                    try {
                        stremioRepository.fetchCatalog(addon.url, "movie", "top", "search=$query")
                            .first()
                            .getOrDefault(emptyList())
                    } catch (_: Exception) { emptyList() }
                },
                async {
                    try {
                        stremioRepository.fetchCatalog(addon.url, "series", "top", "search=$query")
                            .first()
                            .getOrDefault(emptyList())
                    } catch (_: Exception) { emptyList() }
                }
            )
        }.awaitAll().flatten()
    }

    fun quickSearch(query: String): Flow<List<CatalogItem>> = flow {
        val result = kitsuRepository.searchAnime(query, limit = 5)
        result.onSuccess { emit(it) }
    }
}

enum class SearchSource {
    KITSU, STREMIO, LOCAL, DEMO;

    companion object {
        val ALL = setOf(KITSU, STREMIO, LOCAL, DEMO)
    }
}

data class SearchSection(
    val title: String,
    val sourceId: String,
    val items: List<CatalogItem>
)

sealed class SearchResult {
    object Loading : SearchResult()
    data class Partial(val sections: List<SearchSection>) : SearchResult()
    data class Complete(val sections: List<SearchSection>) : SearchResult()
    data class Error(val message: String) : SearchResult()
}
