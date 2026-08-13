/*
 * PlatformProfile — runtime knobs that every client (Android, Android TV,
 * Fire TV, Tizen, webOS, Linux/Windows/macOS desktop) consumes. Mirrors
 * the Kotlin version in :domain/platform/PlatformProfile.kt.
 *
 * The "soft" knobs here decide how aggressively we use each feature; every
 * profile still exposes the same feature set:
 *   - 4K output (when the panel supports it)
 *   - Dolby Atmos (passthrough or transcode)
 *   - AI upscaling (1+ concurrent sessions when memory allows)
 *   - Frame interpolation (capped on TVs, on by default on desktop)
 */
export class PlatformProfile {
  constructor(init) {
    Object.assign(this, init);
  }

  /** RAM cap, megabytes. Soft target; OS may impose a lower hard cap. */
  get ramBudgetMb() { return this._ramBudgetMb; }
  set ramBudgetMb(v) { this._ramBudgetMb = v; }

  /** Cap on catalog rows kept in memory. */
  get inMemoryCatalogCap() { return this._inMemoryCatalogCap; }
  set inMemoryCatalogCap(v) { this._inMemoryCatalogCap = v; }

  /** Bytes of decoded video frames held in flight. */
  get videoFrameCacheBytes() { return this._videoFrameCacheBytes; }
  set videoFrameCacheBytes(v) { this._videoFrameCacheBytes = v; }

  /** When runtime exceeds this, trigger a soft trim. */
  get ramTriggerPurgeMb() { return this._ramTriggerPurgeMb; }
  set ramTriggerPurgeMb(v) { this._ramTriggerPurgeMb = v; }

  /** True if 4K (3840×2160) hardware decode is available. */
  get supports4kDecode() { return this._supports4kDecode; }
  set supports4kDecode(v) { this._supports4kDecode = v; }
  get supports8kDecode() { return this._supports8kDecode; }
  set supports8kDecode(v) { this._supports8kDecode = v; }
  get maxUpscaleWidth() { return this._maxUpscaleWidth; }
  set maxUpscaleWidth(v) { this._maxUpscaleWidth = v; }
  get maxAiUpscaleSessions() { return this._maxAiUpscaleSessions; }
  set maxAiUpscaleSessions(v) { this._maxAiUpscaleSessions = v; }
  get defaultUpscaleAlgorithm() { return this._defaultUpscaleAlgorithm; }
  set defaultUpscaleAlgorithm(v) { this._defaultUpscaleAlgorithm = v; }
  get defaultQualityMode() { return this._defaultQualityMode; }
  set defaultQualityMode(v) { this._defaultQualityMode = v; }
  get supportsDolbyAtmosPassthrough() { return this._supportsDolbyAtmosPassthrough; }
  set supportsDolbyAtmosPassthrough(v) { this._supportsDolbyAtmosPassthrough = v; }
  get supportsDtsHD() { return this._supportsDtsHD; }
  set supportsDtsHD(v) { this._supportsDtsHD = v; }
  get dolbyAtmosTranscode() { return this._dolbyAtmosTranscode; }
  set dolbyAtmosTranscode(v) { this._dolbyAtmosTranscode = v; }
  get maxAudioSampleRateHz() { return this._maxAudioSampleRateHz; }
  set maxAudioSampleRateHz(v) { this._maxAudioSampleRateHz = v; }
  get initialBufferSeconds() { return this._initialBufferSeconds; }
  set initialBufferSeconds(v) { this._initialBufferSeconds = v; }
  get networkThroughputCapMbps() { return this._networkThroughputCapMbps; }
  set networkThroughputCapMbps(v) { this._networkThroughputCapMbps = v; }
  get codecFallback() { return this._codecFallback; }
  set codecFallback(v) { this._codecFallback = v; }
  get supportsVoiceSearch() { return this._supportsVoiceSearch; }
  set supportsVoiceSearch(v) { this._supportsVoiceSearch = v; }
  get supportsDpadNavigation() { return this._supportsDpadNavigation; }
  set supportsDpadNavigation(v) { this._supportsDpadNavigation = v; }
  get supportsTouchInput() { return this._supportsTouchInput; }
  set supportsTouchInput(v) { this._supportsTouchInput = v; }
  get supportsGlobalSearchEntry() { return this._supportsGlobalSearchEntry; }
  set supportsGlobalSearchEntry(v) { this._supportsGlobalSearchEntry = v; }
  get supportsRecommendations() { return this._supportsRecommendations; }
  set supportsRecommendations(v) { this._supportsRecommendations = v; }
  get kind() { return this._kind; }
  set kind(v) { this._kind = v; }
  get displayLabel() { return this._displayLabel; }
  set displayLabel(v) { this._displayLabel = v; }
}

