/*
 * PlatformTuner — Android-side adapter that takes the shared
 * `PlatformProfile` and configures:
 *   - KuroVision engine quality mode & upscale algorithm
 *   - Media3 ExoPlayer buffer sizes, codec priorities
 *   - Memory optimizer for the playback frame cache
 *   - Dolby Atmos / DTS audio track selection
 *
 * Each platform produces a profile via `PlatformProfiles`; this tuner
 * is the only Android-specific glue between the shared domain and the
 * device hardware.
 */
package com.kurostream.playback.kurovision

import com.kurostream.domain.platform.AtmosTranscodeStrategy
import com.kurostream.domain.platform.PlatformKind
import com.kurostream.domain.platform.PlatformProfile
import kotlinx.coroutines.runBlocking

object PlatformTuner {

    fun tuneKuroVision(profile: PlatformProfile, settings: KuroVisionSettings) {
        runBlocking {
            settings.setQualityMode(mapQualityMode(profile.defaultQualityMode))
            settings.setUpscaleAlgorithm(mapUpscaleAlgorithm(profile.defaultUpscaleAlgorithm))
            settings.setFrameInterpolation(profile.kind in setOf(
                PlatformKind.ANDROID_TV,
                PlatformKind.FIRE_TV,
                PlatformKind.LINUX_DESKTOP,
                PlatformKind.WINDOWS_DESKTOP,
                PlatformKind.MACOS_DESKTOP,
            ))
            settings.setDolbyPassthrough(profile.supportsDolbyAtmosPassthrough)
        }
    }

    fun exoBufferSettings(profile: PlatformProfile): ExoBufferSettings {
        val minBufferMs = profile.initialBufferSeconds * 1000
        val maxBufferMs = when (profile.kind) {
            PlatformKind.WEBOS_TV,
            PlatformKind.TIZEN_TV -> minBufferMs * 3
            PlatformKind.ANDROID_PHONE,
            PlatformKind.ANDROID_TABLET -> minBufferMs * 5
            else -> minBufferMs * 8
        }
        val bufferForPlaybackMs = minBufferMs
        val bufferForPlaybackAfterRebufferMs = when (profile.kind) {
            PlatformKind.WEBOS_TV,
            PlatformKind.TIZEN_TV -> minBufferMs
            else -> minBufferMs * 2
        }
        return ExoBufferSettings(
            minBufferMs = minBufferMs,
            maxBufferMs = maxBufferMs,
            bufferForPlaybackMs = bufferForPlaybackMs,
            bufferForPlaybackAfterRebufferMs = bufferForPlaybackAfterRebufferMs,
        )
    }

    fun audioStrategy(profile: PlatformProfile): AudioStrategy {
        return when (profile.dolbyAtmosTranscode) {
            AtmosTranscodeStrategy.PASSTHROUGH -> AudioStrategy.PASSTHROUGH
            AtmosTranscodeStrategy.TRANSCODE_TO_EAC3 -> AudioStrategy.DOWNMIX_EAC3
            AtmosTranscodeStrategy.TRANSCODE_TO_STEREO -> AudioStrategy.DOWNMIX_STEREO
            AtmosTranscodeStrategy.NATIVE_FALLBACK -> AudioStrategy.NATIVE_FALLBACK
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
