package com.kurostream.tv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.scrobbleDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "scrobble_queue"
)

/**
 * Manages offline scrobble queue for AniList and other tracking services
 * Persists scrobbles when offline and syncs when connectivity is restored
 */
@Singleton
class ScrobbleQueueManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    companion object {
        private const val TAG = "ScrobbleQueueManager"
        private val QUEUE_KEY = stringPreferencesKey("pending_scrobbles")
        private val FAILED_KEY = stringPreferencesKey("failed_scrobbles")
        
        // Retry configuration
        private const val MAX_RETRY_ATTEMPTS = 5
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 60000L
        
        // Scrobble thresholds
        const val MIN_WATCH_PERCENT = 0.8f // 80% watched to scrobble
        const val MIN_WATCH_TIME_MS = 60000L // At least 1 minute watched
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    
    private val _queueState = MutableStateFlow(ScrobbleQueueState())
    val queueState: StateFlow<ScrobbleQueueState> = _queueState.asStateFlow()
    
    private val syncListeners = mutableListOf<ScrobbleSyncListener>()
    
    init {
        // Load queue state on init
        scope.launch {
            loadQueueState()
        }
    }
    
    /**
     * Add a scrobble to the queue
     */
    suspend fun enqueueScrobble(scrobble: ScrobbleEntry): Result<Unit> = mutex.withLock {
        try {
            val queue = getPendingQueue().toMutableList()
            
            // Check for duplicate
            val existingIndex = queue.indexOfFirst { 
                it.mediaId == scrobble.mediaId && 
                it.episodeNumber == scrobble.episodeNumber &&
                it.service == scrobble.service
            }
            
            if (existingIndex >= 0) {
                // Update existing entry with newer data
                queue[existingIndex] = scrobble.copy(
                    id = queue[existingIndex].id,
                    queuedAt = System.currentTimeMillis(),
                    retryCount = 0
                )
                Timber.tag(TAG).d("Updated existing scrobble in queue: ${scrobble.mediaId}")
            } else {
                queue.add(scrobble)
                Timber.tag(TAG).d("Added scrobble to queue: ${scrobble.mediaId} ep ${scrobble.episodeNumber}")
            }
            
            savePendingQueue(queue)
            updateQueueState()
            Result.success(Unit)
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to enqueue scrobble")
            Result.failure(e)
        }
    }
    
    /**
     * Process pending scrobbles
     * @param syncFunction Function to sync individual scrobbles to the service
     */
    suspend fun processPendingScrobbles(
        syncFunction: suspend (ScrobbleEntry) -> Result<Unit>
    ): ScrobbleSyncResult = withContext(Dispatchers.IO) {
        val queue = mutex.withLock { getPendingQueue().toMutableList() }
        
        if (queue.isEmpty()) {
            return@withContext ScrobbleSyncResult(0, 0, 0)
        }
        
        var successCount = 0
        var failedCount = 0
        var skippedCount = 0
        
        val remainingQueue = mutableListOf<ScrobbleEntry>()
        val failedQueue = mutex.withLock { getFailedQueue().toMutableList() }
        
        Timber.tag(TAG).d("Processing ${queue.size} pending scrobbles")
        
        for (scrobble in queue) {
            try {
                val result = syncFunction(scrobble)
                
                if (result.isSuccess) {
                    successCount++
                    notifyListeners(scrobble, true, null)
                    Timber.tag(TAG).d("Successfully synced scrobble: ${scrobble.mediaId}")
                } else {
                    handleFailedScrobble(scrobble, remainingQueue, failedQueue, result.exceptionOrNull())
                    failedCount++
                }
            } catch (e: Exception) {
                handleFailedScrobble(scrobble, remainingQueue, failedQueue, e)
                failedCount++
            }
        }
        
        // Save updated queues
        mutex.withLock {
            savePendingQueue(remainingQueue)
            saveFailedQueue(failedQueue)
            updateQueueState()
        }
        
        Timber.tag(TAG).d("Sync complete: $successCount success, $failedCount failed, $skippedCount skipped")
        ScrobbleSyncResult(successCount, failedCount, skippedCount)
    }
    
