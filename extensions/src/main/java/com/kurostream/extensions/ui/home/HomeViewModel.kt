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

package com.kurostream.extensions.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.extensions.domain.model.CatalogItem
import com.kurostream.extensions.kitsu.KitsuRepository
import com.kurostream.extensions.stremio.StremioAddonManager
import com.kurostream.extensions.stremio.StremioAddonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val kitsuRepository: KitsuRepository,
    private val stremioAddonRepository: StremioAddonRepository,
    private val stremioAddonManager: StremioAddonManager
) : ViewModel() {

    private val _homeRows = MutableStateFlow<List<HomeRow>>(emptyList())
    val homeRows: StateFlow<List<HomeRow>> = _homeRows.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadHomeContent()
    }

    fun loadHomeContent() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val rows = mutableListOf<HomeRow>()

                val trendingDeferred = async {
                    kitsuRepository.getTrendingAnime(15).first()
                        .getOrNull()?.let { HomeRow("Trending Now", "kitsu_trending", it) }
                }

                val (season, year) = kitsuRepository.getCurrentSeason()
                val seasonalDeferred = async {
                    kitsuRepository.getSeasonalAnime(season, year, 15).first()
                        .getOrNull()?.let { HomeRow("${season.replaceFirstChar { it.uppercase() }} $year", "kitsu_seasonal", it) }
                }

                val popularDeferred = async {
                    kitsuRepository.getPopularAnime(15).first()
                        .getOrNull()?.let { HomeRow("Most Popular", "kitsu_popular", it) }
                }

                val stremioDeferred = async { loadStremioRows() }

                val results = awaitAll(trendingDeferred, seasonalDeferred, popularDeferred, stremioDeferred)
                results.filterNotNull().forEach { rowOrRows ->
                    when (rowOrRows) {
                        is HomeRow -> rows.add(rowOrRows)
                        is List<*> -> rows.addAll(rowOrRows.filterIsInstance<HomeRow>())
                    }
                }

                _homeRows.value = rows
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadStremioRows(): List<HomeRow> {
        val rows = mutableListOf<HomeRow>()
        val enabledAddons = stremioAddonManager.getEnabledAddons()

        for (addon in enabledAddons.take(3)) {
            try {
                val manifest = stremioAddonRepository.fetchManifest(addon.url).getOrNull() ?: continue

                manifest.catalogs.filter { it.type in listOf("movie", "series", "anime") }
                    .take(2)
                    .forEach { catalog ->
                        try {
                            stremioAddonRepository.fetchCatalog(addon.url, catalog.type, catalog.id)
                                .first()
                                .getOrNull()
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { items ->
                                    rows.add(HomeRow(
                                        title = catalog.name ?: "${manifest.name} - ${catalog.id}",
                                        rowId = "stremio_${manifest.id}_${catalog.id}",
                                        items = items
                                    ))
                                }
                        } catch (_: Exception) {}
                    }
            } catch (_: Exception) {}
        }
        return rows
    }
}

data class HomeRow(
    val title: String,
    val rowId: String,
    val items: List<CatalogItem>
)
