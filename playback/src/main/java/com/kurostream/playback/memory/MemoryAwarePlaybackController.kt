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

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import com.kurostream.playback.p2p.OptimizedP2PEngine
import com.kurostream.players.core.MediaItem as CoreMediaItem
import com.kurostream.players.engine.KuroEngine
import com.kurostream.players.engine.buffer.DiskBufferManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Keep
class MemoryAwarePlaybackController @Inject constructor(
    private val memoryManager: KuroStreamMemoryManager,
    private val optimizedP2PEngine: OptimizedP2PEngine,
    private val thermalQualityController: ThermalQualityController,
    private val adaptivePrebufferManager: AdaptivePrebufferManager,
) {
    companion object {
        private const val TAG = "MemoryAwarePlaybackCtrl"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var currentStreamId: String? = null
    private var exoPlayer: ExoPlayer? = null
    private var kuroEngine: KuroEngine? = null
    private var diskBufferManager: DiskBufferManager? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    private val _currentStats = MutableStateFlow<UnifiedStats?>(null)
    val currentStats: StateFlow<UnifiedStats?> = _currentStats

    private val _memoryPressure = MutableStateFlow<MemoryPressure>(MemoryPressure.NONE)
    val memoryPressure: StateFlow<MemoryPressure> = _memoryPressure

    enum class MemoryPressure {
        NONE, LOW, MODERATE, HIGH, CRITICAL
    }

    data class PlaybackConfig(
        val streamId: String,
        val quality: KuroStreamMemoryManager.VideoQuality,
        val hasUpscaling: Boolean,
        val hasTranscoding: Boolean,
        val networkSpeedMbps: Long,
        val enableP2P: Boolean,
        val enableCompressedFrames: Boolean,
        val enableDeltaP2P: Boolean,
        val maxBitrateKbps: Int,
        val targetFps: Int,
        val upscalingEnabled: Boolean,
        val atmosEnabled: Boolean,
    )

    fun initialize(config: PlaybackConfig): Boolean {
        if (currentStreamId == config.streamId && _isInitialized.value) {
            Log.d(TAG, "Already initialized for stream ${config.streamId}")
            return true
        }

        Log.i(TAG, "Initializing playback controller for ${config.streamId} with quality ${config.quality}")

        // 1. Initialize memory manager
        val unifiedConfig = KuroStreamMemoryManager.UnifiedConfig(
            streamId = config.streamId,
            quality = config.quality,
            hasUpscaling = config.hasUpscaling,
            hasTranscoding = config.hasTranscoding,
            networkSpeedMbps = config.networkSpeedMbps,
            enableP2P = config.enableP2P,
            enableCompressedFrames = config.enableCompressedFrames,
            enableDeltaP2P = config.enableDeltaP2P,
            maxBitrateKbps = config.maxBitrateKbps,
            targetFps = config.targetFps,
            upscalingEnabled = config.upscalingEnabled,
            atmosEnabled = config.atmosEnabled,
        )

        val initSuccess = memoryManager.initialize(unifiedConfig)
        if (!initSuccess) {
            Log.e(TAG, "Failed to initialize memory manager")
            return false
        }

        // 2. Register stream with thermal controller
        thermalQualityController.registerStream(ThermalQualityController.QualityConfig(
            streamId = config.streamId,
            maxQuality = ThermalQualityController.VideoQuality.valueOf(config.quality.name),
            minQuality = ThermalQualityController.VideoQuality.HD_720P,
            currentQuality = ThermalQualityController.VideoQuality.valueOf(config.quality.name),
            enableAutoScaling = true,
            upscalingEnabled = config.upscalingEnabled,
            atmosEnabled = config.atmosEnabled,
            targetFps = config.targetFps,
            maxBitrateKbps = config.maxBitrateKbps
        ))

        // 3. Register stream with adaptive prebuffer manager
        adaptivePrebufferManager.registerStream(
            streamId = config.streamId,
            chunkSize = memoryManager.getCurrentConfig()?.chunkSize ?: (1024 * 1024),
            estimatedBitrateKbps = config.maxBitrateKbps,
            isLive = config.networkSpeedMbps < 5
        )

        // 4. Initialize P2P engine if enabled
        if (config.enableP2P) {
            val memConfig = memoryManager.getCurrentConfig()
            optimizedP2PEngine.initialize(
                torrentHash = config.streamId,
                maxPeers = memConfig?.maxPeers ?: 8,
                chunkSize = memConfig?.chunkSize ?: (1024 * 1024),
                enableDelta = config.enableDeltaP2P
            )
        }

        // 5. Start prebuffering
        adaptivePrebufferManager.startPrebufferSession(config.streamId)

        currentStreamId = config.streamId
        _isInitialized.value = true

        // Start stats monitoring
        startStatsMonitoring()

        Log.i(TAG, "Playback controller initialized successfully for ${config.streamId}")
        return true
    }

    fun attachExoPlayer(player: ExoPlayer, streamId: String, mediaSource: MediaSource) {
        exoPlayer = player
        currentStreamId = streamId
        
        // Apply memory-optimized load control
        val memConfig = memoryManager.getCurrentConfig()
        if (memConfig != null) {
            // Could apply custom load control here based on memConfig
            Log.d(TAG, "Attached ExoPlayer with memory config: ${memConfig.p2pBufferSize}MB P2P buffer")
        }
        
        // Prepare media source
        player.setMediaSource(mediaSource)
        player.prepare()
    }

    fun attachKuroEngine(engine: KuroEngine) {
        kuroEngine = engine
        Log.d(TAG, "KuroEngine attached")
    }

    fun initializeDiskBuffer(bufferSizeMB: Int = 50) {
        diskBufferManager = DiskBufferManager(
            DiskBufferManager.DiskBufferConfig(
                bufferSizeMb = bufferSizeMB,
                maxReadAheadMb = 4,
                averageBitrateBps = 15_000_000
            )
        )
        Log.d(TAG, "Disk buffer initialized with ${bufferSizeMB}MB")
    }

    fun recordChunkDownload(chunkIndex: Long, bytes: Int, durationMs: Long) {
        currentStreamId?.let { streamId ->
            adaptivePrebufferManager.recordChunkDownload(streamId, chunkIndex, bytes, durationMs)
        }
    }

    fun recordChunkConsumed(chunkIndex: Long, bytes: Int) {
        currentStreamId?.let { streamId ->
            adaptivePrebufferManager.recordChunkConsumed(streamId, chunkIndex, bytes)
        }
    }

    fun putCompressedFrame(
        frameId: Long,
        timestamp: Long,
        yuvFrame: YuvFramePool.YuvFrame,
        isKeyFrame: Boolean
    ): Boolean {
        return memoryManager.putCompressedFrame(frameId, timestamp, yuvFrame, isKeyFrame)
    }

    fun getCompressedFrame(frameId: Long): ByteArray? {
        return memoryManager.getCompressedFrame(frameId)
    }

    fun acquireYuvFrame(width: Int, height: Int): YuvFramePool.YuvFrame {
        return memoryManager.acquireYuvFrame(width, height)
    }

    fun releaseYuvFrame(frame: YuvFramePool.YuvFrame) {
        memoryManager.releaseYuvFrame(frame)
    }

    fun acquireDirectBuffer(size: Int): java.nio.ByteBuffer {
        return memoryManager.acquireDirectBuffer(size)
    }

    fun releaseDirectBuffer(buffer: java.nio.ByteBuffer) {
        memoryManager.releaseDirectBuffer(buffer)
    }

    fun getMappedBuffer(file: java.io.File): java.nio.ByteBuffer? {
        return memoryManager.getMappedBuffer(file)
    }

    fun forceQuality(quality: KuroStreamMemoryManager.VideoQuality) {
        currentStreamId?.let { streamId ->
            memoryManager.forceQuality(streamId, quality)
            Log.i(TAG, "Forced quality change to ${quality} for $streamId")
        }
    }

    fun getCurrentQuality(): KuroStreamMemoryManager.VideoQuality? {
        return currentStreamId?.let { memoryManager.getCurrentQuality(it) }
    }

    fun getUnifiedStats(): UnifiedStats? {
        return memoryManager.getUnifiedStats()
    }

    private fun startStatsMonitoring() {
        scope.launch {
            while (currentStreamId != null) {
                try {
                    val stats = memoryManager.getUnifiedStats()
                    _currentStats.value = stats
                    
                    // Update memory pressure state
                    val pressure = stats.memoryPressure
                    _memoryPressure.value = when (pressure) {
                        is KuroStreamMemoryManager.MemoryPressure.CRITICAL -> MemoryPressure.CRITICAL
                        is KuroStreamMemoryManager.MemoryPressure.HIGH -> MemoryPressure.HIGH
                        is KuroStreamMemoryManager.MemoryPressure.MODERATE -> MemoryPressure.MODERATE
                        is KuroStreamMemoryManager.MemoryPressure.LOW -> MemoryPressure.LOW
                        else -> MemoryPressure.NONE
                    }
                    
                    // Auto-trim on high pressure
                    if (pressure.ordinal >= KuroStreamMemoryManager.MemoryPressure.HIGH.ordinal) {
                        val trimLevel = when (pressure) {
                            is KuroStreamMemoryManager.MemoryPressure.CRITICAL -> android.app.ActivityManager.TRIM_MEMORY_RUNNING_CRITICAL
                            is KuroStreamMemoryManager.MemoryPressure.HIGH -> android.app.ActivityManager.TRIM_MEMORY_RUNNING_LOW
                            else -> android.app.ActivityManager.TRIM_MEMORY_RUNNING_MODERATE
                        }
                        memoryManager.trimMemory(trimLevel)
                    }
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Stats monitoring error", e)
                }
                
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    fun shutdown() {
        Log.i(TAG, "Shutting down playback controller for ${currentStreamId ?: "unknown"}")
        
        currentStreamId?.let { streamId ->
            adaptivePrebufferManager.stopPrebufferSession(streamId)
            thermalQualityController.unregisterStream(streamId)
            adaptivePrebufferManager.unregisterStream(streamId)
        }
        
        scope.coroutineContext[Job]?.cancel()
        _isInitialized.value = false
        currentStreamId = null
        exoPlayer = null
        kuroEngine = null
        diskBufferManager = null
        
        // Don't shutdown memoryManager as it's shared
        Log.i(TAG, "Playback controller shutdown complete")
    }
}