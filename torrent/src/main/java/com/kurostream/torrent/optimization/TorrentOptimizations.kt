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

package com.kurostream.torrent.optimization

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Kernel-Bypass Networking - Optimization #5
 * 
 * Uses AF_XDP (eXpress Data Path) for zero-copy packet processing
 * on supported Android kernels (4.18+). Falls back to optimized
 * socket configuration when AF_XDP is not available.
 * 
 * Target: <0.2 sec torrent swarm connection time
 */
class KernelBypassNetworkManager(private val context: Context) {
    
    private val TAG = "KernelBypassNetwork"
    private val xdpSupported = checkXdpSupport()
    private val xdpSocket: XdpSocket? = if (xdpSupported) createXdpSocket() else null
    
    // Fallback: optimized standard sockets
    private val socketConfig = OptimizedSocketConfig()
    
    init {
        applySystemTcpTuning()
    }
    
    private fun checkXdpSupport(): Boolean {
        // AF_XDP requires kernel 4.18+ and CONFIG_XDP_SOCKETS=y
        // Also needs CAP_NET_ADMIN or root
        return try {
            val proc = Runtime.getRuntime().exec("cat /proc/sys/net/core/xdp_sockets")
            proc.waitFor()
            val output = proc.inputStream.readBytes().decodeToString().trim()
            output.toInt() == 1 && hasRootOrCapNetAdmin()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun hasRootOrCapNetAdmin(): Boolean {
        return try {
            val proc = Runtime.getRuntime().exec("id -u")
            proc.waitFor()
            proc.inputStream.readBytes().decodeToString().trim() == "0"
        } catch (e: Exception) {
            false
        }
    }
    
    private fun createXdpSocket(): XdpSocket? {
        // XDP socket creation requires JNI/native code
        // This is a placeholder for the native implementation
        return null
    }
    
    /**
     * TCP/IP Stack Tuning - Optimization #6
     * 
     * Configures optimal TCP buffer sizes, enables BBR congestion control,
     * TCP Fast Open, and other performance optimizations.
     */
    private fun applySystemTcpTuning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Apply per-socket options instead (requires root for sysctl)
            Log.i(TAG, "Applying per-socket TCP optimizations")
        }
        
        // These would require root to modify sysctl:
        // net.core.rmem_max = 8388608 (8MB)
        // net.core.wmem_max = 8388608 (8MB)
        // net.ipv4.tcp_rmem = 4096 87380 8388608
        // net.ipv4.tcp_wmem = 4096 65536 8388608
        // net.ipv4.tcp_congestion_control = bbr
        // net.ipv4.tcp_fastopen = 3
        // net.ipv4.tcp_low_latency = 1
        // net.core.netdev_max_backlog = 5000
        // net.core.somaxconn = 65535
    }
    
    fun configureSocket(socket: java.net.Socket): Boolean {
        return try {
            socket.tcpNoDelay = true // Disable Nagle's algorithm
            socket.soTimeout = 30000
            socket.soLinger = 5
            socket.receiveBufferSize = 2 * 1024 * 1024 // 2MB
            socket.sendBufferSize = 2 * 1024 * 1024 // 2MB
            socket.keepAlive = true
            socket.performancePreferences = java.net.SocketPerformancePreferences(1, 2, 0) // Latency, throughput, bandwidth
            
            // Enable TCP Fast Open if available (Android 9+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // socket.setTcpFastOpen(true) - requires reflection
            }
            
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to configure socket", e)
            false
        }
    }
    
    fun configureDatagramSocket(socket: java.net.DatagramSocket): Boolean {
        return try {
            socket.setReceiveBufferSize(4 * 1024 * 1024) // 4MB for uTP
            socket.setSendBufferSize(4 * 1024 * 1024)
            socket.soTimeout = 5000
            socket.setTrafficClass(0x10) // IPTOS_LOWDELAY
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to configure datagram socket", e)
            false
        }
    }
    
    data class OptimizedSocketConfig(
        val tcpRcvBuf: Int = 2 * 1024 * 1024,
        val tcpSndBuf: Int = 2 * 1024 * 1024,
        val udpRcvBuf: Int = 4 * 1024 * 1024,
        val udpSndBuf: Int = 4 * 1024 * 1024,
        val enableTcpFastOpen: Boolean = true,
        val enableTcpNoDelay: Boolean = true,
        val congestionControl: String = "bbr"
    )
    
    class XdpSocket {
        // Native XDP socket implementation would go here
        // Requires: libbpf, kernel headers, CAP_NET_ADMIN
        fun receivePackets(buffer: ByteBuffer): Int = 0
        fun sendPackets(buffer: ByteBuffer): Int = 0
        fun close() {}
    }
}

