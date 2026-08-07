/*
 * CodecCompatibilityMatrix + TranscodeSelector
 *
 * The root cause of the LG webOS "error occurred when decoding" / "video
 * cannot be played" / "file is not recognized" crashes when streaming 4K
 * P2P content is that the webOS native MSE/EME pipeline can't decode certain
 * HEVC profiles (10-bit, HDR10+, Dolby Vision) and certain audio tracks
 * (DTS-HD MA, TrueHD Atmos). When the player hits one of these it either
 * throws a decode error or falls back to the system Media Player which
 * ALSO fails and shows "File not supported."
 *
 * Fix: NEVER hand the player a stream it can't decode. Filter at the addon
 * layer (only request compatible streams) AND/OR transcode at the resolver
 * layer (downmix to H.264 + AAC on the fly for incompatible streams).
 *
 * This file is shared between Android, desktop, Tizen and webOS clients.
 * Each client picks its strategy via `PlatformProfile.codecFallback` and
 * `PlatformProfile.dolbyAtmosTranscode`.
 */
package com.kurostream.domain.platform

/**
 * Probe of a single stream's codecs. Filled by the metadata layer (probe
 * of the torrent info dict, plus container-level inspection).
 */
data class StreamProbe(
    val videoCodec: String,            // "h264", "hevc", "av1", "vp9"
    val videoProfile: String? = null,  // "main", "main10", "high", etc.
    val videoBitDepth: Int = 8,        // 8 / 10 / 12
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: Double = 0.0,
    val hdrType: HdrType = HdrType.NONE,
    val audioCodec: String,            // "aac", "ac3", "eac3", "truehd", "dts", "dtshd"
    val audioChannels: Int = 2,
    val audioSampleRateHz: Int = 48_000,
    val container: String = "mkv",     // "mkv", "mp4", "avi", "ts"
    val hasMultipleAudioTracks: Boolean = false,
    val hasEmbeddedSubtitles: Boolean = false,
)

enum class HdrType {
    NONE,
    HDR10,
    HDR10_PLUS,
    DOLBY_VISION,
    HLG,
}

/**
 * The compatibility verdict for a given (platform, stream) pair.
 */
data class CompatibilityVerdict(
    val canPlayDirectly: Boolean,
    val reasons: List<String>,
    val transcodeRequired: TranscodeRequirements?,
) {
    val canPlay: Boolean get() = canPlayDirectly || transcodeRequired != null
}

/**
 * What needs to change for the stream to play on this profile.
 * `null` if no transcode can save it (e.g. webOS 4 + AV1).
 */
data class TranscodeRequirements(
    val transcodeVideo: Boolean,
    val transcodeAudio: Boolean,
    val targetVideoCodec: String = "h264",
    val targetVideoProfile: String = "high",
    val targetVideoBitDepth: Int = 8,
    val targetHdr: HdrType = HdrType.NONE,
    val targetAudioCodec: String = "aac",
    val targetAudioChannels: Int = 2,
    val estimatedLatencyMs: Int = 1500,
) {
    val needsAnyTranscode: Boolean get() = transcodeVideo || transcodeAudio
}

object CodecCompatibilityMatrix {

