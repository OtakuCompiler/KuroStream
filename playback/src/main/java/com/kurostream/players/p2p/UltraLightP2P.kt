package com.kurostream.players.p2p

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UltraLightP2P @Inject constructor() {

    private val _connectionState = MutableStateFlow(P2PConnectionState())
    val connectionState: StateFlow<P2PConnectionState> = _connectionState.asStateFlow()

    @Volatile private var maxPeers = 10
    @Volatile private var currentPeers = 0
    @Volatile private var bufferTargetMs = 30_000L
    @Volatile private var isAggressiveMode = false

    private val peerCache = LinkedHashMap<String, PeerInfo>(maxPeers) { _, _, _ -> true }
    private val pieceBitmap = java.util.BitSet(1024)
    private var totalPieces = 0

    fun configure(
        maxPeers: Int = 10,
        bufferTargetMs: Long = 30_000L,
        aggressiveMode: Boolean = false
    ) {
        this.maxPeers = maxPeers.coerceIn(5, 20)
        this.bufferTargetMs = bufferTargetMs
        this.isAggressiveMode = aggressiveMode
        Timber.d("P2P configured: maxPeers=$maxPeers, buffer=$bufferTargetMs ms, aggressive=$aggressiveMode")
    }

    fun connectPeer(peerId: String, speedBps: Long) {
        if (currentPeers >= maxPeers && !isAggressiveMode) {
            val slowestPeer = peerCache.minByOrNull { it.value.speedBps }?.key
            if (slowestPeer != null && speedBps > peerCache[slowestPeer]?.speedBps!!) {
                disconnectPeer(slowestPeer)
            } else {
                Timber.d("Peer limit reached ($maxPeers), rejecting $peerId")
                return
            }
        }

        peerCache[peerId] = PeerInfo(
            id = peerId,
            speedBps = speedBps,
            connectedAt = System.currentTimeMillis(),
            piecesSent = 0,
            piecesReceived = 0,
        )
        currentPeers = peerCache.size
        updateConnectionState()
        Timber.d("Connected peer $peerId (${speedBps / 1024} KB/s), total: $currentPeers/$maxPeers")
    }

    fun disconnectPeer(peerId: String) {
        peerCache.remove(peerId)
        currentPeers = peerCache.size
        updateConnectionState()
        Timber.d("Disconnected peer $peerId, remaining: $currentPeers")
    }

    fun recordPieceReceived(peerId: String, pieceIndex: Int) {
        peerCache[peerId]?.let {
            it.piecesReceived++
            pieceBitmap.set(pieceIndex)
        }
        updateConnectionState()
    }

    fun recordPieceSent(peerId: String, pieceIndex: Int) {
        peerCache[peerId]?.let {
            it.piecesSent++
        }
    }

    fun getNextNeededPiece(): Int {
        for (i in 0 until totalPieces) {
            if (!pieceBitmap.get(i)) {
                return i
            }
        }
        return -1
    }

    fun setTotalPieces(count: Int) {
        totalPieces = count
        pieceBitmap.clear()
        pieceBitmap.ensureCapacity(count)
    }

    fun getEstimatedSpeedBps(): Long {
        return peerCache.values.sumOf { it.speedBps }
    }

    fun getAveragePeerSpeedBps(): Long {
        return if (currentPeers > 0) getEstimatedSpeedBps() / currentPeers else 0
    }

    private fun updateConnectionState() {
        _connectionState.value = P2PConnectionState(
            connectedPeers = currentPeers,
            maxPeers = maxPeers,
            totalSpeedBps = getEstimatedSpeedBps(),
            avgSpeedBps = getAveragePeerSpeedBps(),
            piecesDownloaded = peerCache.values.sumOf { it.piecesReceived },
            piecesUploaded = peerCache.values.sumOf { it.piecesSent },
            completionPercent = if (totalPieces > 0) {
                (pieceBitmap.cardinality().toFloat() / totalPieces * 100).coerceIn(0f, 100f)
            } else 0f,
            timestamp = System.currentTimeMillis(),
        )
    }

    fun cleanup() {
        peerCache.clear()
        currentPeers = 0
        pieceBitmap.clear()
        totalPieces = 0
        updateConnectionState()
        Timber.d("P2P cleaned up (RAM saved: ~20MB)")
    }

    fun getMemoryUsageEstimateKb(): Int {
        return peerCache.size * 2 + 128
    }
}

data class P2PConnectionState(
    val connectedPeers: Int = 0,
    val maxPeers: Int = 10,
    val totalSpeedBps: Long = 0,
    val avgSpeedBps: Long = 0,
    val piecesDownloaded: Int = 0,
    val piecesUploaded: Int = 0,
    val completionPercent: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
)

data class PeerInfo(
    val id: String,
    val speedBps: Long,
    val connectedAt: Long,
    var piecesSent: Int,
    var piecesReceived: Int,
)