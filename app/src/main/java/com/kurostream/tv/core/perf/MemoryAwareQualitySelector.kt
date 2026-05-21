package com.kurostream.tv.core.perf

import android.app.ActivityManager
import android.content.Context
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Selects optimal video resolution and ExoPlayer track constraints based on
 * real-time device capability — RAM, codec hardware support, and display.
 *
 * Decision matrix (at init time):
 *
 * | Total RAM   | Max resolution | Max bitrate | Buffer max |
 * |-------------|----------------|-------------|------------|
 * | ≤ 1 GB      | 1080p          | 8 Mbps      | 15 s       |
 * | 1–2 GB      | 1080p          | 12 Mbps     | 20 s       |
 * | > 2 GB      | 4K if HW dec   | 25 Mbps     | 30 s       |
 *
 * 4K is only offered when the device has hardware HEVC / VP9 / AV1 decode
 * capability at 2160p, otherwise the cap stays at 1080p even on high-RAM devices.
 */
@Singleton
class MemoryAwareQualitySelector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "QualitySelector"

        // RAM buckets in bytes
        private const val LOW_RAM_THRESHOLD = 1_100L * 1024 * 1024   // ≤ 1.1 GB
        private const val MID_RAM_THRESHOLD  = 2_100L * 1024 * 1024  // ≤ 2.1 GB

        // Bitrate caps (bits-per-second)
        private const val BITRATE_LOW  = 8_000_000   // 8 Mbps  — safe for 1080p on 1GB
        private const val BITRATE_MID  = 12_000_000  // 12 Mbps
        private const val BITRATE_HIGH = 25_000_000  // 25 Mbps — 4K capable

        // Buffer durations (ms)
        private const val MIN_BUFFER_LOW  = 6_000
        private const val MIN_BUFFER_MID  = 8_000
        private const val MIN_BUFFER_HIGH = 10_000

        private const val MAX_BUFFER_LOW  = 15_000
        private const val MAX_BUFFER_MID  = 20_000
        private const val MAX_BUFFER_HIGH = 30_000

        // Playback / re-buffer
        private const val PLAYBACK_BUFFER_MS = 2_500
        private const val REBUFFER_MS        = 5_000

        // 4K codec MIME types
        private val HEVC_MIME  = MediaFormat.MIMETYPE_VIDEO_HEVC
        private val VP9_MIME   = "video/x-vnd.on2.vp9"
        private val AV1_MIME   = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaFormat.MIMETYPE_VIDEO_AV1 else "video/av01"
    }

    /** Cached profile, computed once at first access. */
    val profile: QualityProfile by lazy { buildProfile() }

    private fun buildProfile(): QualityProfile {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem

        val ramTier = when {
            totalRam <= LOW_RAM_THRESHOLD -> RamTier.LOW
            totalRam <= MID_RAM_THRESHOLD -> RamTier.MID
            else -> RamTier.HIGH
        }

        val has4kHwDecode = ramTier == RamTier.HIGH && checkHardware4kSupport()

        val profile = QualityProfile(
            ramTier = ramTier,
            totalRamMb = (totalRam / 1024 / 1024).toInt(),
            maxWidth = if (has4kHwDecode) 3840 else 1920,
            maxHeight = if (has4kHwDecode) 2160 else 1080,
            maxBitrateBps = when (ramTier) {
                RamTier.LOW  -> BITRATE_LOW
                RamTier.MID  -> BITRATE_MID
                RamTier.HIGH -> if (has4kHwDecode) BITRATE_HIGH else BITRATE_MID
            },
            minBufferMs = when (ramTier) {
                RamTier.LOW  -> MIN_BUFFER_LOW
                RamTier.MID  -> MIN_BUFFER_MID
                RamTier.HIGH -> MIN_BUFFER_HIGH
            },
            maxBufferMs = when (ramTier) {
                RamTier.LOW  -> MAX_BUFFER_LOW
                RamTier.MID  -> MAX_BUFFER_MID
                RamTier.HIGH -> MAX_BUFFER_HIGH
            },
            playbackBufferMs = PLAYBACK_BUFFER_MS,
            rebufferMs       = REBUFFER_MS,
            has4kCapability  = has4kHwDecode,
            preferHighestBitrate = ramTier != RamTier.LOW,
            allowHardwareAcceleration = true
        )

        Timber.tag(TAG).i(
            "Quality profile — RAM=${profile.totalRamMb}MB tier=${ramTier} " +
            "maxRes=${profile.maxWidth}x${profile.maxHeight} " +
            "maxBitrate=${profile.maxBitrateBps / 1_000_000}Mbps 4K=${has4kHwDecode}"
        )

        return profile
    }

    /**
     * Returns true when the device exposes a hardware decoder that supports
     * HEVC, VP9, or AV1 at 4K (3840×2160).
     */
    private fun checkHardware4kSupport(): Boolean {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val candidates = listOf(HEVC_MIME, VP9_MIME, AV1_MIME)

        return candidates.any { mime ->
            codecList.codecInfos.any { info ->
                if (info.isEncoder) return@any false
                val caps = runCatching { info.getCapabilitiesForType(mime) }.getOrNull()
                    ?: return@any false
                val isHw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    info.isHardwareAccelerated else !info.name.startsWith("OMX.google")
                isHw && (caps.videoCapabilities?.isSizeSupported(3840, 2160) == true)
            }
        }
    }

    /**
     * Apply a quick memory pressure check at runtime.
     * If available RAM drops below 80 MB, force 720p to avoid OOM during playback.
     */
    fun runtimeQualityConstraint(): RuntimeConstraint {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val availMb = memInfo.availMem / 1024 / 1024

        return when {
            memInfo.lowMemory || availMb < 80 -> {
                Timber.tag(TAG).w("Low memory ($availMb MB free) — forcing 720p")
                RuntimeConstraint.Force720p
            }
            availMb < 150 -> {
                Timber.tag(TAG).d("Moderate memory ($availMb MB free) — capping at 1080p")
                RuntimeConstraint.Cap1080p
            }
            else -> RuntimeConstraint.UseProfile
        }
    }
}

// ─── Data models ──────────────────────────────────────────────────────────────

enum class RamTier { LOW, MID, HIGH }

data class QualityProfile(
    val ramTier: RamTier,
    val totalRamMb: Int,
    val maxWidth: Int,
    val maxHeight: Int,
    val maxBitrateBps: Int,
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val playbackBufferMs: Int,
    val rebufferMs: Int,
    val has4kCapability: Boolean,
    val preferHighestBitrate: Boolean,
    val allowHardwareAcceleration: Boolean
)

sealed class RuntimeConstraint {
    object UseProfile : RuntimeConstraint()
    object Cap1080p   : RuntimeConstraint()
    object Force720p  : RuntimeConstraint()
}
