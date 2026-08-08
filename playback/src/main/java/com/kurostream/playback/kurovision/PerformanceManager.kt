// This file is part of KuroStream.
//
// KuroPerformanceManager — dynamic quality adjustment based on real-time
// FPS, memory pressure, thermal state, and decoder load.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.playback.kurovision

import android.os.Process
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class KuroPerformanceManager @Inject constructor(
    private val engine: KuroVisionEngine,
    private val pipeline: KuroVisionPipeline,
) {
    private val perfScope = CoroutineScope(Dispatchers.Default + Job())
    private var monitoringJob: Job? = null

    private val _status = MutableStateFlow(PerfStatus())
    val status: StateFlow<PerfStatus> = _status.asStateFlow()

    fun startMonitoring(intervalMs: Long = 2000) {
        stopMonitoring()
        monitoringJob = perfScope.launch {
            var lastFrameCount = 0L
            var lastTime = System.nanoTime()
            while (isActive) {
                delay(intervalMs)
                val stats = pipeline.frameStats.value
                val now = System.nanoTime()
                val dtSec = (now - lastTime) / 1_000_000_000f
                val measuredFps = if (dtSec > 0) (stats.frameCount - lastFrameCount) / dtSec else 0f
                lastFrameCount = stats.frameCount
                lastTime = now

                val memInfo = getRuntimeMemInfo()
                val targetMode = engine.currentMode
                val newMode = evaluateMode(measuredFps, memInfo, targetMode)
                _status.value = PerfStatus(
                    fps = measuredFps,
                    availableMemMb = memInfo.availMemMb,
                    isLowMemory = memInfo.availMemMb < 128,
                    currentMode = targetMode,
                    suggestedMode = newMode,
                    droppedFrames = stats.dropCount,
                    processTimeUs = stats.lastProcessTimeUs,
                )
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private fun evaluateMode(
        fps: Float,
        mem: MemInfo,
        current: KuroVisionQualityMode,
    ): KuroVisionQualityMode {
        return if (fps < 24 && fps > 0) {
            val downgrade = when (current) {
                KuroVisionQualityMode.ULTRA_DESKTOP -> KuroVisionQualityMode.HDR_VISION
                KuroVisionQualityMode.HDR_VISION -> KuroVisionQualityMode.ANIME_PRO
                KuroVisionQualityMode.ANIME_PRO -> KuroVisionQualityMode.CINEMA
                else -> KuroVisionQualityMode.HARDWARE
            }
            Timber.w(TAG, "Auto-downgrade: $current → $downgrade (fps=$fps)")
            downgrade
        } else if (mem.availMemMb < 90 && current != KuroVisionQualityMode.HARDWARE) {
            KuroVisionQualityMode.HARDWARE
        } else {
            current
        }
    }

    private data class MemInfo(val availMemMb: Long)

    private fun getRuntimeMemInfo(): MemInfo {
        val rt = Runtime.getRuntime()
        val used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
        val max = rt.maxMemory() / (1024 * 1024)
        return MemInfo(availMemMb = max - used)
    }

    data class PerfStatus(
        val fps: Float = 0f,
        val availableMemMb: Long = 0,
        val isLowMemory: Boolean = false,
        val currentMode: KuroVisionQualityMode = KuroVisionQualityMode.HARDWARE,
        val suggestedMode: KuroVisionQualityMode = KuroVisionQualityMode.HARDWARE,
        val droppedFrames: Int = 0,
        val processTimeUs: Long = 0,
    )

    companion object {
        private const val TAG = "KuroPerfMgr"
    }
}
