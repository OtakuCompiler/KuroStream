package com.kurostream.marketplace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.data.kurocloud.KuroCatalogItem
import com.kurostream.domain.sync.KuroEntitlementsState
import com.kurostream.domain.sync.KuroSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val syncRepository: KuroSyncRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketplaceUiState())
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                syncRepository.catalog,
                syncRepository.entitlements,
            ) { catalog, entitlements ->
                val ownedItemIds = when (entitlements) {
                    is KuroEntitlementsState.Loaded -> entitlements.ownedItemIds.toSet()
                    else -> emptySet<String>()
                }
                val hasSkinsPass = when (entitlements) {
                    is KuroEntitlementsState.Loaded -> entitlements.hasSkinsPass
                    else -> false
                }
                val activeSkinId = when (entitlements) {
                    is KuroEntitlementsState.Loaded -> entitlements.activeSkinId
                    else -> null
                }

                val items = catalog.map { item ->
                    MarketplaceItem(
                        item = item,
                        isOwned = item.id in ownedItemIds || (hasSkinsPass && item.type == "skin"),
                        isActive = item.skinId == activeSkinId,
                        canPurchase = item.price > 0 && item.id !in ownedItemIds,
                    )
                }

                MarketplaceUiState(
                    items = items,
                    hasSkinsPass = hasSkinsPass,
                    activeSkinId = activeSkinId,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun claimFreeItem(itemId: String) {
        viewModelScope.launch {
            syncRepository.claimFreeItem(itemId)
        }
    }

    fun setActiveSkin(skinId: String) {
        viewModelScope.launch {
            syncRepository.setActiveSkin(skinId)
        }
    }

    fun getCheckoutUrl(itemId: String): String {
        return syncRepository.checkoutPremiumItem(itemId)
    }

    fun refresh() {
        viewModelScope.launch {
            syncRepository.syncAll()
        }
    }
}

data class MarketplaceUiState(
    val items: List<MarketplaceItem> = emptyList(),
    val hasSkinsPass: Boolean = false,
    val activeSkinId: String? = null,
)

data class MarketplaceItem(
    val item: com.kurostream.data.kurocloud.KuroCatalogItem,
    val isOwned: Boolean,
    val isActive: Boolean,
    val canPurchase: Boolean,
)