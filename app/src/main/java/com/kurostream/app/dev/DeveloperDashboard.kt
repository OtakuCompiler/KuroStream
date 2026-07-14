package com.kurostream.app.dev

import com.kurostream.common.memory.AdaptiveMemoryGovernor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeveloperDashboard @Inject constructor(
    private val memoryGovernor: AdaptiveMemoryGovernor,
) {
    data class Metrics(
        val fps: Float,
        val cpuUsage: Float,
        val ramUsedMb: Long,
        val ramTotalMb: Long,
        val ramPercent: Float,
        val gpuUsage: Float,
        val threadCount: Int,
        val gcCount: Long,
        val jankFrames: Int,
        val totalFrames: Int,
        val jankPercent: Float,
        val heapSizeMb: Long,
        val heapAllocatedMb: Long,
        val heapFreeMb: Long,
        val memoryTier: AdaptiveMemoryGovernor.Tier,
    )

    private var frameTimes = mutableListOf<Long>()
    private var jankCount = 0
    private var frameCount = 0

    fun recordFrameTime(frameTimeNs: Long) {
        frameTimes.add(frameTimeNs)
        frameCount++
        if (frameTimeNs > 16_666_666) jankCount++
        if (frameTimes.size > 120) frameTimes.removeFirst()
    }

    fun collectMetrics(): Metrics {
        val runtime = Runtime.getRuntime()
        val profile = memoryGovernor.profile
        val avgFps = if (frameTimes.isNotEmpty()) {
            1_000_000_000f / frameTimes.average().toFloat()
        } else 0f
        val heapTotal = runtime.totalMemory() / (1024 * 1024)
        val heapFree = runtime.freeMemory() / (1024 * 1024)
        val heapAllocated = heapTotal - heapFree
        val jankPercent = if (frameCount > 0) (jankCount.toFloat() / frameCount) * 100 else 0f
        val gcCount = 0L

        return Metrics(
            fps = avgFps,
            cpuUsage = readCpuUsage(),
            ramUsedMb = profile.totalRamMb - (profile.totalRamMb * memoryGovernor.availableMemoryFraction).toLong(),
            ramTotalMb = profile.totalRamMb,
            ramPercent = (1f - memoryGovernor.availableMemoryFraction) * 100,
            gpuUsage = 0f,
            threadCount = Thread.activeCount(),
            gcCount = gcCount,
            jankFrames = jankCount,
            totalFrames = frameCount,
            jankPercent = jankPercent,
            heapSizeMb = heapTotal,
            heapAllocatedMb = heapAllocated,
            heapFreeMb = heapFree,
            memoryTier = profile.tier,
        )
    }

    private fun readCpuUsage(): Float {
        return try {
            val procStat = java.io.File("/proc/stat").readLines().firstOrNull() ?: return 0f
            val parts = procStat.split("\\s+".toRegex()).drop(1).take(8).mapNotNull { it.toLongOrNull() }
            if (parts.size < 8) return 0f
            val idle = parts[3].toFloat()
            val total = parts.sum().toFloat()
            (1f - idle / total) * 100f
        } catch (e: Exception) { 0f }
    }

    fun reset() {
        frameTimes.clear()
        jankCount = 0
        frameCount = 0
    }
}
