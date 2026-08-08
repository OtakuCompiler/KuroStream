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

package com.kurostream.app.ui.screens.addons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.data.local.dao.AddonDao
import com.kurostream.data.local.entity.AddonConfigEntity
import com.kurostream.domain.extension.UnifiedExtension
import com.kurostream.extensions.stremio.StremioAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import androidx.compose.runtime.Immutable

/**
 * UI State for the Addons screen.
 */
@Immutable
data class AddonsUiState(
    val installedAddons: List<AddonItem> = emptyList(),
    val availableAddons: List<AddonItem> = emptyList(),
    val selectedCategory: AddonCategory = AddonCategory.INSTALLED,
    val isLoading: Boolean = false,
    val error: String? = null,
    val installProgress: String? = null,
)

/**
 * ViewModel for AddonsScreen.
 * Manages loading, installing, and uninstalling add-ons,
 * including installing Stremio add-ons by manifest URL.
 */
@HiltViewModel
class AddonsViewModel @Inject constructor(
    private val addonDao: AddonDao,
    private val stremioAdapter: StremioAdapter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddonsUiState())
    val uiState: StateFlow<AddonsUiState> = _uiState.asStateFlow()

    init {
        loadAddons()
    }

    /**
     * Switch the active category tab.
     */
    fun selectCategory(category: com.kurostream.app.ui.screens.addons.AddonCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    /**
     * Load add-ons from database and refresh available add-ons list.
     */
    fun loadAddons() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val installedEntities = addonDao.getAll()
                val installed = installedEntities.map { it.toAddonItem(isInstalled = true) }

                val available = getAvailableAddons().filter { available ->
                    installed.none { it.id == available.id }
                }

                _uiState.update {
                    it.copy(
                        installedAddons = installed,
                        availableAddons = available,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load addons")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load add-ons",
                    )
                }
            }
        }
    }

    /**
     * Install a Stremio add-on by its manifest URL.
     *
     * Fetches the manifest, converts to a UnifiedExtension, and persists it.
     * Returns Result so the caller can surface success/failure to the user.
     */
    fun installByManifestUrl(manifestUrl: String, onResult: (Result<UnifiedExtension>) -> Unit = {}) {
        val url = manifestUrl.trim()
        if (url.isBlank()) {
            val err = "Manifest URL cannot be empty"
            _uiState.update { it.copy(error = err) }
            onResult(Result.failure(IllegalArgumentException(err)))
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            val err = "Manifest URL must start with http:// or https://"
            _uiState.update { it.copy(error = err) }
            onResult(Result.failure(IllegalArgumentException(err)))
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, installProgress = "Fetching manifest…") }
            try {
                val manifestResult = stremioAdapter.fetchManifest(url)
                val manifest = manifestResult.getOrThrow()
                val extension = stremioAdapter.toUnifiedExtension(manifest, url)

                val entity = AddonConfigEntity(
                    extensionId = extension.id,
                    configJson = stremioAdapter.encodeConfig(extension),
                    isEnabled = true,
                )
                addonDao.insert(entity)
                loadAddons()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        installProgress = "Installed ${extension.name}",
                        error = null,
                    )
                }
                onResult(Result.success(extension))
            } catch (e: Exception) {
                Timber.e(e, "Failed to install addon from $url")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        installProgress = null,
                        error = "Install failed: ${e.message ?: e.javaClass.simpleName}",
                    )
                }
                onResult(Result.failure(e))
            }
        }
    }

    /**
     * Install an add-on from the marketplace catalog.
     */
    fun installAddon(addonId: String) {
        viewModelScope.launch {
            try {
                val addon = findAddonById(addonId) ?: return@launch
                val entity = AddonConfigEntity(
                    extensionId = addonId,
                    configJson = "{}",
                    isEnabled = true,
                )
                addonDao.insert(entity)
                loadAddons()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to install add-on: ${e.message}")
                }
            }
        }
    }

    /**
     * Uninstall an add-on.
     */
    fun uninstallAddon(addonId: String) {
        viewModelScope.launch {
            try {
                val entity = addonDao.getByExtensionId(addonId) ?: return@launch
                addonDao.deleteByExtensionId(addonId)
                loadAddons()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to uninstall add-on: ${e.message}")
                }
            }
        }
    }

    /**
     * Configure an installed add-on.
     * Currently a no-op, would open add-on settings in production.
     */
    fun configureAddon(addonId: String) {
        viewModelScope.launch {
            try {
                val entity = addonDao.getByExtensionId(addonId) ?: return@launch
                // Mark as configured by touching the config row.
                addonDao.insert(entity.copy(configJson = entity.configJson.ifBlank { "{}" }))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to configure add-on") }
            }
        }
    }

    /**
     * Clear transient error/progress messages.
     */
    fun clearMessages() {
        _uiState.update { it.copy(error = null, installProgress = null) }
    }

    private suspend fun findAddonById(id: String): AddonItem? {
        val available = getAvailableAddons()
        return available.firstOrNull { it.id == id }
    }

    private suspend fun getAvailableAddons(): List<AddonItem> = listOf(
        AddonItem(
            id = "stremio_official",
            name = "Stremio Official Catalogs",
            description = "Official Stremio catalogs for movies, series, and anime",
            iconUrl = "https://www.stremio.com/website/stremio-logo-small.png",
            author = "Stremio",
            category = com.kurostream.app.ui.screens.addons.AddonCategory.STREMIO,
        ),
        AddonItem(
            id = "tmdb_popular",
            name = "TMDB Popular",
            description = "Trending movies and TV shows from TMDB",
            iconUrl = "",
            author = "Community",
            category = com.kurostream.app.ui.screens.addons.AddonCategory.STREMIO,
        ),
        AddonItem(
            id = "anilist",
            name = "AniList",
            description = "Anime catalog from AniList with seasonal shows",
            iconUrl = "",
            author = "AniList",
            category = com.kurostream.app.ui.screens.addons.AddonCategory.STREMIO,
        ),
        AddonItem(
            id = "torrentio",
            name = "Torrentio",
            description = "Aggregates streams from public torrent providers (paste your own URL)",
            iconUrl = "",
            author = "Community",
            category = com.kurostream.app.ui.screens.addons.AddonCategory.STREMIO,
        ),
        AddonItem(
            id = "opensubtitles",
            name = "OpenSubtitles v3",
            description = "Subtitles in 50+ languages",
            iconUrl = "",
            author = "OpenSubtitles",
            category = com.kurostream.app.ui.screens.addons.AddonCategory.STREMIO,
        ),
    )
}
