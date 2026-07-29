package com.kurostream.data.sync

import com.kurostream.domain.result.Result
import com.kurostream.domain.sync.SyncPayload
import com.kurostream.domain.sync.SyncProvider
import com.kurostream.domain.sync.SyncTimestamp
import javax.inject.Inject
import javax.inject.Singleton

typealias productionCloudSyncProvider = CloudSyncProvider

@Singleton
class CloudSyncProvider @Inject constructor() : SyncProvider {

    override val providerName: String = "cloud"
    override val isAuthenticated: Boolean = false

    override suspend fun authenticate(credentials: Map<String, String>): Result<Unit> =
        Result.success(Unit)

    override suspend fun signOut(): Result<Unit> = Result.success(Unit)

    override suspend fun push(data: SyncPayload): Result<SyncTimestamp> =
        Result.success(SyncTimestamp(timestamp = data.timestamp, serverTime = System.currentTimeMillis()))

    override suspend fun pull(lastSyncTimestamp: Long?): Result<SyncPayload?> =
        Result.success(null)

    override suspend fun resolveConflicts(local: SyncPayload, remote: SyncPayload): SyncPayload = local

    override suspend fun deleteCloudData(): Result<Unit> = Result.success(Unit)

    suspend fun pushLocalState(): Result<SyncTimestamp> {
        val localPayload = buildPayloadFromLocal()
        return push(localPayload)
    }

    fun buildPayloadFromLocal(): SyncPayload = SyncPayload()

    fun applyToLocal(payload: SyncPayload) { }
}