/**
 * Smart Torrent Piece Re-request - Optimization #15
 * 
 * Immediately re-requests corrupt/missing pieces from fastest peer
 * without waiting for timeout. Uses parallel requests to multiple peers.
 * 
 * Target: <50ms seek latency, 0% frame drops
 */
class SmartPieceRequester(
    private val pieceManager: PieceManager,
    private val peerManager: PeerManager
) {
    private val TAG = "SmartPieceRequester"
    private val requestQueue = Channel<PieceRequest>(1000)
    private val pendingRequests = ConcurrentHashMap<Int, PieceRequest>()
    private val peerSpeeds = ConcurrentHashMap<String, PeerSpeedInfo>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val executor = Executors.newFixedThreadPool(4)
    
    data class PieceRequest(
        val pieceIndex: Int,
        val offset: Long,
        val length: Int,
        val priority: Int = 0,
        val requestTime: Long = System.currentTimeMillis(),
        val retryCount: Int = 0,
        val requestedPeers: MutableSet<String> = mutableSetOf()
    )
    
    data class PeerSpeedInfo(
        var downloadSpeed: Double = 0.0, // bytes/sec
        var latencyMs: Int = 0,
        var successRate: Double = 1.0,
        var lastUpdate: Long = System.currentTimeMillis()
    )
    
    fun requestPiece(pieceIndex: Int, offset: Long = 0, length: Int = -1, priority: Int = 0) {
        val request = PieceRequest(pieceIndex, offset, if (length > 0) length else pieceManager.pieceLength, priority)
        requestQueue.trySend(request)
    }
    
    fun requestPieceImmediate(pieceIndex: Int, excludePeers: Set<String> = emptySet()) {
        // Immediate high-priority request for playback
        val request = PieceRequest(pieceIndex, 0, pieceManager.pieceLength, Int.MAX_VALUE)
        executeImmediateRequest(request, excludePeers)
    }
    
    private fun executeImmediateRequest(request: PieceRequest, excludePeers: Set<String>) {
        scope.launch {
            // Get fastest available peers
            val peers = peerManager.getFastestPeers(3, excludePeers)
            
            peers.forEach { peer ->
                if (!request.requestedPeers.contains(peer.id)) {
                    request.requestedPeers.add(peer.id)
                    requestPieceFromPeer(request, peer)
                }
            }
            
            // If no peers available, queue for retry
            if (request.requestedPeers.isEmpty()) {
                scheduleRetry(request)
            }
        }
    }
    
    private fun requestPieceFromPeer(request: PieceRequest, peer: PeerInfo) {
        scope.launch {
            try {
                val startTime = System.nanoTime()
                val data = peerManager.downloadPiece(peer, request.pieceIndex, request.offset, request.length)
                val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
                
                if (data != null && pieceManager.verifyPiece(request.pieceIndex, data)) {
                    pieceManager.storePiece(request.pieceIndex, data)
                    updatePeerSpeed(peer.id, data.size, elapsedMs, true)
                    pendingRequests.remove(request.pieceIndex)
                } else {
                    updatePeerSpeed(peer.id, 0, elapsedMs, false)
                    scheduleRetry(request, peer.id)
                }
            } catch (e: Exception) {
                updatePeerSpeed(peer.id, 0, 0, false)
                scheduleRetry(request, peer.id)
            }
        }
    }
    
    private fun scheduleRetry(request: PieceRequest, failedPeer: String? = null) {
        scope.launch {
            // Exponential backoff with minimum 10ms
            val delay = minOf(10 * (2.0.pow(request.retryCount)), 1000.0).toLong()
            kotlinx.coroutines.delay(delay)
            
            val newRequest = request.copy(
                retryCount = request.retryCount + 1,
                requestedPeers = request.requestedPeers.toMutableSet()
            )
            
            if (failedPeer != null) {
                newRequest.requestedPeers.remove(failedPeer)
            }
            
            // Find new peer
            val peers = peerManager.getFastestPeers(1, newRequest.requestedPeers)
            if (peers.isNotEmpty()) {
                requestPieceFromPeer(newRequest, peers.first())
            } else {
                // Re-queue
                requestQueue.trySend(newRequest)
            }
        }
    }
    
    private fun updatePeerSpeed(peerId: String, bytes: Int, elapsedMs: Long, success: Boolean) {
        peerSpeeds.compute(peerId) { _, info ->
            val speed = if (elapsedMs > 0) (bytes * 1000.0) / elapsedMs else 0.0
            val oldInfo = info ?: PeerSpeedInfo()
            val newSuccessRate = if (success) {
                (oldInfo.successRate * 0.9) + 0.1
            } else {
                oldInfo.successRate * 0.9
            }
            PeerSpeedInfo(
                downloadSpeed = (oldInfo.downloadSpeed * 0.7) + (speed * 0.3),
                latencyMs = if (elapsedMs > 0) ((oldInfo.latencyMs * 0.7) + (elapsedMs * 0.3)).toInt() else oldInfo.latencyMs,
                successRate = newSuccessRate,
                lastUpdate = System.currentTimeMillis()
            )
        }
    }
    
    fun getPeerStats(): Map<String, PeerSpeedInfo> = peerSpeeds.toMap()
    
    fun shutdown() {
        scope.coroutineContext.cancel()
        executor.shutdown()
    }
}

