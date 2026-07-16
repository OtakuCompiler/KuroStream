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

package com.kurostream.playback.p2p

import android.util.Log
import com.kurostream.playback.memory.AdaptivePrebufferManager
import com.kurostream.playback.memory.ZeroCopyBufferManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OptimizedP2PEngine @Inject constructor(
    private val bufferManager: ZeroCopyBufferManager,
    private val prebufferManager: AdaptivePrebufferManager
) {
    companion object {
        private const val TAG = "OptimizedP2PEngine"
        private const val DEFAULT_CHUNK_SIZE = 1024 * 1024
        private const val MAX_PEERS_2GB = 8
        private const val MAX_PEERS_3GB = 12
        private const val MAX_PEERS_4GB = 16
        private const val PIECE_TIMEOUT_MS = 5000
        private const val PEER_SCORE_DECAY = 0.95
        private const val MIN_PEER_SCORE = 0.1
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val peers = ConcurrentHashMap<String, Peer>()
    private val pieceAvailability = ConcurrentHashMap<Int, java.util.concurrent.ConcurrentSkipListSet<String>>()
    private val pendingRequests = ConcurrentHashMap<Long, PendingRequest>()
    private val pieceDownloadTimes = ConcurrentHashMap<Int, AtomicLong>()
    private val _state = MutableStateFlow(P2PState.IDLE)
    val state: StateFlow<P2PState> = _state
    private val _stats = MutableStateFlow(P2PStats())
    val stats: StateFlow<P2PStats> = _stats

    data class Peer(
        val id: String,
        val ip: String,
        val port: Int,
        val capabilities: PeerCapabilities,
        var score: Double = 1.0,
        var lastSeen: Long = System.currentTimeMillis(),
        var bytesDownloaded: Long = 0,
        var bytesUploaded: Long = 0,
        var activeRequests: Int = 0,
        var connectionQuality: ConnectionQuality = ConnectionQuality.UNKNOWN,
        var supportsDelta: Boolean = false,
        var latencyMs: Long = 0
    ) {
        fun updateScore(success: Boolean, latencyMs: Long, bytes: Long) {
            this.latencyMs = (this.latencyMs * 3 + latencyMs) / 4 // EMA
            
            if (success) {
                score = min(1.0, score * PEER_SCORE_DECAY + 0.1)
                bytesDownloaded += bytes
                connectionQuality = when {
                    latencyMs < 50 -> ConnectionQuality.EXCELLENT
                    latencyMs < 100 -> ConnectionQuality.GOOD
                    latencyMs < 200 -> ConnectionQuality.FAIR
                    else -> ConnectionQuality.POOR
                }
            } else {
                score = max(MIN_PEER_SCORE, score * 0.5)
                connectionQuality = ConnectionQuality.POOR
            }
            activeRequests = max(0, activeRequests - 1)
            lastSeen = System.currentTimeMillis()
        }

        fun getEffectiveScore(): Double {
            val agePenalty = min(0.3, (System.currentTimeMillis() - lastSeen) / 300000.0 * 0.3)
            val loadPenalty = activeRequests * 0.05
            return (score - agePenalty - loadPenalty).coerceAtLeast(MIN_PEER_SCORE)
        }
    }

    data class PeerCapabilities(
        val maxPieceSize: Int = DEFAULT_CHUNK_SIZE,
        val supportsCompression: Boolean = true,
        val supportsEncryption: Boolean = true,
        val supportsDeltaPieces: Boolean = false,
        val uploadSpeedKbps: Long = 0,
        val downloadSpeedKbps: Long = 0
    )

    enum class ConnectionQuality {
        UNKNOWN, EXCELLENT, GOOD, FAIR, POOR
    }

    enum class P2PState {
        IDLE, CONNECTING, STREAMING, BUFFERING, ERROR
    }

    data class PendingRequest(
        val pieceIndex: Int,
        val startTime: Long,
        val peerId: String,
        val retryCount: Int = 0
    )

    data class P2PStats(
        val connectedPeers: Int = 0,
        val activeDownloads: Int = 0,
        val downloadSpeedKbps: Long = 0,
        val uploadSpeedKbps: Long = 0,
        val bufferUsagePercent: Float = 0f,
        val pieceHitRate: Double = 0.0,
        val avgLatencyMs: Long = 0,
        val totalPeers: Int = 0
    )

    data class DeltaPiece(
        val basePieceIndex: Int,
        val deltaData: ByteArray,
        val checksum: Int
    )

    fun initialize(
        torrentHash: String,
        maxPeers: Int,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        enableDelta: Boolean = true
    ) {
        _state.value = P2PState.CONNECTING
        
        // Initialize buffer pool
        bufferManager.initializeBufferPool(chunkSize, maxPeers * 2)
        
        // Start peer discovery and connection
        scope.launch {
            discoverAndConnect(torrentHash, maxPeers, enableDelta)
        }
        
        // Start maintenance tasks
        startMaintenance()
        
        _state.value = P2PState.STREAMING
        Log.d(TAG, "P2P engine initialized for $torrentHash with maxPeers=$maxPeers")
    }

    private suspend fun discoverAndConnect(torrentHash: String, maxPeers: Int, enableDelta: Boolean) {
        // Simulated peer discovery - in real implementation, this would use DHT/trackers
        val discoveredPeers = discoverPeers(torrentHash)
        
        for (peerInfo in discoveredPeers.take(maxPeers)) {
            val peer = Peer(
                id = peerInfo.id,
                ip = peerInfo.ip,
                port = peerInfo.port,
                capabilities = PeerCapabilities(
                    supportsDeltaPieces = enableDelta && peerInfo.supportsDelta,
                    maxPieceSize = chunkSize
                )
            )
            peers[peer.id] = peer
            connectToPeer(peer, torrentHash)
        }
    }

    private fun discoverPeers(torrentHash: String): List<DiscoveredPeer> {
        // Placeholder - real implementation uses DHT, trackers, PEX
        return listOf(
            DiscoveredPeer("peer_1", "192.168.1.100", 6881, true),
            DiscoveredPeer("peer_2", "192.168.1.101", 6881, false),
            DiscoveredPeer("peer_3", "192.168.1.102", 6881, true),
            DiscoveredPeer("peer_4", "192.168.1.103", 6881, true)
        )
    }

    private suspend fun connectToPeer(peer: Peer, torrentHash: String) {
        scope.launch {
            try {
                // Simulated connection
                kotlinx.coroutines.delay(100)
                peer.capabilities = PeerCapabilities(
                    supportsDeltaPieces = peer.capabilities.supportsDeltaPieces,
                    maxPieceSize = peer.capabilities.maxPieceSize
                )
                
                // Announce available pieces
                announcePieces(peer)
                
                // Request missing pieces
                requestMissingPieces(peer, torrentHash)
                
            } catch (e: Exception) {
                Log.w(TAG, "Failed to connect to ${peer.id}", e)
                peers.remove(peer.id)
            }
        }
    }

    private fun announcePieces(peer: Peer) {
        // In real implementation, send HAVE messages for pieces we have
    }

    private suspend fun requestMissingPieces(peer: Peer, torrentHash: String) {
        // Request pieces we need based on playback position
        val neededPieces = getNeededPieces(torrentHash)
        neededPieces.forEach { pieceIndex ->
            requestPiece(pieceIndex, peer.id)
        }
    }

    fun requestPiece(pieceIndex: Int, preferredPeerId: String? = null): ByteBuffer? {
        return scope.launch {
            val peer = selectBestPeer(pieceIndex, preferredPeerId) ?: return@launch null
            
            val buffer = bufferManager.acquireDirectBuffer(DEFAULT_CHUNK_SIZE)
            val startTime = System.currentTimeMillis()
            
            val request = PendingRequest(pieceIndex, startTime, peer.id)
            pendingRequests[pieceIndex.toLong()] = request
            
            try {
                peer.activeRequests++
                
                // Simulate download
                val success = downloadPieceFromPeer(peer, pieceIndex, buffer)
                val latency = System.currentTimeMillis() - startTime
                
                peer.updateScore(success, latency, if (success) buffer.capacity().toLong() else 0)
                
                if (success) {
                    pieceAvailability.computeIfAbsent(pieceIndex) { java.util.concurrent.ConcurrentSkipListSet() }.add(peer.id)
                    pieceDownloadTimes[pieceIndex] = AtomicLong(latency)
                    
                    updateStats()
                    buffer
                } else {
                    bufferManager.releaseDirectBuffer(buffer)
                    null
                }
            } finally {
                pendingRequests.remove(pieceIndex.toLong())
            }
        }.await()
    }

    private fun selectBestPeer(pieceIndex: Int, preferredPeerId: String?): Peer? {
        val availablePeers = pieceAvailability[pieceIndex] ?: return null
        
        // Prefer preferred peer if available and healthy
        preferredPeerId?.let { id ->
            val peer = peers[id]
            if (peer != null && availablePeers.contains(id) && peer.getEffectiveScore() > 0.3) {
                return peer
            }
        }
        
        // Select best peer by score
        return availablePeers
            .map { peers[it] }
            .filterNotNull()
            .filter { it.getEffectiveScore() > 0.2 && it.activeRequests < 3 }
            .maxByOrNull { it.getEffectiveScore() }
    }

    private fun downloadPieceFromPeer(peer: Peer, pieceIndex: Int, buffer: ByteBuffer): Boolean {
        // Simulated download - real implementation uses uTP/TCP
        try {
            // Simulate network latency
            Thread.sleep((10..100).random().toLong())
            
            // Simulate data transfer
            buffer.clear()
            buffer.limit(DEFAULT_CHUNK_SIZE)
            
            // 95% success rate for simulation
            return Math.random() < 0.95
        } catch (e: Exception) {
            return false
        }
    }

    private fun getNeededPieces(torrentHash: String): List<Int> {
        // Return pieces needed for current playback window
        return (0..10).toList() // Placeholder
    }

    private fun startMaintenance() {
        scope.launch {
            while (true) {
                cleanupStalePeers()
                decayPeerScores()
                rebalanceRequests()
                updatePieceAvailability()
                updateStats()
                kotlinx.coroutines.delay(5000)
            }
        }
    }

    private fun cleanupStalePeers() {
        val now = System.currentTimeMillis()
        peers.entries.removeIf { entry ->
            val peer = entry.value
            if (now - peer.lastSeen > 30000) { // 30 seconds stale
                Log.d(TAG, "Removing stale peer ${peer.id}")
                true
            } else false
        }
    }

    private fun decayPeerScores() {
        peers.values.forEach { peer ->
            peer.score *= PEER_SCORE_DECAY
            peer.score = max(MIN_PEER_SCORE, peer.score)
        }
    }

    private fun rebalanceRequests() {
        pendingRequests.entries.forEach { entry ->
            val request = entry.value
            if (System.currentTimeMillis() - request.startTime > PIECE_TIMEOUT_MS) {
                if (request.retryCount < 3) {
                    // Retry with different peer
                    val peer = selectBestPeer(entry.key.toInt(), null)
                    if (peer != null && peer.id != request.peerId) {
                        pendingRequests[entry.key] = request.copy(peerId = peer.id, retryCount = request.retryCount + 1)
                        Log.d(TAG, "Retrying piece ${entry.key} with peer ${peer.id} (attempt ${request.retryCount + 1})")
                    }
                } else {
                    // Give up
                    pendingRequests.remove(entry.key)
                    Log.w(TAG, "Gave up on piece ${entry.key} after 3 retries")
                }
            }
        }
    }

    private fun updatePieceAvailability() {
        // Clean up old piece availability entries
        val now = System.currentTimeMillis()
        pieceAvailability.entries.forEach { entry ->
            entry.value.removeIf { peerId ->
                val peer = peers[peerId]
                peer == null || now - peer.lastSeen > 60000
            }
        }
    }

    private fun updateStats() {
        val connected = peers.values.count { it.connectionQuality != ConnectionQuality.UNKNOWN }
        val activeDownloads = pendingRequests.size
        val downloadSpeed = peers.values.sumOf { it.bytesDownloaded }
        val uploadSpeed = peers.values.sumOf { it.bytesUploaded }
        val avgLatency = if (peers.isNotEmpty()) peers.values.map { it.latencyMs }.average().toLong() else 0L
        
        val totalRequests = pieceDownloadTimes.size
        val successfulRequests = pieceDownloadTimes.values.count { it.get() > 0 }
        val hitRate = if (totalRequests > 0) successfulRequests.toDouble() / totalRequests else 0.0
        
        _stats.value = P2PStats(
            connectedPeers = connected,
            activeDownloads = activeDownloads,
            downloadSpeedKbps = downloadSpeed / 1024,
            uploadSpeedKbps = uploadSpeed / 1024,
            bufferUsagePercent = 0f, // Would come from buffer manager
            pieceHitRate = hitRate,
            avgLatencyMs = avgLatency,
            totalPeers = peers.size
        )
    }

    fun getDeltaPiece(basePieceIndex: Int, targetPieceIndex: Int): DeltaPiece? {
        // Generate delta between two pieces
        // In real implementation, compute binary diff
        return null
    }

    fun applyDeltaPiece(basePieceIndex: Int, delta: DeltaPiece): ByteBuffer? {
        // Apply delta to reconstruct target piece
        return null
    }

    fun getPeerStats(): List<Map<String, Any>> {
        return peers.values.map { peer ->
            mapOf(
                "id" to peer.id,
                "ip" to peer.ip,
                "port" to peer.port,
                "score" to String.format("%.2f", peer.getEffectiveScore()),
                "quality" to peer.connectionQuality.name,
                "latencyMs" to peer.latencyMs,
                "downloadedMB" to peer.bytesDownloaded / 1024 / 1024,
                "uploadedMB" to peer.bytesUploaded / 1024 / 1024,
                "activeRequests" to peer.activeRequests,
                "supportsDelta" to peer.supportsDelta,
                "lastSeenSec" to (System.currentTimeMillis() - peer.lastSeen) / 1000
            )
        }.sortedByDescending { it["score"] as Double }
    }

fun trimMemory(level: Int) {
        val reduction = when (level) {
            android.app.ActivityManager.TRIM_MEMORY_RUNNING_MODERATE -> 0.2
            android.app.ActivityManager.TRIM_MEMORY_RUNNING_LOW -> 0.4
            android.app.ActivityManager.TRIM_MEMORY_RUNNING_CRITICAL -> 0.6
            android.app.ActivityManager.TRIM_MEMORY_UI_HIDDEN -> 0.3
            android.app.ActivityManager.TRIM_MEMORY_BACKGROUND -> 0.5
            android.app.ActivityManager.TRIM_MEMORY_COMPLETE -> 0.8
            else -> 0.0
        }

        if (reduction > 0) {
            peers.values.forEach { peer ->
                if (peer.getEffectiveScore() < 0.5) {
                    peers.remove(peer.id)
                }
            }
        }
    }

    fun shutdown() {
        scope.coroutineContext[Job]?.cancel()
        peers.clear()
        pieceAvailability.clear()
        pendingRequests.clear()
        pieceDownloadTimes.clear()
    }
}

    private data class DiscoveredPeer(
        val id: String,
        val ip: String,
        val port: Int,
        val supportsDelta: Boolean
    )