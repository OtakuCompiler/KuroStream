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

package com.kurostream.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.kurostream.common.result.Result
import com.kurostream.domain.model.WatchProgress
import com.kurostream.domain.sync.CrossDeviceSyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class CrossDeviceSyncRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : CrossDeviceSyncRepository {

    private val COLLECTION_WATCH_PROGRESS = "watch_progress"
    private val COLLECTION_DEVICE_STATE = "device_states"

    override suspend fun syncWatchProgress(progress: WatchProgress): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection(COLLECTION_WATCH_PROGRESS)
                .document(progress.profileId)
                .collection("progress")
                .document(progress.mediaId)

            val data = mapOf(
                "mediaId" to progress.mediaId,
                "episodeId" to progress.episodeId,
                "position" to progress.position,
                "duration" to progress.duration,
                "completionPercent" to progress.completionPercent,
                "lastWatched" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )

            docRef.set(data)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync watch progress")
            Result.failure(e)
        }
    }

    override suspend fun getRemoteWatchProgress(profileId: String, mediaId: String): Result<WatchProgress?> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection(COLLECTION_WATCH_PROGRESS)
                .document(profileId)
                .collection("progress")
                .document(mediaId)
                .get()
                .await()

            if (!doc.exists()) {
                return@withContext Result.success(null)
            }

            val data = doc.data ?: return@withContext Result.success(null)
            val progress = WatchProgress(
                id = doc.id,
                mediaItemId = data["mediaId"] as String,
                profileId = profileId,
                position = (data["position"] as? Long) ?: 0,
                duration = (data["duration"] as? Long) ?: 0,
                watchedAt = (data["lastWatched"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                completionPercent = (data["completionPercent"] as? Float) ?: 0f,
                episodeNumber = data["episodeId"] as? String,
                seasonNumber = null,
            )
            Result.success(progress)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get remote watch progress")
            Result.failure(e)
        }
    }

    override suspend fun getAllRemoteWatchProgress(profileId: String): Result<List<WatchProgress>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(COLLECTION_WATCH_PROGRESS)
                .document(profileId)
                .collection("progress")
                .get()
                .await()

            val progressList = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                WatchProgress(
                    id = doc.id,
                    mediaItemId = data["mediaId"] as String,
                    profileId = profileId,
                    position = (data["position"] as? Long) ?: 0,
                    duration = (data["duration"] as? Long) ?: 0,
                    watchedAt = (data["lastWatched"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                    completionPercent = (data["completionPercent"] as? Float) ?: 0f,
                    episodeNumber = data["episodeId"] as? String,
                    seasonNumber = null,
                )
            }
            Result.success(progressList)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get all remote watch progress")
            Result.failure(e)
        }
    }

    override suspend fun registerDevice(profileId: String, deviceId: String, deviceName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = mapOf(
                "profileId" to profileId,
                "deviceId" to deviceId,
                "deviceName" to deviceName,
                "lastActive" to FieldValue.serverTimestamp(),
                "appVersion" to BuildConfig.VERSION_NAME,
            )
            firestore.collection(COLLECTION_DEVICE_STATE)
                .document(deviceId)
                .set(data)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to register device")
            Result.failure(e)
        }
    }

    override suspend fun updateDeviceHeartbeat(deviceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(COLLECTION_DEVICE_STATE)
                .document(deviceId)
                .update("lastActive", FieldValue.serverTimestamp())
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update device heartbeat")
            Result.failure(e)
        }
    }

    override suspend fun getDevicesForProfile(profileId: String): Result<List<DeviceInfo>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(COLLECTION_DEVICE_STATE)
                .whereEqualTo("profileId", profileId)
                .get()
                .await()

            val devices = snapshot.documents.map { doc ->
                val data = doc.data ?: return@map DeviceInfo(doc.id, "", "", 0, "")
                DeviceInfo(
                    deviceId = doc.id,
                    profileId = data["profileId"] as String,
                    deviceName = data["deviceName"] as String,
                    lastActive = (data["lastActive"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0,
                    appVersion = data["appVersion"] as String,
                )
            }
            Result.success(devices)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get devices for profile")
            Result.failure(e)
        }
    }

    override fun observeRemoteWatchProgress(profileId: String, mediaId: String): kotlinx.coroutines.flow.Flow<Result<WatchProgress?>> {
        return kotlinx.coroutines.flow.callbackFlow {
            val listener = firestore.collection(COLLECTION_WATCH_PROGRESS)
                .document(profileId)
                .collection("progress")
                .document(mediaId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        trySend(Result.success(null))
                        return@addSnapshotListener
                    }
                    val data = snapshot.data ?: return@addSnapshotListener trySend(Result.success(null))
                    val progress = WatchProgress(
                        id = snapshot.id,
                        mediaItemId = data["mediaId"] as String,
                        profileId = profileId,
                        position = (data["position"] as? Long) ?: 0,
                        duration = (data["duration"] as? Long) ?: 0,
                        watchedAt = (data["lastWatched"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                        completionPercent = (data["completionPercent"] as? Float) ?: 0f,
                        episodeNumber = data["episodeId"] as? String,
                        seasonNumber = null,
                    )
                    trySend(Result.success(progress))
                }
            awaitClose { listener.remove() }
        }
    }

    data class DeviceInfo(
        val deviceId: String,
        val profileId: String,
        val deviceName: String,
        val lastActive: Long,
        val appVersion: String,
    )
}