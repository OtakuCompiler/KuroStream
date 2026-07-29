package com.kurostream.backup.domain

interface BackupRepository {
    suspend fun createBackup(): Result<String>
    suspend fun restoreBackup(backupId: String): Result<Unit>
    suspend fun listBackups(): List<String>
}
