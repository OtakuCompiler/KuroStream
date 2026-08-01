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

package com.kurostream.common.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adaptive memory governor for KuroStream.
 * Dynamically adjusts cache sizes, prefetch depth, and resource allocation
 * based on real-time memory pressure, thermal state, and device profile.
 *
 * Targets Snapdragon 680 (1536MB heap, 6-core) and Fire TV Stick HD.
 */
@Singleton
class AdaptiveMemoryGovernor @Inject constructor(
    private val context: Context,
    private val unifiedMemoryManager: UnifiedMemoryManager,
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val handler = Handler(Looper.getMainLooper())

    // Reactive state
    private val _cachePolicy = MutableStateFlow(CachePolicy())
    val cachePolicy: StateFlow<CachePolicy> = _cachePolicy.asStateFlow()

    private val _prefetchPolicy = MutableStateFlow(PrefetchPolicy())
    val prefetchPolicy: StateFlow<PrefetchPolicy> = _prefetchPolicy.asStateFlow()

    private val _deviceProfile = MutableStateFlow(DeviceProfile.UNKNOWN)
    val deviceProfile: StateFlow<DeviceProfile> = _deviceProfile.asStateFlow()

    private val _memoryPressureLevel = MutableStateFlow(MemoryPressureLevel.NOMINAL)
    val memoryPressureLevel: StateFlow<MemoryPressureLevel> = _memoryPressureLevel.asStateFlow()

    // Polling interval: 15s on low-RAM, 10s on normal
    private val pollingIntervalMs = if (LowRamDevice.isLowRamDevice) 15_000L else 10_000L

    private var monitoringJob: kotlinx.coroutines.Job? = null

    init {
        detectDeviceProfile()
        startMonitoring()
        Timber.d("AdaptiveMemoryGovernor initialized: profile=${_deviceProfile.value}, lowRam=${LowRamDevice.isLowRamDevice}")
    }

    /** Detect device profile from hardware characteristics */
    private fun detectDeviceProfile() {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalMemMb = (memInfo.totalMem / (1024 * 1024)).toInt()
        val isLowRam = activityManager.isLowRamDevice
        val isTv = context.packageManager.hasSystemFeature("android.software.leanback")
        val isFireTV = Build.MANUFACTURER.equals("Amazon", ignoreCase = true) ||
            Build.MODEL.contains("AFT", ignoreCase = true) ||
            Build.MODEL.contains("Fire TV", ignoreCase = true)

        _deviceProfile.value = when {
            isFireTV && totalMemMb <= 2048 -> DeviceProfile.FIRE_TV_STICK_HD
            isFireTV && totalMemMb <= 4096 -> DeviceProfile.FIRE_TV_STICK_4K
            isFireTV -> DeviceProfile.FIRE_TV_CUBE
            isTv && totalMemMb <= 2048 -> DeviceProfile.ANDROID_TV_LOW
            isTv && totalMemMb <= 4096 -> DeviceProfile.ANDROID_TV_MID
            isTv -> DeviceProfile.ANDROID_TV_HIGH
            isLowRam -> DeviceProfile.MOBILE_LOW_RAM
            totalMemMb <= 3072 -> DeviceProfile.MOBILE_LOW_RAM
            totalMemMb <= 6144 -> DeviceProfile.MOBILE_MID
            else -> DeviceProfile.MOBILE_HIGH
        }
    }

    private fun startMonitoring() {
        monitoringJob = scope.launch {
            while (true) {
                unifiedMemoryManager.updateMemoryState()
                evaluatePressureLevel()
                updatePolicies()
                kotlinx.coroutines.delay(pollingIntervalMs)
            }
        }
    }

    private fun evaluatePressureLevel() {
        val state = unifiedMemoryManager.memoryState.value
        val pressure = if (state.totalPssMb > 0) {
            state.totalPssMb / 125f
        } else 0f

        _memoryPressureLevel.value = when {
            state.isCritical || pressure > 0.95f -> MemoryPressureLevel.CRITICAL
            pressure > 0.85f -> MemoryPressureLevel.HIGH
            pressure > 0.7f -> MemoryPressureLevel.ELEVATED
            pressure > 0.5f -> MemoryPressureLevel.MODERATE
            else -> MemoryPressureLevel.NOMINAL
        }
    }

    private fun updatePolicies() {
        val profile = _deviceProfile.value
        val pressure = _memoryPressureLevel.value
        val memState = unifiedMemoryManager.memoryState.value

        // Compute cache policy based on memory pressure + device profile
        val newCachePolicy = when (pressure) {
            MemoryPressureLevel.CRITICAL -> CachePolicy(
                imageCacheSizeMb = (profile.baseImageCacheMb * 0.2).toInt().coerceAtLeast(2),
                artworkCacheSizeMb = (profile.baseArtworkCacheMb * 0.2).toInt().coerceAtLeast(1),
                metadataCacheSizeMb = (profile.baseMetadataCacheMb * 0.3).toInt().coerceAtLeast(2),
                enableImageCompression = true,
                imageQuality = 50,
                evictNonEssential = true,
                maxBitmapSize = 1024,
            )
            MemoryPressureLevel.HIGH -> CachePolicy(
                imageCacheSizeMb = (profile.baseImageCacheMb * 0.4).toInt().coerceAtLeast(4),
                artworkCacheSizeMb = (profile.baseArtworkCacheMb * 0.4).toInt().coerceAtLeast(2),
                metadataCacheSizeMb = (profile.baseMetadataCacheMb * 0.5).toInt().coerceAtLeast(4),
                enableImageCompression = true,
                imageQuality = 65,
                evictNonEssential = true,
                maxBitmapSize = 1280,
            )
            MemoryPressureLevel.ELEVATED -> CachePolicy(
                imageCacheSizeMb = (profile.baseImageCacheMb * 0.65).toInt().coerceAtLeast(8),
                artworkCacheSizeMb = (profile.baseArtworkCacheMb * 0.65).toInt().coerceAtLeast(4),
                metadataCacheSizeMb = (profile.baseMetadataCacheMb * 0.75).toInt().coerceAtLeast(8),
                enableImageCompression = true,
                imageQuality = 75,
                evictNonEssential = false,
                maxBitmapSize = 1920,
            )
            MemoryPressureLevel.MODERATE -> CachePolicy(
                imageCacheSizeMb = (profile.baseImageCacheMb * 0.85).toInt(),
                artworkCacheSizeMb = (profile.baseArtworkCacheMb * 0.85).toInt(),
                metadataCacheSizeMb = profile.baseMetadataCacheMb,
                enableImageCompression = false,
                imageQuality = 85,
                evictNonEssential = false,
                maxBitmapSize = 2560,
            )
            MemoryPressureLevel.NOMINAL -> CachePolicy(
                imageCacheSizeMb = profile.baseImageCacheMb,
                artworkCacheSizeMb = profile.baseArtworkCacheMb,
                metadataCacheSizeMb = profile.baseMetadataCacheMb,
                enableImageCompression = false,
                imageQuality = 90,
                evictNonEssential = false,
                maxBitmapSize = 3840,
            )
        }

        // Compute prefetch policy
        val newPrefetchPolicy = when (pressure) {
            MemoryPressureLevel.CRITICAL -> PrefetchPolicy(
                enabled = false,
                maxConcurrentRequests = 1,
                prefetchDepth = 0,
                chunkSizeKb = 64,
            )
            MemoryPressureLevel.HIGH -> PrefetchPolicy(
                enabled = true,
                maxConcurrentRequests = 2,
                prefetchDepth = 1,
                chunkSizeKb = 128,
            )
            MemoryPressureLevel.ELEVATED -> PrefetchPolicy(
                enabled = true,
                maxConcurrentRequests = 3,
                prefetchDepth = 2,
                chunkSizeKb = 256,
            )
            MemoryPressureLevel.MODERATE -> PrefetchPolicy(
                enabled = true,
                maxConcurrentRequests = (profile.maxConcurrentRequests * 0.75).toInt().coerceAtLeast(2),
                prefetchDepth = (profile.basePrefetchDepth * 0.75).toInt().coerceAtLeast(1),
                chunkSizeKb = 512,
            )
            MemoryPressureLevel.NOMINAL -> PrefetchPolicy(
                enabled = true,
                maxConcurrentRequests = profile.maxConcurrentRequests,
                prefetchDepth = profile.basePrefetchDepth,
                chunkSizeKb = 1024,
            )
        }

        if (newCachePolicy != _cachePolicy.value) {
            _cachePolicy.value = newCachePolicy
            Timber.d("Cache policy updated: images=${newCachePolicy.imageCacheSizeMb}MB, quality=${newCachePolicy.imageQuality}, compress=${newCachePolicy.enableImageCompression}")
        }

        if (newPrefetchPolicy != _prefetchPolicy.value) {
            _prefetchPolicy.value = newPrefetchPolicy
            Timber.d("Prefetch policy updated: enabled=${newPrefetchPolicy.enabled}, concurrent=${newPrefetchPolicy.maxConcurrentRequests}, depth=${newPrefetchPolicy.prefetchDepth}")
        }
    }

    /** Returns the recommended buffer size in bytes for playback based on current pressure */
    fun getRecommendedBufferBytes(): Int {
        return when (_memoryPressureLevel.value) {
            MemoryPressureLevel.CRITICAL -> 512 * 1024          // 512KB
            MemoryPressureLevel.HIGH -> 1 * 1024 * 1024        // 1MB
            MemoryPressureLevel.ELEVATED -> 2 * 1024 * 1024    // 2MB
            MemoryPressureLevel.MODERATE -> 4 * 1024 * 1024    // 4MB
            MemoryPressureLevel.NOMINAL -> 8 * 1024 * 1024     // 8MB
        }
    }

    /** Returns the recommended thread pool size for image decoding */
    fun getRecommendedImageDecodeThreads(): Int {
        val profile = _deviceProfile.value
        return when (_memoryPressureLevel.value) {
            MemoryPressureLevel.CRITICAL -> 1
            MemoryPressureLevel.HIGH -> 1
            MemoryPressureLevel.ELEVATED -> 2
            MemoryPressureLevel.MODERATE -> (profile.maxConcurrentRequests / 2).coerceIn(1, 3)
            MemoryPressureLevel.NOMINAL -> profile.maxConcurrentRequests.coerceIn(2, 4)
        }
    }

    /** Returns true if high-quality image rendering should be disabled */
    fun shouldDisableHighQualityImages(): Boolean {
        return _memoryPressureLevel.value >= MemoryPressureLevel.ELEVATED
    }

    /** Returns the recommended animation FPS */
    fun getRecommendedAnimationFps(): Int {
        return when (_memoryPressureLevel.value) {
            MemoryPressureLevel.CRITICAL -> 24
            MemoryPressureLevel.HIGH -> 30
            MemoryPressureLevel.ELEVATED -> 45
            else -> 60
        }
    }

    /** Returns true if non-essential background tasks should be paused */
    fun shouldPauseBackgroundTasks(): Boolean {
        return _memoryPressureLevel.value >= MemoryPressureLevel.HIGH
    }

    /**
     * Updates memory pressure from external source (e.g., NativeMemoryTracker).
     * Combines with internal pressure for more accurate assessment.
     */
    fun updateExternalPressure(pressure: Float) {
        _memoryPressureLevel.value = when {
            pressure > 0.95f -> MemoryPressureLevel.CRITICAL
            pressure > 0.85f -> MemoryPressureLevel.HIGH
            pressure > 0.7f -> MemoryPressureLevel.ELEVATED
            pressure > 0.5f -> MemoryPressureLevel.MODERATE
            else -> MemoryPressureLevel.NOMINAL
        }
        updatePolicies()
    }

    fun shutdown() {
        monitoringJob?.cancel()
        handler.removeCallbacksAndMessages(null)
    }
}

