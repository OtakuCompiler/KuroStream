package com.kurostream.torrent.prioritization

import android.util.Log
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentInfo
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamingPiecePrioritizer @Inject constructor() {

    private val TAG = "StreamingPiecePrioritizer"

    data class PrioritizationConfig(
        val headerPieceCount: Int = 10,
        val trailingPieceCount: Int = 5,
        val lookaheadWindow: Int = 50,
        val behindWindow: Int = 10,
        val deadlineMs: Long = 30_000L,
    )

    fun prioritizeForStreaming(
        handle: TorrentHandle,
        torrentInfo: TorrentInfo,
        playbackPositionBytes: Long = 0,
        totalFileSize: Long = 0,
        config: PrioritizationConfig = PrioritizationConfig(),
    ) {
        if (!handle.isValid) return

        val numPieces = torrentInfo.numPieces()
        val pieceSize = torrentInfo.pieceLength()

        val priorities = IntArray(numPieces)

        val headerPieces = config.headerPieceCount.coerceAtMost(numPieces)
        for (i in 0 until headerPieces) {
            priorities[i] = 7 // highest priority
        }

        val currentPiece = if (totalFileSize > 0 && pieceSize > 0) {
            (playbackPositionBytes / pieceSize).toInt().coerceIn(0, numPieces - 1)
        } else {
            0
        }

        val startPiece = (currentPiece - config.behindWindow).coerceAtLeast(headerPieces)
        val endPiece = (currentPiece + config.lookaheadWindow).coerceAtMost(numPieces - config.trailingPieceCount)

        for (i in startPiece until endPiece) {
            val distance = kotlin.math.abs(i - currentPiece)
            priorities[i] = when {
                distance < 10 -> 7
                distance < 25 -> 6
                distance < 50 -> 4
                else -> 2
            }
        }

        val trailingStart = (numPieces - config.trailingPieceCount).coerceAtLeast(endPiece)
        for (i in trailingStart until numPieces) {
            priorities[i] = 5
        }

        for (i in 0 until numPieces) {
            if (priorities[i] == 0) {
                priorities[i] = 1
            }
        }

        try {
            handle.setPiecePriorities(priorities)
            Log.d(TAG, "Prioritized ${numPieces} pieces for streaming at position $currentPiece")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set piece priorities", e)
        }
    }

    fun updatePlaybackPosition(
        handle: TorrentHandle,
        torrentInfo: TorrentInfo,
        newPositionBytes: Long,
        totalFileSize: Long,
        config: PrioritizationConfig = PrioritizationConfig(),
    ) {
        if (!handle.isValid) return

        val numPieces = torrentInfo.numPieces()
        val pieceSize = torrentInfo.pieceLength()
        if (pieceSize <= 0 || numPieces <= 0) return

        val newPiece = (newPositionBytes / pieceSize).toInt().coerceIn(0, numPieces - 1)

        val s = scope ?: return
        s.launch(Dispatchers.IO) {
            val priorities = IntArray(numPieces)

            val headerPieces = config.headerPieceCount.coerceAtMost(numPieces)
            for (i in 0 until headerPieces) {
                priorities[i] = 7
            }

            val startPiece = (newPiece - config.behindWindow).coerceAtLeast(headerPieces)
            val endPiece = (newPiece + config.lookaheadWindow).coerceAtMost(numPieces - config.trailingPieceCount)

            for (i in startPiece until endPiece) {
                val distance = kotlin.math.abs(i - newPiece)
                priorities[i] = when {
                    distance < 10 -> 7
                    distance < 25 -> 6
                    distance < 50 -> 4
                    else -> 2
                }
            }

            val trailingStart = (numPieces - config.trailingPieceCount).coerceAtLeast(endPiece)
            for (i in trailingStart until numPieces) {
                priorities[i] = 5
            }

            for (i in 0 until numPieces) {
                if (priorities[i] == 0) priorities[i] = 1
            }

            try {
                handle.setPiecePriorities(priorities)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update piece priorities", e)
            }
        }
    }

    private var scope: CoroutineScope? = null

    fun attachScope(scope: CoroutineScope) {
        this.scope = scope
    }
}
