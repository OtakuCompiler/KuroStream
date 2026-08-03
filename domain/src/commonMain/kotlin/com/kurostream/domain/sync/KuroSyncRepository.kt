package com.kurostream.domain.sync

import com.kurostream.domain.kurocloud.KuroCatalogItem
import com.kurostream.domain.kurocloud.KuroPurchase
import com.kurostream.domain.kurocloud.KuroEntitlements
import kotlinx.coroutines.flow.Flow

interface KuroSyncRepository {
    val entitlements: Flow<KuroEntitlementsState>
    val catalog: Flow<List<KuroCatalogItem>>
    val purchases: Flow<List<KuroPurchase>>
    val syncState: Flow<KuroSyncState>

    fun syncAll()
    fun syncEntitlements()
    fun syncCatalog()
    fun syncPurchases()
    fun claimFreeItem(itemId: String)
    fun setActiveSkin(skinId: String)
    fun checkoutPremiumItem(itemId: String): String
    fun onAppStart()
    fun onResume()
    fun onFcmSyncPush()
}

sealed class KuroSyncState {
    object Idle : KuroSyncState()
    object Syncing : KuroSyncState()
    data class Error(val message: String) : KuroSyncState()
}

sealed class KuroEntitlementsState {
    object Empty : KuroEntitlementsState()
    object Loading : KuroEntitlementsState()
    data class Loaded(
        val ownedItemIds: List<String> = emptyList(),
        val hasSkinsPass: Boolean = false,
        val activeSkinId: String? = null,
    ) : KuroEntitlementsState()
    data class Error(val message: String) : KuroEntitlementsState()

    fun toEntitlements(): KuroEntitlements = when (this) {
        is Empty -> KuroEntitlements(ownedItemIds = emptyList(), hasSkinsPass = false, activeSkinId = null)
        is Loading -> KuroEntitlements(ownedItemIds = emptyList(), hasSkinsPass = false, activeSkinId = null)
        is Loaded -> KuroEntitlements(ownedItemIds = ownedItemIds, hasSkinsPass = hasSkinsPass, activeSkinId = activeSkinId)
        is Error -> KuroEntitlements(ownedItemIds = emptyList(), hasSkinsPass = false, activeSkinId = null)
    }
}