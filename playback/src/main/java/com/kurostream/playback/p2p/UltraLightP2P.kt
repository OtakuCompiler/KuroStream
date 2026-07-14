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
import com.kurostream.playback.memory.MemoryManager
import com.kurostream.playback.memory.UltraLowMemoryManager
import com.kurostream.playback.memory.VideoQuality
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UltraLightP2P @Inject constructor(
    private val memoryManager: UltraLowMemoryManager
) {
    companion object {
        private const val TAG = "UltraLightP2P"
        private const val MAX_DIRECT_BUFFER_SIZE = 16 * 1024 * 1024
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val peerConnections = ConcurrentHashMap<String, PeerConnection>()
    private val bufferPool = ByteBufferPool()
    private var currentConfig: UltraLowMemoryManager.MemoryConfig? = null
    
    private val _state = MutableStateFlow(P2PState.IDLE)
    val state: StateFlow<P2PState> = _state.asStateFlow()
    
    private val _bufferUsage = MutableStateFlow(0f)
    val bufferUsage: StateFlow<Float> = _bufferUsage.asStateFlow()
    
    private val _downloadSpeed = MutableStateFlow(0L)
    val downloadSpeed: StateFlow<Long> = _downloadSpeed.asStateFlow()
    
    private val _uploadSpeed = MutableStateFlow(0L)
    val uploadSpeed: StateFlow<Long> = _uploadSpeed.asStateFlow()
    
    private val _connectedPeers = MutableStateFlow(0)
    val connectedPeers: StateFlow<Int> = _connectedPeers.asStateFlow()
    
    fun initialize(quality: VideoQuality, hasUpscaling: Boolean, hasTranscoding: Boolean) {
        currentConfig = memoryManager.getOptimizedConfig(quality, hasUpscaling, hasTranscoding)
        bufferPool.initialize(currentConfig!!.p2pBufferSize)
        Log.d(TAG, "Initialized with config: ${currentConfig!!.p2pBufferSize / 1024 / 1024}MB buffer, ${currentConfig!!.maxPeers} max peers")
    }
    
    suspend fun startStreaming(torrentHash: String) {
        withContext(Dispatchers.IO) {
            try {
                _state.value = P2PState.CONNECTING
                connectToPeers(torrentHash)
                _state.value = P2PState.STREAMING
            } catch (e: Exception) {
                Log.e(TAG, "Streaming error", e)
                _state.value = P2PState.ERROR(e.message ?: "Unknown error")
            }
        }
    }
    
    private suspend fun connectToPeers(torrentHash: String) {
        val maxPeers = currentConfig?.maxPeers ?: 10
        val chunkSize = currentConfig?.chunkSize ?: 1024 * 1024
        
        scope.launch {
            for (i in 0 until maxPeers) {
                if (peerConnections.size >= maxPeers) break
                
                val peerId = "peer_$i"
                val connection = PeerConnection(peerId, bufferPool, chunkSize)
                peerConnections[peerId] = connection
                
                launch {
                    try {
                        connection.connect(torrentHash)
                        _connectedPeers.value = peerConnections.count { it.value.isConnected() }
                    } catch (e: Exception) {
                        Log.w(TAG, "Peer $peerId connection failed", e)
                        peerConnections.remove(peerId)
                        _connectedPeers.value = peerConnections.count { it.value.isConnected() }
                    }
                }
            }
        }
    }
    
    suspend fun getChunk(chunkIndex: Long): ByteBuffer? {
        return withContext(Dispatchers.IO) {
            val buffer = bufferPool.acquire()
            
            val activePeers = peerConnections.values.filter { it.isConnected() }
            if (activePeers.isEmpty()) {
                bufferPool.release(buffer)
                return@withContext null
            }
            
            val peer = activePeers.random()
            try {
                peer.requestChunk(chunkIndex, buffer)
                _downloadSpeed.value = buffer.remaining().toLong()
                _bufferUsage.value = bufferPool.getUsagePercent()
                buffer
            } catch (e: Exception) {
                Log.e(TAG, "Chunk request failed", e)
                bufferPool.release(buffer)
                null
            }
        }
    }
    
    fun releaseChunk(buffer: ByteBuffer) {
        bufferPool.release(buffer)
        _bufferUsage.value = bufferPool.getUsagePercent()
    }
    
    suspend fun stopStreaming() {
        withContext(Dispatchers.IO) {
            scope.coroutineContext[Job]?.cancelChildren()
            peerConnections.values.forEach { it.disconnect() }
            peerConnections.clear()
            bufferPool.clear()
            _state.value = P2PState.IDLE
            _connectedPeers.value = 0
            _bufferUsage.value = 0f
            _downloadSpeed.value = 0L
            _uploadSpeed.value = 0L
        }
    }
    
    fun getMemoryStats(): P2PMemoryStats {
        return P2PMemoryStats(
            bufferSize = currentConfig?.p2pBufferSize ?: 0,
            usedBuffer = bufferPool.getUsedBytes(),
            availableBuffer = bufferPool.getAvailableBytes(),
            usagePercent = bufferPool.getUsagePercent(),
            activePeers = peerConnections.count { it.value.isConnected() },
            maxPeers = currentConfig?.maxPeers ?: 0,
            chunkSize = currentConfig?.chunkSize ?: 0,
            estimatedRAM = estimateTotalRAM()
        )
    }
    
    private fun estimateTotalRAM(): Int {
        val config = currentConfig ?: return 0
        return config.p2pBufferSize +
               config.decoderBuffer +
               config.upscalerBuffer +
               config.audioBuffer +
               config.networkBuffer +
               config.uiBuffer
    }
    
    fun trimMemory(level: Int) {
        when (level) {
            android.app.ActivityManager.TRIM_MEMORY_RUNNING_LOW,
            android.app.ActivityManager.TRIM_MEMORY_RUNNING_CRITICAL -> {
                val reduction = when (level) {
                    android.app.ActivityManager.TRIM_MEMORY_RUNNING_LOW -> 0.4
                    else -> 0.6
                }
                bufferPool.shrink(reduction)
                disconnectExcessPeers()
            }
        }
    }
    
    private fun disconnectExcessPeers() {
        val targetPeers = (currentConfig?.maxPeers ?: 10) / 2
        val excess = peerConnections.count { it.value.isConnected() } - targetPeers
        if (excess > 0) {
            peerConnections.values.take(excess).forEach { it.disconnect() }
            _connectedPeers.value = peerConnections.count { it.value.isConnected() }
        }
    }
}

enum class P2PState {
    IDLE,
    CONNECTING,
    STREAMING,
    BUFFERING,
    ERROR
}

data class P2PMemoryStats(
    val bufferSize: Int,
    val usedBuffer: Int,
    val availableBuffer: Int,
    val usagePercent: Float,
    val activePeers: Int,
    val maxPeers: Int,
    val chunkSize: Int,
    val estimatedRAM: Int
)

class PeerConnection(
    private val peerId: String,
    private val bufferPool: ByteBufferPool,
    private val chunkSize: Int
) {
    private var connected = false
    
    suspend fun connect(torrentHash: String) {
        withContext(Dispatchers.IO) {
            connected = true
        }
    }
    
    fun isConnected(): Boolean = connected
    
    suspend fun requestChunk(chunkIndex: Long, buffer: ByteBuffer) {
        withContext(Dispatchers.IO) {
            buffer.clear()
            buffer.limit(chunkSize)
            buffer.position(0)
        }
    }
    
    fun disconnect() {
        connected = false
    }
}

class ByteBufferPool {
    private val pool = ArrayDeque<ByteBuffer>()
    private var totalSize = 0
    private var usedSize = 0
    private val lock = Any()
    
    fun initialize(size: Int) {
        synchronized(lock) {
            totalSize = size.coerceAtMost(MAX_DIRECT_BUFFER_SIZE)
            val bufferCount = totalSize / (16 * 1024 * 1024)
            for (i in 0 until bufferCount.coerceAtLeast(1)) {
                pool.add(ByteBuffer.allocateDirect(16 * 1024 * 1024))
            }
        }
    }
    
    fun acquire(): ByteBuffer {
        synchronized(lock) {
            val buffer = pool.removeLastOrNull() ?: ByteBuffer.allocateDirect(2 * 1024 * 1024)
            usedSize += buffer.capacity()
            buffer.clear()
            return buffer
        }
    }
    
    fun release(buffer: ByteBuffer) {
        synchronized(lock) {
            usedSize -= buffer.capacity()
            buffer.clear()
            if (pool.size < totalSize / buffer.capacity()) {
                pool.add(buffer)
            }
        }
    }
    
    fun getUsedBytes(): Int {
        synchronized(lock) {
            return usedSize
        }
    }
    
    fun getAvailableBytes(): Int {
        synchronized(lock) {
            return totalSize - usedSize
        }
    }
    
    fun getUsagePercent(): Float {
        synchronized(lock) {
            return if (totalSize > 0) usedSize.toFloat() / totalSize else 0f
        }
    }
    
    fun shrink(percent: Float) {
        synchronized(lock) {
            val toRemove = (pool.size * percent).toInt()
            repeat(toRemove) {
                pool.removeLastOrNull()
            }
            totalSize = (totalSize * (1 - percent)).toInt()
        }
    }
    
    fun clear() {
        synchronized(lock) {
            pool.clear()
            totalSize = 0
            usedSize = 0
        }
    }
}