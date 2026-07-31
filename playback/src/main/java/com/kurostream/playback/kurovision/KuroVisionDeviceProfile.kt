// This file is part of KuroStream.
//
// KuroVisionDeviceProfile — Device-adaptive engine.
// Detects hardware capabilities and categorizes the device so the enhancement
// pipeline can pick safe quality modes, memory budgets, and renderer targets.
//
// One shared core, platform-agnostic. Android-specific detection lives in
// AndroidDeviceInspector so webOS/Tizen/Linux can plug in their own detector
// later without changing this file.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.playback.kurovision

/**
 * Coarse device class used by KuroVision to choose a quality mode and memory
 * budget. Keep this enum stable — external rules and persisted settings key
 * off these names.
 */
enum class DeviceClass {
    /** Fire TV Stick HD / older Android TV boxes. */
    LOW_POWER_TV,

    /** Mid-range Android TV / webOS 2020 era. */
    MID_TV,

    /** Flagship Android TV, LG OLED 2022+, Shield TV. */
    HIGH_END_TV,

    /** 1080p / entry-level Android phones. */
    MOBILE_LOW,

    /** Snapdragon 8-series / A15-class phones and tablets. */
    MOBILE_HIGH,

    /** Low-end laptops / integrated graphics. */
    DESKTOP_LOW,

    /** Discrete GPU / recent iGPU with Vulkan support. */
    DESKTOP_HIGH,

    /** Fallback when nothing is known yet. */
    UNKNOWN,
}

/**
 * GPU vendor reported through [android.opengl.GLES20.glGetString] or the
 * Android Build.MODEL heuristic. Used only to pick optimization hints.
 */
enum class GpuVendor { UNKNOWN, QUALCOMM, MALI, POWERVR, NVIDIA, AMD, INTEL, BROADCOM }

/**
 * Decoder capability snapshot used by KuroVision to decide if hardware
 * decode is safe for a given codec/resolution. Mirrors the values probed
 * by [com.kurostream.common.memory.CodecCapabilityDetector].
 */
data class DecoderCapability(
    val supportsHevc: Boolean,
    val supportsAv1: Boolean,
    val supportsVp9: Boolean,
    val maxSecureResolution: String,
    val isHardwareBacked: Boolean,
)

/**
 * Full device profile consumed by the KuroVision pipeline. Created once per
 * process, cached in [KuroVisionEngine].
 */