/** Memory pressure levels for policy decisions */
enum class MemoryPressureLevel {
    NOMINAL,    // < 50% — full operation
    MODERATE,   // 50-70% — slight reduction
    ELEVATED,   // 70-85% — moderate reduction
    HIGH,       // 85-95% — aggressive reduction
    CRITICAL,   // > 95% — minimal operation
}

/** Cache sizing policy */
data class CachePolicy(
    val imageCacheSizeMb: Int = 40,
    val artworkCacheSizeMb: Int = 20,
    val metadataCacheSizeMb: Int = 10,
    val enableImageCompression: Boolean = false,
    val imageQuality: Int = 90,
    val evictNonEssential: Boolean = false,
    val maxBitmapSize: Int = 2560,
)

/** Prefetch behavior policy */
data class PrefetchPolicy(
    val enabled: Boolean = true,
    val maxConcurrentRequests: Int = 3,
    val prefetchDepth: Int = 2,
    val chunkSizeKb: Int = 512,
)

/** Device profile categories */
enum class DeviceProfile(
    val baseImageCacheMb: Int,
    val baseArtworkCacheMb: Int,
    val baseMetadataCacheMb: Int,
    val basePrefetchDepth: Int,
    val maxConcurrentRequests: Int,
) {
    FIRE_TV_STICK_HD(40, 20, 10, 1, 3),
    FIRE_TV_STICK_4K(80, 40, 20, 2, 4),
    FIRE_TV_CUBE(150, 80, 40, 3, 6),
    ANDROID_TV_LOW(40, 20, 10, 1, 3),
    ANDROID_TV_MID(80, 40, 20, 2, 4),
    ANDROID_TV_HIGH(150, 80, 40, 3, 6),
    MOBILE_LOW_RAM(30, 15, 5, 1, 2),
    MOBILE_MID(80, 40, 15, 2, 4),
    MOBILE_HIGH(200, 100, 30, 3, 6),
    UNKNOWN(60, 30, 15, 2, 4),
}