package com.kurostream.data.kurocloud.sync

import com.kurostream.data.kurocloud.KuroCatalogItem
import com.kurostream.data.kurocloud.KuroCatalogResponse
import com.kurostream.data.kurocloud.KuroClaimPurchaseRequest
import com.kurostream.data.kurocloud.KuroMeResponse
import com.kurostream.data.kurocloud.KuroPurchase
import com.kurostream.data.kurocloud.KuroSetActiveSkinRequest
import com.kurostream.data.kurocloud.db.KuroCatalogEntity
import com.kurostream.data.kurocloud.db.KuroCloudDatabase
import com.kurostream.data.kurocloud.db.KuroEntitlementsEntity
import com.kurostream.data.kurocloud.db.KuroPurchaseEntity
import com.kurostream.domain.sync.KuroEntitlementsState
import com.kurostream.domain.sync.KuroSyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KuroSyncRepository @Inject constructor(
    private val database: KuroCloudDatabase,
    private val api: com.kurostream.data.kurocloud.api.KuroApiService,
    private val tokenManager: com.kurostream.data.kurocloud.auth.KuroTokenManager,
) : com.kurostream.domain.sync.KuroSyncRepository {

    private val _syncState = MutableStateFlow<KuroSyncState>(KuroSyncState.Idle)
    override val syncState: Flow<KuroSyncState> = _syncState.asStateFlow()

    private val _entitlements = database.entitlementsDao().observeEntitlements("current_user")
        .map { entity ->
            if (entity == null) KuroEntitlementsState.Empty else mapToState(entity)
        }
        .distinctUntilChanged()

    override val entitlements: Flow<KuroEntitlementsState> = _entitlements

    private val _catalog = database.catalogDao().observeAllCatalog()
        .map { entities ->
            entities.map { mapToCatalogItem(it) }
        }
        .distinctUntilChanged()

    override val catalog: Flow<List<KuroCatalogItem>> = _catalog

    private val _purchases = database.purchaseDao().observePurchases()
        .map { entities ->
            entities.map { mapToPurchase(it) }
        }
        .distinctUntilChanged()

    override val purchases: Flow<List<KuroPurchase>> = _purchases

    private var syncJob: kotlinx.coroutines.Job? = null

    init {
        // Initial sync on app start
        syncAll()
    }

    override fun syncAll() {
        syncJob?.cancel()
        syncJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                _syncState.value = KuroSyncState.Syncing
                awaitFullSync()
                _syncState.value = KuroSyncState.Idle
            } catch (e: Exception) {
                Timber.e(e, "Full sync failed")
                _syncState.value = KuroSyncState.Error(e.message ?: "Sync failed")
            }
        }
    }

    override fun syncEntitlements() {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                awaitSyncEntitlements()
            } catch (e: Exception) {
                Timber.e(e, "Entitlements sync failed")
            }
        }
    }

    override fun syncCatalog() {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                awaitSyncCatalog()
            } catch (e: Exception) {
                Timber.e(e, "Catalog sync failed")
            }
        }
    }

    override fun syncPurchases() {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                awaitSyncPurchases()
            } catch (e: Exception) {
                Timber.e(e, "Purchases sync failed")
            }
        }
    }

    override fun onAppStart() {
        // Triggered on app start
        syncAll()
    }

    override fun onResume() {
        // Triggered on app resume - quick sync
        syncEntitlements()
    }

    override fun onFcmSyncPush() {
        // Triggered from FCM service when sync push received
        syncAll()
    }

    override fun claimFreeItem(itemId: String) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val auth = getAuthHeader()
                val response = api.claimPurchase(auth, KuroClaimPurchaseRequest(itemId))
                if (response.added.skins > 0) {
                    awaitSyncEntitlements()
                    awaitSyncCatalog()
                }
            } catch (e: Exception) {
                Timber.e(e, "Claim free item failed")
            }
        }
    }

    override fun setActiveSkin(skinId: String) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val auth = getAuthHeader()
                api.setActiveSkin(auth, KuroSetActiveSkinRequest(skinId))
                awaitSyncEntitlements()
            } catch (e: Exception) {
                Timber.e(e, "Set active skin failed")
            }
        }
    }

    override fun checkoutPremiumItem(itemId: String): String {
        // Returns web checkout URL
        return "https://kuro-stream-tv.lovable.app/marketplace/$itemId"
    }

    private suspend fun awaitFullSync() {
        awaitSyncEntitlements()
        awaitSyncCatalog()
        awaitSyncPurchases()
    }

    private suspend fun awaitSyncEntitlements() {
        val auth = getAuthHeader()
        val me = api.getMe(auth)
        val entity = KuroEntitlementsEntity(
            userId = "current_user",
            ownedItemIds = kotlinx.serialization.json.Json.encodeToString(me.entitlements.ownedItemIds),
            hasSkinsPass = me.entitlements.hasSkinsPass,
            activeSkinId = me.entitlements.activeSkinId,
            lastSynced = System.currentTimeMillis(),
        )
        database.entitlementsDao().insert(entity)
        Timber.d("Entitlements synced: ${me.entitlements.ownedItemIds.size} items")
    }

    private suspend fun awaitSyncCatalog() {
        val catalog = api.getCatalog()
        val entities = (catalog.skins + catalog.passes).map { item ->
            KuroCatalogEntity(
                itemId = item.id,
                name = item.name,
                description = item.description,
                price = item.price,
                currency = item.currency,
                skinId = item.skinId,
                type = item.type,
                tier = item.tier,
                previewImageUrl = item.previewImageUrl,
                updatedAt = System.currentTimeMillis(),
            )
        }
        database.catalogDao().insertAll(entities)
        Timber.d("Catalog synced: ${entities.size} items")
    }

    private suspend fun awaitSyncPurchases() {
        val auth = getAuthHeader()
        val purchases = api.getPurchases(auth)
        val entities = purchases.map { purchase ->
            KuroPurchaseEntity(
                id = "${purchase.itemId}_${purchase.createdAt.hashCode()}",
                itemId = purchase.itemId,
                amount = purchase.amount,
                status = purchase.status,
                createdAt = parseTimestamp(purchase.createdAt),
            )
        }
        database.purchaseDao().insertAll(entities)
        Timber.d("Purchases synced: ${entities.size} items")
    }

    private fun getAuthHeader(): String {
        val token = tokenManager.accessToken ?: throw IllegalStateException("No access token")
        return "Bearer $token"
    }

    private fun mapToState(entity: KuroEntitlementsEntity): KuroEntitlementsState {
        val ownedItemIds = kotlinx.serialization.json.Json.decodeFromString<List<String>>(entity.ownedItemIds)
        return KuroEntitlementsState.Loaded(
            ownedItemIds = ownedItemIds,
            hasSkinsPass = entity.hasSkinsPass,
            activeSkinId = entity.activeSkinId,
        )
    }

    private fun mapToCatalogItem(entity: KuroCatalogEntity): KuroCatalogItem {
        return KuroCatalogItem(
            id = entity.itemId,
            name = entity.name,
            description = entity.description,
            price = entity.price,
            currency = entity.currency,
            skinId = entity.skinId,
            type = entity.type,
            tier = entity.tier,
            previewImageUrl = entity.previewImageUrl,
        )
    }

    private fun mapToPurchase(entity: KuroPurchaseEntity): KuroPurchase {
        return KuroPurchase(
            itemId = entity.itemId,
            amount = entity.amount,
            status = entity.status,
            createdAt = entity.createdAt.toString(),
        )
    }

    private fun parseTimestamp(isoString: String): Long {
        return try {
            java.time.Instant.parse(isoString).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}