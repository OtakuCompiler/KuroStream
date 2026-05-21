package com.kurostream.tv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kurostream.tv.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import timber.log.Timber
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks active torrent sessions and manages their lifecycle.
 * Handles session persistence, cleanup, and resource management.
 */
@Singleton
class TorrentSessionTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    
    companion object {
        private const val TAG = "TorrentSessionTracker"
        
        private val KEY_ACTIVE_SESSIONS = stringPreferencesKey("active_torrent_sessions")
        private val KEY_SESSION_COUNT = intPreferencesKey("torrent_session_count")
        private val KEY_TOTAL_DOWNLOADED = longPreferencesKey("total_torrent_downloaded")
        
        // Memory limits for 1GB RAM devices
        private const val MAX_CONCURRENT_SESSIONS = 1
        private const val MAX_CACHE_SIZE_MB = 100L // 100MB max cache
        private const val SESSION_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes
    }
    
    private val activeSessions = mutableMapOf<String, TrackedTorrentSession>()
    private val torrentCacheDir: File by lazy {
        File(context.cacheDir, "torrent_cache").also { it.mkdirs() }
    }
    
    /**
     * Start tracking a new torrent session.
     */
    suspend fun trackSession(
        infoHash: String,
        magnetUri: String,
        fileIndex: Int,
        animeId: String,
        episodeId: String
    ): Result<TrackedTorrentSession> = withContext(ioDispatcher) {
        try {
            // Check concurrent session limit
            if (activeSessions.size >= MAX_CONCURRENT_SESSIONS) {
                // Stop oldest session
                val oldest = activeSessions.values.minByOrNull { it.startTime }
                oldest?.let { stopSession(it.sessionId) }
            }
            
            // Check cache size
            cleanupCacheIfNeeded()
            
            val session = TrackedTorrentSession(
                sessionId = UUID.randomUUID().toString(),
                infoHash = infoHash,
                magnetUri = magnetUri,
                fileIndex = fileIndex,
                animeId = animeId,
                episodeId = episodeId,
                startTime = System.currentTimeMillis(),
                status = TorrentSessionStatus.STARTING
            )
            
            activeSessions[session.sessionId] = session
            
            // Persist session
            saveSessions()
            
            Result.success(session)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to track session")
            Result.failure(e)
        }
    }
    
    /**
     * Update session status.
     */
    suspend fun updateSession(
        sessionId: String,
        status: TorrentSessionStatus? = null,
        downloadedBytes: Long? = null,
        totalBytes: Long? = null,
        downloadSpeed: Long? = null,
        seeders: Int? = null,
        peers: Int? = null,
        progress: Float? = null
    ) = withContext(ioDispatcher) {
        activeSessions[sessionId]?.let { session ->
            activeSessions[sessionId] = session.copy(
                status = status ?: session.status,
                downloadedBytes = downloadedBytes ?: session.downloadedBytes,
                totalBytes = totalBytes ?: session.totalBytes,
                downloadSpeed = downloadSpeed ?: session.downloadSpeed,
                seeders = seeders ?: session.seeders,
                peers = peers ?: session.peers,
                progress = progress ?: session.progress,
                lastUpdateTime = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Stop and remove a session.
     */
    suspend fun stopSession(sessionId: String) = withContext(ioDispatcher) {
        val session = activeSessions.remove(sessionId) ?: return@withContext
        
        // Update total downloaded
        val currentTotal = dataStore.data.first()[KEY_TOTAL_DOWNLOADED] ?: 0L
        dataStore.edit { prefs ->
            prefs[KEY_TOTAL_DOWNLOADED] = currentTotal + session.downloadedBytes
        }
        
        // Clean up session cache
        cleanupSessionCache(session.infoHash)
        
        saveSessions()
    }
    
    /**
     * Get all active sessions.
     */
    fun getActiveSessions(): List<TrackedTorrentSession> {
        return activeSessions.values.toList()
    }
    
    /**
     * Get session by ID.
     */
    fun getSession(sessionId: String): TrackedTorrentSession? {
        return activeSessions[sessionId]
    }
    
    /**
     * Get session by info hash.
     */
    fun getSessionByInfoHash(infoHash: String): TrackedTorrentSession? {
        return activeSessions.values.find { it.infoHash == infoHash }
    }
    
    /**
     * Check if a torrent is already being tracked.
     */
    fun isTracked(infoHash: String): Boolean {
        return activeSessions.values.any { it.infoHash == infoHash }
    }
    
    /**
     * Get total downloaded bytes across all sessions.
     */
    val totalDownloaded: Flow<Long> = dataStore.data.map { prefs ->
        prefs[KEY_TOTAL_DOWNLOADED] ?: 0L
    }
    
    /**
     * Get current cache size.
     */
    fun getCacheSize(): Long {
        return torrentCacheDir.walkTopDown().sumOf { it.length() }
    }
    
    /**
     * Clean up stale sessions.
     */
    suspend fun cleanupStaleSessions() = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val stale = activeSessions.filter { (_, session) ->
            now - session.lastUpdateTime > SESSION_TIMEOUT_MS
        }
        
        stale.forEach { (id, _) ->
            stopSession(id)
        }
    }
    
    /**
     * Stop all active sessions.
     */
    suspend fun stopAllSessions() = withContext(ioDispatcher) {
        activeSessions.keys.toList().forEach { sessionId ->
            stopSession(sessionId)
        }
    }
    
    /**
     * Clear all torrent cache.
     */
    suspend fun clearCache() = withContext(ioDispatcher) {
        stopAllSessions()
        torrentCacheDir.deleteRecursively()
        torrentCacheDir.mkdirs()
    }
    
    /**
     * Restore sessions from persistence.
     */
    suspend fun restoreSessions() = withContext(ioDispatcher) {
        try {
            val sessionsJson = dataStore.data.first()[KEY_ACTIVE_SESSIONS]
            if (sessionsJson.isNullOrEmpty()) return@withContext
            
            val array = JSONArray(sessionsJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val session = TrackedTorrentSession(
                    sessionId = obj.getString("sessionId"),
                    infoHash = obj.getString("infoHash"),
                    magnetUri = obj.getString("magnetUri"),
                    fileIndex = obj.getInt("fileIndex"),
                    animeId = obj.getString("animeId"),
                    episodeId = obj.getString("episodeId"),
                    startTime = obj.getLong("startTime"),
                    status = TorrentSessionStatus.PAUSED, // Restore as paused
                    downloadedBytes = obj.optLong("downloadedBytes", 0),
                    totalBytes = obj.optLong("totalBytes", 0),
                    lastUpdateTime = System.currentTimeMillis()
                )
                
                // Only restore if not too old
                if (System.currentTimeMillis() - session.startTime < SESSION_TIMEOUT_MS * 2) {
                    activeSessions[session.sessionId] = session
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to restore sessions")
        }
    }
    
    private suspend fun saveSessions() {
        try {
            val array = JSONArray()
            activeSessions.values.forEach { session ->
                val obj = JSONObject().apply {
                    put("sessionId", session.sessionId)
                    put("infoHash", session.infoHash)
                    put("magnetUri", session.magnetUri)
                    put("fileIndex", session.fileIndex)
                    put("animeId", session.animeId)
                    put("episodeId", session.episodeId)
                    put("startTime", session.startTime)
                    put("downloadedBytes", session.downloadedBytes)
                    put("totalBytes", session.totalBytes)
                }
                array.put(obj)
            }
            
            dataStore.edit { prefs ->
                prefs[KEY_ACTIVE_SESSIONS] = array.toString()
                prefs[KEY_SESSION_COUNT] = activeSessions.size
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to save sessions")
        }
    }
    
    private fun cleanupCacheIfNeeded() {
        val maxCacheBytes = MAX_CACHE_SIZE_MB * 1024 * 1024
        val currentSize = getCacheSize()
        
        if (currentSize > maxCacheBytes) {
            // Delete oldest files first
            torrentCacheDir.listFiles()
                ?.sortedBy { it.lastModified() }
                ?.forEach { file ->
                    if (getCacheSize() <= maxCacheBytes * 0.8) return
                    file.deleteRecursively()
                }
        }
    }
    
    private fun cleanupSessionCache(infoHash: String) {
        File(torrentCacheDir, infoHash).deleteRecursively()
    }
}

/**
 * Tracked torrent session data class.
 */
data class TrackedTorrentSession(
    val sessionId: String,
    val infoHash: String,
    val magnetUri: String,
    val fileIndex: Int,
    val animeId: String,
    val episodeId: String,
    val startTime: Long,
    val status: TorrentSessionStatus,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val downloadSpeed: Long = 0, // bytes/sec
    val seeders: Int = 0,
    val peers: Int = 0,
    val progress: Float = 0f,
    val lastUpdateTime: Long = System.currentTimeMillis()
) {
    val downloadSpeedMbps: Float
        get() = downloadSpeed / (1024f * 1024f)
    
    val progressPercent: Int
        get() = (progress * 100).toInt()
    
    val durationMs: Long
        get() = System.currentTimeMillis() - startTime
    
    val isActive: Boolean
        get() = status in listOf(
            TorrentSessionStatus.STARTING,
            TorrentSessionStatus.DOWNLOADING,
            TorrentSessionStatus.STREAMING
        )
}

/**
 * Torrent session status.
 */
enum class TorrentSessionStatus {
    STARTING,    // Initializing torrent session
    METADATA,    // Fetching torrent metadata
    DOWNLOADING, // Downloading pieces
    STREAMING,   // Actively streaming video
    PAUSED,      // Temporarily paused
    SEEDING,     // Download complete, seeding
    ERROR,       // Error occurred
    STOPPED      // Session stopped
}

/**
 * Torrent settings for the app.
 */
data class TorrentSettings(
    val maxDownloadSpeed: Long = 0, // 0 = unlimited
    val maxUploadSpeed: Long = 100 * 1024, // 100 KB/s default
    val maxConnections: Int = 50,
    val enableDHT: Boolean = true,
    val enablePEX: Boolean = true,
    val enableEncryption: Boolean = true,
    val sequentialDownload: Boolean = true, // Prioritize for streaming
    val maxCacheSizeMb: Int = 100
)
