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
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.work.HiltWorkerFactory
import javax.inject.Inject

@AndroidEntryPoint
class AutoBackupWorker @Inject constructor(
    @Suppress("UNUSED_PARAMETER") context: Context,
    params: WorkerParameters,
    private val repository: BackupRepositoryImpl,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val config = repository.getBackupConfig()
        if (!config.autoBackupEnabled) return Result.success()

        val lastBackup = config.lastBackupTimestamp
        val intervalMs = config.autoBackupIntervalDays.toLong() * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()

        if (now - lastBackup < intervalMs) return Result.success()

        val password = repository.getBackupPassword() ?: return Result.success()

        val result = repository.createBackup(config, password)
        return if (result is com.kurostream.common.result.Result.Success) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}