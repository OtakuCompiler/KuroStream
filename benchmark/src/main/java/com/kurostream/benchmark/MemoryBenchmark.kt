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

package com.kurostream.benchmark

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.SystemClock
import android.util.Log
import com.kurostream.playback.memory.AdaptivePrebufferManager
import com.kurostream.playback.memory.CompressedFrameCache
import com.kurostream.playback.memory.KuroStreamMemoryManager
import com.kurostream.playback.memory.OptimizedP2PEngine
import com.kurostream.playback.memory.ThermalQualityController
import com.kurostream.playback.memory.UltraLowMemoryManagerV3
import com.kurostream.playback.memory.YuvFramePool
import com.kurostream.playback.memory.ZeroCopyBufferManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Memory regression benchmark for Phase 4+ optimizations
 * Target: <30MB for 1080p P2P, <40MB for 4K+upscale on 2GB device
 */
class MemoryBenchmark @Inject constructor(
    private val context: Context,
    private val memoryManager: UltraLowMemoryManagerV3,
    private val zeroCopyBufferManager: ZeroCopyBufferManager,
    private val yuvFramePool: YuvFramePool,
    private val compressedFrameCache: CompressedFrameCache,
    private val adaptivePrebufferManager: AdaptivePrebufferManager,
    private val thermalQualityController: ThermalQualityController,
    private val optimizedP2PEngine: OptimizedP2PEngine,
    private val kuroStreamMemoryManager: KuroStreamMemoryManager,
) {
    companion object {
        private const val TAG = "MemoryBenchmark"
        private const val BENCHMARK_DURATION_MS = 60_000 // 1 minute
        private const val SAMPLE_INTERVAL_MS = 1000
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private var isRunning = false
    private var results = mutableListOf<BenchmarkResult>()

    data class BenchmarkResult(
        val timestamp: Long,
        val scenario: String,
        val pssKb: Long,
        val rssKb: Long,
        val javaHeapKb: Long,
        val nativeHeapKb: Long,
        val totalPssKb: Long,
        val config: Map<String, Any>
    )

    data class ScenarioConfig(
        val name: String,
        val quality: KuroStreamMemoryManager.VideoQuality,
        val hasUpscaling: Boolean,
        val hasTranscoding: Boolean,
        val networkSpeedMbps: Long,
        val enableP2P: Boolean,
        val durationMs: Long = BENCHMARK_DURATION_MS,
    )

    val scenarios = listOf(
        ScenarioConfig("1080p_P2P_Direct", KuroStreamMemoryManager.VideoQuality.FHD_1080P, false, false, 10, true),
        ScenarioConfig("1080p_Upscale_4K", KuroStreamMemoryManager.VideoQuality.FHD_1080P, true, false, 15, true),
        ScenarioConfig("4K_Direct", KuroStreamMemoryManager.VideoQuality.UHD_4K, false, false, 20, true),
        ScenarioConfig("4K_Atmos", KuroStreamMemoryManager.VideoQuality.UHD_4K, false, true, 25, true),
        ScenarioConfig("Idle_Home_Scrolling", KuroStreamMemoryManager.VideoQuality.FHD_1080P, false, false, 10, false, 30_000),
    )

    suspend fun runAllScenarios(onProgress: (String, Double) -> Unit = { _, _ -> }): List<BenchmarkResult> {
        results.clear()
        isRunning = true

        for ((index, scenario) in scenarios.withIndex()) {
            if (!isRunning) break
            
            onProgress(scenario.name, index.toDouble() / scenarios.size)
            val result = runScenario(scenario)
            results.add(result)
            
            // Cool down between scenarios
            kotlinx.coroutines.delay(5000)
            forceGC()
        }

        isRunning = false
        return results
    }

    private suspend fun runScenario(scenario: ScenarioConfig): BenchmarkResult {
        Log.i(TAG, "Starting benchmark: ${scenario.name}")

        // Initialize memory manager with scenario config
        val thermalState = when {
            scenario.quality == KuroStreamMemoryManager.VideoQuality.UHD_4K && scenario.hasTranscoding -> 
                KuroStreamMemoryManager.ThermalState.LIGHT
            else -> KuroStreamMemoryManager.ThermalState.NORMAL
        }

        val memoryConfig = memoryManager.getOptimizedConfig(
            quality = scenario.quality,
            hasUpscaling = scenario.hasUpscaling,
            hasTranscoding = scenario.hasTranscoding,
            networkSpeedMbps = scenario.networkSpeedMbps,
            thermalState = thermalState
        )

        // Initialize KuroStream memory manager
        val unifiedConfig = KuroStreamMemoryManager.UnifiedConfig(
            streamId = "benchmark_${scenario.name}",
            quality = scenario.quality,
            hasUpscaling = scenario.hasUpscaling,
            hasTranscoding = scenario.hasTranscoding,
            networkSpeedMbps = scenario.networkSpeedMbps,
            enableP2P = scenario.enableP2P,
            enableCompressedFrames = true,
            enableDeltaP2P = true,
            maxBitrateKbps = when (scenario.quality) {
                KuroStreamMemoryManager.VideoQuality.UHD_4K -> 25000
                KuroStreamMemoryManager.VideoQuality.FHD_1080P -> 8000
                else -> 3000
            },
            targetFps = if (scenario.quality == KuroStreamMemoryManager.VideoQuality.UHD_4K) 60 else 60,
            upscalingEnabled = scenario.hasUpscaling,
            atmosEnabled = scenario.hasTranscoding,
        )

        kuroStreamMemoryManager.initialize(unifiedConfig)

        // Simulate workload
        val samples = mutableListOf<BenchmarkResult>()
        val startTime = SystemClock.uptimeMillis()
        var frameId = 0L

        while (SystemClock.uptimeMillis() - startTime < scenario.durationMs && isRunning) {
            // Simulate frame processing
            if (scenario.quality != KuroStreamMemoryManager.VideoQuality.FHD_1080P || scenario.hasUpscaling) {
                val frame = yuvFramePool.acquireFrame(
                    width = when (scenario.quality) {
                        KuroStreamMemoryManager.VideoQuality.UHD_4K -> 3840
                        else -> 1920
                    },
                    height = when (scenario.quality) {
                        KuroStreamMemoryManager.VideoQuality.UHD_4K -> 2160
                        else -> 1080
                    }
                )
                
                // Simulate compression
                if (frameId % 3 == 0) { // Key frame every 3 frames
                    compressedFrameCache.putFrame(
                        frameId = frameId,
                        timestamp = System.currentTimeMillis(),
                        width = frame.width,
                        height = frame.height,
                        format = 35, // YUV_420_888
                        yuvFrame = frame,
                        isKeyFrame = true
                    )
                }
                frameId++
            }

            // Simulate chunk download
            if (scenario.enableP2P) {
                val chunkSize = memoryConfig.chunkSize
                val duration = (chunkSize * 8.0 / (scenario.networkSpeedMbps * 1000000)).toLong() * 1000
                adaptivePrebufferManager.recordChunkDownload("benchmark", frameId, chunkSize, duration)
                adaptivePrebufferManager.recordChunkConsumed("benchmark", frameId, chunkSize)
            }

            // Sample memory
            val memInfo = sampleMemory()
            samples.add(memInfo)

            kotlinx.coroutines.delay(SAMPLE_INTERVAL_MS)
        }

        // Cleanup
        kuroStreamMemoryManager.shutdown()
        
        // Calculate averages (excluding first 10% warmup)
        val warmupCount = (samples.size * 0.1).toInt()
        val validSamples = samples.drop(warmupCount)
        
        val avgPss = if (validSamples.isNotEmpty()) validSamples.map { it.pssKb }.average() else 0.0
        val avgRss = if (validSamples.isNotEmpty()) validSamples.map { it.rssKb }.average() else 0.0
        val avgJavaHeap = if (validSamples.isNotEmpty()) validSamples.map { it.javaHeapKb }.average() else 0.0
        val avgNativeHeap = if (validSamples.isNotEmpty()) validSamples.map { it.nativeHeapKb }.average() else 0.0
        val peakPss = samples.maxOfOrNull { it.pssKb } ?: 0
        val peakRss = samples.maxOfOrNull { it.rssKb } ?: 0

        val result = BenchmarkResult(
            timestamp = System.currentTimeMillis(),
            scenario = scenario.name,
            pssKb = avgPss.toLong(),
            rssKb = avgRss.toLong(),
            javaHeapKb = avgJavaHeap.toLong(),
            nativeHeapKb = avgNativeHeap.toLong(),
            totalPssKb = peakPss,
            config = mapOf(
                "quality" to scenario.quality.name,
                "hasUpscaling" to scenario.hasUpscaling,
                "hasTranscoding" to scenario.hasTranscoding,
                "networkSpeedMbps" to scenario.networkSpeedMbps,
                "enableP2P" to scenario.enableP2P,
                "peakPssMB" to (peakPss / 1024.0),
                "avgPssMB" to (avgPss / 1024.0),
                "avgRssMB" to (avgRss / 1024.0),
                "samples" to samples.size
            )
        )

        Log.i(TAG, "Scenario ${scenario.name} complete: Peak PSS=${peakPss/1024}MB, Avg PSS=${avgPss/1024}MB")
        return result
    }

    private fun sampleMemory(): BenchmarkResult {
        val memInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memInfo)

        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)

        val pss = memInfo.totalPss
        val rss = memInfo.dalvikPss + memInfo.nativePss + memInfo.otherPss
        val javaHeap = memInfo.dalvikPss
        val nativeHeap = memInfo.nativePss

        return BenchmarkResult(
            timestamp = System.currentTimeMillis(),
            scenario = "sampling",
            pssKb = pss,
            rssKb = rss,
            javaHeapKb = javaHeap,
            nativeHeapKb = nativeHeap,
            totalPssKb = pss,
            config = emptyMap()
        )
    }

    private fun forceGC() {
        scope.launch(Dispatchers.IO) {
            System.gc()
            System.runFinalization()
            kotlinx.coroutines.delay(500)
        }
    }

    fun stop() {
        isRunning = false
        scope.coroutineContext[Job]?.cancel()
    }

    fun generateReport(): String {
        val sb = StringBuilder()
        sb.appendLine("# Memory Benchmark Report")
        sb.appendLine("Generated: ${java.util.Date()}")
        sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("RAM: ${activityManager.memoryClass}MB")
        sb.appendLine("")

        sb.appendLine("## Results Summary")
        sb.appendLine("")
        sb.appendLine("| Scenario | Peak PSS (MB) | Avg PSS (MB) | Avg RSS (MB) | Java Heap (MB) | Native Heap (MB) | Target | Status |")
        sb.appendLine("|----------|---------------|--------------|--------------|----------------|------------------|--------|--------|")

        val targets = mapOf(
            "1080p_P2P_Direct" to 30.0,
            "1080p_Upscale_4K" to 40.0,
            "4K_Direct" to 50.0,
            "4K_Atmos" to 45.0,
            "Idle_Home_Scrolling" to 25.0
        )

        results.forEach { result ->
            val target = targets[result.scenario] ?: 100.0
            val peakMB = result.totalPssKb / 1024.0
            val avgMB = result.pssKb / 1024.0
            val status = if (peakMB <= target) "✅ PASS" else "❌ FAIL"
            
            sb.appendLine("| ${result.scenario} | ${String.format("%.1f", peakMB)} | ${String.format("%.1f", avgMB)} | ${String.format("%.1f", result.rssKb / 1024.0)} | ${String.format("%.1f", result.javaHeapKb / 1024.0)} | ${String.format("%.1f", result.nativeHeapKb / 1024.0)} | ${target}MB | $status |")
        }

        sb.appendLine("")
        sb.appendLine("## Detailed Results")
        sb.appendLine("")
        results.forEach { result ->
            sb.appendLine("### ${result.scenario}")
            sb.appendLine("")
            sb.appendLine("```")
            result.config.forEach { (k, v) ->
                sb.appendLine("  $k: $v")
            }
            sb.appendLine("```")
            sb.appendLine("")
        }

        return sb.toString()
    }

    fun checkRegression(): Boolean {
        val targets = mapOf(
            "1080p_P2P_Direct" to 30.0,
            "1080p_Upscale_4K" to 40.0,
            "4K_Direct" to 50.0,
            "4K_Atmos" to 45.0,
            "Idle_Home_Scrolling" to 25.0
        )

        return results.all { result ->
            val target = targets[result.scenario] ?: 100.0
            val peakMB = result.totalPssKb / 1024.0
            peakMB <= target
        }
    }
}