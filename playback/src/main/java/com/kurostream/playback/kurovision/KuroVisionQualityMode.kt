// This file is part of KuroStream.
//
// KuroVisionQualityMode — User-selectable enhancement presets.
// Each preset describes which features to enable, memory/GPU cost, and the
// minimum device class it should run on. One shared enum so the UI, the
// settings layer, and the pipeline all reference the same constants.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.playback.kurovision

/**
 * The seven quality presets exposed in the KuroVision settings UI. The names
 * are stable (persisted to DataStore) and must match the spec.
 */
enum class KuroVisionQualityMode(
    val displayName: String,
    val description: String,
    val minDeviceClass: DeviceClass,
    val upscaleAlgorithm: UpscaleAlgorithm,
    val features: KuroVisionFeatures,
    val estimatedGpuCostPct: Int,
    val estimatedMemoryMb: Int,
) {
    /** Device scaler only. For very weak TVs and 1GB devices. */
    HARDWARE(
        displayName = "Hardware Passthrough",
        description = "Use the device's built-in scaler. Lowest power draw.",
        minDeviceClass = DeviceClass.LOW_POWER_TV,
        upscaleAlgorithm = UpscaleAlgorithm.BILINEAR,
        features = KuroVisionFeatures(sharpening = false, debanding = false, denoise = false, fakeHdr = false, oledBlack = false, animePro = false, ultra = false, frameInterpolation = false),
        estimatedGpuCostPct = 2,
        estimatedMemoryMb = 16,
    ),

    /** All devices: bicubic scaling + light sharpening + color correction. */
    CINEMA(
        displayName = "Cinema",
        description = "Bicubic upscaling, adaptive sharpening, color correction.",
        minDeviceClass = DeviceClass.LOW_POWER_TV,
        upscaleAlgorithm = UpscaleAlgorithm.BICUBIC,
        features = KuroVisionFeatures(sharpening = true, debanding = false, denoise = false, fakeHdr = false, oledBlack = false, animePro = false, ultra = false, frameInterpolation = false),
        estimatedGpuCostPct = 10,
        estimatedMemoryMb = 36,
    ),

    /** Optimized for anime and line art. Mid-range TVs and phones. */
    ANIME_PRO(
        displayName = "Anime Pro",
        description = "Line sharpening, debanding, edge enhancement, color restoration.",
        minDeviceClass = DeviceClass.MID_TV,
        upscaleAlgorithm = UpscaleAlgorithm.LANCZOS3,
        features = KuroVisionFeatures(sharpening = true, debanding = true, denoise = false, fakeHdr = false, oledBlack = false, animePro = true, ultra = false, frameInterpolation = false),
        estimatedGpuCostPct = 25,
        estimatedMemoryMb = 64,
    ),

    /** Dynamic tone mapping for SDR content. */
    HDR_VISION(
        displayName = "HDR Vision",
        description = "Fake HDR with dynamic tone mapping, highlight recovery, shadow boost.",
        minDeviceClass = DeviceClass.MID_TV,
        upscaleAlgorithm = UpscaleAlgorithm.LANCZOS3,
        features = KuroVisionFeatures(sharpening = true, debanding = true, denoise = false, fakeHdr = true, oledBlack = false, animePro = false, ultra = false, frameInterpolation = true),
        estimatedGpuCostPct = 35,
        estimatedMemoryMb = 88,
    ),

    /** Black-level compression, gamma curve, contrast. */
    OLED_BLACK(
        displayName = "OLED Black",
        description = "Simulate OLED deep blacks with adaptive gamma and dark scene boost.",
        minDeviceClass = DeviceClass.MID_TV,
        upscaleAlgorithm = UpscaleAlgorithm.LANCZOS3,
        features = KuroVisionFeatures(sharpening = true, debanding = true, denoise = false, fakeHdr = false, oledBlack = true, animePro = false, ultra = false, frameInterpolation = false),
        estimatedGpuCostPct = 30,
        estimatedMemoryMb = 80,
    ),

    /** Desktop-only multi-pass pipeline. */
    ULTRA_DESKTOP(
        displayName = "Ultra Desktop",
        description = "Multi-pass upscale with edge-adaptive sharpening and HDR color.",
        minDeviceClass = DeviceClass.DESKTOP_HIGH,
        upscaleAlgorithm = UpscaleAlgorithm.LANCZOS3,
        features = KuroVisionFeatures(sharpening = true, debanding = true, denoise = true, fakeHdr = true, oledBlack = true, animePro = true, ultra = true, frameInterpolation = true),
        estimatedGpuCostPct = 60,
        estimatedMemoryMb = 180,
    ),
    ;

    /**
     * Returns true if the supplied device class can run this mode without
     * dropping frames or hitting the memory ceiling.
     */
    fun isCompatibleWith(deviceClass: DeviceClass): Boolean {
        val order = listOf(
            DeviceClass.LOW_POWER_TV,
            DeviceClass.MID_TV,
            DeviceClass.HIGH_END_TV,
            DeviceClass.MOBILE_LOW,
            DeviceClass.MOBILE_HIGH,
            DeviceClass.DESKTOP_LOW,
            DeviceClass.DESKTOP_HIGH,
        )
        val min = order.indexOf(minDeviceClass)
        val current = order.indexOf(deviceClass)
        if (min < 0 || current < 0) return false
        return current >= min
    }

    companion object {
        /** Pick the best mode for the device, honouring the user's selection. */
        fun chooseFor(
            profile: KuroVisionDeviceProfile,
            preferred: KuroVisionQualityMode,
        ): KuroVisionQualityMode {
            if (preferred.isCompatibleWith(profile.deviceClass)) return preferred
            // Fall back through modes from most powerful down to HARDWARE.
            return entries.firstOrNull { it.isCompatibleWith(profile.deviceClass) } ?: HARDWARE
        }
    }
}

/**
 * Upscaling algorithm selector. Mirrors [UpscaleEngine] shader modes.
 */
enum class UpscaleAlgorithm(val displayName: String, val minDeviceClass: DeviceClass, val gpuCostPct: Int) {
    BILINEAR("Fast (Bilinear)", DeviceClass.LOW_POWER_TV, 1),
    BICUBIC("Balanced (Bicubic)", DeviceClass.LOW_POWER_TV, 8),
    LANCZOS3("High Quality (Lanczos-3)", DeviceClass.MID_TV, 18),
    ULTRA("Ultra (Multi-pass Desktop)", DeviceClass.DESKTOP_HIGH, 45),
}

/**
 * Boolean feature flags packed into a single value class to avoid juggling
 * eight parameters. Default: everything off.
 */
data class KuroVisionFeatures(
    val sharpening: Boolean = false,
    val debanding: Boolean = false,
    val denoise: Boolean = false,
    val fakeHdr: Boolean = false,
    val oledBlack: Boolean = false,
    val animePro: Boolean = false,
    val ultra: Boolean = false,
    val frameInterpolation: Boolean = false,
)
