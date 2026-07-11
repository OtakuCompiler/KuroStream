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

package com.kurostream.data.anistream.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncProvider @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_WATCH_HISTORY = "watch_history"
        const val COLLECTION_PROFILES = "profiles"
        const val COLLECTION_SETTINGS = "settings"
    }

    suspend fun canUseCloudBackup(): Boolean {
        val user = auth.currentUser ?: return false
        val purchaseDoc = firestore.collection("purchases")
            .document(user.uid)
            .get()
            .await()
        return purchaseDoc.exists() && purchaseDoc.getBoolean("hasCloudBackup") == true
    }

    suspend fun backupUserData(data: UserBackupData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            if (!canUseCloudBackup()) {
                return@withContext Result.failure(Exception("Cloud backup requires purchased addon"))
            }

            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .set(mapOf("lastBackup" to System.currentTimeMillis()), SetOptions.merge())
                .await()

            data.profiles.forEach { profile ->
                firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_PROFILES)
                    .document(profile.id)
                    .set(profile)
                    .await()
            }

            data.watchHistory.forEach { item ->
                firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_WATCH_HISTORY)
                    .document(item.animeId)
                    .set(item)
                    .await()
            }

            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SETTINGS)
                .document("app_settings")
                .set(data.settings)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreUserData(): Result<UserBackupData> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            if (!canUseCloudBackup()) {
                return@withContext Result.failure(Exception("Cloud backup requires purchased addon"))
            }

            val profiles = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_PROFILES)
                .get()
                .await()
                .toObjects(ProfileBackup::class.java)

            val watchHistory = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_WATCH_HISTORY)
                .get()
                .await()
                .toObjects(WatchHistoryBackup::class.java)

            val settingsDoc = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SETTINGS)
                .document("app_settings")
                .get()
                .await()

            Result.success(UserBackupData(
                profiles = profiles,
                watchHistory = watchHistory,
                settings = settingsDoc.data ?: emptyMap()
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncWatchHistoryItem(item: WatchHistoryItem): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_WATCH_HISTORY)
                .document(item.animeId)
                .set(item)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class UserBackupData(
    val profiles: List<ProfileBackup>,
    val watchHistory: List<WatchHistoryBackup>,
    val settings: Map<String, Any>
)

data class ProfileBackup(
    val id: String = "",
    val name: String = "",
    val isKidsMode: Boolean = false
)

data class WatchHistoryBackup(
    val animeId: String = "",
    val title: String = "",
    val episodesWatched: Int = 0
)
