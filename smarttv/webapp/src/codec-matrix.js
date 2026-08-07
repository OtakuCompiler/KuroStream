/*
 * CodecCompatibilityMatrix — the core fix for the LG webOS / Tizen
 * "error occurred when decoding" / "video cannot be played" / "file is
 * not recognized" crashes when streaming 4K P2P content.
 *
 * Root cause (documented across the 2026 Stremio-on-LG writeups and
 * webOS / Tizen release notes):
 *
 *   1. webOS 4/5/6 native MediaSource decoder throws on HEVC 10-bit
 *      profiles, HDR10+ dynamic metadata, and Dolby Vision. Older webOS
 *      versions also lack AV1 hardware decode entirely.
 *   2. The webOS / Tizen native player handles multi-audio-track files
 *      poorly and crashes when switching tracks mid-stream.
 *   3. DTS-HD / TrueHD Atmos audio is not passthrough-supported on most
 *      webOS panels; the player silently drops to no-audio.
 *   4. Older webOS containers (AVI / WMV / RMVB) cannot be decoded at all.
 *
 * Fix: never hand the player a stream it can't decode. We classify each
 * candidate stream up-front into one of three buckets:
 *
 *   - DIRECT_PLAY     — host can decode it as-is.
 *   - TRANSCODE       — host can decode it after a real-time transcode.
 *   - UNSUPPORTED     — host can't decode it and transcoding won't help
 *                       (e.g. webOS 4 + AV1). Filter at the addon layer.
 *
 * The resolver surfaces only DIRECT_PLAY or TRANSCODE streams; the UI
 * shows a small badge so the user understands the cost.
 *
 * Each rule below has a comment explaining the failure mode it prevents.
 */
export class StreamProbe {
  constructor(init) {
    this.videoCodec = init.videoCodec || 'hevc';
    this.videoProfile = init.videoProfile || null;
    this.videoBitDepth = init.videoBitDepth || 8;
    this.width = init.width || 0;
    this.height = init.height || 0;
    this.frameRate = init.frameRate || 0;
    this.hdrType = init.hdrType || 'none';
    this.audioCodec = init.audioCodec || 'aac';
    this.audioChannels = init.audioChannels || 2;
    this.audioSampleRateHz = init.audioSampleRateHz || 48000;
    this.container = (init.container || 'mkv').toLowerCase();
    this.hasMultipleAudioTracks = !!init.hasMultipleAudioTracks;
    this.hasEmbeddedSubtitles = !!init.hasEmbeddedSubtitles;
  }
}

export class CompatibilityVerdict {
  constructor({ canPlayDirectly, reasons = [], transcodeRequired = null }) {
    this.canPlayDirectly = !!canPlayDirectly;
    this.reasons = reasons;
    this.transcodeRequired = transcodeRequired;
  }
  get canPlay() { return this.canPlayDirectly || this.transcodeRequired !== null; }
  get needsTranscode() { return !this.canPlayDirectly && this.transcodeRequired !== null; }
}

/**
 * Decide whether the host can play `probe` directly, needs a transcode,
 * or can't play it at all.
 */