interface PieceManager {
    val pieceLength: Int
    fun verifyPiece(index: Int, data: ByteArray): Boolean
    fun storePiece(index: Int, data: ByteArray)
}

interface PeerManager {
    fun getFastestPeers(count: Int, exclude: Set<String>): List<PeerInfo>
    fun downloadPiece(peer: PeerInfo, pieceIndex: Int, offset: Long, length: Int): ByteArray?
}

data class PeerInfo(
    val id: String,
    val address: String,
    val port: Int
)

/**
 * Predictive Torrent Swarm Expansion - Optimization #16
 * 
 * Proactively joins additional swarms of similar content when
 * playback starts to warm up connections.
 * 
 * Target: <0.2 sec swarm connection time (pre-warmed)
 */
class PredictiveSwarmExpander(
    private val torrentClient: TorrentClient,
    private val metadataProvider: MetadataProvider
) {
    private val TAG = "PredictiveSwarmExpander"
    private val expandedSwarms = ConcurrentHashMap<String, SwarmExpansion>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val executor = Executors.newSingleThreadScheduledExecutor()
    
    data class SwarmExpansion(
        val infoHash: String,
        val similarity: Double,
        val startTime: Long,
        var peerCount: Int = 0,
        var connected: Boolean = false
    )
    
    fun onPlaybackStart(currentInfoHash: String, mediaType: MediaType, metadata: MediaMetadata) {
        scope.launch {
            // Find similar content
            val similarTorrents = metadataProvider.findSimilarTorrents(currentInfoHash, mediaType, metadata)
            
            // Expand to top 5 similar swarms
            similarTorrents.take(5).forEach { similar ->
                if (!expandedSwarms.containsKey(similar.infoHash)) {
                    expandToSwarm(currentInfoHash, similar)
                }
            }
        }
    }
    
    private fun expandToSwarm(sourceHash: String, similar: SimilarTorrent) {
        val expansion = SwarmExpansion(
            infoHash = similar.infoHash,
            similarity = similar.similarity,
            startTime = System.currentTimeMillis()
        )
        expandedSwarms[similar.infoHash] = expansion
        
        scope.launch {
            try {
                // Add torrent without downloading (just connect to peers)
                val handle = torrentClient.addTorrent(
                    infoHash = similar.infoHash,
                    savePath = "/dev/null", // Don't save
                    flags = TorrentFlags.PEEK_ONLY or TorrentFlags.NO_DOWNLOAD
                )
                
                expansion.connected = true
                
                // Monitor peer connections
                executor.scheduleAtFixedRate({
                    val peers = handle.getPeers()
                    expansion.peerCount = peers.size
                    
                    if (peers.size >= 10) {
                        // Sufficient peers warmed up
                        executor.shutdown()
                    }
                }, 1, 2, TimeUnit.SECONDS)
                
                Log.i(TAG, "Expanded to swarm: ${similar.infoHash} (similarity: ${similar.similarity})")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to expand to swarm: ${similar.infoHash}", e)
            }
        }
    }
    
    fun getWarmedPeers(targetInfoHash: String): List<PeerInfo> {
        return expandedSwarms[targetInfoHash]?.let { expansion ->
            if (expansion.connected) {
                torrentClient.getPeers(targetInfoHash)
            } else emptyList()
        } ?: emptyList()
    }
    
    fun shutdown() {
        scope.coroutineContext.cancel()
        executor.shutdown()
    }
}

interface TorrentClient {
    fun addTorrent(infoHash: String, savePath: String, flags: Int): TorrentHandle
    fun getPeers(infoHash: String): List<PeerInfo>
}

interface TorrentHandle {
    fun getPeers(): List<PeerInfo>
}

interface MetadataProvider {
    fun findSimilarTorrents(infoHash: String, type: MediaType, metadata: MediaMetadata): List<SimilarTorrent>
}

enum class MediaType { MOVIE, TV_SHOW, ANIME }

data class MediaMetadata(
    val title: String,
    val year: Int?,
    val genres: List<String>,
    val tags: List<String>
)

data class SimilarTorrent(
    val infoHash: String,
    val similarity: Double,
    val quality: String,
    val seeders: Int
)

class TorrentFlags {
    companion object {
        const val PEEK_ONLY = 1 shl 0
        const val NO_DOWNLOAD = 1 shl 1
    }
}

