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

package com.kurostream.playback.memory

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdaptivePrebufferManager @Inject constructor() {
    companion object {
        private const val TAG = "AdaptivePrebuffer"
        private const val MIN_PREFETCH_CHUNKS = 1
        private const val MAX_PREFETCH_CHUNKS = 10
        private const val BITRATE_HISTORY_SIZE = 30
        private const val NETWORK_SAMPLE_WINDOW_MS = 5000
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val prebufferConfigs = ConcurrentHashMap<String, PrebufferConfig>()
    private val activePrebuffers = ConcurrentHashMap<String, PrebufferSession>()
    private val bitrateHistory = ConcurrentHashMap<String, CircularBuffer<Double>>()
    private val networkSpeedHistory = ConcurrentHashMap<String, CircularBuffer<Long>>()
    private val totalPrefetched = AtomicLong(0)
    private val totalUsed = AtomicLong(0)
    private val totalWasted = AtomicLong(0)

    data class PrebufferConfig(
        val streamId: String,
        val chunkSize: Int,
        val targetDurationMs: Long,
        val minChunks: Int = MIN_PREFETCH_CHUNKS,
        val maxChunks: Int = MAX_PREFETCH_CHUNKS,
        val aggressiveMode: Boolean = false,
        val priority: Int = 0
    )

    data class PrebufferSession(
        val streamId: String,
        val config: PrebufferConfig,
        val currentChunk: Long = 0,
        val prefetchedChunks: java.util.concurrent.ConcurrentSkipListSet<Long> = java.util.concurrent.ConcurrentSkipListSet(),
        val startTime: Long = System.currentTimeMillis(),
        var bytesPrefetched: Long = 0,
        var bytesUsed: Long = 0,
        var lastSpeedMbps: Double = 0.0
    ) {
        fun getProgress(): Double = if (config.maxChunks > 0) {
            prefetchedChunks.size.toDouble() / config.maxChunks
        } else 0.0

        fun getEfficiency(): Double = if (bytesPrefetched > 0) {
            bytesUsed.toDouble() / bytesPrefetched
        } else 0.0
    }

    data class NetworkStats(
        val currentSpeedMbps: Double,
        val avgSpeedMbps: Double,
        val minSpeedMbps: Double,
        val maxSpeedMbps: Double,
        val stability: Double, // 0-1, higher = more stable
        val trend: Trend
    )

    enum class Trend {
        IMPROVING, STABLE, DEGRADING
    }

    private class CircularBuffer<T>(capacity: Int) {
        private val buffer = Array<Any?>(capacity) { null }
        private var head = 0
        private var size = 0
        private val lock = Any()

        fun add(item: T) {
            synchronized(lock) {
                buffer[head] = item
                head = (head + 1) % capacity
                if (size < capacity) size++
            }
        }

        fun getAll(): List<T> {
            synchronized(lock) {
                return if (size < capacity) {
                    buffer.take(size).filterNotNull() as List<T>
                } else {
                    val result = mutableListOf<T>()
                    for (i in 0 until capacity) {
                        val idx = (head + i) % capacity
                        buffer[idx]?.let { result.add(it as T) }
                    }
                    result
                }
            }
        }

        fun getAverage(): Double {
            val items = getAll()
            return if (items.isEmpty()) 0.0 else items.map { (it as? Number)?.toDouble() ?: 0.0 }.average()
        }

        fun getMin(): Double {
            val items = getAll()
            return if (items.isEmpty()) 0.0 else items.map { (it as? Number)?.toDouble() ?: Double.MAX_VALUE }.minOrNull() ?: 0.0
        }

        fun getMax(): Double {
            val items = getAll()
            return if (items.isEmpty()) 0.0 else items.map { (it as? Number)?.toDouble() ?: 0.0 }.maxOrNull() ?: 0.0
        }

        fun getTrend(): Trend {
            val items = getAll().map { (it as? Number)?.toDouble() ?: 0.0 }
            if (items.size < 3) return Trend.STABLE
            
            val recent = items.takeLast(3).average()
            val older = items.dropLast(3).takeLast(3).average()
            val diff = recent - older
            
            return when {
                diff > older * 0.1 -> Trend.IMPROVING
                diff < -older * 0.1 -> Trend.DEGRADING
                else -> Trend.STABLE
            }
        }
    }

    fun registerStream(streamId: String, chunkSize: Int, estimatedBitrateKbps: Int, isLive: Boolean = false) {
        val targetDuration = if (isLive) 8000L else 15000L // 8s for live, 15s for VOD
        val chunkDurationMs = (chunkSize * 8.0 / estimatedBitrateKbps * 1000).toLong()
        val chunksNeeded = max(2, (targetDuration / chunkDurationMs).toInt().coerceIn(2, 8))
        
        val config = PrebufferConfig(
            streamId = streamId,
            chunkSize = chunkSize,
            targetDurationMs = targetDuration,
            minChunks = 2,
            maxChunks = chunksNeeded,
            aggressiveMode = !isLive && estimatedBitrateKbps > 5000,
            priority = if (isLive) 10 else 5
        )
        
        prebufferConfigs[streamId] = config
        bitrateHistory[streamId] = CircularBuffer(BITRATE_HISTORY_SIZE)
        networkSpeedHistory[streamId] = CircularBuffer(BITRATE_HISTORY_SIZE)
        
        Log.d(TAG, "Registered stream $streamId: chunkSize=${chunkSize/1024}KB, targetDuration=${targetDuration}ms, maxChunks=$chunksNeeded")
    }

    fun recordChunkDownload(streamId: String, chunkIndex: Long, bytes: Int, durationMs: Long) {
        val speedMbps = (bytes * 8.0 / durationMs).toDouble()
        
        networkSpeedHistory.getOrPut(streamId) { CircularBuffer(BITRATE_HISTORY_SIZE) }.add(speedMbps.toLong())
        
        activePrebuffers[streamId]?.let { session ->
            session.bytesPrefetched += bytes
            session.lastSpeedMbps = speedMbps
            session.prefetchedChunks.add(chunkIndex)
        }
    }

    fun recordChunkConsumed(streamId: String, chunkIndex: Long, bytes: Int) {
        activePrebuffers[streamId]?.let { session ->
            session.bytesUsed += bytes
            session.prefetchedChunks.remove(chunkIndex)
        }
    }

    fun getOptimalPrebufferChunks(streamId: String): Int {
        val config = prebufferConfigs[streamId] ?: return 2
        val networkStats = getNetworkStats(streamId)
        
        return when {
            networkStats.currentSpeedMbps <= 0 -> config.minChunks
            networkStats.trend == Trend.DEGRADING -> (config.maxChunks * 1.5).toInt().coerceIn(config.minChunks, config.maxChunks)
            networkStats.stability < 0.5 -> (config.maxChunks * 1.3).toInt().coerceIn(config.minChunks, config.maxChunks)
            networkStats.avgSpeedMbps > 25.0 -> config.minChunks // Fast network, less buffer needed
            config.aggressiveMode -> config.maxChunks
            else -> ((config.maxChunks + config.minChunks) / 2.0).toInt()
        }
    }

    fun getNetworkStats(streamId: String): NetworkStats {
        val history = networkSpeedHistory[streamId]
        val speeds = history?.getAll() ?: emptyList()
        
        if (speeds.isEmpty()) {
            return NetworkStats(0.0, 0.0, 0.0, 0.0, 1.0, Trend.STABLE)
        }
        
        val speedsMbps = speeds.map { it.toDouble() }
        val current = speedsMbps.lastOrNull() ?: 0.0
        val avg = speedsMbps.average()
        val min = speedsMbps.minOrNull() ?: 0.0
        val max = speedsMbps.maxOrNull() ?: 0.0
        
        val variance = speedsMbps.map { (it - avg) * (it - avg) }.average()
        val stdDev = Math.sqrt(variance)
        val stability = if (avg > 0) 1.0 - min(stdDev / avg, 1.0) else 1.0
        
        return NetworkStats(
            currentSpeedMbps = current,
            avgSpeedMbps = avg,
            minSpeedMbps = min,
            maxSpeedMbps = max,
            stability = stability,
            trend = history?.getTrend() ?: Trend.STABLE
        )
    }

    fun shouldPrebuffer(streamId: String, currentChunk: Long): Boolean {
        val config = prebufferConfigs[streamId] ?: return false
        val session = activePrebuffers[streamId]
        
        if (session == null) {
            // No active prebuffer, start one
            activePrebuffers[streamId] = PrebufferSession(streamId, config, currentChunk = currentChunk)
            return true
        }
        
        val optimalChunks = getOptimalPrebufferChunks(streamId)
        val currentProgress = session.getProgress()
        
        return currentProgress < 0.7 || session.prefetchedChunks.size < optimalChunks
    }

    fun getNextChunksToPrefetch(streamId: String, currentChunk: Long): List<Long> {
        val config = prebufferConfigs[streamId] ?: return emptyList()
        val session = activePrebuffers[streamId]
        
        val optimalChunks = getOptimalPrebufferChunks(streamId)
        val alreadyPrefetched = session?.prefetchedChunks ?: java.util.concurrent.ConcurrentSkipListSet<Long>()
        val needed = optimalChunks - alreadyPrefetched.size
        
        if (needed <= 0) return emptyList()
        
        val chunks = mutableListOf<Long>()
        var nextChunk = currentChunk + 1
        
        while (chunks.size < needed && nextChunk < currentChunk + config.maxChunks.toLong() * 2) {
            if (nextChunk !in alreadyPrefetched) {
                chunks.add(nextChunk)
            }
            nextChunk++
        }
        
        return chunks
    }

    fun markChunkPrefetched(streamId: String, chunkIndex: Long) {
        activePrebuffers[streamId]?.prefetchedChunks?.add(chunkIndex)
    }

    fun getPrebufferEfficiency(streamId: String): Double {
        return activePrebuffers[streamId]?.getEfficiency() ?: 0.0
    }

    fun stopPrebuffer(streamId: String) {
        val session = activePrebuffers.remove(streamId)
        if (session != null) {
            val efficiency = session.getEfficiency()
            totalPrefetched.addAndGet(session.bytesPrefetched)
            totalUsed.addAndGet(session.bytesUsed)
            totalWasted.addAndGet(session.bytesPrefetched - session.bytesUsed)
            Log.d(TAG, "Stopped prebuffer for $streamId: efficiency=${String.format("%.1f%%", efficiency * 100)}")
        }
    }

    fun getGlobalStats(): Map<String, Any> {
        val totalPrefetchedBytes = totalPrefetched.get()
        val totalUsedBytes = totalUsed.get()
        val globalEfficiency = if (totalPrefetchedBytes > 0) totalUsedBytes.toDouble() / totalPrefetchedBytes else 0.0
        
        val activeSessions = activePrebuffers.values.map { session ->
            mapOf(
                "streamId" to session.streamId,
                "progress" to String.format("%.1f%%", session.getProgress() * 100),
                "efficiency" to String.format("%.1f%%", session.getEfficiency() * 100),
                "prefetchedMB" to session.bytesPrefetched / 1024 / 1024,
                "usedMB" to session.bytesUsed / 1024 / 1024,
                "speedMbps" to String.format("%.1f", session.lastSpeedMbps)
            )
        }
        
        return mapOf(
            "totalPrefetchedMB" to totalPrefetchedBytes / 1024 / 1024,
            "totalUsedMB" to totalUsedBytes / 1024 / 1024,
            "globalEfficiency" to String.format("%.1f%%", globalEfficiency * 100),
            "activeSessions" to activePrebuffers.size,
            "sessions" to activeSessions
        )
    }

    fun trimMemory(level: Int) {
        val reduction = when (level) {
            android.app.ActivityManager.TRIM_MEMORY_RUNNING_MODERATE -> 0.2f
            android.app.ActivityManager.TRIM_MEMORY_RUNNING_LOW -> 0.4f
            android.app.ActivityManager.TRIM_MEMORY_RUNNING_CRITICAL -> 0.6f
            android.app.ActivityManager.TRIM_MEMORY_UI_HIDDEN -> 0.3f
            android.app.ActivityManager.TRIM_MEMORY_BACKGROUND -> 0.5f
            android.app.ActivityManager.TRIM_MEMORY_COMPLETE -> 0.8f
            else -> 0f
        }

        if (reduction > 0) {
            activePrebuffers.values.forEach { session ->
                val currentChunks = session.prefetchedChunks.size
                val targetChunks = (currentChunks * (1 - reduction)).toInt()
                while (session.prefetchedChunks.size > targetChunks) {
                    session.prefetchedChunks.pollFirst()
                }
            }
        }
    }

    fun shutdown() {
        scope.coroutineContext[Job]?.cancel()
        activePrebuffers.clear()
        prebufferConfigs.clear()
        bitrateHistory.clear()
        networkSpeedHistory.clear()
    }
}