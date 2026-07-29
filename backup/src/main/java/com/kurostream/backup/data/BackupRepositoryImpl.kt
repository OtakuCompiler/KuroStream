package com.kurostream.backup.data

import com.kurostream.backup.domain.BackupRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor() : BackupRepository {
    override suspend fun createBackup(): Result<String> {
        return Result.success("backup_${System.currentTimeMillis()}")
    }

    override suspend fun restoreBackup(backupId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun listBackups(): List<String> {
        return emptyList()
    }
}
