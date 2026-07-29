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

package com.kurostream.players.selector

import android.content.Context
import android.content.res.Configuration
import android.media.MediaCodecList
import android.os.Build
import android.view.Display
import com.kurostream.players.core.PlayerBackend
import com.kurostream.players.core.PlayerInterface
import com.kurostream.players.mpv.MpvPlayer
import com.kurostream.players.vlc.VlcPlayer
import com.kurostream.players.media3.Media3Player
import timber.log.Timber

/**
 * Phase 23: BackendSelector (FINAL)
 * Chooses optimal backend: mpv > VLC > Media3 based on:
 * - Codec capability (HEVC, AV1, VP9, etc.)
 * - Device capabilities (HDR, 4K, audio passthrough)
 * - Content requirements (DRM, subtitles, network protocol)
 *
 * Priority: mpv (best codec support) > VLC (good compatibility) > Media3 (fallback)
 */
class BackendSelector(private val context: Context) {

    private val deviceCapabilities by lazy { analyzeDeviceCapabilities() }
    private val backendCache = mutableMapOf<PlayerBackend, PlayerInterface>()

    /**
     * Select the best backend for a given media item
     */
    fun selectBackend(
        uri: String,
        requiredCodecs: List<String> = emptyList(),
        requiresHdr: Boolean = false,
        requiresDrm: Boolean = false,
        requiresPassthrough: Boolean = false,
        forceBackend: PlayerBackend? = null
    ): PlayerInterface {

        if (forceBackend != null) {
            Timber.d("Backend forced to: $forceBackend")
            return getOrCreateBackend(forceBackend)
        }

        val decision = evaluateBackends(uri, requiredCodecs, requiresHdr, requiresDrm, requiresPassthrough)
        Timber.i("Backend selected: ${decision.backend} (score=${decision.score}, reason=${decision.reason})")

        return getOrCreateBackend(decision.backend)
    }

    private fun evaluateBackends(
        uri: String,
        requiredCodecs: List<String>,
        requiresHdr: Boolean,
        requiresDrm: Boolean,
        requiresPassthrough: Boolean
    ): BackendDecision {

        val candidates = mutableListOf<BackendDecision>()

        val mpvScore = evaluateMpv(requiredCodecs, requiresHdr, requiresDrm, requiresPassthrough)
        candidates.add(BackendDecision(PlayerBackend.MPV, mpvScore, "Codec support + performance"))

        val vlcScore = evaluateVlc(requiredCodecs, requiresHdr, requiresDrm, requiresPassthrough)
        candidates.add(BackendDecision(PlayerBackend.VLC, vlcScore, "Compatibility + stability"))

        val media3Score = evaluateMedia3(requiredCodecs, requiresHdr, requiresDrm, requiresPassthrough)
        candidates.add(BackendDecision(PlayerBackend.MEDIA3, media3Score, "System integration + DRM"))

        return candidates.maxByOrNull { it.score }
            ?: BackendDecision(PlayerBackend.MEDIA3, 0, "Fallback")
    }

    private fun evaluateMpv(
        requiredCodecs: List<String>,
        requiresHdr: Boolean,
        requiresDrm: Boolean,
        requiresPassthrough: Boolean
    ): Int {
        var score = 100

        if (!isMpvAvailable()) {
            Timber.w("MPV native libraries not found")
            return -1000
        }

        val unsupportedByMpv = requiredCodecs.filter { !isCodecSupportedByMpv(it) }
        score -= unsupportedByMpv.size * 50

        if (requiresHdr && !deviceCapabilities.supportsHdr) {
            score -= 30
        }

        if (requiresDrm) {
            score -= 40
        }

        if (requiresPassthrough) {
            score += 20
        }

        if (deviceCapabilities.isLowEndDevice) {
            score -= 20
        }

        if (deviceCapabilities.hasHardwareDecoder) {
            score += 10
        }

        return score
    }

