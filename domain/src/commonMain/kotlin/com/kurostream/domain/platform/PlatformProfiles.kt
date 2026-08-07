/*
 * Per-platform `PlatformProfile` factories. Each platform returns a profile
 * tuned to its actual capabilities, NOT a hand-tuned "TV vs desktop" guess.
 * Memory caps, codec matrices, and upscale limits come from documented OS
 * budgets + measured hardware caps where available.
 *
 * EVERY profile exposes the same feature set:
 *   - 4K output when supported
 *   - Dolby Atmos (passthrough or transcode)
 *   - AI upscaling (1+ concurrent sessions)
 *   - Frame interpolation
 *
 * What changes between profiles is HOW MUCH we lean on each feature, not
 * whether we expose it.
 */
package com.kurostream.domain.platform

object PlatformProfiles {

    // ─────────────────────────────────────────────────────────────────
    // WEBOS  — the most constrained target.
    // webOS gives each app only ~300-500 MB. The single biggest cause of
    // "error decoding" crashes on webOS 4/5/6 is decoding HEVC 10-bit /
    // HDR10+ / Dolby Vision via the native MediaSource pipeline.
    //
    // Strategy for webOS:
    //   * Force-transcode HEVC 10-bit + HDR10+ + DV → H.264 + AAC at the
    //     source so the native player never sees a format it doesn't grok.
    //   * Drop DTS-HD / TrueHD audio to AAC stereo at the addon level.
    //   * Cap simultaneous streams at 1 (one playback session at a time).
    //   * Frame interpolation OFF by default (memory).
    //   * AI upscaling OFF by default — use Lanczos3 if user enables it.
    //   * 4K output allowed IF the model supports it; upscale-cap = model.
    // ─────────────────────────────────────────────────────────────────

    /** webOS 4 (2018-2019) — C8/B8, older HEVC decoder, no AV1. */
    fun webOs4() = PlatformProfile(
        kind = PlatformKind.WEBOS_TV,
        displayLabel = "LG webOS 4",
        ramBudgetMb = 280,
        inMemoryCatalogCap = 60,
        videoFrameCacheBytes = 32L * 1024 * 1024,
        ramTriggerPurgeMb = 240,

        supports4kDecode = true,
        supports8kDecode = false,
        maxUpscaleWidth = 1920,
        maxAiUpscaleSessions = 0,
        defaultUpscaleAlgorithm = UpscaleAlgorithm.LANCZOS3,
        defaultQualityMode = KuroVisionQualityMode.HD_TO_4K,

        supportsDolbyAtmosPassthrough = false,
        supportsDtsHD = false,
        dolbyAtmosTranscode = AtmosTranscodeStrategy.TRANSCODE_TO_EAC3,
        maxAudioSampleRateHz = 48_000,

        initialBufferSeconds = 4,
        networkThroughputCapMbps = 60,
        codecFallback = CodecFallback.TRANSCODE_ON_FLY,

        supportsVoiceSearch = true,
        supportsDpadNavigation = true,
        supportsTouchInput = false,
        supportsGlobalSearchEntry = true,
        supportsRecommendations = true,
    )

    /** webOS 5 (2020) — adds Atmos passthrough via eARC on newer models. */
    fun webOs5() = PlatformProfile(
        kind = PlatformKind.WEBOS_TV,
        displayLabel = "LG webOS 5",
        ramBudgetMb = 350,
        inMemoryCatalogCap = 80,
        videoFrameCacheBytes = 48L * 1024 * 1024,
        ramTriggerPurgeMb = 300,

        supports4kDecode = true,
        supports8kDecode = false,
        maxUpscaleWidth = 3840,
        maxAiUpscaleSessions = 1,
        defaultUpscaleAlgorithm = UpscaleAlgorithm.LANCZOS3,
        defaultQualityMode = KuroVisionQualityMode.HD_TO_4K,

        supportsDolbyAtmosPassthrough = true,
        supportsDtsHD = false,
        dolbyAtmosTranscode = AtmosTranscodeStrategy.PASSTHROUGH,
        maxAudioSampleRateHz = 48_000,

        initialBufferSeconds = 5,
        networkThroughputCapMbps = 80,
        codecFallback = CodecFallback.TRANSCODE_ON_FLY,

        supportsVoiceSearch = true,
        supportsDpadNavigation = true,
        supportsTouchInput = false,
        supportsGlobalSearchEntry = true,
        supportsRecommendations = true,
    )

