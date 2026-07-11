package com.kurostream.torrent.cache

import android.content.Context
import android.util.Log
import com.kurostream.torrent.domain.TorrentInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentMetadataCache @Inject constructor(
    @androidx.hilt.android.qualifiers.ApplicationContext private val context: Context,
) {

    private val TAG = "TorrentMetadataCache"

    @Serializable
    data class CachedTorrentMetadata(
        val infoHash: String,
        val name: String,
        val totalSize: Long,
        val numPieces: Int,
        val pieceSize: Int,
        val fileCount: Int,
        val fileNames: List<String>,
        val cachedAt: Long = System.currentTimeMillis(),
        val lastAccessedAt: Long = System.currentTimeMillis(),
        var accessCount: Int = 1,
    )

    private val memoryCache = ConcurrentHashMap<String, CachedTorrentMetadata>()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val maxCacheSize = 500

    private val cacheDir: File by lazy {
        File(context.filesDir, "torrent_metadata_cache").apply { mkdirs() }
    }

    private val _cacheStats = MutableStateFlow(CacheStats())
    val cacheStats: StateFlow<CacheStats> = _cacheStats.asStateFlow()

    data class CacheStats(
        val memoryEntries: Int = 0,
        val diskEntries: Int = 0,
        val hits: Long = 0,
        val misses: Long = 0,
    )

    private var hits = 0L
    private var misses = 0L

    suspend fun cacheTorrentInfo(info: TorrentInfo) = withContext(Dispatchers.IO) {
        val fileNames = info.files.map { it.path }
        val metadata = CachedTorrentMetadata(
            infoHash = info.infoHash,
            name = info.name,
            totalSize = info.totalSize,
            numPieces = info.numPieces,
            pieceSize = info.pieceSize,
            fileCount = fileNames.size,
            fileNames = fileNames,
        )

        memoryCache[info.infoHash] = metadata

        try {
            val file = File(cacheDir, "${info.infoHash}.json")
            file.writeText(json.encodeToString(metadata))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache metadata to disk for ${info.infoHash}", e)
        }

        evictIfNeeded()
        updateStats()
    }

    suspend fun getCachedMetadata(infoHash: String): CachedTorrentMetadata? = withContext(Dispatchers.IO) {
        val memResult = memoryCache[infoHash]
        if (memResult != null) {
            memResult.lastAccessedAt = System.currentTimeMillis()
            memResult.accessCount++
            hits++
            updateStats()
            return@withContext memResult
        }

        try {
            val file = File(cacheDir, "$infoHash.json")
            if (file.exists()) {
                val metadata = json.decodeFromString<CachedTorrentMetadata>(file.readText())
                memoryCache[infoHash] = metadata
                metadata.lastAccessedAt = System.currentTimeMillis()
                metadata.accessCount++
                hits++
                updateStats()
                return@withContext metadata
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load cached metadata for $infoHash", e)
        }

        misses++
        updateStats()
        null
    }

    suspend fun hasCachedMetadata(infoHash: String): Boolean {
        return memoryCache.containsKey(infoHash) || withContext(Dispatchers.IO) {
            File(cacheDir, "$infoHash.json").exists()
        }
    }

    suspend fun removeCachedMetadata(infoHash: String) = withContext(Dispatchers.IO) {
        memoryCache.remove(infoHash)
        try {
            File(cacheDir, "$infoHash.json").delete()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete cached metadata for $infoHash", e)
        }
        updateStats()
    }

    private fun evictIfNeeded() {
        if (memoryCache.size > maxCacheSize) {
            val toEvict = memoryCache.values
                .sortedBy { it.lastAccessedAt }
                .take(memoryCache.size - maxCacheSize + 50)

            toEvict.forEach { metadata ->
                memoryCache.remove(metadata.infoHash)
            }
        }
    }

    private fun updateStats() {
        _cacheStats.value = CacheStats(
            memoryEntries = memoryCache.size,
            diskEntries = cacheDir.listFiles()?.size ?: 0,
            hits = hits,
            misses = misses,
        )
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        memoryCache.clear()
        cacheDir.listFiles()?.forEach { it.delete() }
        hits = 0
        misses = 0
        updateStats()
    }
}