/**
 * GPU Memory Torrent Piece Cache - Optimization #7
 * 
 * Stores frequently accessed torrent pieces in GPU memory
 * via OpenGL textures to reduce memory bandwidth.
 * 
 * Target: <80MB RAM during 4K P2P playback
 */
class GpuPieceCache(
    private val maxSizeBytes: Long = 50 * 1024 * 1024 // 50MB GPU cache
) {
    private val TAG = "GpuPieceCache"
    private val pieceCache = ConcurrentHashMap<Int, GpuPieceEntry>()
    private val accessOrder = mutableListOf<Int>()
    private var currentSize = 0L
    private val mutex = java.util.concurrent.locks.ReentrantLock()
    
    data class GpuPieceEntry(
        val pieceIndex: Int,
        val textureId: Int,
        val size: Long,
        val lastAccess: Long = System.currentTimeMillis(),
        val accessCount: Int = 0
    )
    
    fun put(pieceIndex: Int, data: ByteArray): Boolean {
        mutex.lock()
        return try {
            if (currentSize + data.size > maxSizeBytes) {
                evictOldest()
            }
            
            // Upload to GPU texture (requires OpenGL context)
            val textureId = uploadToGpuTexture(data)
            if (textureId <= 0) return false
            
            val entry = GpuPieceEntry(pieceIndex, textureId, data.size.toLong())
            pieceCache[pieceIndex] = entry
            accessOrder.add(0, pieceIndex)
            currentSize += data.size
            
            true
        } finally {
            mutex.unlock()
        }
    }
    
    fun get(pieceIndex: Int): GpuPieceEntry? {
        mutex.lock()
        return try {
            val entry = pieceCache[pieceIndex]?.copy(lastAccess = System.currentTimeMillis(), accessCount = (pieceCache[pieceIndex]?.accessCount ?: 0) + 1)
            entry?.let { pieceCache[pieceIndex] = it }
            
            // Update access order
            accessOrder.remove(pieceIndex)
            accessOrder.add(0, pieceIndex)
            
            entry
        } finally {
            mutex.unlock()
        }
    }
    
    fun remove(pieceIndex: Int): Boolean {
        mutex.lock()
        return try {
            pieceCache.remove(pieceIndex)?.let { entry ->
                deleteGpuTexture(entry.textureId)
                currentSize -= entry.size
                accessOrder.remove(pieceIndex)
                true
            } ?: false
        } finally {
            mutex.unlock()
        }
    }
    
    private fun evictOldest() {
        while (currentSize > maxSizeBytes * 0.8 && accessOrder.isNotEmpty()) {
            val oldest = accessOrder.removeAt(accessOrder.lastIndex)
            pieceCache.remove(oldest)?.let { entry ->
                deleteGpuTexture(entry.textureId)
                currentSize -= entry.size
            }
        }
    }
    
    private fun uploadToGpuTexture(data: ByteArray): Int {
        // Requires OpenGL context - placeholder for actual implementation
        // GLES30.glGenTextures(1, textureIdArray, 0)
        // GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        // GLES30.glTexImage2D(...)
        return 0 // Placeholder
    }
    
    private fun deleteGpuTexture(textureId: Int) {
        // GLES30.glDeleteTextures(1, intArrayOf(textureId), 0)
    }
    
    fun getCacheStats(): CacheStats {
        mutex.lock()
        return try {
            CacheStats(
                entryCount = pieceCache.size,
                totalSizeBytes = currentSize,
                maxSizeBytes = maxSizeBytes,
                hitRate = 0.0 // Would need hit/miss tracking
            )
        } finally {
            mutex.unlock()
        }
    }
    
    fun clear() {
        mutex.lock()
        try {
            pieceCache.values.forEach { deleteGpuTexture(it.textureId) }
            pieceCache.clear()
            accessOrder.clear()
            currentSize = 0
        } finally {
            mutex.unlock()
        }
    }
    
    data class CacheStats(
        val entryCount: Int,
        val totalSizeBytes: Long,
        val maxSizeBytes: Long,
        val hitRate: Double
    )
}

/**
 * File System Alignment - Optimization #14
 * 
 * Ensures all file writes are aligned to 4KB block boundaries
 * to minimize I/O overhead and filesystem fragmentation.
 * 
 * Target: Reduced I/O latency, better throughput
 */
class AlignedFileWriter(private val file: File, private val blockSize: Int = 4096) {
    private var channel: FileChannel? = null
    private var buffer: ByteBuffer = ByteBuffer.allocateDirect(blockSize * 64).apply { order(java.nio.ByteOrder.nativeOrder()) }
    private var position = 0L
    private val mutex = java.util.concurrent.locks.ReentrantLock()
    
