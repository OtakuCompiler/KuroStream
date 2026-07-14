package com.kurostream.torrent.streaming

import android.util.Log
import com.frostwire.jlibtorrent.Session
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.swig.add_torrent_params_flags_t
import com.frostwire.jlibtorrent.swig.piece_priority_t
import com.kurostream.torrent.domain.TorrentInfo as DomainTorrentInfo
import com.kurostream.torrent.domain.TorrentStatus
import com.kurostream.torrent.engine.TorrentEngine
import com.kurostream.torrent.prioritization.StreamingPiecePrioritizer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamingTorrentManager @Inject constructor(
    private val torrentEngine: TorrentEngine,
    private val streamingPiecePrioritizer: StreamingPiecePrioritizer,
) {

    private val TAG = "StreamingTorrentManager"

    private val streamStates = ConcurrentHashMap<String, StreamState>()
    private val healthMonitorJobs = ConcurrentHashMap<String, Job>()
    private val retryJobs = ConcurrentHashMap<String, Job>()

    private val _globalStreamHealth = MutableStateFlow(StreamHealth())
    val globalStreamHealth: StateFlow<StreamHealth> = _globalStreamHealth.distinctUntilChanged().asStateFlow()

    data class StreamState(
        val infoHash: String,
        val fileIndex: Int,
        val playbackPositionMs: Long = 0L,
        val bufferHealth: BufferHealth = BufferHealth(),
        val pieceRequests: MutableList<PieceRequest> = mutableListOf(),
        var consecutiveUnderruns: Int = 0,
        var lastRetryAttempt: Long = 0,
        var totalRetries: Int = 0,
        var isStreaming: Boolean = false,
        var fallbackTriggered: Boolean = false,
    )

    data class BufferHealth(
        val bufferedMs: Long = 0,
        val bufferPercentage: Float = 0f,
        val downloadSpeedBps: Long = 0,
        val piecesAhead: Int = 0,
        val piecesNeeded: Int = 0,
        val estimatedBufferTimeSec: Float = 0f,
        val isUnderrun: Boolean = false,
        val consecutiveUnderruns: Int = 0,
        val lastUpdate: Long = System.currentTimeMillis(),
    )

    data class PieceRequest(
        val pieceIndex: Int,
        val priority: Int,
        val requestedAt: Long,
        var completed: Boolean = false,
    )

    data class StreamHealth(
        val activeStreams: Int = 0,
        val healthyStreams: Int = 0,
        val underrunStreams: Int = 0,
        val totalDownloadSpeed: Long = 0,
        val avgBufferHealth: Float = 0f,
        val fallbackActive: Int = 0,
        val lastUpdate: Long = System.currentTimeMillis(),
    )

    private val MIN_BUFFER_MS = 30_000L
    private val TARGET_BUFFER_MS = 60_000L
    private val CRITICAL_BUFFER_MS = 10_000L
    private val MAX_RETRIES = 5
    private val BASE_RETRY_DELAY_MS = 1000L
    private val MAX_RETRY_DELAY_MS = 30_000L

    fun startStreaming(infoHash: String, fileIndex: Int): Result<StreamState> {
        val handle = torrentEngine.torrents[infoHash] ?: return Result.failure(Exception("Torrent not found"))
        val torrentInfo = handle.torrentFile() ?: return Result.failure(Exception("Torrent metadata not available"))
        val files = torrentInfo.files()
        if (fileIndex >= files.numFiles()) return Result.failure(Exception("Invalid file index"))

        val file = files.fileAt(fileIndex)
        val filePath = file.path
        val fileSize = file.size
        val pieceSize = torrentInfo.pieceLength()
        val startPiece = (file.offset / pieceSize).toInt()
        val endPiece = ((file.offset + fileSize - 1) / pieceSize).toInt()

        val state = StreamState(
            infoHash = infoHash,
            fileIndex = fileIndex,
        )

        streamStates[infoHash] = state
        startHealthMonitor(infoHash, fileIndex, fileSize, pieceSize, startPiece, endPiece)
        startSequentialPrefetch(infoHash, handle, startPiece, endPiece)

        state.isStreaming = true
        updateGlobalHealth()

        return Result.success(state)
    }

    fun updatePlaybackPosition(infoHash: String, positionMs: Long, durationMs: Long, bufferedMs: Long) {
        val state = streamStates[infoHash] ?: return
        val handle = torrentEngine.torrents[infoHash] ?: return
        val torrentInfo = handle.torrentFile() ?: return
        val files = torrentInfo.files()
        if (state.fileIndex >= files.numFiles()) return
        val file = files.fileAt(state.fileIndex)
        val fileSize = file.size
        val pieceSize = torrentInfo.pieceLength()

        val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
        val currentByte = (progress * fileSize).toLong()
        val currentPiece = (currentByte / pieceSize).toInt()

        val bufferPercentage = if (durationMs > 0) bufferedMs.toFloat() / durationMs else 0f
        val downloadSpeed = handle.status().downloadRate

        val piecesAhead = calculatePiecesAhead(handle, state.fileIndex, currentPiece, pieceSize)
        val piecesNeeded = maxOf(1, (MIN_BUFFER_MS * downloadSpeed / pieceSize).toInt())

        val estimatedBufferTimeSec = if (downloadSpeed > 0) {
            (bufferedMs / 1000f).coerceAtLeast(0f)
        } else 0f

        val isUnderrun = bufferedMs < CRITICAL_BUFFER_MS && state.isStreaming
        val newConsecutiveUnderruns = if (isUnderrun) state.consecutiveUnderruns + 1 else 0

        val bufferHealth = BufferHealth(
            bufferedMs = bufferedMs,
            bufferPercentage = bufferPercentage,
            downloadSpeedBps = downloadSpeed,
            piecesAhead = piecesAhead,
            piecesNeeded = piecesNeeded,
            estimatedBufferTimeSec = estimatedBufferTimeSec,
            isUnderrun = isUnderrun,
            consecutiveUnderruns = newConsecutiveUnderruns,
            lastUpdate = System.currentTimeMillis(),
        )

        streamStates[infoHash] = state.copy(
            playbackPositionMs = positionMs,
            bufferHealth = bufferHealth,
            consecutiveUnderruns = newConsecutiveUnderruns,
        )

        if (isUnderrun && !state.fallbackTriggered) {
            handleBufferUnderrun(infoHash, handle, torrentInfo, state)
        }

        updateGlobalHealth()
    }

    private fun calculatePiecesAhead(handle: TorrentHandle, fileIndex: Int, currentPiece: Int, pieceSize: Int): Int {
        val status = handle.status()
        val torrentInfo = handle.torrentFile() ?: return 0
        val files = torrentInfo.files()
        if (fileIndex >= files.numFiles()) return 0
        val file = files.fileAt(fileIndex)
        val startPiece = (file.offset / pieceSize).toInt()
        val endPiece = ((file.offset + file.size - 1) / pieceSize).toInt()

        var count = 0
        for (i in currentPiece..endPiece) {
            if (status.piecePriorities?.get(i) != piece_priority_t.priority_0.value) {
                count++
            }
        }
        return count
    }

    private fun startSequentialPrefetch(
        infoHash: String,
        handle: TorrentHandle,
        startPiece: Int,
        endPiece: Int
    ) {
        val state = streamStates[infoHash] ?: return
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        scope.launch {
            var currentPiece = startPiece
            while (isActive && currentPiece <= endPiece && state.isStreaming) {
                val piecePriority = when {
                    currentPiece <= startPiece + 2 -> piece_priority_t.priority_7.value
                    currentPiece <= startPiece + 5 -> piece_priority_t.priority_5.value
                    currentPiece <= startPiece + 10 -> piece_priority_t.priority_3.value
                    else -> piece_priority_t.priority_1.value
                }

                handle.setPiecePriority(currentPiece, piecePriority)
                state.pieceRequests.add(PieceRequest(currentPiece, piecePriority, System.currentTimeMillis()))

                currentPiece++
                delay(100)
            }
        }
    }

    private fun startHealthMonitor(
        infoHash: String,
        fileIndex: Int,
        fileSize: Long,
        pieceSize: Int,
        startPiece: Int,
        endPiece: Int
    ) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        healthMonitorJobs[infoHash] = scope.launch {
            while (isActive) {
                val state = streamStates[infoHash] ?: break
                if (!state.isStreaming) break

                val handle = torrentEngine.torrents[infoHash] ?: break
                val torrentInfo = handle.torrentFile() ?: break

                val status = handle.status()
                val progress = status.progress
                val downloadSpeed = status.downloadRate
                val uploadSpeed = status.uploadRate
                val peers = status.numPeers
                val seeds = status.numSeeds

                val bufferHealth = state.bufferHealth.copy(
                    downloadSpeedBps = downloadSpeed,
                    piecesNeeded = maxOf(1, (MIN_BUFFER_MS * downloadSpeed / pieceSize).toInt()),
                )

                val swarmSpeed = downloadSpeed + uploadSpeed
                val pieceProgress = if (endPiece > startPiece) {
                    (status.pieces?.count { it } ?: 0).toFloat() / (endPiece - startPiece + 1)
                } else 0f

                Log.d(TAG, "Stream health: $infoHash buffer=${bufferHealth.bufferedMs}ms speed=${formatSpeed(downloadSpeed)} peers=$peers seeds=$seeds pieceProgress=${String.format("%.1f%%", pieceProgress * 100)}")

                delay(1000)
            }
        }
    }

    private fun handleBufferUnderrun(
        infoHash: String,
        handle: TorrentHandle,
        torrentInfo: TorrentInfo,
        state: StreamState
    ) {
        if (state.totalRetries >= MAX_RETRIES) {
            triggerHttpFallback(infoHash, state)
            return
        }

        val now = System.currentTimeMillis()
        val delaySinceLastRetry = now - state.lastRetryAttempt
        val exponentialDelay = (BASE_RETRY_DELAY_MS * (2.0).pow(state.totalRetries.toDouble())).toLong().coerceAtMost(MAX_RETRY_DELAY_MS)

        if (delaySinceLastRetry < exponentialDelay) {
            return
        }

        Log.w(TAG, "Buffer underrun detected for $infoHash, retry ${state.totalRetries + 1}/$MAX_RETRIES after ${exponentialDelay}ms")

        retryJobs[infoHash]?.cancel()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        retryJobs[infoHash] = scope.launch {
            delay(exponentialDelay)

            if (!isActive) return@launch

            val currentState = streamStates[infoHash] ?: return@launch
            if (currentState.bufferHealth.bufferedMs >= MIN_BUFFER_MS) return@launch

            streamingPiecePrioritizer.prioritizeForStreaming(
                handle,
                torrentInfo,
                currentState.playbackPositionMs,
                torrentInfo.files().fileAt(currentState.fileIndex).size
            )

            handle.forceReannounce()
            torrentEngine.seederHunt().startHunting(infoHash, handle, scope)

            streamStates[infoHash] = currentState.copy(
                lastRetryAttempt = System.currentTimeMillis(),
                totalRetries = currentState.totalRetries + 1,
            )
        }
    }

    private fun triggerHttpFallback(infoHash: String, state: StreamState) {
        Log.w(TAG, "Max retries reached for $infoHash, triggering HTTP fallback")
        streamStates[infoHash] = state.copy(fallbackTriggered = true)
        updateGlobalHealth()
    }

    fun getStreamState(infoHash: String): StreamState? = streamStates[infoHash]

    fun getBufferHealth(infoHash: String): BufferHealth? = streamStates[infoHash]?.bufferHealth

    fun isFallbackTriggered(infoHash: String): Boolean = streamStates[infoHash]?.fallbackTriggered == true

    fun stopStreaming(infoHash: String) {
        streamStates[infoHash]?.let { state ->
            streamStates[infoHash] = state.copy(isStreaming = false)
        }
        healthMonitorJobs[infoHash]?.cancel()
        healthMonitorJobs.remove(infoHash)
        retryJobs[infoHash]?.cancel()
        retryJobs.remove(infoHash)
        streamStates.remove(infoHash)
        updateGlobalHealth()
    }

    private fun updateGlobalHealth() {
        val states = streamStates.values
        val active = states.count { it.isStreaming }
        val healthy = states.count { it.bufferHealth.bufferedMs >= MIN_BUFFER_MS && it.isStreaming }
        val underrun = states.count { it.bufferHealth.isUnderrun }
        val fallback = states.count { it.fallbackTriggered }
        val totalSpeed = states.sumOf { it.bufferHealth.downloadSpeedBps }
        val streamingStates = states.filter { it.isStreaming }
        val avgHealth = if (streamingStates.isNotEmpty()) streamingStates.map { it.bufferHealth.bufferPercentage }.sum() / streamingStates.size else 0f

        _globalStreamHealth.value = StreamHealth(
            activeStreams = active,
            healthyStreams = healthy,
            underrunStreams = underrun,
            totalDownloadSpeed = totalSpeed,
            avgBufferHealth = avgHealth,
            fallbackActive = fallback,
            lastUpdate = System.currentTimeMillis(),
        )
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec < 1024 -> "${bytesPerSec} B/s"
            bytesPerSec < 1024 * 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
            bytesPerSec < 1024 * 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024))
            else -> String.format("%.1f GB/s", bytesPerSec / (1024.0 * 1024 * 1024))
        }
    }

    fun shutdown() {
        healthMonitorJobs.values.forEach { it.cancel() }
        healthMonitorJobs.clear()
        retryJobs.values.forEach { it.cancel() }
        retryJobs.clear()
        streamStates.clear()
    }

    sealed class Result<out T> {
        data class Success<out T>(val value: T) : Result<T>()
        data class Failure(val error: Exception) : Result<Nothing>()
        companion object {
            fun <T> success(value: T): Result<T> = Success(value)
            fun <T> failure(error: Exception): Result<T> = Failure(error)
        }
    }
}