/* ─── WEBOS ──────────────────────────────────────────────────────── */

/**
 * webOS 4 (2018-2019) — older HEVC decoder, no AV1.
 * The C8/B8 series shipped with webOS 4.x. Memory budget is the
 * tightest of any TV target because webOS 4 runs a heavier system UI
 * than later versions.
 */
export function webOs4() {
  return new PlatformProfile({
    kind: 'webos_tv',
    displayLabel: 'LG webOS 4',
    ramBudgetMb: 280,
    inMemoryCatalogCap: 60,
    videoFrameCacheBytes: 32 * 1024 * 1024,
    ramTriggerPurgeMb: 240,
    supports4kDecode: true,
    supports8kDecode: false,
    maxUpscaleWidth: 1920,
    maxAiUpscaleSessions: 0,
    defaultUpscaleAlgorithm: 'lanczos3',
    defaultQualityMode: 'hd_to_4k',
    supportsDolbyAtmosPassthrough: false,
    supportsDtsHD: false,
    dolbyAtmosTranscode: 'transcode_to_eac3',
    maxAudioSampleRateHz: 48000,
    initialBufferSeconds: 4,
    networkThroughputCapMbps: 60,
    codecFallback: 'transcode_on_fly',
    supportsVoiceSearch: true,
    supportsDpadNavigation: true,
    supportsTouchInput: false,
    supportsGlobalSearchEntry: true,
    supportsRecommendations: true,
  });
}

/**
 * webOS 5 (2020) — adds Atmos passthrough via eARC on newer models.
 */
export function webOs5() {
  return new PlatformProfile({
    kind: 'webos_tv',
    displayLabel: 'LG webOS 5',
    ramBudgetMb: 350,
    inMemoryCatalogCap: 80,
    videoFrameCacheBytes: 48 * 1024 * 1024,
    ramTriggerPurgeMb: 300,
    supports4kDecode: true,
    supports8kDecode: false,
    maxUpscaleWidth: 3840,
    maxAiUpscaleSessions: 1,
    defaultUpscaleAlgorithm: 'lanczos3',
    defaultQualityMode: 'hd_to_4k',
    supportsDolbyAtmosPassthrough: true,
    supportsDtsHD: false,
    dolbyAtmosTranscode: 'passthrough',
    maxAudioSampleRateHz: 48000,
    initialBufferSeconds: 5,
    networkThroughputCapMbps: 80,
    codecFallback: 'transcode_on_fly',
    supportsVoiceSearch: true,
    supportsDpadNavigation: true,
    supportsTouchInput: false,
    supportsGlobalSearchEntry: true,
    supportsRecommendations: true,
  });
}

/**
 * webOS 6 (2021+) and webOS 22/23/24 — modern full HD/4K HDR pipeline.
 */