    init {
        file.parentFile?.mkdirs()
        channel = FileOutputStream(file, true).channel
        // Align to block boundary
        val currentPos = channel!!.position()
        val alignedPos = ((currentPos + blockSize - 1) / blockSize) * blockSize
        if (alignedPos != currentPos) {
            channel!!.position(alignedPos)
            position = alignedPos
        }
    }
    
    fun write(data: ByteArray): Int {
        mutex.lock()
        return try {
            var written = 0
            val src = ByteBuffer.wrap(data)
            
            while (src.hasRemaining()) {
                val remainingInBuffer = buffer.remaining()
                val toWrite = minOf(src.remaining(), remainingInBuffer)
                
                val oldLimit = buffer.limit()
                buffer.limit(buffer.position() + toWrite)
                buffer.put(src)
                buffer.limit(oldLimit)
                written += toWrite
                
                if (!buffer.hasRemaining()) {
                    flushBuffer()
                }
            }
            
            position += written
            written
        } finally {
            mutex.unlock()
        }
    }
    
    fun writeAligned(data: ByteArray): Int {
        // Ensure write starts at block boundary
        mutex.lock()
        return try {
            val currentPos = channel!!.position()
            val alignedPos = ((currentPos + blockSize - 1) / blockSize) * blockSize
            if (alignedPos != currentPos) {
                // Pad with zeros
                val padding = alignedPos - currentPos
                val zeroBuffer = ByteBuffer.allocateDirect(padding.toInt())
                channel!!.write(zeroBuffer)
            }
            write(data)
        } finally {
            mutex.unlock()
        }
    }
    
    private fun flushBuffer() {
        buffer.flip()
        channel!!.write(buffer)
        buffer.clear()
    }
    
    fun flush() {
        mutex.lock()
        try {
            flushBuffer()
            channel!!.force(true)
        } finally {
            mutex.unlock()
        }
    }
    
    fun close() {
        flush()
        channel?.close()
    }
    
    fun getPosition(): Long = position
}

/**
 * Adaptive Frame Rate Output - Optimization #8
 * 
 * Dynamically switches display refresh rate to match content FPS
 * (23.976/24/25/30/60) to eliminate 3:2 pulldown and reduce jitter.
 * 
 * Target: 0.0% frame drops, <50ms seek latency
 */
class AdaptiveFrameRateSwitcher(private val context: Context) {
    private val TAG = "FrameRateSwitcher"
    private val supportedRates = mutableSetOf<Float>()
    private var currentRate = 60f
    private var targetRate = 60f
    private val switchCallback: (Float) -> Unit = { rate -> 
        // Implementation would call DisplayManager.setPreferredRefreshRate()
    }
    
    init {
        detectSupportedRates()
    }
    
    private fun detectSupportedRates() {
        // Query supported refresh rates from DisplayManager
        // Common rates: 23.976, 24, 25, 29.97, 30, 48, 50, 59.94, 60, 120
        supportedRates.addAll(setOf(23.976f, 24f, 25f, 29.97f, 30f, 48f, 50f, 59.94f, 60f, 120f))
    }
    
    fun onContentFrameRate(fps: Float) {
        val bestMatch = findBestRefreshRate(fps)
        if (bestMatch != currentRate && abs(bestMatch - currentRate) > 0.1f) {
            switchRefreshRate(bestMatch)
        }
    }
    
    private fun findBestRefreshRate(fps: Float): Float {
        // Find exact match or closest multiple
        return supportedRates.minByOrNull { abs(it - fps) } ?: 60f
    }
    
    private fun switchRefreshRate(rate: Float) {
        Log.i(TAG, "Switching refresh rate: $currentRate -> $rate")
        currentRate = rate
        targetRate = rate
        // switchCallback(rate) - Would call DisplayManager API
    }
    
    fun getCurrentRate(): Float = currentRate
    fun getTargetRate(): Float = targetRate
    
    fun reset() {
        if (currentRate != 60f) {
            switchRefreshRate(60f)
        }
    }
}

/**
 * Dynamic Resolution Scaling - Optimization #9
 * 
 * Temporarily lowers decoding resolution when bandwidth drops,
 * then upscales back. Maintains frame rate during network issues.
 * 
 * Target: <50ms seek latency, continuous playback
 */
