package com.kurostream.players.buffer

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZeroSeekPlaybackManager @Inject constructor() {

    private val TAG = "ZeroSeekPlayback"

    private val _firstPieceAvailable = MutableSharedFlow<FirstPieceEvent>(extraBufferCapacity = 5)
    val firstPieceAvailable: SharedFlow<FirstPieceEvent> = _firstPieceAvailable.asSharedFlow()

    private val _playbackReady = MutableSharedFlow<PlaybackReadyEvent>(extraBufferCapacity = 5)
    val playbackReady: SharedFlow<PlaybackReadyEvent> = _playbackReady.asSharedFlow()

    private val firstPieceTimeMs = AtomicLong(0)
    private val playbackStartTimeMs = AtomicLong(0)
    private val isWaitingForData = AtomicBoolean(false)
    private var stallWatchJob: Job? = null

    data class FirstPieceEvent(
        val infoHash: String,
        val fileIndex: Int,
        val pieceIndex: Int,
        val timeToFirstPieceMs: Long,
    )

    data class PlaybackReadyEvent(
        val infoHash: String,
        val fileIndex: Int,
        val timeToPlaybackMs: Long,
        val bufferLevelMs: Long,
    )

    data class PlaybackMetrics(
        val timeToFirstPieceMs: Long = 0,
        val timeToPlaybackMs: Long = 0,
        val stallsCount: Int = 0,
        val totalStallTimeMs: Long = 0,
        val minBufferLevelMs: Long = Long.MAX_VALUE,
    )

    private val metrics = PlaybackMetrics()
    private var stallCount = 0
    private var totalStallTime = 0L
    private var stallStart = 0L

    fun onFirstPieceAvailable(infoHash: String, fileIndex: Int, pieceIndex: Int) {
        val now = System.currentTimeMillis()
        if (firstPieceTimeMs.get() == 0L) {
            firstPieceTimeMs.set(now)
        }

        val timeToFirstPiece = if (playbackStartTimeMs.get() > 0) {
            now - playbackStartTimeMs.get()
        } else {
            now - firstPieceTimeMs.get()
        }

        CoroutineScope(Dispatchers.IO).launch {
            _firstPieceAvailable.emit(
                FirstPieceEvent(
                    infoHash = infoHash,
                    fileIndex = fileIndex,
                    pieceIndex = pieceIndex,
                    timeToFirstPieceMs = timeToFirstPiece,
                )
            )
        }

        Log.i(TAG, "First piece available for $infoHash: file=$fileIndex, piece=$pieceIndex, " +
                "time=${timeToFirstPiece}ms")
    }

    fun onPlaybackStarted(infoHash: String, fileIndex: Int, bufferLevelMs: Long) {
        val now = System.currentTimeMillis()
        playbackStartTimeMs.set(now)
        isWaitingForData.set(false)

        val timeToPlayback = if (firstPieceTimeMs.get() > 0) {
            now - firstPieceTimeMs.get()
        } else {
            0L
        }

        CoroutineScope(Dispatchers.IO).launch {
            _playbackReady.emit(
                PlaybackReadyEvent(
                    infoHash = infoHash,
                    fileIndex = fileIndex,
                    timeToPlaybackMs = timeToPlayback,
                    bufferLevelMs = bufferLevelMs,
                )
            )
        }

        Log.i(TAG, "Playback started for $infoHash: timeToPlayback=${timeToPlayback}ms, buffer=${bufferLevelMs}ms")
    }

    fun onBufferStall(infoHash: String) {
        if (isWaitingForData.getAndSet(true)) return
        stallStart = System.currentTimeMillis()
        stallCount++
        Log.w(TAG, "Buffer stall detected for $infoHash")
    }

    fun onBufferResumed(infoHash: String) {
        if (!isWaitingForData.getAndSet(false)) return
        val stallDuration = System.currentTimeMillis() - stallStart
        totalStallTime += stallDuration
        Log.i(TAG, "Buffer resumed for $infoHash after ${stallDuration}ms stall")
    }

    fun startStallWatch(scope: CoroutineScope, getBufferLevelMs: suspend () -> Long) {
        stallWatchJob?.cancel()
        stallWatchJob = scope.launch {
            while (isActive) {
                val bufferLevel = getBufferLevelMs()
                if (bufferLevel < 500) {
                    onBufferStall("")
                } else if (bufferLevel > 2000) {
                    onBufferResumed("")
                }
                delay(200)
            }
        }
    }

    fun stopStallWatch() {
        stallWatchJob?.cancel()
        stallWatchJob = null
    }

    fun getMetrics(): PlaybackMetrics = metrics.copy(
        stallsCount = stallCount,
        totalStallTimeMs = totalStallTime,
    )

    fun reset() {
        firstPieceTimeMs.set(0)
        playbackStartTimeMs.set(0)
        isWaitingForData.set(false)
        stallCount = 0
        totalStallTime = 0L
        stallStart = 0L
        stopStallWatch()
    }
}