export function webOs6Plus() {
  return new PlatformProfile({
    kind: 'webos_tv',
    displayLabel: 'LG webOS 6+',
    ramBudgetMb: 450,
    inMemoryCatalogCap: 120,
    videoFrameCacheBytes: 64 * 1024 * 1024,
    ramTriggerPurgeMb: 380,
    supports4kDecode: true,
    supports8kDecode: false,
    maxUpscaleWidth: 3840,
    maxAiUpscaleSessions: 2,
    defaultUpscaleAlgorithm: 'fsr_amd',
    defaultQualityMode: 'ai_neural',
    supportsDolbyAtmosPassthrough: true,
    supportsDtsHD: false,
    dolbyAtmosTranscode: 'passthrough',
    maxAudioSampleRateHz: 192000,
    initialBufferSeconds: 6,
    networkThroughputCapMbps: 100,
    codecFallback: 'transcode_on_fly',
    supportsVoiceSearch: true,
    supportsDpadNavigation: true,
    supportsTouchInput: false,
    supportsGlobalSearchEntry: true,
    supportsRecommendations: true,
  });
}

/* ─── TIZEN ──────────────────────────────────────────────────────── */

export function tizen4() {
  return new PlatformProfile({
    kind: 'tizen_tv',
    displayLabel: 'Samsung Tizen 4',
    ramBudgetMb: 320,
    inMemoryCatalogCap: 70,
    videoFrameCacheBytes: 40 * 1024 * 1024,
    ramTriggerPurgeMb: 270,
    supports4kDecode: true,
    supports8kDecode: false,
    maxUpscaleWidth: 1920,
    maxAiUpscaleSessions: 0,
    defaultUpscaleAlgorithm: 'bicubic',
    defaultQualityMode: 'hd_to_4k',
    supportsDolbyAtmosPassthrough: false,
    supportsDtsHD: false,
    dolbyAtmosTranscode: 'transcode_to_eac3',
    maxAudioSampleRateHz: 48000,
    initialBufferSeconds: 4,
    networkThroughputCapMbps: 60,
    codecFallback: 'transcode_on_fly',
    supportsVoiceSearch: true,
    supportsDpadNavigation: true,
    supportsTouchInput: false,
    supportsGlobalSearchEntry: true,
    supportsRecommendations: true,
  });
}

export function tizen5() {
  return new PlatformProfile({
    kind: 'tizen_tv',
    displayLabel: 'Samsung Tizen 5',
    ramBudgetMb: 420,
    inMemoryCatalogCap: 90,
    videoFrameCacheBytes: 56 * 1024 * 1024,
    ramTriggerPurgeMb: 360,
    supports4kDecode: true,
    supports8kDecode: false,
    maxUpscaleWidth: 3840,
    maxAiUpscaleSessions: 1,
    defaultUpscaleAlgorithm: 'lanczos3',
    defaultQualityMode: 'hd_to_4k',
    supportsDolbyAtmosPassthrough: true,
    supportsDtsHD: false,
    dolbyAtmosTranscode: 'passthrough',
    maxAudioSampleRateHz: 48000,
    initialBufferSeconds: 5,
    networkThroughputCapMbps: 80,
    codecFallback: 'transcode_on_fly',
    supportsVoiceSearch: true,
    supportsDpadNavigation: true,
    supportsTouchInput: false,
    supportsGlobalSearchEntry: true,
    supportsRecommendations: true,
  });
}

export function tizen6Plus() {
  return new PlatformProfile({
    kind: 'tizen_tv',
    displayLabel: 'Samsung Tizen 6+',
    ramBudgetMb: 520,
    inMemoryCatalogCap: 130,
    videoFrameCacheBytes: 72 * 1024 * 1024,
    ramTriggerPurgeMb: 440,
    supports4kDecode: true,
    supports8kDecode: true,
    maxUpscaleWidth: 3840,
    maxAiUpscaleSessions: 2,
    defaultUpscaleAlgorithm: 'fsr_amd',
    defaultQualityMode: 'ai_neural',
    supportsDolbyAtmosPassthrough: true,
    supportsDtsHD: true,
    dolbyAtmosTranscode: 'passthrough',
    maxAudioSampleRateHz: 192000,
    initialBufferSeconds: 6,
    networkThroughputCapMbps: 100,
    codecFallback: 'filter_at_source',
    supportsVoiceSearch: true,
    supportsDpadNavigation: true,
    supportsTouchInput: false,
    supportsGlobalSearchEntry: true,
    supportsRecommendations: true,
  });
}