class DynamicResolutionScaler(
    private val player: ResolutionAwarePlayer
) {
    private val TAG = "DynamicResolutionScaler"
    private var currentScale = 1.0f
    private var targetScale = 1.0f
    private val bandwidthHistory = mutableListOf<Long>()
    private val maxHistory = 10
    
    fun onBandwidthChange(bandwidthBps: Long) {
        bandwidthHistory.add(bandwidthBps)
        if (bandwidthHistory.size > maxHistory) bandwidthHistory.removeAt(0)
        
        val avgBandwidth = bandwidthHistory.average()
        val requiredBitrate = estimateRequiredBitrate()
        
        if (avgBandwidth < requiredBitrate * 0.7 && currentScale > 0.5f) {
            // Scale down
            targetScale = maxOf(currentScale * 0.75f, 0.5f)
            applyScale()
        } else if (avgBandwidth > requiredBitrate * 1.3 && currentScale < 1.0f) {
            // Scale up
            targetScale = minOf(currentScale * 1.25f, 1.0f)
            applyScale()
        }
    }
    
    private fun estimateRequiredBitrate(): Long {
        // Rough estimates for different resolutions
        return when {
            player.currentHeight >= 2160 -> 25_000_000 // 4K
            player.currentHeight >= 1440 -> 12_000_000 // 1440p
            player.currentHeight >= 1080 -> 8_000_000  // 1080p
            player.currentHeight >= 720 -> 4_000_000   // 720p
            else -> 2_000_000
        }
    }
    
    private fun applyScale() {
        if (abs(targetScale - currentScale) > 0.05f) {
            currentScale = targetScale
            val newWidth = (player.currentWidth * currentScale).toInt()
            val newHeight = (player.currentHeight * currentScale).toInt()
            player.setDecodingResolution(newWidth, newHeight)
            Log.i(TAG, "Resolution scaled to ${newWidth}x${newHeight} (${(currentScale * 100).toInt()}%)")
        }
    }
    
    fun reset() {
        if (currentScale != 1.0f) {
            targetScale = 1.0f
            applyScale()
        }
    }
}

interface ResolutionAwarePlayer {
    val currentWidth: Int
    val currentHeight: Int
    fun setDecodingResolution(width: Int, height: Int)
}

/**
 * Just-In-Time Codec Loading - Optimization #10
 * 
 * Loads codec libraries only when needed, unloads after use
 * to save memory.
 * 
 * Target: <80MB RAM during 4K P2P playback
 */
class JitCodecLoader(private val context: Context) {
    private val loadedCodecs = ConcurrentHashMap<String, CodecHandle>()
    private val codecRefCounts = ConcurrentHashMap<String, Int>()
    private val mutex = java.util.concurrent.locks.ReentrantLock()
    
    data class CodecHandle(
        val name: String,
        val handle: Long,
        val loadTime: Long = System.currentTimeMillis()
    )
    
    fun loadCodec(codec: String): Boolean {
        mutex.lock()
        return try {
            val count = codecRefCounts.getOrDefault(codec, 0) + 1
            codecRefCounts[codec] = count
            
            if (loadedCodecs.containsKey(codec)) {
                return true
            }
            
            val libName = when (codec.lowercase()) {
                "av1", "av01" -> "kuroengine_dav1d"
                "hevc", "h265" -> "kuroengine_hevc"
                "vp9" -> "kuroengine_vp9"
                "avc", "h264" -> "kuroengine_avc"
                "vp8" -> "kuroengine_vp8"
                else -> return false
            }
            
            try {
                System.loadLibrary(libName)
                val handle = nativeInitCodec(codec)
                loadedCodecs[codec] = CodecHandle(codec, handle)
                Log.i("JitCodecLoader", "Loaded codec on demand: $codec")
                true
            } catch (e: UnsatisfiedLinkError) {
                codecRefCounts[codec] = count - 1
                false
            }
        } finally {
            mutex.unlock()
        }
    }
    
    fun unloadCodec(codec: String) {
        mutex.lock()
        try {
            val count = codecRefCounts.getOrDefault(codec, 0) - 1
            if (count <= 0) {
                codecRefCounts.remove(codec)
                loadedCodecs.remove(codec)?.let { handle ->
                    nativeReleaseCodec(handle.handle)
                    Log.i("JitCodecLoader", "Unloaded codec: $codec")
                }
            } else {
                codecRefCounts[codec] = count
            }
        } finally {
            mutex.unlock()
        }
    }
    
    fun getLoadedCodecs(): List<String> = loadedCodecs.keys.toList()
    
    private external fun nativeInitCodec(codec: String): Long
    private external fun nativeReleaseCodec(handle: Long)
}

/**
 * Texture Atlas for UI - Optimization #11
 * 
 * Combines small UI images into single texture to reduce
 * draw calls and GPU state changes.
 * 
 * Target: Reduced GPU overhead, better battery
 */