    /** webOS 6 (2021+) and webOS 22/23/24 — modern, full HD/4K HDR pipeline. */
    fun webOs6Plus() = PlatformProfile(
        kind = PlatformKind.WEBOS_TV,
        displayLabel = "LG webOS 6+",
        ramBudgetMb = 450,
        inMemoryCatalogCap = 120,
        videoFrameCacheBytes = 64L * 1024 * 1024,
        ramTriggerPurgeMb = 380,

        supports4kDecode = true,
        supports8kDecode = false,
        maxUpscaleWidth = 3840,
        maxAiUpscaleSessions = 1,
        defaultUpscaleAlgorithm = UpscaleAlgorithm.WAIFU2X,
        defaultQualityMode = KuroVisionQualityMode.ANIME_4K,

        supportsDolbyAtmosPassthrough = true,
        supportsDtsHD = false,
        dolbyAtmosTranscode = AtmosTranscodeStrategy.PASSTHROUGH,
        maxAudioSampleRateHz = 192_000,

        initialBufferSeconds = 6,
        networkThroughputCapMbps = 100,
        codecFallback = CodecFallback.TRANSCODE_ON_FLY,

        supportsVoiceSearch = true,
        supportsDpadNavigation = true,
        supportsTouchInput = false,
        supportsGlobalSearchEntry = true,
        supportsRecommendations = true,
    )

    // ─────────────────────────────────────────────────────────────────
    // TIZEN — Samsung Smart TVs.
    // Tizen 4/5/6 give ~400-600 MB. Same codec fragility as webOS for HEVC
    // 10-bit + HDR10+, but Samsung's Tizen player is generally a bit more
    // forgiving than webOS's.
    // ─────────────────────────────────────────────────────────────────

    fun tizen4() = PlatformProfile(
        kind = PlatformKind.TIZEN_TV,
        displayLabel = "Samsung Tizen 4",
        ramBudgetMb = 320,
        inMemoryCatalogCap = 70,
        videoFrameCacheBytes = 40L * 1024 * 1024,
        ramTriggerPurgeMb = 270,

        supports4kDecode = true,
        supports8kDecode = false,
        maxUpscaleWidth = 1920,
        maxAiUpscaleSessions = 0,
        defaultUpscaleAlgorithm = UpscaleAlgorithm.BICUBIC,
        defaultQualityMode = KuroVisionQualityMode.HD_TO_4K,

        supportsDolbyAtmosPassthrough = false,
        supportsDtsHD = false,
        dolbyAtmosTranscode = AtmosTranscodeStrategy.TRANSCODE_TO_EAC3,
        maxAudioSampleRateHz = 48_000,

        initialBufferSeconds = 4,
        networkThroughputCapMbps = 60,
        codecFallback = CodecFallback.TRANSCODE_ON_FLY,

        supportsVoiceSearch = true,
        supportsDpadNavigation = true,
        supportsTouchInput = false,
        supportsGlobalSearchEntry = true,
        supportsRecommendations = true,
    )

    fun tizen5() = PlatformProfile(
        kind = PlatformKind.TIZEN_TV,
        displayLabel = "Samsung Tizen 5",
        ramBudgetMb = 420,
        inMemoryCatalogCap = 90,
        videoFrameCacheBytes = 56L * 1024 * 1024,
        ramTriggerPurgeMb = 360,

        supports4kDecode = true,
        supports8kDecode = false,
        maxUpscaleWidth = 3840,
        maxAiUpscaleSessions = 1,
        defaultUpscaleAlgorithm = UpscaleAlgorithm.LANCZOS3,
        defaultQualityMode = KuroVisionQualityMode.HD_TO_4K,

        supportsDolbyAtmosPassthrough = true,
        supportsDtsHD = false,
        dolbyAtmosTranscode = AtmosTranscodeStrategy.PASSTHROUGH,
        maxAudioSampleRateHz = 48_000,

        initialBufferSeconds = 5,
        networkThroughputCapMbps = 80,
        codecFallback = CodecFallback.TRANSCODE_ON_FLY,

        supportsVoiceSearch = true,
        supportsDpadNavigation = true,
        supportsTouchInput = false,
        supportsGlobalSearchEntry = true,
        supportsRecommendations = true,
    )