/* ─── ANDROID ────────────────────────────────────────────────────── */

export function androidPhone() {
  return new PlatformProfile({
    kind: 'android_phone',
    displayLabel: 'Android Phone',
    ramBudgetMb: 800,
    inMemoryCatalogCap: 200,
    videoFrameCacheBytes: 128 * 1024 * 1024,
    ramTriggerPurgeMb: 600,
    supports4kDecode: true,
    supports8kDecode: false,
    maxUpscaleWidth: 3840,
    maxAiUpscaleSessions: 1,
    defaultUpscaleAlgorithm: 'waifu2x',
    defaultQualityMode: 'anime_4k',
    supportsDolbyAtmosPassthrough: true,
    supportsDtsHD: true,
    dolbyAtmosTranscode: 'passthrough',
    maxAudioSampleRateHz: 192000,
    initialBufferSeconds: 8,
    networkThroughputCapMbps: 200,
    codecFallback: 'filter_at_source',
    supportsVoiceSearch: true,
    supportsDpadNavigation: false,
    supportsTouchInput: true,
    supportsGlobalSearchEntry: true,
    supportsRecommendations: true,
  });
}

export function androidTv() {
  return new PlatformProfile({
    kind: 'android_tv',
    displayLabel: 'Android TV',
    ramBudgetMb: 1024,
    inMemoryCatalogCap: 250,
    videoFrameCacheBytes: 192 * 1024 * 1024,
    ramTriggerPurgeMb: 800,
    supports4kDecode: true,
    supports8kDecode: false,
    maxUpscaleWidth: 3840,
    maxAiUpscaleSessions: 3,
    defaultUpscaleAlgorithm: 'fsr_amd',
    defaultQualityMode: 'ai_neural',
    supportsDolbyAtmosPassthrough: true,
    supportsDtsHD: true,
    dolbyAtmosTranscode: 'passthrough',
    maxAudioSampleRateHz: 192000,
    initialBufferSeconds: 10,
    networkThroughputCapMbps: 300,
    codecFallback: 'filter_at_source',
    supportsVoiceSearch: true,
    supportsDpadNavigation: true,
    supportsTouchInput: false,
    supportsGlobalSearchEntry: true,
    supportsRecommendations: true,
  });
}

export function fireTv() {
  return new PlatformProfile({
    kind: 'fire_tv',
    displayLabel: 'Fire TV',
    ramBudgetMb: 1024,
    inMemoryCatalogCap: 250,
    videoFrameCacheBytes: 192 * 1024 * 1024,
    ramTriggerPurgeMb: 800,
    supports4kDecode: true,
    supports8kDecode: false,
    maxUpscaleWidth: 3840,
    maxAiUpscaleSessions: 3,
    defaultUpscaleAlgorithm: 'ngX_dlss',
    defaultQualityMode: 'ai_neural_ultra',
    supportsDolbyAtmosPassthrough: true,
    supportsDtsHD: true,
    dolbyAtmosTranscode: 'passthrough',
    maxAudioSampleRateHz: 192000,
    initialBufferSeconds: 10,
    networkThroughputCapMbps: 300,
    codecFallback: 'filter_at_source',
    supportsVoiceSearch: true,
    supportsDpadNavigation: true,
    supportsTouchInput: false,
    supportsGlobalSearchEntry: true,
    supportsRecommendations: true,
  });
}

/* ─── DESKTOP ────────────────────────────────────────────────────── */

