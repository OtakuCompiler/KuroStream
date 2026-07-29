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

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class UltraLowMemoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val memoryClass = activityManager.memoryClass
    private val largeMemoryClass = activityManager.largeMemoryClass
    private val isLowRamDevice = activityManager.isLowRamDevice
    
    private val availableRAM = getAvailableMemory()
    private val memoryThreshold = when {
        memoryClass < 256 -> 0.5
        memoryClass < 512 -> 0.6
        memoryClass < 1024 -> 0.7
        else -> 0.8
    }
    
    data class MemoryConfig(
        val p2pBufferSize: Int,
        val decoderBuffer: Int,
        val upscalerBuffer: Int,
        val audioBuffer: Int,
        val networkBuffer: Int,
        val uiBuffer: Int,
        val maxPeers: Int,
        val chunkSize: Int,
        val prebufferChunks: Int
    )
    
    fun getOptimizedConfig(quality: VideoQuality, hasUpscaling: Boolean, hasTranscoding: Boolean): MemoryConfig {
        val targetBudget = calculateTargetBudget()
        
        return when {
            quality == VideoQuality.UHD_4K && hasUpscaling && hasTranscoding -> {
                getUltraOptimized4KConfig(targetBudget)
            }
            quality == VideoQuality.UHD_4K -> {
                getOptimized4KConfig(targetBudget)
            }
            quality == VideoQuality.FHD_1080P && hasUpscaling -> {
                getOptimized1080pUpscaleConfig(targetBudget)
            }
            else -> {
                getStandardConfig(targetBudget, quality)
            }
        }
    }
    
    private fun calculateTargetBudget(): Int {
        val available = availableRAM * memoryThreshold
        return min(available.toInt(), largeMemoryClass * 1024 * 1024 / 2)
    }
    
    private fun getUltraOptimized4KConfig(budget: Int): MemoryConfig {
        val p2pBuffer = min(12 * 1024 * 1024, budget * 0.15)
        val decoderBuffer = min(18 * 1024 * 1024, budget * 0.20)
        val upscalerBuffer = min(25 * 1024 * 1024, budget * 0.28)
        val audioBuffer = min(8 * 1024 * 1024, budget * 0.09)
        val networkBuffer = min(4 * 1024 * 1024, budget * 0.05)
        val uiBuffer = min(6 * 1024 * 1024, budget * 0.07)
        
        return MemoryConfig(
            p2pBufferSize = p2pBuffer.toInt(),
            decoderBuffer = decoderBuffer.toInt(),
            upscalerBuffer = upscalerBuffer.toInt(),
            audioBuffer = audioBuffer.toInt(),
            networkBuffer = networkBuffer.toInt(),
            uiBuffer = uiBuffer.toInt(),
            maxPeers = 12,
            chunkSize = 2 * 1024 * 1024,
            prebufferChunks = 3
        )
    }
    
    private fun getOptimized4KConfig(budget: Int): MemoryConfig {
        val p2pBuffer = min(15 * 1024 * 1024, budget * 0.18)
        val decoderBuffer = min(20 * 1024 * 1024, budget * 0.22)
        val upscalerBuffer = 0
        val audioBuffer = min(6 * 1024 * 1024, budget * 0.07)
        val networkBuffer = min(5 * 1024 * 1024, budget * 0.06)
        val uiBuffer = min(8 * 1024 * 1024, budget * 0.09)
        
        return MemoryConfig(
            p2pBufferSize = p2pBuffer.toInt(),
            decoderBuffer = decoderBuffer.toInt(),
            upscalerBuffer = upscalerBuffer,
            audioBuffer = audioBuffer.toInt(),
            networkBuffer = networkBuffer.toInt(),
            uiBuffer = uiBuffer.toInt(),
            maxPeers = 15,
            chunkSize = 2 * 1024 * 1024,
            prebufferChunks = 4
        )
    }
    
    private fun getOptimized1080pUpscaleConfig(budget: Int): MemoryConfig {
        val p2pBuffer = min(8 * 1024 * 1024, budget * 0.12)
        val decoderBuffer = min(12 * 1024 * 1024, budget * 0.16)
        val upscalerBuffer = min(18 * 1024 * 1024, budget * 0.24)
        val audioBuffer = min(5 * 1024 * 1024, budget * 0.07)
        val networkBuffer = min(3 * 1024 * 1024, budget * 0.04)
        val uiBuffer = min(5 * 1024 * 1024, budget * 0.07)
        
        return MemoryConfig(
            p2pBufferSize = p2pBuffer.toInt(),
            decoderBuffer = decoderBuffer.toInt(),
            upscalerBuffer = upscalerBuffer.toInt(),
            audioBuffer = audioBuffer.toInt(),
            networkBuffer = networkBuffer.toInt(),
            uiBuffer = uiBuffer.toInt(),
            maxPeers = 10,
            chunkSize = 1 * 1024 * 1024,
            prebufferChunks = 5
        )
    }
    
    private fun getStandardConfig(budget: Int, quality: VideoQuality): MemoryConfig {
        val baseBuffer = when (quality) {
            VideoQuality.UHD_4K -> 15 * 1024 * 1024
            VideoQuality.FHD_1080P -> 8 * 1024 * 1024
            VideoQuality.HD_720P -> 4 * 1024 * 1024
            else -> 2 * 1024 * 1024
        }
        
        val p2pBuffer = min(baseBuffer, budget * 0.15)
        val decoderBuffer = min(baseBuffer * 1.2, budget * 0.18)
        val networkBuffer = min(baseBuffer * 0.4, budget * 0.05)
        val uiBuffer = min(6 * 1024 * 1024, budget * 0.08)
        
        return MemoryConfig(
            p2pBufferSize = p2pBuffer.toInt(),
            decoderBuffer = decoderBuffer.toInt(),
            upscalerBuffer = 0,
            audioBuffer = min(4 * 1024 * 1024, budget * 0.05),
            networkBuffer = networkBuffer.toInt(),
            uiBuffer = uiBuffer.toInt(),
            maxPeers = when (quality) {
                VideoQuality.UHD_4K -> 15
                VideoQuality.FHD_1080P -> 12
                else -> 8
            },
            chunkSize = when (quality) {
                VideoQuality.UHD_4K -> 2 * 1024 * 1024
                VideoQuality.FHD_1080P -> 1 * 1024 * 1024
                else -> 512 * 1024
            },
            prebufferChunks = 5
        )
    }
    
    private fun getAvailableMemory(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val info = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(info)
            info.availMem
        } else {
            @Suppress("DEPRECATION")
            activityManager.memoryInfo.let { info ->
                val memInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memInfo)
                memInfo.availMem
            }
        }
    }
    
    fun trimMemory(level: Int) {
        when (level) {
            ActivityManager.TRIM_MEMORY_RUNNING_MODERATE -> {
                // Reduce buffer by 20%
            }
            ActivityManager.TRIM_MEMORY_RUNNING_LOW -> {
                // Reduce buffer by 40%
            }
            ActivityManager.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // Reduce buffer by 60%
            }
            ActivityManager.TRIM_MEMORY_UI_HIDDEN -> {
                // Reduce UI buffer
            }
            ActivityManager.TRIM_MEMORY_BACKGROUND -> {
                // Aggressive cleanup
            }
            ActivityManager.TRIM_MEMORY_COMPLETE -> {
                // Release everything possible
            }
        }
    }
}

enum class VideoQuality {
    LD_360P,
    SD_480P,
    HD_720P,
    FHD_1080P,
    UHD_4K
}