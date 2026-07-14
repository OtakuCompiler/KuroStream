package com.kurostream.sync.data

import com.kurostream.core.common.result.Result
import com.kurostream.domain.entity.PlaybackState
import com.kurostream.domain.entity.SyncState
import com.kurostream.domain.repository.SyncProvider
import com.kurostream.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val syncProvider: SyncProvider,
) : SyncRepository {
    private val _syncState = MutableStateFlow(SyncState(isSyncing = false))
    override val syncState: Flow<SyncState> = _syncState.asStateFlow()

    override suspend fun sync(): Result<Unit> {
        _syncState.value = _syncState.value.copy(isSyncing = true)
        return try {
            val authResult = syncProvider.authenticate(SyncCredentials.Token(""))
            if (authResult is Result.Error) {
                _syncState.value = _syncState.value.copy(isSyncing = false, lastError = authResult.exception.message)
                return Result.Error(authResult.exception)
            }

            val localStates = listOf<PlaybackState>()
            val pushResult = syncProvider.pushPlaybackStates(localStates)
            if (pushResult is Result.Error) {
                return Result.Error(pushResult.exception)
            }

            val pushState = pushResult.data
            _syncState.value = _syncState.value.copy(
                isSyncing = false, lastSyncedAt = System.currentTimeMillis(),
                lastSyncResult = pushState
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            _syncState.value = _syncState.value.copy(isSyncing = false, lastError = e.message)
            Result.Error(e)
        }
    }

    override suspend fun forceSync(): Result<Unit> {
        return sync()
    }
}