class TextureAtlasBuilder(
    private val maxAtlasPage)
{
    private val regions = mutableListOf<AtlasRegion>()
    private var currentX = 0
    private var currentY = 0
    private var rowHeight = 0
    private val atlasWidth = 2048
    private val atlasHeight = 2048
    private val padding = 2
    
    data class AtlasRegion(
        val name: String,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val u1: Float,
        val v1: Float,
        val u2: Float,
        val v2: Float
    )
    
    fun addImage(name: String, width: Int, height: Int): Boolean {
        val paddedWidth = width + padding * 2
        val paddedHeight = height + padding * 2
        
        if (currentX + paddedWidth > atlasWidth) {
            // Next row
            currentX = 0
            currentY += rowHeight + padding
            rowHeight = 0
        }
        
        if (currentY + paddedHeight > atlasHeight) {
            return false // Atlas full
        }
        
        val region = AtlasRegion(
            name = name,
            x = currentX + padding,
            y = currentY + padding,
            width = width,
            height = height,
            u1 = (currentX + padding).toFloat() / atlasWidth,
            v1 = (currentY + padding).toFloat() / atlasHeight,
            u2 = (currentX + padding + width).toFloat() / atlasWidth,
            v2 = (currentY + padding + height).toFloat() / atlasHeight
        )
        
        regions.add(region)
        currentX += paddedWidth
        rowHeight = maxOf(rowHeight, paddedHeight)
        return true
    }
    
    fun build(): TextureAtlas {
        return TextureAtlas(regions.toList(), atlasWidth, atlasHeight)
    }
    
    fun clear() {
        regions.clear()
        currentX = 0
        currentY = 0
        rowHeight = 0
    }
}

data class TextureAtlas(
    val regions: List<TextureAtlasBuilder.AtlasRegion>,
    val width: Int,
    val height: Int
) {
    fun getRegion(name: String): TextureAtlasBuilder.AtlasRegion? {
        return regions.find { it.name == name }
    }
}

/**
 * Shader Pre-compilation - Optimization #12
 * 
 * Compiles all GLSL shaders at app startup and caches them
 * to avoid runtime compilation stalls.
 * 
 * Target: Smooth 60fps UI, no frame drops during playback start
 */
class ShaderPrecompiler(private val glContext: Any) {
    private val compiledShaders = ConcurrentHashMap<String, Int>()
    private val shaderSources = mapOf(
        "yuv_to_rgb" to YUV_TO_RGB_SHADER,
        "yuv_to_rgb_hdr" to YUV_TO_RGB_HDR_SHADER,
        "tonemap_bt2390" to TONEMAP_BT2390_SHADER,
        "tonemap_hable" to TONEMAP_HABLE_SHADER,
        "deband" to DEBAND_SHADER,
        "upscale_fsr" to UPSCALE_FSR_SHADER,
        "interpolation" to FRAME_INTERPOLATION_SHADER
    )
    
    companion object {
        const val YUV_TO_RGB_SHADER = """
            #version 300 es
            precision highp float;
            in vec2 v_texCoord;
            uniform sampler2D u_yTexture;
            uniform sampler2D u_uTexture;
            uniform sampler2D u_vTexture;
            uniform mat3 u_colorMatrix;
            uniform vec3 u_colorOffset;
            out vec4 fragColor;
            void main() {
                float y = texture(u_yTexture, v_texCoord).r;
                float u = texture(u_uTexture, v_texCoord).r;
                float v = texture(u_vTexture, v_texCoord).r;
                vec3 yuv = vec3(y, u, v) + u_colorOffset;
                vec3 rgb = u_colorMatrix * yuv;
                fragColor = vec4(rgb, 1.0);
            }
        """.trimIndent()
        
        const val YUV_TO_RGB_HDR_SHADER = """
            #version 300 es
            precision highp float;
            in vec2 v_texCoord;
            uniform sampler2D u_yTexture;
            uniform sampler2D u_uTexture;
            uniform sampler2D u_vTexture;
            uniform mat3 u_colorMatrix;
            uniform vec3 u_colorOffset;
            uniform float u_maxLuminance;
            out vec4 fragColor;
            void main() {
                float y = texture(u_yTexture, v_texCoord).r;
                float u = texture(u_uTexture, v_texCoord).r;
                float v = texture(u_vTexture, v_texCoord).r;
                vec3 yuv = vec3(y, u, v) + u_colorOffset;
                vec3 rgb = u_colorMatrix * yuv;
                // PQ encoding for HDR
                rgb = rgb * u_maxLuminance;
                fragColor = vec4(rgb, 1.0);
            }
        """.trimIndent()
        
        const val TONEMAP_BT2390_SHADER = """
            #version 300 es
            precision highp float;
            in vec2 v_texCoord;
            uniform sampler2D u_inputTexture;
            uniform float u_displayPeakNits;
            uniform float u_contentPeakNits;
            out vec4 fragColor;
            void main() {
                vec3 color = texture(u_inputTexture, v_texCoord).rgb;
                // BT.2390 tone mapping
                float ratio = u_displayPeakNits / u_contentPeakNits;
                color = color / (color + 1.0) * ratio; // Simplified
                fragColor = vec4(color, 1.0);
            }
        """.trimIndent()
        
        const val TONEMAP_HABLE_SHADER = """
            #version 300 es
            precision highp float;
            in vec2 v_texCoord;
            uniform sampler2D u_inputTexture;
            out vec4 fragColor;
            vec3 hable(vec3 x) {
                const vec3 A = vec3(0.15, 0.50, 0.10);
                const vec3 B = vec3(0.50, 0.50, 0.50);
                const vec3 C = vec3(0.10, 0.00, 0.00);
                const vec3 D = vec3(0.20, 0.20, 0.20);
                const vec3 E = vec3(0.02, 0.02, 0.02);
                const vec3 F = vec3(0.30, 0.30, 0.30);
                return ((x*(A*x+C*B)+D*E)/(x*(A*x+B)+D*F))-E/F;
            }
            void main() {
                vec3 color = texture(u_inputTexture, v_texCoord).rgb;
                color = hable(color);
                fragColor = vec4(color, 1.0);
            }
        """.trimIndent()
        
        const val DEBAND_SHADER = """
            #version 300 es
            precision highp float;
            in vec2 v_texCoord;
            uniform sampler2D u_inputTexture;
            uniform float u_strength;
            out vec4 fragColor;
            void main() {
                vec3 color = texture(u_inputTexture, v_texCoord).rgb;
                // Simple debanding via noise dithering
                float noise = fract(sin(dot(v_texCoord * 100.0, vec2(12.9898, 78.233))) * 43758.5453);
                color += (noise - 0.5) * u_strength / 255.0;
                fragColor = vec4(color, 1.0);
            }
        """.trimIndent()
        
        const val UPSCALE_FSR_SHADER = """
            #version 300 es
            precision highp float;
            in vec2 v_texCoord;
            uniform sampler2D u_inputTexture;
            uniform vec2 u_inputSize;
            uniform vec2 u_outputSize;
            out vec4 fragColor;
            void main() {
                // FSR EASU (Edge Adaptive Spatial Upsampling) - simplified
                vec2 scale = u_outputSize / u_inputSize;
                vec2 uv = v_texCoord * scale;
                fragColor = texture(u_inputTexture, uv);
            }
        """.trimIndent()
        
        const val FRAME_INTERPOLATION_SHADER = """
            #version 300 es
            precision highp float;
            in vec2 v_texCoord;
            uniform sampler2D u_frame1;
            uniform sampler2D u_frame2;
            uniform float u_t;
            out vec4 fragColor;
            void main() {
                vec4 f1 = texture(u_frame1, v_texCoord);
                vec4 f2 = texture(u_frame2, v_texCoord);
                fragColor = mix(f1, f2, u_t);
            }
        """.trimIndent()
    }
    