    /**
     * Get count of pending scrobbles
     */
    suspend fun getPendingCount(): Int = mutex.withLock {
        getPendingQueue().size
    }
    
    /**
     * Clear all pending scrobbles
     */
    suspend fun clearPendingQueue(): Result<Unit> = mutex.withLock {
        try {
            savePendingQueue(emptyList())
            updateQueueState()
            Timber.tag(TAG).d("Cleared pending scrobble queue")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Clear failed scrobbles
     */
    suspend fun clearFailedQueue(): Result<Unit> = mutex.withLock {
        try {
            saveFailedQueue(emptyList())
            updateQueueState()
            Timber.tag(TAG).d("Cleared failed scrobble queue")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Retry failed scrobbles
     */
    suspend fun retryFailedScrobbles(): Result<Unit> = mutex.withLock {
        try {
            val failedQueue = getFailedQueue()
            val pendingQueue = getPendingQueue().toMutableList()
            
            // Move failed back to pending with reset retry count
            failedQueue.forEach { entry ->
                val resetEntry = entry.copy(
                    retryCount = 0,
                    queuedAt = System.currentTimeMillis()
                )
                pendingQueue.add(resetEntry)
            }
            
            savePendingQueue(pendingQueue)
            saveFailedQueue(emptyList())
            updateQueueState()
            
            Timber.tag(TAG).d("Moved ${failedQueue.size} failed scrobbles to pending")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add sync listener
     */
    fun addSyncListener(listener: ScrobbleSyncListener) {
        syncListeners.add(listener)
    }
    
    /**
     * Remove sync listener
     */
    fun removeSyncListener(listener: ScrobbleSyncListener) {
        syncListeners.remove(listener)
    }
    
    /**
     * Check if an episode should be scrobbled based on watch progress
     */
    fun shouldScrobble(
        watchedTimeMs: Long,
        totalDurationMs: Long,
        minWatchPercent: Float = MIN_WATCH_PERCENT,
        minWatchTimeMs: Long = MIN_WATCH_TIME_MS
    ): Boolean {
        if (totalDurationMs <= 0) return false
        if (watchedTimeMs < minWatchTimeMs) return false
        
        val watchedPercent = watchedTimeMs.toFloat() / totalDurationMs
        return watchedPercent >= minWatchPercent
    }
    
    /**
     * Create a scrobble entry for an episode
     */
    fun createScrobbleEntry(
        mediaId: Long,
        episodeNumber: Int,
        service: ScrobbleService,
        watchedTimeMs: Long,
        totalDurationMs: Long,
        status: WatchStatus = WatchStatus.WATCHING,
        score: Float? = null
    ): ScrobbleEntry {
        return ScrobbleEntry(
            id = UUID.randomUUID().toString(),
            mediaId = mediaId,
            episodeNumber = episodeNumber,
            service = service,
            watchedTimeMs = watchedTimeMs,
            totalDurationMs = totalDurationMs,
            watchedAt = System.currentTimeMillis(),
            queuedAt = System.currentTimeMillis(),
            status = status,
            score = score
        )
    }
    
    private fun handleFailedScrobble(
        scrobble: ScrobbleEntry,
        remainingQueue: MutableList<ScrobbleEntry>,
        failedQueue: MutableList<ScrobbleEntry>,
        error: Throwable?
    ) {
        val updatedScrobble = scrobble.copy(
            retryCount = scrobble.retryCount + 1,
            lastError = error?.message
        )
        
        if (updatedScrobble.retryCount >= MAX_RETRY_ATTEMPTS) {
            // Move to failed queue
            failedQueue.add(updatedScrobble)
            Timber.tag(TAG).w("Scrobble exceeded max retries, moved to failed: ${scrobble.mediaId}")
        } else {
            // Keep in pending queue for retry
            remainingQueue.add(updatedScrobble)
            Timber.tag(TAG).d("Scrobble failed, will retry: ${scrobble.mediaId} (attempt ${updatedScrobble.retryCount})")
        }
        
        notifyListeners(scrobble, false, error)
    }
    
    private fun notifyListeners(scrobble: ScrobbleEntry, success: Boolean, error: Throwable?) {
        syncListeners.forEach { listener ->
            try {
                if (success) {
                    listener.onScrobbleSynced(scrobble)
                } else {
                    listener.onScrobbleFailed(scrobble, error)
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error notifying listener")
            }
        }
    }
    
    private suspend fun loadQueueState() {
        updateQueueState()
    }
    
    private suspend fun updateQueueState() {
        val pending = getPendingQueue()
        val failed = getFailedQueue()
        
        _queueState.value = ScrobbleQueueState(
            pendingCount = pending.size,
            failedCount = failed.size,
            lastSyncAttempt = pending.maxOfOrNull { it.queuedAt },
            oldestPending = pending.minByOrNull { it.queuedAt }?.queuedAt
        )
    }
    
    private suspend fun getPendingQueue(): List<ScrobbleEntry> {
        return context.scrobbleDataStore.data.map { prefs ->
            val jsonString = prefs[QUEUE_KEY] ?: "[]"
            try {
                json.decodeFromString<List<ScrobbleEntry>>(jsonString)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to parse pending queue")
                emptyList()
            }
        }.first()
    }
    
    private suspend fun savePendingQueue(queue: List<ScrobbleEntry>) {
        context.scrobbleDataStore.edit { prefs ->
            prefs[QUEUE_KEY] = json.encodeToString(queue)
        }
    }
    
    private suspend fun getFailedQueue(): List<ScrobbleEntry> {
        return context.scrobbleDataStore.data.map { prefs ->
            val jsonString = prefs[FAILED_KEY] ?: "[]"
            try {
                json.decodeFromString<List<ScrobbleEntry>>(jsonString)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to parse failed queue")
                emptyList()
            }
        }.first()
    }
    
    private suspend fun saveFailedQueue(queue: List<ScrobbleEntry>) {
        context.scrobbleDataStore.edit { prefs ->
            prefs[FAILED_KEY] = json.encodeToString(queue)
        }
    }
}

/**
 * Represents a scrobble entry to be synced
 */
@Serializable
data class ScrobbleEntry(
    val id: String,
    val mediaId: Long,
    val episodeNumber: Int,
    val service: ScrobbleService,
    val watchedTimeMs: Long,
    val totalDurationMs: Long,
    val watchedAt: Long,
    val queuedAt: Long,
    val status: WatchStatus = WatchStatus.WATCHING,
    val score: Float? = null,
    val retryCount: Int = 0,
    val lastError: String? = null
) {
    val watchedPercent: Float
        get() = if (totalDurationMs > 0) watchedTimeMs.toFloat() / totalDurationMs else 0f
}

/**
 * Tracking service types
 */
@Serializable
enum class ScrobbleService {
    ANILIST,
    MAL,
    KITSU,
    SIMKL
}

/**
 * Watch status for tracking
 */
@Serializable
enum class WatchStatus {
    WATCHING,
    COMPLETED,
    ON_HOLD,
    DROPPED,
    PLAN_TO_WATCH,
    REWATCHING
}

/**
 * Scrobble queue state for UI
 */
data class ScrobbleQueueState(
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val lastSyncAttempt: Long? = null,
    val oldestPending: Long? = null
) {
    val hasUnsynced: Boolean get() = pendingCount > 0
    val hasFailed: Boolean get() = failedCount > 0
    val totalCount: Int get() = pendingCount + failedCount
}

/**
 * Result of scrobble sync operation
 */
data class ScrobbleSyncResult(
    val successCount: Int,
    val failedCount: Int,
    val skippedCount: Int
) {
    val totalProcessed: Int get() = successCount + failedCount + skippedCount
    val isComplete: Boolean get() = failedCount == 0
}

/**
 * Listener for scrobble sync events
 */
interface ScrobbleSyncListener {
    fun onScrobbleSynced(entry: ScrobbleEntry)
    fun onScrobbleFailed(entry: ScrobbleEntry, error: Throwable?)
}
