package com.kurostream.common.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdaptiveMemoryGovernor @Inject constructor(
    private val context: Context,
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val memoryInfo = ActivityManager.MemoryInfo()

    enum class Tier { LOW_END, MID_RANGE, HIGH_END, FLAGSHIP }

    data class MemoryProfile(
        val tier: Tier,
        val totalRamMb: Long,
        val availableRamMb: Long,
        val isLowRamDevice: Boolean,
        val maxImageCacheMb: Int,
        val maxPrefetchDepth: Int,
        val maxConcurrentRequests: Int,
        val imageDecodeMaxPixels: Int,
        val useHardwareLayer: Boolean,
    )

    val profile: MemoryProfile
        get() {
            activityManager.getMemoryInfo(memoryInfo)
            val totalRamMb = if (Build.VERSION.SDK_INT >= 16) {
                memoryInfo.totalMem / (1024 * 1024)
            } else 2048L
            val availableRamMb = memoryInfo.availMem / (1024 * 1024)
            val isLowRamDevice = activityManager.isLowRamDevice

            val tier = when {
                isLowRamDevice || totalRamMb <= 1024 -> Tier.LOW_END
                totalRamMb <= 2048 -> Tier.MID_RANGE
                totalRamMb <= 4096 -> Tier.HIGH_END
                else -> Tier.FLAGSHIP
            }

            return MemoryProfile(
                tier = tier,
                totalRamMb = totalRamMb,
                availableRamMb = availableRamMb,
                isLowRamDevice = isLowRamDevice,
                maxImageCacheMb = when (tier) {
                    Tier.LOW_END -> 16
                    Tier.MID_RANGE -> 32
                    Tier.HIGH_END -> 64
                    Tier.FLAGSHIP -> 128
                },
                maxPrefetchDepth = when (tier) {
                    Tier.LOW_END -> 1
                    Tier.MID_RANGE -> 3
                    Tier.HIGH_END -> 5
                    Tier.FLAGSHIP -> 10
                },
                maxConcurrentRequests = when (tier) {
                    Tier.LOW_END -> 2
                    Tier.MID_RANGE -> 4
                    Tier.HIGH_END -> 6
                    Tier.FLAGSHIP -> 12
                },
                imageDecodeMaxPixels = when (tier) {
                    Tier.LOW_END -> 512 * 512
                    Tier.MID_RANGE -> 1024 * 1024
                    Tier.HIGH_END -> 2048 * 2048
                    Tier.FLAGSHIP -> 4096 * 4096
                },
                useHardwareLayer = tier != Tier.LOW_END,
            )
        }

    val availableMemoryFraction: Float
        get() {
            activityManager.getMemoryInfo(memoryInfo)
            return if (memoryInfo.totalMem > 0) {
                memoryInfo.availMem.toFloat() / memoryInfo.totalMem.toFloat()
            } else 0.5f
        }

    fun shouldThrottleBackgroundWork(): Boolean {
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem < memoryInfo.totalMem * 0.15 ||
            memoryInfo.lowMemory ||
            activityManager.isLowRamDevice
    }
}
