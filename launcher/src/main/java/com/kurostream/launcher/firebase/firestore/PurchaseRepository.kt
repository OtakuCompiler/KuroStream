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

package com.kurostream.launcher.firebase.firestore

import com.kurostream.launcher.data.local.PurchaseDao
import com.kurostream.launcher.data.local.entity.PurchaseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseRepository @Inject constructor(
    private val firestoreSyncManager: FirestoreSyncManager,
    private val purchaseDao: PurchaseDao
) {

    /**
     * Record a new purchase and sync to Firestore
     */
    suspend fun recordPurchase(userId: String, addonId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // Save locally first
                purchaseDao.insert(
                    PurchaseEntity(
                        id = "${userId}_$addonId",
                        userId = userId,
                        addonId = addonId,
                        purchasedAt = System.currentTimeMillis(),
                        synced = false
                    )
                )

                // Sync to Firestore
                val result = firestoreSyncManager.addPurchase(userId, addonId)

                if (result.isSuccess) {
                    purchaseDao.markAsSynced("${userId}_$addonId")
                }

                result
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Get all purchased addons for a user (local + remote)
     */
    suspend fun getPurchasedAddons(userId: String): List<String> =
        withContext(Dispatchers.IO) {
            // Get local purchases
            val localPurchases = purchaseDao.getPurchasesForUser(userId).map { it.addonId }

            // Try to sync with Firestore
            try {
                val remoteResult = firestoreSyncManager.getPurchases(userId)
                if (remoteResult.isSuccess) {
                    val remotePurchases = remoteResult.getOrDefault(emptyList())

                    // Merge and update local
                    val merged = (localPurchases + remotePurchases).distinct()

                    // Update local DB with any missing remote purchases
                    remotePurchases.forEach { addonId ->
                        if (addonId !in localPurchases) {
                            purchaseDao.insert(
                                PurchaseEntity(
                                    id = "${userId}_$addonId",
                                    userId = userId,
                                    addonId = addonId,
                                    purchasedAt = System.currentTimeMillis(),
                                    synced = true
                                )
                            )
                        }
                    }

                    merged
                } else {
                    localPurchases
                }
            } catch (e: Exception) {
                localPurchases
            }
        }

    /**
     * Check if user has purchased a specific addon
     */
    suspend fun hasPurchase(userId: String, addonId: String): Boolean {
        return purchaseDao.getPurchase("${userId}_$addonId") != null
    }

    /**
     * Sync all unsynced local purchases to Firestore
     */
    suspend fun syncPendingPurchases(userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val unsynced = purchaseDao.getUnsyncedPurchases(userId)

                unsynced.forEach { purchase ->
                    firestoreSyncManager.addPurchase(userId, purchase.addonId)
                    purchaseDao.markAsSynced(purchase.id)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Restore purchases on new device
     */
    suspend fun restorePurchases(userId: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val restoredData = firestoreSyncManager.restoreUserData(userId)

                if (restoredData.isSuccess) {
                    val data = restoredData.getOrThrow()

                    // Clear local purchases and restore from cloud
                    purchaseDao.deleteAllForUser(userId)

                    data.purchasedAddons.forEach { addonId ->
                        purchaseDao.insert(
                            PurchaseEntity(
                                id = "${userId}_$addonId",
                                userId = userId,
                                addonId = addonId,
                                purchasedAt = System.currentTimeMillis(),
                                synced = true
                            )
                        )
                    }

                    Result.success(data.purchasedAddons)
                } else {
                    Result.failure(restoredData.exceptionOrNull() ?: Exception("Restore failed"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Delete all purchases for a user
     */
    suspend fun deleteAllPurchases(userId: String) {
        purchaseDao.deleteAllForUser(userId)
        firestoreSyncManager.deleteAllUserData(userId)
    }

    /**
     * Observe purchases in real-time
     */
    fun observePurchases(userId: String): Flow<List<String>> = flow {
        firestoreSyncManager.observePurchases(userId).collect { addons ->
            emit(addons)
        }
    }
}