export function linuxDesktop() {
  return new PlatformProfile({
    kind: 'linux_desktop',
    displayLabel: 'Linux Desktop',
    ramBudgetMb: 4096,
    inMemoryCatalogCap: 1000,
    videoFrameCacheBytes: 1024 * 1024 * 1024,
    ramTriggerPurgeMb: 3500,
    supports4kDecode: true,
    supports8kDecode: true,
    maxUpscaleWidth: 7680,
    maxAiUpscaleSessions: 8,
    defaultUpscaleAlgorithm: 'ngX_dlss',
    defaultQualityMode: 'desktop_ultra',
    supportsDolbyAtmosPassthrough: true,
    supportsDtsHD: true,
    dolbyAtmosTranscode: 'passthrough',
    maxAudioSampleRateHz: 192000,
    initialBufferSeconds: 12,
    networkThroughputCapMbps: 0,
    codecFallback: 'filter_at_source',
    supportsVoiceSearch: true,
    supportsDpadNavigation: false,
    supportsTouchInput: true,
    supportsGlobalSearchEntry: true,
    supportsRecommendations: true,
  });
}

export function windowsDesktop() {
  return new PlatformProfile({
    kind: 'windows_desktop',
    displayLabel: 'Windows Desktop',
    ramBudgetMb: 4096,
    inMemoryCatalogCap: 1000,
    videoFrameCacheBytes: 1024 * 1024 * 1024,
    ramTriggerPurgeMb: 3500,
    supports4kDecode: true,
    supports8kDecode: true,
    maxUpscaleWidth: 7680,
    maxAiUpscaleSessions: 8,
    defaultUpscaleAlgorithm: 'ngX_dlss',
    defaultQualityMode: 'desktop_ultra',
    supportsDolbyAtmosPassthrough: true,
    supportsDtsHD: true,
    dolbyAtmosTranscode: 'passthrough',
    maxAudioSampleRateHz: 192000,
    initialBufferSeconds: 12,
    networkThroughputCapMbps: 0,
    codecFallback: 'filter_at_source',
    supportsVoiceSearch: true,
    supportsDpadNavigation: false,
    supportsTouchInput: true,
    supportsGlobalSearchEntry: true,
    supportsRecommendations: true,
  });
}

export function macosDesktop() {
  return new PlatformProfile({
    kind: 'macos_desktop',
    displayLabel: 'macOS Desktop',
    ramBudgetMb: 4096,
    inMemoryCatalogCap: 1000,
    videoFrameCacheBytes: 1024 * 1024 * 1024,
    ramTriggerPurgeMb: 3500,
    supports4kDecode: true,
    supports8kDecode: false,
    maxUpscaleWidth: 5120,
    maxAiUpscaleSessions: 6,
    defaultUpscaleAlgorithm: 'ngX_dlss',
    defaultQualityMode: 'desktop_ultra',
    supportsDolbyAtmosPassthrough: true,
    supportsDtsHD: true,
    dolbyAtmosTranscode: 'passthrough',
    maxAudioSampleRateHz: 192000,
    initialBufferSeconds: 12,
    networkThroughputCapMbps: 0,
    codecFallback: 'filter_at_source',
    supportsVoiceSearch: true,
    supportsDpadNavigation: false,
    supportsTouchInput: true,
    supportsGlobalSearchEntry: true,
    supportsRecommendations: true,
  });
}

export function unknown() { return androidTv(); }

/**
 * Auto-detect and return the right profile.
 */
export function profileFor(kind, version) {
  if (kind === 'webos_tv') {
    if (!version || version.major <= 4) return webOs4();
    if (version.major === 5) return webOs5();
    return webOs6Plus();
  }
  if (kind === 'tizen_tv') {
    if (!version || version.major <= 4) return tizen4();
    if (version.major === 5) return tizen5();
    return tizen6Plus();
  }
  switch (kind) {
    case 'android_phone': return androidPhone();
    case 'android_tv': return androidTv();
    case 'fire_tv': return fireTv();
    case 'linux_desktop': return linuxDesktop();
    case 'windows_desktop': return windowsDesktop();
    case 'macos_desktop': return macosDesktop();
    default: return androidTv();
  }
}
