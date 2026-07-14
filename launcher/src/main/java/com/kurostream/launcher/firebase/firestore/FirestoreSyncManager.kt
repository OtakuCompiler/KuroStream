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

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val firestore: FirebaseFirestore = Firebase.firestore

    companion object {
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_PURCHASES = "purchases"
        private const val COLLECTION_DEVICES = "devices"
        private const val FIELD_PURCHASED_ADDONS = "purchasedAddons"
        private const val FIELD_LAST_SYNCED = "lastSynced"
        private const val FIELD_DEVICE_COUNT = "deviceCount"
    }

    /**
     * Sync purchased addons to Firestore
     */
    suspend fun syncPurchases(userId: String, addonIds: List<String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val data = hashMapOf(
                    FIELD_PURCHASED_ADDONS to addonIds,
                    FIELD_LAST_SYNCED to System.currentTimeMillis()
                )

                firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_PURCHASES)
                    .document("addons")
                    .set(data, SetOptions.merge())
                    .await()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Add a single purchased addon
     */
    suspend fun addPurchase(userId: String, addonId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val docRef = firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_PURCHASES)
                    .document("addons")

                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    val currentAddons = snapshot.get(FIELD_PURCHASED_ADDONS) as? List<String> ?: emptyList()

                    if (!currentAddons.contains(addonId)) {
                        val updatedAddons = currentAddons + addonId
                        transaction.set(
                            docRef,
                            hashMapOf(
                                FIELD_PURCHASED_ADDONS to updatedAddons,
                                FIELD_LAST_SYNCED to System.currentTimeMillis()
                            ),
                            SetOptions.merge()
                        )
                    }
                }.await()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Remove a purchased addon
     */
    suspend fun removePurchase(userId: String, addonId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val docRef = firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_PURCHASES)
                    .document("addons")

                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    val currentAddons = snapshot.get(FIELD_PURCHASED_ADDONS) as? List<String> ?: emptyList()
                    val updatedAddons = currentAddons.filter { it != addonId }

                    transaction.set(
                        docRef,
                        hashMapOf(
                            FIELD_PURCHASED_ADDONS to updatedAddons,
                            FIELD_LAST_SYNCED to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                }.await()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Get purchased addons from Firestore
     */
    suspend fun getPurchases(userId: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val document = firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_PURCHASES)
                    .document("addons")
                    .get()
                    .await()

                val addons = document.get(FIELD_PURCHASED_ADDONS) as? List<String> ?: emptyList()
                Result.success(addons)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Observe purchased addons in real-time
     */
    fun observePurchases(userId: String): Flow<List<String>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_PURCHASES)
            .document("addons")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val addons = snapshot?.get(FIELD_PURCHASED_ADDONS) as? List<String> ?: emptyList()
                trySend(addons)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Register device for user (for tracking active devices)
     */
    suspend fun registerDevice(userId: String, deviceId: String, deviceInfo: DeviceInfo): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val deviceData = hashMapOf(
                    "deviceId" to deviceId,
                    "model" to deviceInfo.model,
                    "manufacturer" to deviceInfo.manufacturer,
                    "androidVersion" to deviceInfo.androidVersion,
                    "appVersion" to deviceInfo.appVersion,
                    "lastActive" to System.currentTimeMillis()
                )

                firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_DEVICES)
                    .document(deviceId)
                    .set(deviceData)
                    .await()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Get all registered devices
     */
    suspend fun getDevices(userId: String): Result<List<DeviceInfo>> =
        withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_DEVICES)
                    .get()
                    .await()

                val devices = snapshot.documents.mapNotNull { doc ->
                    DeviceInfo(
                        deviceId = doc.getString("deviceId") ?: "",
                        model = doc.getString("model") ?: "",
                        manufacturer = doc.getString("manufacturer") ?: "",
                        androidVersion = doc.getString("androidVersion") ?: "",
                        appVersion = doc.getString("appVersion") ?: "",
                        lastActive = doc.getLong("lastActive") ?: 0L
                    )
                }
                Result.success(devices)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Restore all user data on new device
     */
    suspend fun restoreUserData(userId: String): Result<RestoredUserData> =
        withContext(Dispatchers.IO) {
            try {
                val purchasesResult = getPurchases(userId)
                val devicesResult = getDevices(userId)

                val purchases = purchasesResult.getOrDefault(emptyList())
                val devices = devicesResult.getOrDefault(emptyList())

                // Register current device
                val currentDevice = DeviceInfo.getCurrentDevice(context)
                registerDevice(userId, currentDevice.deviceId, currentDevice)

                Result.success(
                    RestoredUserData(
                        purchasedAddons = purchases,
                        devices = devices + currentDevice
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Delete all user data (GDPR compliance)
     */
    suspend fun deleteAllUserData(userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // Delete purchases
                firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_PURCHASES)
                    .document("addons")
                    .delete()
                    .await()

                // Delete devices
                val devicesSnapshot = firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_DEVICES)
                    .get()
                    .await()

                val batch = firestore.batch()
                devicesSnapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()

                // Delete user document
                firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .delete()
                    .await()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

data class DeviceInfo(
    val deviceId: String,
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val appVersion: String,
    val lastActive: Long = System.currentTimeMillis()
) {
    companion object {
        fun getCurrentDevice(context: Context): DeviceInfo {
            return DeviceInfo(
                deviceId = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                ) ?: "unknown",
                model = android.os.Build.MODEL,
                manufacturer = android.os.Build.MANUFACTURER,
                androidVersion = android.os.Build.VERSION.RELEASE,
                appVersion = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
                } catch (e: Exception) {
                    "1.0.0"
                }
            )
        }
    }
}

data class RestoredUserData(
    val purchasedAddons: List<String>,
    val devices: List<DeviceInfo>
)
