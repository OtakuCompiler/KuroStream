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

package com.kurostream.backup.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kurostream.backup.data.BackupRepositoryImpl
import com.kurostream.backup.domain.BackupConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AutoBackupWorker @Inject constructor(
    @Suppress("UNUSED_PARAMETER") context: Context,
    params: WorkerParameters,
    private val repository: BackupRepositoryImpl,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val config = repository.backupConfig.first()

        if (!config.autoBackupEnabled) {
            return Result.success()
        }

        val lastBackup = config.lastBackupTimestamp
        val intervalMs = config.autoBackupIntervalDays.toLong() * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()

        if (now - lastBackup < intervalMs) {
            return Result.success()
        }

        // Get password from secure storage or use device-generated key
        val password = getBackupPassword() ?: return Result.success()

        val result = repository.createBackup(config, password)
        return if (result is Result.Success) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    private fun getBackupPassword(): String? {
        // Retrieve from EncryptedSharedPreferences or generate device-specific key
        return null // TODO: Implement password retrieval
    }
}

@AndroidEntryPoint
class RestoreWorker @Inject constructor(
    context: Context,
    params: WorkerParameters,
    private val repository: BackupRepositoryImpl,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val metadataId = inputData.getString("metadata_id") ?: return Result.failure()
        val password = inputData.getString("password")
        val config = repository.backupConfig.first()

        // Find metadata
        val backupsResult = repository.listBackups(config)
        if (backupsResult is Result.Failure) return Result.failure()

        val metadata = backupsResult.data.firstOrNull { it.id == metadataId }
            ?: return Result.failure()

        val result = repository.restoreBackup(config, metadata, password)
        return if (result is Result.Success) Result.success() else Result.failure()
    }
}

@AndroidEntryPoint
class SyncWorker @Inject constructor(
    context: Context,
    params: WorkerParameters,
    private val repository: BackupRepositoryImpl,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Sync local changes to remote backup
        val config = repository.backupConfig.first()
        if (!config.autoBackupEnabled) return Result.success()

        val password = getBackupPassword() ?: return Result.success()
        val result = repository.createBackup(config, password)
        return if (result is Result.Success) Result.success() else Result.retry()
    }

    private fun getBackupPassword(): String? = null // TODO
}