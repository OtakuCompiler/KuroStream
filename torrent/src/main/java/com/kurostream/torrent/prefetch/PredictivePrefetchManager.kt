package com.kurostream.torrent.prefetch

import android.util.Log
import com.kurostream.torrent.domain.TorrentInfo
import com.kurostream.torrent.domain.TorrentStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PredictivePrefetchManager @Inject constructor() {

    private val TAG = "PredictivePrefetch"

    data class PrefetchPrediction(
        val infoHash: String,
        val name: String,
        val magnetUri: String,
        val confidence: Float,
        val predictedAt: Long,
    )

    data class WatchPattern(
        val seriesName: String,
        val lastWatchedEpisode: Int,
        val watchCount: Int,
        val avgIntervalMs: Long,
        val lastWatchTime: Long,
    )

    private val watchPatterns = ConcurrentHashMap<String, WatchPattern>()
    private val prefetched = ConcurrentHashMap<String, PrefetchPrediction>()
    private val activePrefetchJobs = ConcurrentHashMap<String, Job>()

    private val _prefetchState = MutableStateFlow(PrefetchState())
    val prefetchState: StateFlow<PrefetchState> = _prefetchState.asStateFlow()

    data class PrefetchState(
        val activePrefetches: Int = 0,
        val completedPrefetches: Int = 0,
        val predictions: List<PrefetchPrediction> = emptyList(),
    )

    fun recordWatch(seriesName: String, episodeNumber: Int) {
        val existing = watchPatterns[seriesName]
        val now = System.currentTimeMillis()

        if (existing != null) {
            val interval = now - existing.lastWatchTime
            val newAvgInterval = if (existing.watchCount > 0) {
                (existing.avgIntervalMs * existing.watchCount + interval) / (existing.watchCount + 1)
            } else {
                interval
            }
            watchPatterns[seriesName] = existing.copy(
                lastWatchedEpisode = episodeNumber,
                watchCount = existing.watchCount + 1,
                avgIntervalMs = newAvgInterval,
                lastWatchTime = now,
            )
        } else {
            watchPatterns[seriesName] = WatchPattern(
                seriesName = seriesName,
                lastWatchedEpisode = episodeNumber,
                watchCount = 1,
                avgIntervalMs = 0,
                lastWatchTime = now,
            )
        }
    }

    fun predictNextContent(): List<PrefetchPrediction> {
        val now = System.currentTimeMillis()
        val predictions = mutableListOf<PrefetchPrediction>()

        for ((name, pattern) in watchPatterns) {
            if (pattern.watchCount < 2) continue
            if (pattern.avgIntervalMs <= 0) continue

            val timeSinceLastWatch = now - pattern.lastWatchTime
            val expectedInterval = pattern.avgIntervalMs
            val confidence = when {
                timeSinceLastWatch > expectedInterval * 0.8f -> 0.9f
                timeSinceLastWatch > expectedInterval * 0.5f -> 0.6f
                timeSinceLastWatch > expectedInterval * 0.3f -> 0.3f
                else -> 0.1f
            }

            if (confidence > 0.3f) {
                predictions.add(
                    PrefetchPrediction(
                        infoHash = "",
                        name = "$name - Episode ${pattern.lastWatchedEpisode + 1}",
                        magnetUri = "",
                        confidence = confidence,
                        predictedAt = now,
                    )
                )
            }
        }

        _prefetchState.value = _prefetchState.value.copy(predictions = predictions)
        return predictions.sortedByDescending { it.confidence }
    }

    fun isAlreadyPrefetched(infoHash: String): Boolean {
        return prefetched.containsKey(infoHash)
    }

    fun markPrefetched(infoHash: String) {
        prefetched[infoHash] = PrefetchPrediction(
            infoHash = infoHash,
            name = "",
            magnetUri = "",
            confidence = 1.0f,
            predictedAt = System.currentTimeMillis(),
        )
        _prefetchState.value = _prefetchState.value.copy(
            completedPrefetches = prefetched.size,
        )
    }

    fun startPrefetchJob(infoHash: String, job: Job) {
        activePrefetchJobs[infoHash]?.cancel()
        activePrefetchJobs[infoHash] = job
        _prefetchState.value = _prefetchState.value.copy(
            activePrefetches = activePrefetchJobs.size,
        )
    }

    fun cancelPrefetch(infoHash: String) {
        activePrefetchJobs[infoHash]?.cancel()
        activePrefetchJobs.remove(infoHash)
        _prefetchState.value = _prefetchState.value.copy(
            activePrefetches = activePrefetchJobs.size,
        )
    }

    fun cancelAll() {
        activePrefetchJobs.values.forEach { it.cancel() }
        activePrefetchJobs.clear()
        _prefetchState.value = PrefetchState()
    }
}