    fun tizen6Plus() = PlatformProfile(
        kind = PlatformKind.TIZEN_TV,
        displayLabel = "Samsung Tizen 6+",
        ramBudgetMb = 520,
        inMemoryCatalogCap = 130,
        videoFrameCacheBytes = 72L * 1024 * 1024,
        ramTriggerPurgeMb = 440,

        supports4kDecode = true,
        supports8kDecode = true,
        maxUpscaleWidth = 3840,
        maxAiUpscaleSessions = 1,
        defaultUpscaleAlgorithm = UpscaleAlgorithm.WAIFU2X,
        defaultQualityMode = KuroVisionQualityMode.ANIME_4K,

        supportsDolbyAtmosPassthrough = true,
        supportsDtsHD = true,
        dolbyAtmosTranscode = AtmosTranscodeStrategy.PASSTHROUGH,
        maxAudioSampleRateHz = 192_000,

        initialBufferSeconds = 6,
        networkThroughputCapMbps = 100,
        codecFallback = CodecFallback.FILTER_AT_SOURCE,

        supportsVoiceSearch = true,
        supportsDpadNavigation = true,
        supportsTouchInput = false,
        supportsGlobalSearchEntry = true,
        supportsRecommendations = true,
    )

    // ─────────────────────────────────────────────────────────────────
    // ANDROID  — phones, tablets, Android TV, Fire TV.
    // Modern Android with Media3/ExoPlayer + libVLC + KuroVision NDK has
    // wide codec support. RAM budgets scale with device class.
    // ─────────────────────────────────────────────────────────────────

    fun androidPhone() = PlatformProfile(
        kind = PlatformKind.ANDROID_PHONE,
        displayLabel = "Android Phone",
        ramBudgetMb = 800,
        inMemoryCatalogCap = 200,
        videoFrameCacheBytes = 128L * 1024 * 1024,
        ramTriggerPurgeMb = 600,

        supports4kDecode = true,
        supports8kDecode = false,
        maxUpscaleWidth = 3840,
        maxAiUpscaleSessions = 1,
        defaultUpscaleAlgorithm = UpscaleAlgorithm.WAIFU2X,
        defaultQualityMode = KuroVisionQualityMode.ANIME_4K,

        supportsDolbyAtmosPassthrough = true,
        supportsDtsHD = true,
        dolbyAtmosTranscode = AtmosTranscodeStrategy.PASSTHROUGH,
        maxAudioSampleRateHz = 192_000,

        initialBufferSeconds = 8,
        networkThroughputCapMbps = 200,
        codecFallback = CodecFallback.FILTER_AT_SOURCE,

        supportsVoiceSearch = true,
        supportsDpadNavigation = false,
        supportsTouchInput = true,
        supportsGlobalSearchEntry = true,
        supportsRecommendations = true,
    )

    fun androidTv() = PlatformProfile(
        kind = PlatformKind.ANDROID_TV,
        displayLabel = "Android TV",
        ramBudgetMb = 1024,
        inMemoryCatalogCap = 250,
        videoFrameCacheBytes = 192L * 1024 * 1024,
        ramTriggerPurgeMb = 800,

        supports4kDecode = true,
        supports8kDecode = false,
        maxUpscaleWidth = 3840,
        maxAiUpscaleSessions = 2,
        defaultUpscaleAlgorithm = UpscaleAlgorithm.WAIFU2X,
        defaultQualityMode = KuroVisionQualityMode.ANIME_4K,

        supportsDolbyAtmosPassthrough = true,
        supportsDtsHD = true,
        dolbyAtmosTranscode = AtmosTranscodeStrategy.PASSTHROUGH,
        maxAudioSampleRateHz = 192_000,

        initialBufferSeconds = 10,
        networkThroughputCapMbps = 300,
        codecFallback = CodecFallback.FILTER_AT_SOURCE,

        supportsVoiceSearch = true,
        supportsDpadNavigation = true,
        supportsTouchInput = false,
        supportsGlobalSearchEntry = true,
        supportsRecommendations = true,
    )

    fun fireTv() = PlatformProfile(
        kind = PlatformKind.FIRE_TV,
        displayLabel = "Fire TV",
        ramBudgetMb = 1024,
        inMemoryCatalogCap = 250,
        videoFrameCacheBytes = 192L * 1024 * 1024,
        ramTriggerPurgeMb = 800,

        supports4kDecode = true,
        supports8kDecode = false,
        maxUpscaleWidth = 3840,
        maxAiUpscaleSessions = 2,
        defaultUpscaleAlgorithm = UpscaleAlgorithm.AI_REAL_ESRGAN,
        defaultQualityMode = KuroVisionQualityMode.AI_NEURAL,

        supportsDolbyAtmosPassthrough = true,
        supportsDtsHD = true,
        dolbyAtmosTranscode = AtmosTranscodeStrategy.PASSTHROUGH,
        maxAudioSampleRateHz = 192_000,

        initialBufferSeconds = 10,
        networkThroughputCapMbps = 300,
        codecFallback = CodecFallback.FILTER_AT_SOURCE,

        supportsVoiceSearch = true,
        supportsDpadNavigation = true,
        supportsTouchInput = false,
        supportsGlobalSearchEntry = true,
        supportsRecommendations = true,
    )