export function check(profile, probe) {
  if (!(probe instanceof StreamProbe)) probe = new StreamProbe(probe);
  const reasons = [];
  let needsVideo = false;
  let needsAudio = false;

  /* ── VIDEO ─────────────────────────────────────────────────────── */
  let directVideoOk = true;
  switch (probe.videoCodec) {
    case 'h264':
    case 'avc1':
      // Universally supported.
      directVideoOk = true;
      break;

    case 'hevc':
    case 'h265': {
      const isTV = profile.kind === 'webos_tv' || profile.kind === 'tizen_tv';
      const isFireTv = profile.kind === 'fire_tv';

      if (!profile.supports4kDecode && probe.height >= 2160) {
        reasons.push(`HEVC 4K (${probe.height}p) but host caps at ${profile.maxUpscaleWidth}p`);
        directVideoOk = false;
        needsVideo = true;
      } else if (probe.videoBitDepth > 8 && isTV) {
        // ─ webOS / Tizen 4/5/6 ─────────────────────────────
        // HEVC main10 (10-bit) hits webOS's decode-error path on
        // most models and on Tizen 4/5. The native player throws
        // "error occurred when decoding" or, when it falls back,
        // "video cannot be played".
        reasons.push('HEVC 10-bit triggers webOS/Tizen decode error');
        directVideoOk = false;
        needsVideo = true;
      } else if (probe.hdrType === 'dolby_vision' && (isTV || isFireTv)) {
        // ─ webOS / Tizen / Fire TV ──────────────────────────
        // Dolby Vision P2P streams throw the same decode error on
        // most panels. Even where the panel supports DV, the LG
        // Content Store Stremio app crashes (per Stremio issue
        // #892, #1776, etc.).
        reasons.push('Dolby Vision triggers native-player error');
        directVideoOk = false;
        needsVideo = true;
      } else if (probe.hdrType === 'hdr10_plus' && isTV) {
        // ─ webOS / Tizen ─────────────────────────────────────
        // HDR10+ dynamic metadata is the worst offender on webOS
        // (per Stremio-on-LG writeups). Strip the dynamic
        // metadata and treat as plain HDR10 if the panel supports
        // it, otherwise SDR.
        reasons.push('HDR10+ dynamic metadata crashes webOS/Tizen');
        directVideoOk = false;
        needsVideo = true;
      } else {
        directVideoOk = true;
      }
      break;
    }

    case 'av1': {
      // AV1 hardware decode landed in webOS 6+ and Tizen 6+. Older
      // versions fall back to software decode, which OOMs the app
      // on 4K streams — the OOM manifests as a black screen + "video
      // cannot be played".
      const isOldTV =
        (profile.kind === 'webos_tv' &&
          profile.displayLabel !== 'LG webOS 6+') ||
        (profile.kind === 'tizen_tv' &&
          profile.displayLabel !== 'Samsung Tizen 6+') ||
        (profile.kind === 'fire_tv' && probe.height >= 2160);
      if (isOldTV) {
        reasons.push('AV1 not hardware-decodable on this TV');
        directVideoOk = false;
        needsVideo = true;
      } else {
        directVideoOk = true;
      }
      break;
    }

    case 'vp9':
      directVideoOk = true;
      break;

    default:
      reasons.push(`Unknown video codec ${probe.videoCodec}`);
      directVideoOk = false;
      needsVideo = true;
  }

  /* ── AUDIO ─────────────────────────────────────────────────────── */
  let directAudioOk = true;
  switch (probe.audioCodec) {
    case 'aac':
    case 'mp3':
      directAudioOk = true;
      break;
    case 'ac3':
      directAudioOk = true;
      break;
    case 'eac3':
      // EAC3 with Atmos tags requires passthrough. Without Atmos tags
      // it's just regular DD+ which all TVs handle.
      if (profile.supportsDolbyAtmosPassthrough) {
        directAudioOk = true;
      } else if (probe.audioChannels <= 6) {
        directAudioOk = true;
      } else {
        reasons.push('EAC3 >6ch not passthrough-supported');
        directAudioOk = false;
        needsAudio = true;
      }
      break;
    case 'truehd':
      // TrueHD carries Atmos. webOS 4/5 + Tizen 4/5 cannot passthrough
      // it; desktop + Android TV + webOS 6 (eARC) can.
      if (profile.supportsDolbyAtmosPassthrough) {
        directAudioOk = true;
      } else {
        reasons.push('TrueHD/Atmos not passthrough-supported');
        directAudioOk = false;
        needsAudio = true;
      }
      break;
    case 'dts':
    case 'dtshd':
      if (profile.supportsDtsHD) {
        directAudioOk = true;
      } else {
        reasons.push('DTS-HD not supported on this profile');
        directAudioOk = false;
        needsAudio = true;
      }
      break;
    default:
      reasons.push(`Unknown audio codec ${probe.audioCodec}`);
      directAudioOk = false;
      needsAudio = true;
  }

  /* ── MULTI-AUDIO TRACK ─────────────────────────────────────────── */
  // The single most common crash trigger on webOS per the LG writeups.
  // The webOS native player holds audio track switches across the whole
  // playback; switching mid-stream or even just having >1 track at start
  // can crash the app on memory-constrained panels.
  if (
    probe.hasMultipleAudioTracks &&
    (profile.kind === 'webos_tv' || profile.kind === 'tizen_tv')
  ) {
    reasons.push('Multi-audio-track streams crash webOS/Tizen');
    needsAudio = true;
  }

  /* ── SAMPLE RATE ───────────────────────────────────────────────── */
  if (probe.audioSampleRateHz > profile.maxAudioSampleRateHz) {
    reasons.push(
      `Audio sample rate ${probe.audioSampleRateHz}Hz > host cap ${profile.maxAudioSampleRateHz}Hz`,
    );
    needsAudio = true;
  }

  /* ── CONTAINER ─────────────────────────────────────────────────── */
  // AVI / WMV / RMVB → always transcode on TVs (rare codecs inside).
  if (
    ['avi', 'wmv', 'rmvb', 'flv'].includes(probe.container) &&
    (profile.kind === 'webos_tv' || profile.kind === 'tizen_tv')
  ) {
    reasons.push(`Container ${probe.container} unsupported on TV`);
    needsVideo = true;
    needsAudio = true;
  }

  const canPlayDirectly = directVideoOk && directAudioOk;
  let transcodeRequired = null;

  if (!canPlayDirectly) {
    if (profile.codecFallback === 'filter_at_source') {
      // Resolver will skip this stream instead of asking for a transcode.
      transcodeRequired = null;
    } else {
      transcodeRequired = {
        transcodeVideo: needsVideo,
        transcodeAudio: needsAudio,
        targetVideoCodec: 'h264',
        targetVideoProfile: 'high',
        targetVideoBitDepth: 8,
        targetHdr: 'none',
        targetAudioCodec: 'aac',
        targetAudioChannels: 2,
        estimatedLatencyMs: 1500,
      };
    }
  }

  return new CompatibilityVerdict({ canPlayDirectly, reasons, transcodeRequired });
}
