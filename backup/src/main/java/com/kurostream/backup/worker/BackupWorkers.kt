package com.kurostream.backup.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kurostream.backup.data.BackupRepositoryImpl
import com.kurostream.core.common.result.Result
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AutoBackupWorker @Inject constructor(
    @Suppress("UNUSED_PARAMETER") context: Context,
    params: WorkerParameters,
    private val repository: BackupRepositoryImpl,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): androidx.work.Result {
        val config = repository.backupConfig.first()

        if (!config.autoBackupEnabled) {
            return androidx.work.Result.success()
        }

        val lastBackup = config.lastBackupTimestamp
        val intervalMs = config.autoBackupIntervalDays.toLong() * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()

        if (now - lastBackup < intervalMs) {
            return androidx.work.Result.success()
        }

        val password = getBackupPassword() ?: return androidx.work.Result.success()

        val result = repository.createBackup(config, password)
        return if (result is Result.Success) {
            androidx.work.Result.success()
        } else {
            androidx.work.Result.retry()
        }
    }

    private fun getBackupPassword(): String? {
        return null
    }
}

@AndroidEntryPoint
class RestoreWorker @Inject constructor(
    context: Context,
    params: WorkerParameters,
    private val repository: BackupRepositoryImpl,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): androidx.work.Result {
        val metadataId = inputData.getString("metadata_id") ?: return androidx.work.Result.failure()
        val password = inputData.getString("password")
        val config = repository.backupConfig.first()

        val backupsResult = repository.listBackups(config)
        if (backupsResult is Result.Error) return androidx.work.Result.failure()

        val metadata = backupsResult.getOrNull()?.firstOrNull { it.id == metadataId }
            ?: return androidx.work.Result.failure()

        val result = repository.restoreBackup(config, metadata, password)
        return if (result is Result.Success) androidx.work.Result.success() else androidx.work.Result.failure()
    }
}

@AndroidEntryPoint
class SyncWorker @Inject constructor(
    context: Context,
    params: WorkerParameters,
    private val repository: BackupRepositoryImpl,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): androidx.work.Result {
        val config = repository.backupConfig.first()
        if (!config.autoBackupEnabled) return androidx.work.Result.success()

        val password = getBackupPassword() ?: return androidx.work.Result.success()
        val result = repository.createBackup(config, password)
        return if (result is Result.Success) androidx.work.Result.success() else androidx.work.Result.retry()
    }

    private fun getBackupPassword(): String? = null
}
