/*
 * PlatformProfile — runtime knobs for every KuroStream client.
 *
 * Mirrors the Kotlin `PlatformProfile` in :domain/platform so all clients
 * (Android, Android TV, Fire TV, Tizen, webOS, Linux, Windows, macOS)
 * share the same budget logic.
 *
 * Two physical realities motivate the per-OS numbers:
 *
 *   - webOS gives each app ~300-500 MB of RAM. The single biggest cause
 *     of "error occurred when decoding" crashes on webOS 4/5/6 is feeding
 *     HEVC 10-bit / HDR10+ / Dolby Vision to the native MediaSource
 *     pipeline. The numbers below keep the working set inside that
 *     envelope while still letting the app start.
 *
 *   - Tizen 4/5/6 gives ~400-600 MB and is more forgiving with HEVC main
 *     profile but still chokes on HDR10+ metadata and multi-audio tracks.
 */
export const PlatformKind = Object.freeze({
  ANDROID_PHONE: 'android_phone',
  ANDROID_TABLET: 'android_tablet',
  ANDROID_TV: 'android_tv',
  FIRE_TV: 'fire_tv',
  TIZEN_TV: 'tizen_tv',
  WEBOS_TV: 'webos_tv',
  LINUX_DESKTOP: 'linux_desktop',
  WINDOWS_DESKTOP: 'windows_desktop',
  MACOS_DESKTOP: 'macos_desktop',
  UNKNOWN: 'unknown',
});

export const HdrType = Object.freeze({
  NONE: 'none',
  HDR10: 'hdr10',
  HDR10_PLUS: 'hdr10_plus',
  DOLBY_VISION: 'dolby_vision',
  HLG: 'hlg',
});

export const UpscaleAlgorithm = Object.freeze({
  NEAREST: 'nearest',
  BILINEAR: 'bilinear',
  BICUBIC: 'bicubic',
  LANCZOS3: 'lanczos3',
  WAIFU2X: 'waifu2x',
  AI_REAL_ESRGAN: 'ai_real_esrgan',
});

export const KuroVisionQualityMode = Object.freeze({
  HARDWARE_PASSTHROUGH: 'hardware_passthrough',
  SD_TO_HD: 'sd_to_hd',
  HD_TO_4K: 'hd_to_4k',
  ANIME_4K: 'anime_4k',
  AI_NEURAL: 'ai_neural',
  DESKTOP_FULL: 'desktop_full',
});

export const AtmosTranscodeStrategy = Object.freeze({
  PASSTHROUGH: 'passthrough',
  TRANSCODE_TO_EAC3: 'transcode_to_eac3',
  TRANSCODE_TO_STEREO: 'transcode_to_stereo',
  NATIVE_FALLBACK: 'native_fallback',
});

export const CodecFallback = Object.freeze({
  TRANSCODE_ON_FLY: 'transcode_on_fly',
  SKIP_AND_RETRY: 'skip_and_retry',
  FILTER_AT_SOURCE: 'filter_at_source',
});

/**
 * Detect the current platform.
 * Order matters: webOS reports itself as "webOS" / "Web0S" / "WebOS";
 * Tizen reports as "Tizen" / "SMART-TV"; everything else falls through.
 */
export function detectPlatformKind() {
  const ua = (navigator.userAgent || '').toLowerCase();
  if (ua.includes('webos') || ua.includes('web0s')) return PlatformKind.WEBOS_TV;
  if (ua.includes('tizen') || ua.includes('smb-t') || ua.includes('smart-tv'))
    return PlatformKind.TIZEN_TV;
  if (ua.includes('aftt') || ua.includes('aftb') || ua.includes('amazon'))
    return PlatformKind.FIRE_TV;
  if (ua.includes('androidtv') || ua.includes('shield') || ua.includes('nexus player'))
    return PlatformKind.ANDROID_TV;
  if (ua.includes('android')) return PlatformKind.ANDROID_PHONE;
  if (ua.includes('mac os')) return PlatformKind.MACOS_DESKTOP;
  if (ua.includes('windows')) return PlatformKind.WINDOWS_DESKTOP;
  if (ua.includes('linux') || ua.includes('x11')) return PlatformKind.LINUX_DESKTOP;
  return PlatformKind.UNKNOWN;
}

/**
 * Detect the webOS major version when running on webOS. Returns
 * { major, minor } or null. webOS exposes this via webOS.sysinfo or the
 * older window.PalmSystem bridge.
 */
export function detectWebOSVersion() {
  try {
    // webOS TV 4+ exposes `webOS` global
    if (typeof webOS !== 'undefined' && webOS.platform) {
      // webOS 5+ uses `webOS.platform.tv`; older uses `webOS.systemInfo`
      if (webOS.platform.tv && typeof webOS.platform.tv === 'object') {
        const ver = webOS.platform.tv.version || '';
        return parseVersionString(ver);
      }
      if (webOS.systemInfo && typeof webOS.systemInfo === 'object') {
        const ver = (webOS.systemInfo.version || webOS.systemInfo.platformVersion || '');
        return parseVersionString(ver);
      }
    }
    // Legacy PalmSystem bridge (webOS 3 and earlier)
    if (typeof window.PalmSystem !== 'undefined' && window.PalmSystem.platform) {
      return parseVersionString(window.PalmSystem.platform);
    }
  } catch (_) { /* ignore */ }
  // Fallback: scrape the user agent for the version segment.
  const ua = navigator.userAgent || '';
  const m = ua.match(/web[Oo][Ss]\/(\d+)\.(\d+)/);
  if (m) return { major: Number(m[1]), minor: Number(m[2]) };
  return null;
}

function parseVersionString(s) {
  if (!s) return null;
  const m = String(s).match(/(\d+)\.(\d+)/);
  if (!m) return null;
  return { major: Number(m[1]), minor: Number(m[2]) };
}

/**
 * Detect the Tizen major version when running on Tizen.
 */
export function detectTizenVersion() {
  try {
    if (typeof tizen !== 'undefined' && tizen.systeminfo) {
      // Tizen 4+: tizen.systeminfo.getProperty('http://tizen.org/feature/platform.version')
      // Tizen 5+: same but synchronous access through tizen.build.version
      const v =
        (tizen.systeminfo.build && tizen.systeminfo.build.version) ||
        (tizen.build && tizen.build.version) ||
        '';
      const m = String(v).match(/(\d+)\.(\d+)/);
      if (m) return { major: Number(m[1]), minor: Number(m[2]) };
    }
  } catch (_) { /* ignore */ }
  const ua = navigator.userAgent || '';
  const m = ua.match(/tizen[/\s]?(\d+)\.(\d+)/);
  if (m) return { major: Number(m[1]), minor: Number(m[2]) };
  return null;
}
