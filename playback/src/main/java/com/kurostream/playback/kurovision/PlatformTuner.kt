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
 * device hardware. Keeping it thin means the same logic ships to webOS
 * and Tizen (where the implementation will use a similar thin adapter
 * against their respective player APIs).
 */
package com.kurostream.playback.kurovision

import com.kurostream.domain.platform.AtmosTranscodeStrategy
import com.kurostream.domain.platform.KuroVisionQualityMode
import com.kurostream.domain.platform.PlatformProfile
import com.kurostream.domain.platform.UpscaleAlgorithm

object PlatformTuner {

    /**
     * Translate the shared profile into KuroVision engine settings.
     */
    fun tuneKuroVision(profile: PlatformProfile, settings: KuroVisionSettings) {
        settings.setQualityMode(translateQualityMode(profile.defaultQualityMode))
        settings.setUpscaleAlgorithm(translateUpscale(profile.defaultUpscaleAlgorithm))
        settings.setFrameInterpolation(profile.kind in setOf(
            com.kurostream.domain.platform.PlatformKind.ANDROID_TV,
            com.kurostream.domain.platform.PlatformKind.FIRE_TV,
            com.kurostream.domain.platform.PlatformKind.LINUX_DESKTOP,
            com.kurostream.domain.platform.PlatformKind.WINDOWS_DESKTOP,
            com.kurostream.domain.platform.PlatformKind.MACOS_DESKTOP,
        ))
        settings.setDolbyPassthrough(profile.supportsDolbyAtmosPassthrough)
    }

    /**
     * Buffer sizes for ExoPlayer. Aggressive caps on TV, generous on desktop.
     */
    fun exoBufferSettings(profile: PlatformProfile): ExoBufferSettings {
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

    /**
     * Pick the audio track strategy from the profile.
     */
    fun audioStrategy(profile: PlatformProfile): AudioStrategy {
        return when (profile.dolbyAtmosTranscode) {
            AtmosTranscodeStrategy.PASSTHROUGH -> AudioStrategy.PASSTHROUGH
            AtmosTranscodeStrategy.TRANSCODE_TO_EAC3 -> AudioStrategy.DOWNMIX_EAC3
            AtmosTranscodeStrategy.TRANSCODE_TO_STEREO -> AudioStrategy.DOWNMIX_STEREO
            AtmosTranscodeStrategy.NATIVE_FALLBACK -> AudioStrategy.NATIVE_FALLBACK
        }
    }

    private fun translateQualityMode(m: KuroVisionQualityMode): KuroVisionQualityModeDto {
        return when (m) {
            KuroVisionQualityMode.HARDWARE_PASSTHROUGH -> KuroVisionQualityModeDto.HARDWARE
            KuroVisionQualityMode.SD_TO_HD -> KuroVisionQualityModeDto.CINEMA
            KuroVisionQualityMode.HD_TO_4K -> KuroVisionQualityModeDto.HDR_ULTRA
            KuroVisionQualityMode.ANIME_4K -> KuroVisionQualityModeDto.ANIME_PRO
            KuroVisionQualityMode.AI_NEURAL -> KuroVisionQualityModeDto.ULTRA_DESKTOP
            KuroVisionQualityMode.DESKTOP_FULL -> KuroVisionQualityModeDto.ULTRA_DESKTOP
        }
    }

    private fun translateUpscale(a: UpscaleAlgorithm): UpscaleAlgorithmDto {
        return when (a) {
            UpscaleAlgorithm.NEAREST -> UpscaleAlgorithmDto.BILINEAR
            UpscaleAlgorithm.BILINEAR -> UpscaleAlgorithmDto.BILINEAR
            UpscaleAlgorithm.BICUBIC -> UpscaleAlgorithmDto.BICUBIC
            UpscaleAlgorithm.LANCZOS3 -> UpscaleAlgorithmDto.LANCZOS3
            UpscaleAlgorithm.WAIFU2X -> UpscaleAlgorithmDto.WAIFU2X
            UpscaleAlgorithm.AI_REAL_ESRGAN -> UpscaleAlgorithmDto.ULTRA
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