    fun check(profile: PlatformProfile, probe: StreamProbe): CompatibilityVerdict {
        val reasons = mutableListOf<String>()
        var needsVideo = false
        var needsAudio = false
        val targetVideoCodec = "h264"
        val targetVideoProfile = "high"
        val targetAudioCodec = when {
            profile.maxAudioSampleRateHz < probe.audioSampleRateHz -> "aac"
            !profile.supportsDolbyAtmosPassthrough && probe.audioCodec in setOf("truehd", "eac3") -> "aac"
            !profile.supportsDtsHD && probe.audioCodec in setOf("dts", "dtshd") -> "aac"
            else -> null
        }

        // ── Video checks ─────────────────────────────────────────────
        val directVideoOk = when (probe.videoCodec) {
            "h264", "avc1" -> true
            "hevc", "h265" -> {
                when {
                    !profile.supports4kDecode && probe.height >= 2160 -> false
                    probe.videoBitDepth > 8 && profile.kind in setOf(
                        PlatformKind.WEBOS_TV,
                        PlatformKind.TIZEN_TV,
                    ) -> {
                        reasons += "HEVC 10-bit not hardware-decodable on ${profile.kind}"
                        needsVideo = true
                        false
                    }
                    probe.hdrType == HdrType.DOLBY_VISION && profile.kind in setOf(
                        PlatformKind.WEBOS_TV,
                        PlatformKind.TIZEN_TV,
                    ) -> {
                        reasons += "Dolby Vision not decodable on ${profile.kind}"
                        needsVideo = true
                        false
                    }
                    probe.hdrType == HdrType.HDR10_PLUS && profile.kind in setOf(
                        PlatformKind.WEBOS_TV,
                        PlatformKind.TIZEN_TV,
                    ) -> {
                        // HDR10+ is the worst offender on webOS — strip the
                        // dynamic metadata and decode as plain HDR10 if the
                        // panel supports it; otherwise SDR.
                        reasons += "HDR10+ metadata crashes webOS/Tizen native player"
                        needsVideo = true
                        false
                    }
                    else -> true
                }
            }
            "av1" -> {
                // webOS 4/5 + Tizen 4/5 + low-end Fire TV stick lack AV1 hw.
                when {
                    profile.kind in setOf(
                        PlatformKind.WEBOS_TV,
                        PlatformKind.TIZEN_TV,
                        PlatformKind.FIRE_TV,
                    ) && probe.height >= 2160 -> {
                        reasons += "AV1 4K not hardware-decodable"
                        needsVideo = true
                        false
                    }
                    else -> true
                }
            }
            "vp9" -> true  // supported in MSE on webOS 5+
            else -> {
                reasons += "Unknown video codec ${probe.videoCodec}"
                needsVideo = true
                false
            }
        }

        // ── Audio checks ─────────────────────────────────────────────
        val directAudioOk = when (probe.audioCodec) {
            "aac", "mp3" -> true
            "ac3" -> true
            "eac3" -> {
                if (profile.supportsDolbyAtmosPassthrough) true
                else {
                    // EAC3 without Atmos is fine on most TVs; Atmos tags
                    // require either passthrough or transcode.
                    if (probe.audioChannels <= 6) true
                    else {
                        reasons += "EAC3 >6ch not passthrough-supported"
                        needsAudio = true
                        false
                    }
                }
            }
            "truehd" -> {
                // TrueHD is the Atmos carrier. webOS 4/5 + Tizen 4/5 cannot
                // passthrough it; desktop + Android TV can.
                if (profile.supportsDolbyAtmosPassthrough) true
                else {
                    reasons += "TrueHD/Atmos not passthrough-supported"
                    needsAudio = true
                    false
                }
            }
            "dts", "dtshd" -> {
                if (profile.supportsDtsHD) true
                else {
                    reasons += "DTS-HD not supported on this profile"
                    needsAudio = true
                    false
                }
            }
            else -> {
                reasons += "Unknown audio codec ${probe.audioCodec}"
                needsAudio = true
                false
            }
        }

        // ── Multi-track check ────────────────────────────────────────
        // The webOS native player handles multi-audio tracks poorly and
        // often crashes when switching. Always downmix to a single track
        // on webOS/Tizen.
        if (probe.hasMultipleAudioTracks && profile.kind in setOf(
                PlatformKind.WEBOS_TV,
                PlatformKind.TIZEN_TV,
            )) {
            reasons += "Multi-audio-track streams crash webOS/Tizen player"
            needsAudio = true
        }

        // ── Container check ──────────────────────────────────────────
        // AVI / WMV / RMVB → always transcode on TVs (rare codecs inside).
        if (probe.container.lowercase() in setOf("avi", "wmv", "rmvb", "flv") &&
            profile.kind in setOf(PlatformKind.WEBOS_TV, PlatformKind.TIZEN_TV)
        ) {
            reasons += "Container ${probe.container} unsupported on TV"
            needsVideo = true
            needsAudio = true
        }

        val canPlayDirectly = directVideoOk && directAudioOk
        val transcode = if (!canPlayDirectly) {
            // Bail out if codecFallback is FILTER_AT_SOURCE — caller will
            // pick a different stream instead of transcoding.
            if (profile.codecFallback == CodecFallback.FILTER_AT_SOURCE) null
            else TranscodeRequirements(
                transcodeVideo = needsVideo,
                transcodeAudio = needsAudio,
                targetVideoCodec = targetVideoCodec,
                targetVideoProfile = targetVideoProfile,
                targetAudioCodec = targetAudioCodec ?: "aac",
            )
        } else null

        return CompatibilityVerdict(
            canPlayDirectly = canPlayDirectly,
            reasons = reasons,
            transcodeRequired = transcode,
        )
    }
}
