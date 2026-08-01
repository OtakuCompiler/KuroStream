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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the Addons screen.
 */
import androidx.compose.runtime.Immutable

@Immutable
data class AddonsUiState(
    val installedAddons: List<AddonItem> = emptyList(),
    val availableAddons: List<AddonItem> = emptyList(),
    val selectedCategory: AddonCategory = AddonCategory.INSTALLED,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * ViewModel for AddonsScreen.
 * Manages loading, installing, and uninstalling add-ons.
 */
@HiltViewModel
class AddonsViewModel @Inject constructor(
    private val addonDao: AddonDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddonsUiState())
    val uiState: StateFlow<AddonsUiState> = _uiState.asStateFlow()

    init {
        loadAddons()
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

                // Available add-ons - in production this would come from a remote source
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
     * Refresh add-ons - called by pull-to-refresh or retry.
     */
    fun refreshAddons() {
        loadAddons()
    }

    /**
     * Install an add-on.
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
        // In production, this would navigate to add-on configuration screen
    }

    /**
     * Select a category tab.
     */
    fun selectCategory(category: AddonCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    /**
     * Clear any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun findAddonById(addonId: String): AddonItem? {
        return _uiState.value.installedAddons.find { it.id == addonId }
            ?: _uiState.value.availableAddons.find { it.id == addonId }
    }

    /**
     * Get list of available add-ons.
     * In production, this would come from a remote repository.
     */
    private fun getAvailableAddons(): List<AddonItem> {
        return listOf(
            AddonItem(
                id = "stremio_torrentio",
                name = "Torrentio",
                description = "Real-Debrid torrent streaming with cached Debrid links",
                category = AddonCategory.STREMIO,
                isInstalled = false,
                version = "2.0.0",
                author = "lZv4",
            ),
            AddonItem(
                id = "stremio_community",
                name = "Stremio Addons",
                description = "Watch content from Stremio catalog",
                category = AddonCategory.STREMIO,
                isInstalled = false,
                version = "1.0.0",
                author = "Stremio",
            ),
            AddonItem(
                id = "kitsu_anime",
                name = "Kitsu Anime",
                description = "Anime from Kitsu API with metadata and images",
                category = AddonCategory.KITSU,
                isInstalled = false,
                version = "1.5.0",
                author = "KuroStream",
            ),
            AddonItem(
                id = "kitsu_trending",
                name = "Kitsu Trending",
                description = "Trending anime from Kitsu",
                category = AddonCategory.KITSU,
                isInstalled = false,
                version = "1.2.0",
                author = "KuroStream",
            ),
            AddonItem(
                id = "community_notes",
                name = "Community Notes",
                description = "Community metadata and notes for content",
                category = AddonCategory.COMMUNITY,
                isInstalled = false,
                version = "1.0.0",
                author = "Community",
            ),
            AddonItem(
                id = "community_custom",
                name = "Custom Repository",
                description = "Add your own content sources",
                category = AddonCategory.COMMUNITY,
                isInstalled = false,
                version = "0.9.0",
                author = "KuroStream",
            ),
        )
    }

    private fun AddonConfigEntity.toAddonItem(isInstalled: Boolean): AddonItem {
        // Parse category from extension ID or default to STREMIO
        val category = when {
            extensionId.startsWith("kitsu_") -> AddonCategory.KITSU
            extensionId.startsWith("community_") -> AddonCategory.COMMUNITY
            else -> AddonCategory.STREMIO
        }
        return AddonItem(
            id = extensionId,
            name = extensionId.replace("_", " ").replaceFirstChar { it.uppercase() },
            description = "Installed add-on",
            category = category,
            isInstalled = isInstalled,
            version = "1.0.0",
            author = "Unknown",
        )
    }
}