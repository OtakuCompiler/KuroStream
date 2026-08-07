/*
 * PlatformProfile — runtime knobs that every platform (Android, Android TV,
 * Fire TV, Tizen, webOS, Linux desktop, Windows desktop, macOS) consumes
 * to deliver every feature but with budgets tuned for the host.
 *
 * The principle: each platform CAN do 4K + Atmos + AI upscaling + frame
 * interpolation. The "soft" knobs here decide how aggressively we use each
 * feature, not whether we use it.
 *
 * Reference data (educated from real-world webOS / Tizen docs + the
 * 2026 Stremio-on-LG writeups):
 *   - webOS 4/5/6: ~300-500 MB RAM cap per app
 *   - Tizen 4/5/6: ~400-600 MB RAM cap
 *   - Android TV / Fire TV: 1-2 GB usable (midrange), 3-4 GB on Shield
 *   - Linux/Windows desktop: 4+ GB, GPU dictates upscaling ceiling
 *
 * The profile is computed once at app start (see `Platform.detect()`)
 * and exposed to the player, search, and UI layers via
 * `PlatformProfile.current`.
 */
package com.kurostream.domain.platform

enum class PlatformKind {
    ANDROID_PHONE,
    ANDROID_TABLET,
    ANDROID_TV,
    FIRE_TV,
    TIZEN_TV,
    WEBOS_TV,
    LINUX_DESKTOP,
    WINDOWS_DESKTOP,
    MACOS_DESKTOP,
    UNKNOWN,
}

data class PlatformProfile(
    val kind: PlatformKind,
    val displayLabel: String,

    // ── Memory budget ─────────────────────────────────────────────────
    /** Soft target peak working set in megabytes. Hard ceiling is OS-imposed. */
    val ramBudgetMb: Int,
    /** Maximum number of media items to hold in any in-memory cache. */
    val inMemoryCatalogCap: Int,
    /** Maximum bytes of decoded video frames held in flight. */
    val videoFrameCacheBytes: Long,
    /** When the runtime exceeds this, force a buffer purge. */
    val ramTriggerPurgeMb: Int,

    // ── Video pipeline ───────────────────────────────────────────────
    /** True if the host supports 4K (3840×2160) hardware decoding. */
    val supports4kDecode: Boolean,
    /** True if the host supports 8K decoding (Linux/Windows only). */
    val supports8kDecode: Boolean,
    /** Maximum upscaled output width. webOS caps at 1080p or 4K depending on model. */
    val maxUpscaleWidth: Int,
    /** Maximum simultaneous AI-upscaling sessions (1 on TVs, more on desktop). */
    val maxAiUpscaleSessions: Int,
    /** Default upscale algorithm for this profile. */
    val defaultUpscaleAlgorithm: UpscaleAlgorithm,
    /** Default quality mode for this profile. */
    val defaultQualityMode: KuroVisionQualityMode,

    // ── Audio pipeline ───────────────────────────────────────────────
    val supportsDolbyAtmosPassthrough: Boolean,
    val supportsDtsHD: Boolean,
    val dolbyAtmosTranscode: AtmosTranscodeStrategy,
    /** Maximum audio sample rate (Hz) for this profile. */
    val maxAudioSampleRateHz: Int,

    // ── Network / playback resilience ────────────────────────────────
    /** When to start buffering ahead in seconds. Smaller = less RAM, more rebuffer risk. */
    val initialBufferSeconds: Int,
    /** Maximum network throughput cap in Mbps (0 = unlimited). Used for memory budgeting. */
    val networkThroughputCapMbps: Int,
    /** When the addon's preferred stream exceeds the device codec matrix, transcode via this strategy. */
    val codecFallback: CodecFallback,

    // ── UI / lifecycle ────────────────────────────────────────────────
    val supportsVoiceSearch: Boolean,
    val supportsDpadNavigation: Boolean,
    val supportsTouchInput: Boolean,
    val supportsGlobalSearchEntry: Boolean,
    val supportsRecommendations: Boolean,
)

enum class UpscaleAlgorithm {
    NEAREST, BILINEAR, BICUBIC, LANCZOS3, WAIFU2X, AI_REAL_ESRGAN,
}

enum class KuroVisionQualityMode {
    /** No upscaling, just hardware passthrough. */
    HARDWARE_PASSTHROUGH,
    /** 1080p software upscale from SD/HD sources. */
    SD_TO_HD,
    /** 4K software upscale from HD sources. */
    HD_TO_4K,
    /** Anime-tuned 4K upscale with line art preservation. */
    ANIME_4K,
    /** AI neural upscale (Real-ESRGAN / Waifu2x). */
    AI_NEURAL,
    /** Desktop only: full pipeline, no caps. */
    DESKTOP_FULL,
}

enum class AtmosTranscodeStrategy {
    /** Direct HDMI/ARC/eARC passthrough (desktop + Android TV ≥9). */
    PASSTHROUGH,
    /** Transcode Atmos → EAC3 + DD+ on TVs without HDMI ARC. */
    TRANSCODE_TO_EAC3,
    /** Transcode Atmos → stereo on webOS 4/5 (older HDMI stacks). */
    TRANSCODE_TO_STEREO,
    /** Defer to native player — last resort. */
    NATIVE_FALLBACK,
}

enum class CodecFallback {
    /** Transcode HEVC 10-bit / HDR10+ / DV / DTS-HD → H.264 + AAC on the fly. */
    TRANSCODE_ON_FLY,
    /** Skip incompatible stream and request next from addon. */
    SKIP_AND_RETRY,
    /** Only offer streams that match the host codec matrix. */
    FILTER_AT_SOURCE,
}
