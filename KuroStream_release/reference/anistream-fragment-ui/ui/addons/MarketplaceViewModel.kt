package com.kurostream.legacyui.anistream.ui.addons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.data.anistream.addons.AddonRepository
import com.kurostream.data.anistream.addons.MarketplaceApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val addonRepository: AddonRepository,
    private val marketplaceApi: MarketplaceApi
) : ViewModel() {

    private val _marketplaceItems = MutableStateFlow<List<MarketplaceAddon>>(emptyList())
    val marketplaceItems: StateFlow<List<MarketplaceAddon>> = _marketplaceItems.asStateFlow()

    private val _events = MutableSharedFlow<MarketplaceEvent>()
    val events: SharedFlow<MarketplaceEvent> = _events.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadMarketplace() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val items = marketplaceApi.getAddons()
                _marketplaceItems.value = items
            } catch (e: Exception) {
                _events.emit(MarketplaceEvent.InstallError(e.message ?: "Failed to load marketplace"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun installAddon(addon: MarketplaceAddon) {
        viewModelScope.launch {
            try {
                val installed = addonRepository.installAddon(addon.toAddon())
                installed.fold(
                    onSuccess = {
                        _events.emit(MarketplaceEvent.InstallSuccess(addon.name))
                    },
                    onFailure = { e ->
                        _events.emit(MarketplaceEvent.InstallError(e.message ?: "Unknown error"))
                    }
                )
            } catch (e: Exception) {
                _events.emit(MarketplaceEvent.InstallError(e.message ?: "Installation failed"))
            }
        }
    }

    fun onAddonFocused(addon: MarketplaceAddon) {
        // Preload details
    }
}

sealed class MarketplaceEvent {
    data class InstallSuccess(val addonName: String) : MarketplaceEvent()
    data class InstallError(val message: String) : MarketplaceEvent()
    data class ShowDetails(val addon: MarketplaceAddon) : MarketplaceEvent()
}
