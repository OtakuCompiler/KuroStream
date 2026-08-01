package com.kurostream.app.ui.screens.extensions

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.domain.extension.ExtensionMarketplace
import com.kurostream.domain.extension.ExtensionRepository
import com.kurostream.domain.extension.MarketplaceCategory
import com.kurostream.domain.extension.MarketplaceFilters
import com.kurostream.domain.extension.MarketplaceItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val marketplace: ExtensionMarketplace,
    private val extensionRepository: ExtensionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketplaceUiState())
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        loadFeatured()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val categories = marketplace.getCategories()
                _uiState.update { it.copy(categories = categories) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load categories")
            }
        }
    }

    private fun loadFeatured() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val featured = marketplace.getFeatured()
                _uiState.update { it.copy(items = featured, isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load featured")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val results = marketplace.search(query, MarketplaceFilters())
                _uiState.update { it.copy(items = results, isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "Search failed")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun filterByCategory(categoryId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val results = marketplace.search("", MarketplaceFilters())
                _uiState.update { it.copy(items = results, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun install(extension: com.kurostream.domain.extension.UnifiedExtension) {
        viewModelScope.launch {
            try {
                extensionRepository.install(extension)
            } catch (e: Exception) {
                Timber.e(e, "Install failed")
            }
        }
    }
}

@Immutable
data class MarketplaceUiState(
    val items: List<MarketplaceItem> = emptyList(),
    val categories: List<MarketplaceCategory> = emptyList(),
    val isLoading: Boolean = false,
)
