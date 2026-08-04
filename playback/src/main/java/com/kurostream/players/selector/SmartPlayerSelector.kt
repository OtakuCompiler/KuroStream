// This file is part of KuroStream.
//
// SmartPlayerSelector — automatically selects the best playback backend
// and quality based on source, device, network, and user preferences.
//
// Decision tree:
//   AV1 + hardware decoder → Media3 (preferred)
//   Dolby Vision + compatible engine → Media3
//   Advanced subtitle + MPV/VLC → MPV/VLC
//   Fallback → Media3
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.playback.selector

import com.kurostream.playback.kurovision.DeviceClass
import com.kurostream.playback.kurovision.KuroVisionDeviceProfile
import com.kurostream.playback.kurovision.KuroVisionQualityMode
import com.kurostream.players.selector.PlayerBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartPlayerSelector @Inject constructor(
    private val profile: KuroVisionDeviceProfile,
) {

    suspend fun selectBackend(
        sourceCodec: String,
        isHdr: Boolean,
        isDolbyVision: Boolean,
        hasAdvancedSubs: Boolean,
        networkMbps: Int,
    ): PlayerBackend = withContext(Dispatchers.Default) {
        when {
            isDolbyVision && profile.supportsHardwareDecoder -> PlayerBackend.MEDIA3
            sourceCodec.equals("av1", true) && profile.supportsHardwareDecoder -> PlayerBackend.MEDIA3
            hasAdvancedSubs && isAdvancedCodec(sourceCodec) -> if (hasNativeMpv()) PlayerBackend.MPV else PlayerBackend.VLC
            networkMbps < 5 && profile.deviceClass == DeviceClass.LOW_POWER_TV -> PlayerBackend.MEDIA3
            else -> PlayerBackend.MEDIA3
        }
    }

    suspend fun selectQuality(
        sourceQualities: List<String>,
        networkMbps: Int,
        deviceClass: DeviceClass,
    ): String = withContext(Dispatchers.Default) {
        val maxAllowed = when {
            networkMbps >= 50 && deviceClass in listOf(DeviceClass.HIGH_END_TV, DeviceClass.DESKTOP_HIGH) -> "4K"
            networkMbps >= 25 && deviceClass in listOf(DeviceClass.MID_TV, DeviceClass.MOBILE_HIGH, DeviceClass.DESKTOP_LOW) -> "1080p"
            networkMbps >= 10 -> "720p"
            else -> "480p"
        }
        sourceQualities.filter { q -> qualityRank(q) <= qualityRank(maxAllowed) }.maxByOrNull { qualityRank(it) } ?: "auto"
    }

    fun selectKuroVisionMode(
        sourceQuality: String,
        isHdr: Boolean,
        networkMbps: Int,
    ): KuroVisionQualityMode {
        val base = when {
            sourceQuality.contains("4K", true) && networkMbps >= 25 -> KuroVisionQualityMode.HDR_VISION
            sourceQuality.contains("1080", true) && networkMbps >= 10 -> KuroVisionQualityMode.CINEMA
            else -> KuroVisionQualityMode.HARDWARE
        }
        return KuroVisionQualityMode.chooseFor(profile, base)
    }

    private fun isAdvancedCodec(codec: String): Boolean =
        codec.equals("hevc", true) || codec.equals("av1", true) || codec.equals("vp9", true)

    private fun hasNativeMpv(): Boolean {
        return try {
            System.loadLibrary("mpv")
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        }
    }

    private fun qualityRank(quality: String): Int = when {
        quality.contains("8K", true) -> 5
        quality.contains("4K", true) || quality.contains("2160", true) -> 4
        quality.contains("1080", true) || quality.contains("FHD", true) -> 3
        quality.contains("720", true) || quality.contains("HD", true) -> 2
        quality.contains("480", true) -> 1
        else -> 0
    }
}
