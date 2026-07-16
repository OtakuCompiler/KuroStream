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
import android.os.Build
import android.util.Log
import androidx.annotation.Keep
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.kurostream.playback.p2p.OptimizedP2PEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Keep
class KuroStreamMemoryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ultraLowMemoryManager: UltraLowMemoryManagerV3,
    private val zeroCopyBufferManager: ZeroCopyBufferManager,
    private val yuvFramePool: YuvFramePool,
    private val compressedFrameCache: CompressedFrameCache,
    private val adaptivePrebufferManager: AdaptivePrebufferManager,
    private val thermalQualityController: ThermalQualityController,
    private val optimizedP2PEngine: OptimizedP2PEngine
) {
    companion object {
        private const val TAG = "KuroStreamMemoryManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var initialized = false
    private var currentConfig: UnifiedConfig? = null

    data class UnifiedConfig(
        val streamId: String,
        val quality: UltraLowMemoryManagerV3.VideoQuality,
        val hasUpscaling: Boolean,
        val hasTranscoding: Boolean,
        val networkSpeedMbps: Long,
        val enableP2P: Boolean,
        val enableCompressedFrames: Boolean,
        val enableDeltaP2P: Boolean,
        val maxBitrateKbps: Int,
        val targetFps: Int,
        val upscalingEnabled: Boolean,
        val atmosEnabled: Boolean
    )

    enum class VideoQuality {
        LD_360P,
        SD_480P,
        HD_720P,
        FHD_1080P,
        UHD_4K
    }

    enum class ThermalState {
        NORMAL, LIGHT, MODERATE, SEVERE, CRITICAL
    }

    data class UnifiedStats(
        val totalRAM_MB: Long,
        val p2pRAM_MB: Long,
        val decoderRAM_MB: Long,
        val upscalerRAM_MB: Long,
        val audioRAM_MB: Long,
        val networkRAM_MB: Long,
        val uiRAM_MB: Long,
        val framePoolRAM_MB: Long,
        val compressedCacheRAM_MB: Long,
        val zeroCopyRAM_MB: Long,
        val thermalState: ThermalState,
        val memoryPressure: UltraLowMemoryManagerV3.MemoryPressure,
        val p2pStats: Map<String, Any>,
        val prebufferStats: Map<String, Any>,
        val frameCacheStats: Map<String, Any>,
        val bufferPoolStats: Map<String, Any>,
        val qualityState: Map<String, Any>
    )

    init {
        // Start monitoring
        scope.launch {
            monitorSystemHealth()
        }
    }

    fun initialize(config: UnifiedConfig): Boolean {
        if (initialized && currentConfig?.streamId == config.streamId) {
            return true
        }

        Log.i(TAG, "Initializing KuroStream Memory Manager for stream: ${config.streamId}")

        // 1. Get optimized memory config from ultra-low memory manager
        val memoryConfig = ultraLowMemoryManager.getOptimizedConfig(
            quality = config.quality,
            hasUpscaling = config.hasUpscaling,
            hasTranscoding = config.hasTranscoding,
            networkSpeedMbps = config.networkSpeedMbps,
            thermalState = getCurrentThermalState()
        )

        // 2. Initialize buffer pools
        zeroCopyBufferManager.initializeBufferPool(
            chunkSize = memoryConfig.chunkSize,
            capacity = memoryConfig.maxPeers * 2
        )

        // 3. Initialize YUV frame pool (it's lazy initialized)

        // 4. Initialize compressed frame cache
        compressedFrameCache.initialize(CompressedFrameCache.CacheConfig(
            maxSizeBytes = memoryConfig.compressedCacheSize.toLong(),
            maxFrames = 60,
            enableCompression = config.enableCompressedFrames,
            keepKeyFrames = true
        ))

        // 5. Register stream with adaptive prebuffer manager
        val estimatedBitrate = config.maxBitrateKbps
        val isLive = config.networkSpeedMbps < 5 // Heuristic
        adaptivePrebufferManager.registerStream(
            streamId = config.streamId,
            chunkSize = memoryConfig.chunkSize,
            estimatedBitrateKbps = estimatedBitrate,
            isLive = isLive
        )

        // 6. Register stream with thermal quality controller
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

        // 7. Initialize P2P engine if enabled
        if (config.enableP2P) {
            optimizedP2PEngine.initialize(
                torrentHash = config.streamId,
                maxPeers = memoryConfig.maxPeers,
                chunkSize = memoryConfig.chunkSize,
                enableDelta = config.enableDeltaP2P
            )
        }

        currentConfig = config
        initialized = true

        Log.i(TAG, "Memory manager initialized: ${getStatsSummary()}")
        return true
    }

    fun getUnifiedStats(): UnifiedStats {
        val memoryStats = ultraLowMemoryManager.getMemoryStats()
        val p2pStats = optimizedP2PEngine.stats.value
        val prebufferStats = adaptivePrebufferManager.getGlobalStats()
        val frameCacheStats = compressedFrameCache.getStats()
        val bufferPoolStats = zeroCopyBufferManager.getCacheStats()
        val qualityState = thermalQualityController.getThermalStats()
        val thermalState = getCurrentThermalState()

        return UnifiedStats(
            totalRAM_MB = calculateTotalRAM(memoryStats),
            p2pRAM_MB = memoryStats["p2pBufferMB"] as? Long ?: 0,
            decoderRAM_MB = memoryStats["decoderBufferMB"] as? Long ?: 0,
            upscalerRAM_MB = memoryStats["upscalerBufferMB"] as? Long ?: 0,
            audioRAM_MB = memoryStats["audioBufferMB"] as? Long ?: 0,
            networkRAM_MB = memoryStats["networkBufferMB"] as? Long ?: 0,
            uiRAM_MB = memoryStats["uiBufferMB"] as? Long ?: 0,
            framePoolRAM_MB = memoryStats["framePoolMB"] as? Long ?: 0,
            compressedCacheRAM_MB = memoryStats["compressedCacheMB"] as? Long ?: 0,
            zeroCopyRAM_MB = bufferPoolStats["directBufferPoolMB"] as? Long ?: 0,
            thermalState = thermalState,
            memoryPressure = getMemoryPressure(),
            p2pStats = mapOf(
                "connectedPeers" to p2pStats.connectedPeers,
                "downloadSpeedKbps" to p2pStats.downloadSpeedKbps,
                "uploadSpeedKbps" to p2pStats.uploadSpeedKbps,
                "pieceHitRate" to p2pStats.pieceHitRate,
                "avgLatencyMs" to p2pStats.avgLatencyMs
            ),
            prebufferStats = prebufferStats,
            frameCacheStats = frameCacheStats,
            bufferPoolStats = bufferPoolStats,
            qualityState = qualityState
        )
    }

    private fun calculateTotalRAM(memoryStats: Map<String, Any>): Long {
        return listOf(
            "p2pBufferMB", "decoderBufferMB", "upscalerBufferMB",
            "audioBufferMB", "networkBufferMB", "uiBufferMB",
            "compressedCacheMB", "framePoolMB"
        ).sumOf { key -> (memoryStats[key] as? Long) ?: 0 }
    }

    fun acquireYuvFrame(width: Int, height: Int): YuvFramePool.YuvFrame {
        return yuvFramePool.acquireFrame(width, height)
    }

    fun releaseYuvFrame(frame: YuvFramePool.YuvFrame) {
        yuvFramePool.releaseFrame(frame)
    }

    fun acquireDirectBuffer(size: Int): ByteBuffer {
        return zeroCopyBufferManager.acquireDirectBuffer(size)
    }

    fun releaseDirectBuffer(buffer: ByteBuffer) {
        zeroCopyBufferManager.releaseDirectBuffer(buffer)
    }

    fun getMappedBuffer(file: java.io.File): ByteBuffer? {
        return zeroCopyBufferManager.acquireMappedBuffer(file)?.buffer
    }

    fun putCompressedFrame(
        frameId: Long,
        timestamp: Long,
        yuvFrame: YuvFramePool.YuvFrame,
        isKeyFrame: Boolean
    ): Boolean {
        return compressedFrameCache.putFrame(frameId, timestamp, yuvFrame, isKeyFrame)
    }

    fun getCompressedFrame(frameId: Long): ByteArray? {
        return compressedFrameCache.getFrame(frameId)
    }

    fun startPrebuffering(streamId: String) {
        adaptivePrebufferManager.startPrebufferSession(streamId)
    }

    fun stopPrebuffering(streamId: String) {
        adaptivePrebufferManager.stopPrebufferSession(streamId)
    }

    fun recordChunkDownload(streamId: String, chunkIndex: Long, bytes: Int, durationMs: Long) {
        adaptivePrebufferManager.recordChunkDownload(streamId, chunkIndex, bytes, durationMs)
    }

    fun recordChunkConsumed(streamId: String, chunkIndex: Long, bytes: Int) {
        adaptivePrebufferManager.recordChunkConsumed(streamId, chunkIndex, bytes)
    }

    fun forceQuality(streamId: String, quality: UltraLowMemoryManagerV3.VideoQuality) {
        val thermalQuality = ThermalQualityController.VideoQuality.valueOf(quality.name)
        thermalQualityController.forceQuality(streamId, thermalQuality)
    }

    fun getCurrentQuality(streamId: String): UltraLowMemoryManagerV3.VideoQuality? {
        return thermalQualityController.getCurrentQuality(streamId)?.currentQuality?.let {
            UltraLowMemoryManagerV3.VideoQuality.valueOf(it.name)
        }
    }

    fun trimMemory(level: Int) {
        ultraLowMemoryManager.trimMemory(level)
        zeroCopyBufferManager.trimMemory(level)
        yuvFramePool.trimMemory(level)
        compressedFrameCache.trimMemory(level)
        adaptivePrebufferManager.trimMemory(level)
        optimizedP2PEngine.trimMemory?.invoke(level)
    }

    fun shutdown() {
        scope.coroutineContext[Job]?.cancel()
        ultraLowMemoryManager.shutdown()
        zeroCopyBufferManager.shutdown()
        yuvFramePool.shutdown()
        compressedFrameCache.shutdown()
        adaptivePrebufferManager.shutdown()
        thermalQualityController.shutdown()
        // optimizedP2PEngine.shutdown()
        initialized = false
        Log.i(TAG, "KuroStream Memory Manager shutdown complete")
    }

    private fun monitorSystemHealth() {
        while (true) {
            try {
                val stats = getUnifiedStats()
                
                // Log periodic stats
                Log.d(TAG, "Health: RAM=${stats.totalRAM_MB}MB, Thermal=${stats.thermalState}, Pressure=${stats.memoryPressure}")
                
                // Auto-trim if pressure is high
                if (stats.memoryPressure.ordinal >= UltraLowMemoryManagerV3.MemoryPressure.HIGH.ordinal) {
                    val trimLevel = when (stats.memoryPressure) {
                        UltraLowMemoryManagerV3.MemoryPressure.CRITICAL -> android.app.ActivityManager.TRIM_MEMORY_RUNNING_CRITICAL
                        UltraLowMemoryManagerV3.MemoryPressure.HIGH -> android.app.ActivityManager.TRIM_MEMORY_RUNNING_LOW
                        else -> android.app.ActivityManager.TRIM_MEMORY_RUNNING_MODERATE
                    }
                    trimMemory(trimLevel)
                }
                
            } catch (e: Exception) {
                Log.w(TAG, "Health monitoring error", e)
            }
            
            kotlinx.coroutines.delay(5000)
        }
    }

    private fun getCurrentThermalState(): UltraLowMemoryManagerV3.ThermalState {
        try {
            val thermalManager = context.getSystemService(Context.THERMAL_SERVICE) as? android.hardware.thermal.ThermalManager
            return when (thermalManager?.currentThermalStatus) {
                1 -> UltraLowMemoryManagerV3.ThermalState.NORMAL
                2 -> UltraLowMemoryManagerV3.ThermalState.LIGHT
                3 -> UltraLowMemoryManagerV3.ThermalState.MODERATE
                4 -> UltraLowMemoryManagerV3.ThermalState.SEVERE
                5 -> UltraLowMemoryManagerV3.ThermalState.CRITICAL
                else -> UltraLowMemoryManagerV3.ThermalState.NORMAL
            }
        } catch (e: Exception) {
            UltraLowMemoryManagerV3.ThermalState.NORMAL
        }
    }

    private fun getMemoryPressure(): UltraLowMemoryManagerV3.MemoryPressure {
        val info = android.app.ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(info)
        
        return when {
            info.availMem < info.totalMem * 0.04 -> UltraLowMemoryManagerV3.MemoryPressure.CRITICAL
            info.availMem < info.totalMem * 0.08 -> UltraLowMemoryManagerV3.MemoryPressure.HIGH
            info.availMem < info.totalMem * 0.12 -> UltraLowMemoryManagerV3.MemoryPressure.MODERATE
            info.availMem < info.totalMem * 0.20 -> UltraLowMemoryManagerV3.MemoryPressure.LOW
            else -> UltraLowMemoryManagerV3.MemoryPressure.NONE
        }
    }

    private fun getStatsSummary(): String {
        val stats = getUnifiedStats()
        return "RAM: ${stats.totalRAM_MB}MB (P2P: ${stats.p2pRAM_MB}, Dec: ${stats.decoderRAM_MB}, Up: ${stats.upscalerRAM_MB}, Frame: ${stats.framePoolRAM_MB}, Cache: ${stats.compressedCacheRAM_MB}, ZeroCopy: ${stats.zeroCopyRAM_MB})"
    }

    // Convenience methods for common use cases
    fun createOptimizedConfig1080p(
        streamId: String,
        networkSpeedMbps: Long,
        enableP2P: Boolean = true
    ): UnifiedConfig = UnifiedConfig(
        streamId = streamId,
        quality = UltraLowMemoryManagerV3.VideoQuality.FHD_1080P,
        hasUpscaling = false,
        hasTranscoding = false,
        networkSpeedMbps = networkSpeedMbps,
        enableP2P = enableP2P,
        enableCompressedFrames = true,
        enableDeltaP2P = true,
        maxBitrateKbps = 8000,
        targetFps = 60,
        upscalingEnabled = false,
        atmosEnabled = false
    )

    fun createOptimizedConfig1080pTo4K(
        streamId: String,
        networkSpeedMbps: Long,
        enableP2P: Boolean = true
    ): UnifiedConfig = UnifiedConfig(
        streamId = streamId,
        quality = UltraLowMemoryManagerV3.VideoQuality.FHD_1080P,
        hasUpscaling = true,
        hasTranscoding = false,
        networkSpeedMbps = networkSpeedMbps,
        enableP2P = enableP2P,
        enableCompressedFrames = true,
        enableDeltaP2P = true,
        maxBitrateKbps = 12000,
        targetFps = 60,
        upscalingEnabled = true,
        atmosEnabled = false
    )

    fun createOptimizedConfig4K(
        streamId: String,
        networkSpeedMbps: Long,
        enableP2P: Boolean = true,
        enableAtmos: Boolean = false
    ): UnifiedConfig = UnifiedConfig(
        streamId = streamId,
        quality = UltraLowMemoryManagerV3.VideoQuality.UHD_4K,
        hasUpscaling = false,
        hasTranscoding = enableAtmos,
        networkSpeedMbps = networkSpeedMbps,
        enableP2P = enableP2P,
        enableCompressedFrames = true,
        enableDeltaP2P = true,
        maxBitrateKbps = if (enableAtmos) 25000 else 20000,
        targetFps = 60,
        upscalingEnabled = false,
        atmosEnabled = enableAtmos
    )

    fun createUltraLowConfig4K(
        streamId: String,
        networkSpeedMbps: Long
    ): UnifiedConfig = UnifiedConfig(
        streamId = streamId,
        quality = UltraLowMemoryManagerV3.VideoQuality.UHD_4K,
        hasUpscaling = false,
        hasTranscoding = false,
        networkSpeedMbps = networkSpeedMbps,
        enableP2P = true,
        enableCompressedFrames = true,
        enableDeltaP2P = true,
        maxBitrateKbps = 15000,
        targetFps = 30,
        upscalingEnabled = false,
        atmosEnabled = false
    )
}