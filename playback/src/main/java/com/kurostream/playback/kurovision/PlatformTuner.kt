// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.playback.kurovision

import android.os.Handler
import android.os.Looper

data class ProcessedFrame(
    val texture: Int,
    val width: Int,
    val height: Int,
    val isPassthrough: Boolean,
) {
    companion object {
        fun passthrough(tex: Int, w: Int, h: Int) = ProcessedFrame(tex, w, h, true)
    }
}

object PlatformTuner {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun tuneKuroVision(profile: com.kurostream.domain.platform.PlatformProfile, settings: KuroVisionSettings) {
        val job = kotlinx.coroutines.runBlocking {
            settings.setQualityMode(mapQualityMode(profile.defaultQualityMode))
            settings.setUpscaleAlgorithm(mapUpscaleAlgorithm(profile.defaultUpscaleAlgorithm))
            settings.setFrameInterpolation(profile.kind in setOf(
                com.kurostream.domain.platform.PlatformKind.ANDROID_TV,
                com.kurostream.domain.platform.PlatformKind.FIRE_TV,
                com.kurostream.domain.platform.PlatformKind.LINUX_DESKTOP,
                com.kurostream.domain.platform.PlatformKind.WINDOWS_DESKTOP,
                com.kurostream.domain.platform.PlatformKind.MACOS_DESKTOP,
            ))
            settings.setDolbyPassthrough(profile.supportsDolbyAtmosPassthrough)
        }
    }

    fun exoBufferSettings(profile: com.kurostream.domain.platform.PlatformProfile): ExoBufferSettings {
        val minBufferMs = profile.initialBufferSeconds * 1000
        val maxBufferMs = when (profile.kind) {
            com.kurostream.domain.platform.PlatformKind.WEBOS_TV,
            com.kurostream.domain.platform.PlatformKind.TIZEN_TV -> minBufferMs * 3
            com.kurostream.domain.platform.PlatformKind.ANDROID_PHONE,
            com.kurostream.domain.platform.PlatformKind.ANDROID_TABLET -> minBufferMs * 5
            else -> minBufferMs * 8
        }
        val bufferForPlaybackMs = minBufferMs
        val bufferForPlaybackAfterRebufferMs = when (profile.kind) {
            com.kurostream.domain.platform.PlatformKind.WEBOS_TV,
            com.kurostream.domain.platform.PlatformKind.TIZEN_TV -> minBufferMs
            else -> minBufferMs * 2
        }
        return ExoBufferSettings(
            minBufferMs = minBufferMs,
            maxBufferMs = maxBufferMs,
            bufferForPlaybackMs = bufferForPlaybackMs,
            bufferForPlaybackAfterRebufferMs = bufferForPlaybackAfterRebufferMs,
        )
    }

    fun audioStrategy(profile: com.kurostream.domain.platform.PlatformProfile): AudioStrategy {
        return when (profile.dolbyAtmosTranscode) {
            com.kurostream.domain.platform.AtmosTranscodeStrategy.PASSTHROUGH -> AudioStrategy.PASSTHROUGH
            com.kurostream.domain.platform.AtmosTranscodeStrategy.TRANSCODE_TO_EAC3 -> AudioStrategy.DOWNMIX_EAC3
            com.kurostream.domain.platform.AtmosTranscodeStrategy.TRANSCODE_TO_STEREO -> AudioStrategy.DOWNMIX_STEREO
            com.kurostream.domain.platform.AtmosTranscodeStrategy.NATIVE_FALLBACK -> AudioStrategy.NATIVE_FALLBACK
        }
    }

    private fun mapQualityMode(m: com.kurostream.domain.platform.KuroVisionQualityMode): KuroVisionQualityMode {
        return when (m) {
            com.kurostream.domain.platform.KuroVisionQualityMode.HARDWARE_PASSTHROUGH -> KuroVisionQualityMode.HARDWARE
            com.kurostream.domain.platform.KuroVisionQualityMode.SD_TO_HD -> KuroVisionQualityMode.CINEMA
            com.kurostream.domain.platform.KuroVisionQualityMode.HD_TO_4K -> KuroVisionQualityMode.HDR_ULTRA
            com.kurostream.domain.platform.KuroVisionQualityMode.ANIME_4K -> KuroVisionQualityMode.ANIME_4K
            com.kurostream.domain.platform.KuroVisionQualityMode.AI_NEURAL -> KuroVisionQualityMode.ULTRA_DESKTOP
            com.kurostream.domain.platform.KuroVisionQualityMode.DESKTOP_FULL -> KuroVisionQualityMode.ULTRA_DESKTOP
        }
    }

    private fun mapUpscaleAlgorithm(a: com.kurostream.domain.platform.UpscaleAlgorithm): UpscaleAlgorithm {
        return when (a) {
            com.kurostream.domain.platform.UpscaleAlgorithm.NEAREST -> UpscaleAlgorithm.BILINEAR
            com.kurostream.domain.platform.UpscaleAlgorithm.BILINEAR -> UpscaleAlgorithm.BILINEAR
            com.kurostream.domain.platform.UpscaleAlgorithm.BICUBIC -> UpscaleAlgorithm.BICUBIC
            com.kurostream.domain.platform.UpscaleAlgorithm.LANCZOS3 -> UpscaleAlgorithm.LANCZOS3
            com.kurostream.domain.platform.UpscaleAlgorithm.WAIFU2X -> UpscaleAlgorithm.WAIFU2X
            com.kurostream.domain.platform.UpscaleAlgorithm.AI_REAL_ESRGAN -> UpscaleAlgorithm.ULTRA
        }
    }
}

data class ExoBufferSettings(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int,
)

enum class AudioStrategy { PASSTHROUGH, DOWNMIX_EAC3, DOWNMIX_STEREO, NATIVE_FALLBACK }
