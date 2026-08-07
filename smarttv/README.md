# KuroStream Smart TV App (webOS / Tizen)

Native HTML5 + WebAssembly app for Samsung Tizen and LG webOS Smart TVs.

## What this fixes

The webOS / Tizen app fixes the well-documented "error occurred when
decoding" / "video cannot be played" / "file is not recognized" crashes
documented in:

  - [Stremio bug #892](https://github.com/Stremio/stremio-bugs/issues/892) — LG TV error decoding
  - [Stremio bug #1776](https://github.com/Stremio/stremio-bugs/issues/1776) — Frequent crashes and buffering
  - [Stremio Tech Update #35](https://blog.stremio.com/stremio-tech-update-35-lg-tv-fixes-and-stremio-web-updated/) — Stremio webOS fixes
  - [Ultimate Guide to Fixing Stremio on LG webOS](https://stremioaddonmanager.org/blog/the-ultimate-guide-to-fixing-stremio-crashing-on-lg-web-os-t-vs-from-root-cause-to-permanent-solution) — Stremio Addon Manager Blog (Feb 2026)
  - [How to Fix Stremio "Video is Not Supported" on WebOS](https://webvator.com/how-to-fix-stremio-error-video-is-not-supported/) — Webvator (Feb 2026)

### Root cause

The LG webOS / Samsung Tizen native MediaSource decoder chokes on:

  1. **HEVC 10-bit** (Main10 / Main10 Still Picture / range extensions)
  2. **HDR10+ dynamic metadata** — the worst offender
  3. **Dolby Vision** P2P streams (DV Profile 8/9 especially)
  4. **DTS-HD / TrueHD Atmos** audio (silently drops to no-audio)
  5. **Multi-audio-track MKV/MP4 files** (crashes on track switch)
  6. **Old containers** (AVI / WMV / RMVB / FLV)

The native player falls back to the system Media Player when it can't
decode, which then throws "File is not supported." and the user is
stuck.

### Our fix

Two layers:

#### 1. `CodecCompatibilityMatrix` (JS port of `:domain/platform/`)

Every candidate stream is checked against the host's profile **before**
it reaches the player. Streams the host can't decode (and can't
transcode) are filtered out at the addon layer; streams that need a
transcode are routed through the FFmpeg.wasm worker.

#### 2. `PlatformMemoryOptimizer`

webOS gives each app ~300-500 MB. Tizen ~400-600 MB. The optimizer
watches `performance.memory` (Chromium-based webOS 5+/Tizen 5+) and
triggers soft/hard cache trims at the per-profile budget. This is what
keeps 4K + Atmos from triggering "this app will now restart to free
up memory" on the OS side.

## Per-version profile matrix

| Platform | RAM budget | 4K decode | Atmos | Default upscale |
|----------|------------|-----------|-------|-----------------|
| webOS 4 (2018-2019) | 280 MB | ✓ | transcode→EAC3 | Lanczos3 (no AI) |
| webOS 5 (2020) | 350 MB | ✓ | passthrough | Lanczos3 |
| webOS 6+ (2021+) | 450 MB | ✓ | passthrough | Waifu2x |
| Tizen 4 (2018) | 320 MB | ✓ | transcode→EAC3 | Bicubic |
| Tizen 5 (2019-2020) | 420 MB | ✓ | passthrough | Lanczos3 |
| Tizen 6+ (2021+) | 520 MB | ✓ | passthrough | Waifu2x |

Same feature set everywhere; only the **budgets** change.

## Building

### webOS

```bash
# Requires webOS TV CLI: npm install -g @webos-tools/cli
ares-package smarttv/webos/ \
    --appinfo smarttv/webos/appinfo.json \
    --out build/
ares-install build/kurostream_1.0.0.ipk -d <tv-name>
```

### Tizen

```bash
# Requires Tizen Studio CLI
tizen package-web \
    -s smarttv/tizen/ \
    -t wgt \
    -o build/
tizen install --pkg build/KuroStream.wgt -s <tv-name>
```

## Source layout

```
smarttv/
├── webapp/
│   ├── index.html              Entry HTML
│   ├── styles.css              Arctic Fuse 3-styled CSS
│   ├── src/
│   │   ├── platform.js         UA-based platform detection
│   │   ├── profiles.js         Per-OS PlatformProfile (mirror of :domain)
│   │   ├── codec-matrix.js     ★ The LG "error decoding" fix
│   │   ├── memory-optimizer.js ★ Per-profile RAM watchdog
│   │   ├── webgl-upscaler.js   Browser-side AI upscaling
│   │   ├── mse-player.js       MSE player with transcode fallback
│   │   └── app.js              Boot + UI wiring
│   └── ffmpeg-wasm/
│       └── worker.js           On-the-fly HEVC→H.264 transcode
├── tizen/
│   └── config.xml              Tizen packaging metadata
└── webos/
    └── appinfo.json            webOS packaging metadata
```
