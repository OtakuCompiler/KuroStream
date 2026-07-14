package com.kurostream.torrent.engine

import android.util.Log
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentInfo
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LazyVerifier @Inject constructor() {

    private val TAG = "LazyVerifier"

    data class PieceVerification(
        val pieceIndex: Int,
        val verified: Boolean,
        val timestamp: Long,
    )

    private val verificationResults = ConcurrentHashMap<String, MutableMap<Int, PieceVerification>>()
    private val verificationQueue = ConcurrentLinkedQueue<PieceVerifyRequest>()
    private val isRunning = AtomicBoolean(false)
    private var verificationJob: Job? = null

    data class PieceVerifyRequest(
        val infoHash: String,
        val pieceIndex: Int,
        val priority: VerifyPriority,
    )

    enum class VerifyPriority {
        IMMEDIATE,
        HIGH,
        LOW,
        IDLE,
    }

    data class VerifyStats(
        val totalQueued: Int,
        val verified: Int,
        val pending: Int,
        val skipped: Int,
    )

    fun enqueueVerification(infoHash: String, pieceIndex: Int, priority: VerifyPriority) {
        if (isAlreadyVerified(infoHash, pieceIndex)) return
        verificationQueue.offer(PieceVerifyRequest(infoHash, pieceIndex, priority))
    }

    fun verifyPieceImmediate(handle: TorrentHandle, pieceIndex: Int): Boolean {
        if (!handle.isValid) return false

        return try {
            val status = handle.status()
            val fileProgress = status.fileProgress
            val torrentInfo = handle.torrentFile() ?: return false

            val isPieceComplete = checkPieceComplete(handle, pieceIndex)

            if (isPieceComplete) {
                recordVerification(handle.infoHash().toHex(), pieceIndex, true)
                true
            } else {
                recordVerification(handle.infoHash().toHex(), pieceIndex, false)
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Piece verification failed for piece $pieceIndex", e)
            false
        }
    }

    fun startBackgroundVerification(scope: CoroutineScope) {
        if (isRunning.getAndSet(true)) return

        verificationJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                processVerificationQueue()
                delay(500)
            }
        }

        Log.i(TAG, "Lazy background verification started")
    }

    fun stopBackgroundVerification() {
        isRunning.set(false)
        verificationJob?.cancel()
        verificationJob = null
    }

    private suspend fun processVerificationQueue() {
        val batch = mutableListOf<PieceVerifyRequest>()

        val immediate = mutableListOf<PieceVerifyRequest>()
        val high = mutableListOf<PieceVerifyRequest>()
        val low = mutableListOf<PieceVerifyRequest>()
        val idle = mutableListOf<PieceVerifyRequest>()

        while (verificationQueue.isNotEmpty()) {
            val request = verificationQueue.poll() ?: break
            batch.add(request)
            when (request.priority) {
                VerifyPriority.IMMEDIATE -> immediate.add(request)
                VerifyPriority.HIGH -> high.add(request)
                VerifyPriority.LOW -> low.add(request)
                VerifyPriority.IDLE -> idle.add(request)
            }
        }

        val sorted = immediate + high + low + idle

        for (request in sorted.take(50)) {
            if (isAlreadyVerified(request.infoHash, request.pieceIndex)) continue

            // TODO: Implement actual SHA-1 hash verification against torrent metadata
            recordVerification(request.infoHash, request.pieceIndex, false)
        }
    }

    private fun checkPieceComplete(handle: TorrentHandle, pieceIndex: Int): Boolean {
        return try {
            val pieces = handle.torrentFile()?.numPieces() ?: return false
            if (pieceIndex >= pieces) return false
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun recordVerification(infoHash: String, pieceIndex: Int, verified: Boolean) {
        val map = verificationResults.getOrPut(infoHash) { mutableMapOf() }
        map[pieceIndex] = PieceVerification(
            pieceIndex = pieceIndex,
            verified = verified,
            timestamp = System.currentTimeMillis(),
        )
    }

    fun isAlreadyVerified(infoHash: String, pieceIndex: Int): Boolean {
        return verificationResults[infoHash]?.get(pieceIndex)?.verified == true
    }

    fun getVerificationStats(infoHash: String): VerifyStats {
        val map = verificationResults[infoHash] ?: emptyMap()
        val verified = map.values.count { it.verified }
        return VerifyStats(
            totalQueued = verified + verificationQueue.size,
            verified = verified,
            pending = verificationQueue.size,
            skipped = 0,
        )
    }

    fun clearForTorrent(infoHash: String) {
        verificationResults.remove(infoHash)
        verificationQueue.removeAll { it.infoHash == infoHash }
    }
}