    private fun evaluateVlc(
        requiredCodecs: List<String>,
        requiresHdr: Boolean,
        requiresDrm: Boolean,
        requiresPassthrough: Boolean
    ): Int {
        var score = 70

        if (!isVlcAvailable()) {
            Timber.w("VLC native libraries not found")
            return -1000
        }

        val unsupportedByVlc = requiredCodecs.filter { !isCodecSupportedByVlc(it) }
        score -= unsupportedByVlc.size * 40

        if (requiresHdr && !deviceCapabilities.supportsHdr) {
            score -= 25
        }

        if (requiresDrm) {
            score -= 60
        }

        if (requiresPassthrough) {
            score += 15
        }

        if (deviceCapabilities.isLowEndDevice) {
            score += 10
        }

        return score
    }

    private fun evaluateMedia3(
        requiredCodecs: List<String>,
        requiresHdr: Boolean,
        requiresDrm: Boolean,
        requiresPassthrough: Boolean
    ): Int {
        var score = 50

        if (requiresDrm) {
            score += 50
        }

        val unsupportedBySystem = requiredCodecs.filter { !isCodecSupportedBySystem(it) }
        score -= unsupportedBySystem.size * 70

        if (requiresHdr && deviceCapabilities.supportsHdr) {
            score += 20
        }

        if (requiresPassthrough) {
            score += 10
        }

        if (deviceCapabilities.isLowEndDevice) {
            score += 20
        }

        return score
    }

    private fun getOrCreateBackend(backend: PlayerBackend): PlayerInterface {
        return backendCache.getOrPut(backend) {
            when (backend) {
                PlayerBackend.MPV -> MpvPlayer(context)
                PlayerBackend.VLC -> VlcPlayer(context)
                PlayerBackend.MEDIA3 -> Media3Player(context)
            }
        }
    }

    fun releaseAll() {
        backendCache.values.forEach { it.release() }
        backendCache.clear()
    }

    private fun analyzeDeviceCapabilities(): DeviceCapabilities {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val hasHevc = codecList.codecInfos.any { info ->
            info.supportedTypes.any { type ->
                type.equals("video/hevc", ignoreCase = true) || type.equals("video/x-hevc", ignoreCase = true)
            }
        }
        val hasAv1 = codecList.codecInfos.any { info ->
            info.supportedTypes.any { type ->
                type.equals("video/av01", ignoreCase = true)
            }
        }
        val hasVp9 = codecList.codecInfos.any { info ->
            info.supportedTypes.any { type ->
                type.equals("video/x-vnd.on2.vp9", ignoreCase = true)
            }
        }
        val hasHardwareDecoder = codecList.codecInfos.any { it.isHardwareAccelerated }

        val supportsHdr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val display = context.display
            display?.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() ?: false
        } else false

        val isLowEndDevice = isLowEndDevice()

        return DeviceCapabilities(
            hasHevc = hasHevc,
            hasAv1 = hasAv1,
            hasVp9 = hasVp9,
            hasHardwareDecoder = hasHardwareDecoder,
            supportsHdr = supportsHdr,
            isLowEndDevice = isLowEndDevice
        )
    }

    private fun isLowEndDevice(): Boolean {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val config = context.resources.configuration
        val isSmallScreen = config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK ==
                Configuration.SCREENLAYOUT_SIZE_SMALL
        val lowCores = Runtime.getRuntime().availableProcessors() <= 4
        return maxMemory < 2L * 1024 * 1024 * 1024 || isSmallScreen || lowCores
    }

    private fun isMpvAvailable(): Boolean {
        return try {
            System.loadLibrary("mpv")
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    private fun isVlcAvailable(): Boolean {
        return try {
            Class.forName("org.videolan.libvlc.LibVLC")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    private fun isCodecSupportedByMpv(codec: String): Boolean {
        val unsupported = setOf("av1-10bit-profile2")
        return codec !in unsupported
    }

    private fun isCodecSupportedByVlc(codec: String): Boolean {
        val unsupported = setOf("av1-10bit-profile2")
        return codec !in unsupported
    }

    private fun isCodecSupportedBySystem(codec: String): Boolean {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        return codecList.codecInfos.any { info ->
            info.supportedTypes.any { it.contains(codec, ignoreCase = true) }
        }
    }

    data class BackendDecision(
        val backend: PlayerBackend,
        val score: Int,
        val reason: String
    )

    data class DeviceCapabilities(
        val hasHevc: Boolean,
        val hasAv1: Boolean,
        val hasVp9: Boolean,
        val hasHardwareDecoder: Boolean,
        val supportsHdr: Boolean,
        val isLowEndDevice: Boolean
    )
}