    fun precompileAll() {
        shaderSources.forEach { (name, source) ->
            compileShader(name, source)
        }
    }
    
    private fun compileShader(name: String, source: String): Int {
        // Compile vertex + fragment shader pair
        // This is a placeholder - actual implementation needs GL context
        val programId = 0 // glCreateProgram()
        compiledShaders[name] = programId
        return programId
    }
    
    fun getShader(name: String): Int? = compiledShaders[name]
    
    fun releaseAll() {
        compiledShaders.values.forEach { programId ->
            // glDeleteProgram(programId)
        }
        compiledShaders.clear()
    }
}

/**
 * Memory Defragmentation - Optimization #13
 * 
 * Periodically compacts heap to avoid OOM on long-running playback.
 * 
 * Target: Stable memory, no OOM crashes
 */
class MemoryDefragmenter(private val context: Context) {
    private val TAG = "MemoryDefragmenter"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var defragJob: kotlinx.coroutines.Job? = null
    private val lastDefragTime = AtomicLong(0)
    private val DEFRAG_INTERVAL_MS = 5 * 60 * 1000 // 5 minutes
    
    fun start() {
        defragJob = scope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(60_000) // Check every minute
                checkAndDefrag()
            }
        }
    }
    
    fun stop() {
        defragJob?.cancel()
        defragJob = null
    }
    
    private fun checkAndDefrag() {
        val now = System.currentTimeMillis()
        if (now - lastDefragTime.get() < DEFRAG_INTERVAL_MS) return
        
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory() - runtime.freeMemory()
        val max = runtime.maxMemory()
        val usageRatio = used.toDouble() / max
        
        if (usageRatio > 0.75) {
            Log.i(TAG, "Memory usage high (${(usageRatio * 100).toInt()}%), triggering defragmentation")
            defragment()
            lastDefragTime.set(now)
        }
    }
    
    private fun defragment() {
        // Force GC
        System.gc()
        System.runFinalization()
        System.gc()
        
        // Trim native memory if possible
        try {
            val trimMemory = Class.forName("android.os.Debug").getMethod("trimMemory", Int::class.java)
            trimMemory.invoke(null, 20) // TRIM_MEMORY_RUNNING_LOW
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    fun forceDefrag() {
        defragment()
        lastDefragTime.set(System.currentTimeMillis())
    }
}