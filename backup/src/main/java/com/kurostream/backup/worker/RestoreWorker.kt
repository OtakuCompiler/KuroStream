package com.kurostream.backup.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.kurostream.backup.data.BackupRepositoryImpl
import com.kurostream.core.common.result.Result
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class RestoreWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: BackupRepositoryImpl,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): ListenableWorker.Result {
        val metadataId = inputData.getString("metadata_id") ?: return ListenableWorker.Result.failure()
        val password = inputData.getString("password")
        val config = repository.backupConfig.first()

        val backupsResult = repository.listBackups(config)
        if (backupsResult is Result.Error) return ListenableWorker.Result.failure()

        val metadata = backupsResult.getOrNull()?.firstOrNull { it.id == metadataId }
            ?: return ListenableWorker.Result.failure()

        val result = repository.restoreBackup(config, metadata, password)
        return if (result is Result.Success) ListenableWorker.Result.success() else ListenableWorker.Result.failure()
    }
}