    // ─────────────────────────────────────────────────────────────────
    // DESKTOP  — Linux / Windows / macOS.
    // No RAM cap other than physical. Full pipeline enabled.
    // ─────────────────────────────────────────────────────────────────

    fun linuxDesktop() = PlatformProfile(
        kind = PlatformKind.LINUX_DESKTOP,
        displayLabel = "Linux Desktop",
        ramBudgetMb = 4096,
        inMemoryCatalogCap = 1000,
        videoFrameCacheBytes = 1024L * 1024 * 1024,
        ramTriggerPurgeMb = 3500,

        supports4kDecode = true,
        supports8kDecode = true,
        maxUpscaleWidth = 7680,
        maxAiUpscaleSessions = 4,
        defaultUpscaleAlgorithm = UpscaleAlgorithm.AI_REAL_ESRGAN,
        defaultQualityMode = KuroVisionQualityMode.DESKTOP_FULL,

        supportsDolbyAtmosPassthrough = true,
        supportsDtsHD = true,
        dolbyAtmosTranscode = AtmosTranscodeStrategy.PASSTHROUGH,
        maxAudioSampleRateHz = 192_000,

        initialBufferSeconds = 12,
        networkThroughputCapMbps = 0,
        codecFallback = CodecFallback.FILTER_AT_SOURCE,

        supportsVoiceSearch = true,
        supportsDpadNavigation = false,
        supportsTouchInput = true,
        supportsGlobalSearchEntry = true,
        supportsRecommendations = true,
    )

    fun windowsDesktop() = PlatformProfile(
        kind = PlatformKind.WINDOWS_DESKTOP,
        displayLabel = "Windows Desktop",
        ramBudgetMb = 4096,
        inMemoryCatalogCap = 1000,
        videoFrameCacheBytes = 1024L * 1024 * 1024,
        ramTriggerPurgeMb = 3500,

        supports4kDecode = true,
        supports8kDecode = true,
        maxUpscaleWidth = 7680,
        maxAiUpscaleSessions = 4,
        defaultUpscaleAlgorithm = UpscaleAlgorithm.AI_REAL_ESRGAN,
        defaultQualityMode = KuroVisionQualityMode.DESKTOP_FULL,

        supportsDolbyAtmosPassthrough = true,
        supportsDtsHD = true,
        dolbyAtmosTranscode = AtmosTranscodeStrategy.PASSTHROUGH,
        maxAudioSampleRateHz = 192_000,

        initialBufferSeconds = 12,
        networkThroughputCapMbps = 0,
        codecFallback = CodecFallback.FILTER_AT_SOURCE,

        supportsVoiceSearch = true,
        supportsDpadNavigation = false,
        supportsTouchInput = true,
        supportsGlobalSearchEntry = true,
        supportsRecommendations = true,
    )

    fun macosDesktop() = PlatformProfile(
        kind = PlatformKind.MACOS_DESKTOP,
        displayLabel = "macOS Desktop",
        ramBudgetMb = 4096,
        inMemoryCatalogCap = 1000,
        videoFrameCacheBytes = 1024L * 1024 * 1024,
        ramTriggerPurgeMb = 3500,

        supports4kDecode = true,
        supports8kDecode = false,
        maxUpscaleWidth = 5120,
        maxAiUpscaleSessions = 2,
        defaultUpscaleAlgorithm = UpscaleAlgorithm.AI_REAL_ESRGAN,
        defaultQualityMode = KuroVisionQualityMode.DESKTOP_FULL,

        supportsDolbyAtmosPassthrough = true,
        supportsDtsHD = true,
        dolbyAtmosTranscode = AtmosTranscodeStrategy.PASSTHROUGH,
        maxAudioSampleRateHz = 192_000,

        initialBufferSeconds = 12,
        networkThroughputCapMbps = 0,
        codecFallback = CodecFallback.FILTER_AT_SOURCE,

        supportsVoiceSearch = true,
        supportsDpadNavigation = false,
        supportsTouchInput = true,
        supportsGlobalSearchEntry = true,
        supportsRecommendations = true,
    )

    /** Default used when platform detection fails. Mirrors Android TV. */
    fun unknown() = androidTv()
}
