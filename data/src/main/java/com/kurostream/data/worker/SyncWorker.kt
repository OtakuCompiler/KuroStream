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

package com.kurostream.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.kurostream.data.local.preferences.SettingsDataStore
import com.kurostream.data.sync.DemoCloudSyncProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncProvider: DemoCloudSyncProvider,
    private val settingsDataStore: SettingsDataStore
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "kurostream_periodic_sync"
        const val KEY_SYNC_RESULT = "sync_result"
        const val KEY_SYNC_TIMESTAMP = "sync_timestamp"

        fun schedule(
            context: Context,
            repeatIntervalHours: Long = 6,
            requiresCharging: Boolean = false,
            requiresWifiOnly: Boolean = true
        ): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (requiresWifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresCharging(requiresCharging)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(repeatIntervalHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .addTag(WORK_NAME)
                .build()

            return WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun isScheduled(context: Context): Boolean {
            val info = WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(WORK_NAME).value
            return info?.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING } ?: false
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            if (!settingsDataStore.syncEnabled.first()) {
                return@withContext Result.success(workDataOf(KEY_SYNC_RESULT to "sync_disabled"))
            }

            val lastSync = settingsDataStore.lastSyncTimestamp.first()

            val pushResult = syncProvider.pushLocalState()
            if (pushResult.isFailure) return@withContext Result.retry()
            val pushTimestamp = pushResult.getOrNull()?.timestamp ?: System.currentTimeMillis()

            val pullResult = syncProvider.pull(lastSync)
            if (pullResult.isFailure) return@withContext Result.retry()

            pullResult.getOrNull()?.let { remote ->
                val local = syncProvider.buildPayloadFromLocal()
                val resolved = syncProvider.resolveConflicts(local, remote)
                if (resolved.timestamp == remote.timestamp) {
                    syncProvider.applyToLocal(resolved)
                }
            }

            settingsDataStore.setLastSyncTimestamp(pushTimestamp)
            Result.success(workDataOf(KEY_SYNC_RESULT to "success", KEY_SYNC_TIMESTAMP to pushTimestamp))
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