data class KuroVisionDeviceProfile(
    val deviceClass: DeviceClass,
    val cpuCores: Int,
    val gpuVendor: GpuVendor,
    val gpuModel: String,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val gpuMemoryMb: Int,
    val displayWidth: Int,
    val displayHeight: Int,
    val refreshRateHz: Float,
    val supportsVulkan: Boolean,
    val supportsOpenGlEs3: Boolean,
    val supportsHardwareDecoder: Boolean,
    val decoder: DecoderCapability,
    val modelLabel: String,
) {
    /**
     * Memory budget in MB for the enhancement pipeline. Tuned per device
     * class — TVs cap at 125MB, mobile gets 250MB, desktop is unbounded.
     */
    val memoryBudgetMb: Int
        get() = when (deviceClass) {
            DeviceClass.LOW_POWER_TV -> 90
            DeviceClass.MID_TV -> 125
            DeviceClass.HIGH_END_TV -> 180
            DeviceClass.MOBILE_LOW -> 120
            DeviceClass.MOBILE_HIGH -> 250
            DeviceClass.DESKTOP_LOW -> 350
            DeviceClass.DESKTOP_HIGH -> 600
            DeviceClass.UNKNOWN -> 150
        }

    /** Decoder surface budget in MB. */
    val decoderBudgetMb: Int
        get() = when (deviceClass) {
            DeviceClass.LOW_POWER_TV -> 32
            DeviceClass.MID_TV -> 40
            DeviceClass.HIGH_END_TV -> 56
            DeviceClass.MOBILE_LOW -> 36
            DeviceClass.MOBILE_HIGH -> 64
            DeviceClass.DESKTOP_LOW -> 64
            DeviceClass.DESKTOP_HIGH -> 96
            DeviceClass.UNKNOWN -> 40
        }

    /** GPU texture budget in MB (input + output textures, scratch buffers). */
    val gpuTextureBudgetMb: Int
        get() = when (deviceClass) {
            DeviceClass.LOW_POWER_TV -> 28
            DeviceClass.MID_TV -> 40
            DeviceClass.HIGH_END_TV -> 64
            DeviceClass.MOBILE_LOW -> 32
            DeviceClass.MOBILE_HIGH -> 80
            DeviceClass.DESKTOP_LOW -> 96
            DeviceClass.DESKTOP_HIGH -> 192
            DeviceClass.UNKNOWN -> 40
        }

    /** Whether the device should engage multi-pass shaders. */
    val supportsMultiPassShaders: Boolean
        get() = when (deviceClass) {
            DeviceClass.LOW_POWER_TV, DeviceClass.MOBILE_LOW -> false
            else -> true
        }

    /** Whether the device can sustain 4K upscale without dropping frames. */
    val supports4KUpscale: Boolean
        get() = when (deviceClass) {
            DeviceClass.LOW_POWER_TV, DeviceClass.MOBILE_LOW -> false
            else -> true
        }

    /** Whether 8K upscale is worth trying (desktop only). */
    val supports8KUpscale: Boolean
        get() = deviceClass == DeviceClass.DESKTOP_HIGH

    /**
     * The recommended default [KuroVisionQualityMode]. Users can still
     * override this via settings; this is just the safe starting point.
     */
    val recommendedQualityMode: KuroVisionQualityMode
        get() = when (deviceClass) {
            DeviceClass.LOW_POWER_TV -> KuroVisionQualityMode.HARDWARE
            DeviceClass.MID_TV -> KuroVisionQualityMode.CINEMA
            DeviceClass.HIGH_END_TV -> KuroVisionQualityMode.HDR_VISION
            DeviceClass.MOBILE_LOW -> KuroVisionQualityMode.CINEMA
            DeviceClass.MOBILE_HIGH -> KuroVisionQualityMode.ANIME_PRO
            DeviceClass.DESKTOP_LOW -> KuroVisionQualityMode.HDR_VISION
            DeviceClass.DESKTOP_HIGH -> KuroVisionQualityMode.ULTRA_DESKTOP
            DeviceClass.UNKNOWN -> KuroVisionQualityMode.CINEMA
        }

    /** Recommended upscale algorithm for this device. */
    val recommendedUpscaleAlgorithm: UpscaleAlgorithm
        get() = when (deviceClass) {
            DeviceClass.LOW_POWER_TV -> UpscaleAlgorithm.BILINEAR
            DeviceClass.MID_TV -> UpscaleAlgorithm.BICUBIC
            DeviceClass.HIGH_END_TV, DeviceClass.MOBILE_HIGH -> UpscaleAlgorithm.LANCZOS3
            DeviceClass.DESKTOP_LOW, DeviceClass.DESKTOP_HIGH -> UpscaleAlgorithm.LANCZOS3
            else -> UpscaleAlgorithm.BICUBIC
        }

    companion object {
        /** Returns a safe fallback profile used until detection completes. */
        fun unknown(cpuCores: Int = 4, ramMb: Long = 2048): KuroVisionDeviceProfile =
            KuroVisionDeviceProfile(
                deviceClass = DeviceClass.UNKNOWN,
                cpuCores = cpuCores,
                gpuVendor = GpuVendor.UNKNOWN,
                gpuModel = "Unknown",
                totalRamMb = ramMb,
                availableRamMb = ramMb / 2,
                gpuMemoryMb = 64,
                displayWidth = 1920,
                displayHeight = 1080,
                refreshRateHz = 60f,
                supportsVulkan = false,
                supportsOpenGlEs3 = true,
                supportsHardwareDecoder = true,
                decoder = DecoderCapability(
                    supportsHevc = true,
                    supportsAv1 = false,
                    supportsVp9 = true,
                    maxSecureResolution = "4K",
                    isHardwareBacked = true,
                ),
                modelLabel = "Unknown",
            )
    }
}
