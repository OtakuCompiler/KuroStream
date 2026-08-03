// This file is part of KuroStream.
//
// AndroidDeviceInspector — Android-specific device detection for KuroVision.
// Probes CPU, GPU, RAM, display, decoder caps, and Vulkan/GL support.
// Returns a fully-populated [KuroVisionDeviceProfile] used by the pipeline.
//
// One shared core, platform-specific detection here. webOS/Tizen/Linux
// would provide their own implementations of this interface.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.playback.kurovision

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLES20
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.kurostream.common.memory.CodecCapabilityDetector
import com.kurostream.common.memory.LowRamDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of device inspection. Inject via Hilt and call
 * [inspect] once during app startup to cache the profile.
 */
@Singleton
class AndroidDeviceInspector @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Synchronously inspects the device and returns a complete profile.
     * Called once per process; result is cached in [KuroVisionEngine].
     */
    @SuppressLint("WrongThread")
    fun inspect(): KuroVisionDeviceProfile {
        // 1) CPU cores
        val cpuCores = Runtime.getRuntime().availableProcessors()

        // 2) RAM
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        val totalRamMb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            memInfo.totalMem / (1024 * 1024)
        } else {
            Runtime.getRuntime().maxMemory() / (1024 * 1024)
        }
        val availableRamMb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            memInfo.availMem / (1024 * 1024)
        } else {
            totalRamMb / 2
        }

        // 3) Display
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(dm)
        val displayWidth = dm.widthPixels
        val displayHeight = dm.heightPixels
        val refreshRateHz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            wm.defaultDisplay.refreshRate
        } else {
            dm.density * 60f // fallback approximation
        }

        // 4) GPU vendor + model via GLES
        var gpuVendor = GpuVendor.UNKNOWN
        var gpuModel = "Unknown"
        try {
            val renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: ""
            val vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: ""
            gpuModel = renderer
            gpuVendor = when {
                vendor.contains("Qualcomm", true) || renderer.contains("Adreno", true) -> GpuVendor.QUALCOMM
                vendor.contains("ARM", true) || renderer.contains("Mali", true) -> GpuVendor.MALI
                vendor.contains("Imagination", true) || renderer.contains("PowerVR", true) -> GpuVendor.POWERVR
                vendor.contains("NVIDIA", true) -> GpuVendor.NVIDIA
                vendor.contains("AMD", true) -> GpuVendor.AMD
                vendor.contains("Intel", true) -> GpuVendor.INTEL
                vendor.contains("Broadcom", true) -> GpuVendor.BROADCOM
                else -> GpuVendor.UNKNOWN
            }
        } catch (e: Exception) {
            // GLES not available yet; fall back to heuristics
        }

        // 5) Vulkan support
        val supportsVulkan = checkVulkanSupport()

        // 6) OpenGL ES 3.0+
        val glEsVersion = (context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager)
            ?.deviceConfigurationInfo?.reqGlEsVersion ?: 0
        val supportsOpenGlEs3 = glEsVersion >= 0x30000

        // 7) Decoder capabilities
        CodecCapabilityDetector.detect()
        val decoder = DecoderCapability(
            supportsHevc = CodecCapabilityDetector.supportsHevc,
            supportsAv1 = CodecCapabilityDetector.supportsAv1,
            supportsVp9 = true,
            maxSecureResolution = "4K",
            isHardwareBacked = CodecCapabilityDetector.hasHardwareDecoder,
        )

        // 8) Device class heuristic
        val deviceClass = determineDeviceClass(
            totalRamMb = totalRamMb,
            cpuCores = cpuCores,
            gpuVendor = gpuVendor,
            displayWidth = displayWidth,
            displayHeight = displayHeight,
            isTv = isTvDevice(),
            buildModel = Build.MODEL,
        )

        // 9) GPU memory estimate (VRAM not directly exposed; heuristic by class + vendor)
        val gpuMemoryMb = estimateGpuMemoryMb(deviceClass, gpuVendor, totalRamMb)

        return KuroVisionDeviceProfile(
            deviceClass = deviceClass,
            cpuCores = cpuCores,
            gpuVendor = gpuVendor,
            gpuModel = gpuModel,
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            gpuMemoryMb = gpuMemoryMb,
            displayWidth = displayWidth,
            displayHeight = displayHeight,
            refreshRateHz = refreshRateHz,
            supportsVulkan = supportsVulkan,
            supportsOpenGlEs3 = supportsOpenGlEs3,
            supportsHardwareDecoder = decoder.isHardwareBacked,
            decoder = decoder,
            modelLabel = "${Build.MANUFACTURER} ${Build.MODEL}",
        )
    }

    private fun checkVulkanSupport(): Boolean {
        return try {
            val pm = context.packageManager
            // Check for Vulkan feature and at least one device
            pm.hasSystemFeature("android.hardware.vulkan.level") &&
            pm.hasSystemFeature("android.hardware.vulkan.version") &&
            VulkanInfo.hasVulkanDevice()
        } catch (e: Exception) {
            false
        }
    }

    private fun isTvDevice(): Boolean {
        val uiMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK
        return uiMode == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    }

    private fun determineDeviceClass(
        totalRamMb: Long,
        cpuCores: Int,
        gpuVendor: GpuVendor,
        displayWidth: Int,
        displayHeight: Int,
        isTv: Boolean,
        buildModel: String,
    ): DeviceClass {
        val modelLower = buildModel.lowercase()
        val isFireStick = modelLower.contains("fire") || modelLower.contains("afts")
        val isShield = modelLower.contains("shield")

        // High-end TV
        if (isTv && (totalRamMb >= 3072 || isShield)) return DeviceClass.HIGH_END_TV

        // Fire TV Stick HD (1GB) / low-end TV boxes
        if (isTv && (isFireStick || totalRamMb <= 1536)) return DeviceClass.LOW_POWER_TV

        // Mid-range TV
        if (isTv) return DeviceClass.MID_TV

        // Mobile classification
        if (!isTv) {
            val isHighEndMobile = cpuCores >= 8 && totalRamMb >= 6144 &&
                (gpuVendor == GpuVendor.QUALCOMM || gpuVendor == GpuVendor.MALI)
            val isLowEndMobile = cpuCores <= 4 && totalRamMb <= 3072
            return if (isHighEndMobile) DeviceClass.MOBILE_HIGH
            else if (isLowEndMobile) DeviceClass.MOBILE_LOW
            else DeviceClass.MOBILE_HIGH
        }

        // Desktop fallback (unlikely on Android but for completeness)
        return if (totalRamMb >= 8192) DeviceClass.DESKTOP_HIGH else DeviceClass.DESKTOP_LOW
    }

    private fun estimateGpuMemoryMb(
        deviceClass: DeviceClass,
        gpuVendor: GpuVendor,
        totalRamMb: Long,
    ): Int {
        return when (deviceClass) {
            DeviceClass.LOW_POWER_TV -> 64
            DeviceClass.MID_TV -> 128
            DeviceClass.HIGH_END_TV -> 256
            DeviceClass.MOBILE_LOW -> 128
            DeviceClass.MOBILE_HIGH -> {
                when (gpuVendor) {
                    GpuVendor.QUALCOMM, GpuVendor.MALI -> 256
                    else -> 128
                }
            }
            DeviceClass.DESKTOP_LOW -> 512
            DeviceClass.DESKTOP_HIGH -> 1024
            else -> 128
        }
    }
}

object VulkanInfo {
    @SuppressLint("WrongThread")
    fun hasVulkanDevice(): Boolean {
        // Placeholder: actual check would use Vulkan API via NDK or vkEnumeratePhysicalDevices
        // For now, assume if the features are present, a device exists.
        return true
    }
}
