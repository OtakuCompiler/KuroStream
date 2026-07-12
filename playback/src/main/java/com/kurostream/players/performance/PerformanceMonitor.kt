package com.kurostream.players.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceMonitor @Inject constructor(
    private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private val _memoryStats = MutableStateFlow(MemoryStats())
    val memoryStats: StateFlow<MemoryStats> = _memoryStats.asStateFlow()

    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()

    fun updateMemoryStats() {
        val memInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memInfo)

        val processMemory = memInfo.getTotalPrivateMemory()
        val nativeHeap = Debug.getNativeHeapAllocatedSize() / 1024
        val dalvikHeap = Debug.getDalvikHeapSize() / 1024

        val appProcesses = activityManager.runningAppProcesses
        val isForeground = appProcesses.any {
            it.pid == android.os.Process.myPid() && it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }

        _memoryStats.value = MemoryStats(
            totalPrivateMemoryKb = processMemory,
            nativeHeapKb = nativeHeap,
            dalvikHeapKb = dalvikHeap,
            isForeground = isForeground,
            timestamp = System.currentTimeMillis(),
        )
    }

    fun updatePlaybackMetrics(
        resolution: Pair<Int, Int>,
        isUpscaling: Boolean,
        isAudioTranscoding: Boolean,
        frameRate: Float,
        droppedFrames: Long,
        bufferHealth: Float,
        downloadSpeedBps: Long,
        cpuUsage: Float,
    ) {
        _performanceMetrics.value = PerformanceMetrics(
            resolution = resolution,
            isUpscaling = isUpscaling,
            isAudioTranscoding = isAudioTranscoding,
            frameRate = frameRate,
            droppedFrames = droppedFrames,
            bufferHealth = bufferHealth,
            downloadSpeedBps = downloadSpeedBps,
            cpuUsage = cpuUsage,
            timestamp = System.currentTimeMillis(),
        )
    }

    fun getEstimatedRamUsage(config: PlaybackConfig): RamEstimate {
        val baseRam = when (config.resolution) {
            "4K" -> 512
            "1080p" -> 256
            "720p" -> 128
            else -> 96
        }

        val upscalingOverhead = if (config.isUpscaling) {
            when (config.upscaleTarget) {
                "4K" -> 384
                "1440p" -> 192
                else -> 96
            }
        } else 0

        val audioTranscodingOverhead = if (config.isAudioTranscoding) {
            when (config.audioCodec) {
                "DTS" -> 128
                "DTS-HD" -> 192
                "TrueHD" -> 160
                "AC3" -> 64
                else -> 96
            }
        } else 0

        val p2pOverhead = if (config.isP2P) {
            when (config.swarmSize) {
                "large" -> 256
                "medium" -> 128
                "small" -> 64
                else -> 96
            }
        } else 0

        val totalEstimated = baseRam + upscalingOverhead + audioTranscodingOverhead + p2pOverhead

        return RamEstimate(
            baseMemoryMb = baseRam,
            upscalingOverheadMb = upscalingOverhead,
            audioTranscodingOverheadMb = audioTranscodingOverhead,
            p2pOverheadMb = p2pOverhead,
            totalEstimatedMb = totalEstimated,
            confidence = 0.85f,
        )
    }

    fun getOptimizationSuggestions(): List<OptimizationSuggestion> {
        val suggestions = mutableListOf<OptimizationSuggestion>()
        val currentMem = _memoryStats.value.totalPrivateMemoryKb / 1024
        val currentMetrics = _performanceMetrics.value

        if (currentMem > 1024) {
            suggestions.add(
                OptimizationSuggestion(
                    priority = Priority.HIGH,
                    category = Category.MEMORY,
                    title = "High Memory Usage",
                    description = "Memory usage exceeds 1GB. Consider disabling upscaling or reducing buffer size.",
                    action = "Reduce buffer size or disable upscaling",
                    estimatedSavingsMb = 200,
                )
            )
        }

        if (currentMetrics.droppedFrames > 100) {
            suggestions.add(
                OptimizationSuggestion(
                    priority = Priority.MEDIUM,
                    category = Category.PERFORMANCE,
                    title = "Frame Drops Detected",
                    description = "${currentMetrics.droppedFrames} frames dropped. Consider disabling 4K upscaling.",
                    action = "Disable upscaling or use hardware decoder",
                    estimatedSavingsMb = 384,
                )
            )
        }

        if (currentMetrics.isAudioTranscoding && currentMem > 768) {
            suggestions.add(
                OptimizationSuggestion(
                    priority = Priority.MEDIUM,
                    category = Category.AUDIO,
                    title = "Audio Transcoding Active",
                    description = "Audio transcoding is using additional memory. Consider passthrough if supported.",
                    action = "Enable audio passthrough",
                    estimatedSavingsMb = 128,
                )
            )
        }

        return suggestions
    }
}

data class MemoryStats(
    val totalPrivateMemoryKb: Int = 0,
    val nativeHeapKb: Long = 0,
    val dalvikHeapKb: Long = 0,
    val isForeground: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
)

data class PerformanceMetrics(
    val resolution: Pair<Int, Int> = 1920 to 1080,
    val isUpscaling: Boolean = false,
    val isAudioTranscoding: Boolean = false,
    val frameRate: Float = 30f,
    val droppedFrames: Long = 0,
    val bufferHealth: Float = 1f,
    val downloadSpeedBps: Long = 0,
    val cpuUsage: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
)

data class PlaybackConfig(
    val resolution: String = "1080p",
    val isUpscaling: Boolean = false,
    val upscaleTarget: String = "4K",
    val isAudioTranscoding: Boolean = false,
    val audioCodec: String = "AAC",
    val isP2P: Boolean = false,
    val swarmSize: String = "medium",
)

data class RamEstimate(
    val baseMemoryMb: Int,
    val upscalingOverheadMb: Int,
    val audioTranscodingOverheadMb: Int,
    val p2pOverheadMb: Int,
    val totalEstimatedMb: Int,
    val confidence: Float,
)

data class OptimizationSuggestion(
    val priority: Priority,
    val category: Category,
    val title: String,
    val description: String,
    val action: String,
    val estimatedSavingsMb: Int,
)

enum class Priority { LOW, MEDIUM, HIGH, CRITICAL }
enum class Category { MEMORY, PERFORMANCE, AUDIO, VIDEO, NETWORK }
