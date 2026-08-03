# KuroStream — Premium Android TV Streaming App

> The all-in-one streaming experience for your TV: anime, movies, and shows from multiple sources, all in one beautifully crafted interface.

**KuroStream** is a feature-rich, privacy-focused streaming application for Android TV. It combines a custom high-performance playback engine, smart subtitle orchestration, Trakt.tv sync, an extension marketplace, torrent + debrid support, and enterprise-grade security — with zero telemetry.

---

## ✨ Why You'll Love KuroStream

### 🎬 Cinematic Playback
- **KuroVision engine** — custom playback core built on Media3 (ExoPlayer) with OpenGL ES rendering for smooth, hardware-accelerated video
- **4K HDR ready** — HDR10, HDR10+ and Dolby Vision pass-through where supported
- **Quality selection** — choose from available stream qualities (up to 4K) with smart auto-selection
- **Hardware decoding** — automatic codec selection tuned for low-power TV devices
- **Background audio & Picture-in-Picture** — keep watching while you browse
- **Offline downloads** — download episodes and movies to watch without internet, with resume support and AES-256 encrypted storage

### 🧠 Smart Subtitle Engine
- **Auto subtitle matching** — KuroSubtitleEngine finds the best subtitles automatically, with smart language detection (English & Japanese by default)
- **Multiple sources** — OpenSubtitles, SubDL, embedded torrent subtitles, extension providers, HTTP stream subtitles, and local files
- **Smart ranking** — the best-matching subtitle is ranked by quality, download count, and hearing-impaired support
- **Format conversion** — SRT, ASS, VTT, TTML, and PGS handled seamlessly
- **Caching** — downloaded subtitles are cached locally so they never need re-downloading

### 📺 TV-First Experience
- **10-foot UI** — Material 3 design built for the living room (Arctic Fuse theme)
- **Voice Search** — press the mic on your remote and just say it
- **D-pad / Gamepad** — full remote and gamepad navigation, no mouse needed
- **Leanback launcher integration** — recommendations and continue-watching right on the home screen
- **Multiple profiles** — separate favorites, watch history, and continue-watching per family member
- **Home rows** — Continue Watching, Popular, New Releases, Genres, "Because You Watched" recommendations, and seasonal picks

### 📚 Your Library, Your Rules
- **Favorites & My List** — one press to save anything
- **Watch history** — every episode tracked, pick up where you left off
- **Library management** — your shows, movies, and downloads in one place
- **Source Lock** — pin your preferred stream sources per show with fallback rules
- **Backup & restore** — settings and library backups

### 🔗 Trakt.tv Integration
- **Scrobbling** — playback start/pause/resume syncs automatically
- **Watchlist import** — pull your Trakt watchlist straight into KuroStream
- **History sync** — two-way watch history sync

### 🧩 Extension Marketplace
- **Addons & extensions** — expand content sources and subtitle providers from the built-in marketplace
- **Cloud catalog** — purchase & entitlement sync (KuroCloud), so your addons follow your account
- **Plugin SDK** — a clean SDK for third-party extension developers

### ⚡ Torrents & Debrid
- **Built-in torrent streaming** — stream torrents directly without downloading first
- **Debrid integration** — connect your debrid service for instant, high-speed streams

### 🛡️ Security & Privacy (Fort Knox Grade)
- **Encrypted storage** — AES-256 GCM for preferences and tokens
- **Encrypted database** — SQLCipher-backed local database
- **Certificate pinning** — TLS 1.3 only with pinned certificates
- **Play Integrity API** — device & app attestation
- **Firebase App Check** — protects your cloud backend from abuse
- **Zero telemetry** — no analytics, no tracking, no data collection. Ever.

---

## 🏗 Architecture

Multi-module Gradle project with a clean domain layer (Kotlin Multiplatform):

| Module | Purpose |
|---|---|
| `app` | Android TV UI, navigation, theme, sync manager |
| `playback` | KuroVision playback engine (Media3 + OpenGL ES) |
| `domain` | KMP business models, repositories, use cases |
| `data` | Trakt, OpenSubtitles/SubDL, Room database, KuroCloud sync, subtitle engine |
| `cache` | Namespaced disk caching (SimpleCache, LRU eviction, low-RAM aware) |
| `torrent` | Torrent streaming |
| `extensions` | Extension/plugin runtime |
| `marketplace` | Addon marketplace + Firebase backend (functions, Firestore, Storage) |
| `plugin-sdk` | Public SDK for extensions |
| `common` | Shared utilities, theme, memory guards |
| `config` | App configuration |
| `ui` | Shared Compose UI components |
| `baseline-profile` / `benchmark` | Startup optimization & performance tests |

**Tech stack:** Kotlin 2.0.21 · Jetpack Compose · Material 3 · Hilt (DI) · Room (KSP) · Media3 1.4.1 · OpenGL ES · OkHttp · Retrofit · kotlinx.serialization · Coroutines/Flow · Firebase (Firestore, Functions, Storage, FCM, App Check) · Trakt.tv API · OpenSubtitles API · Gradle 9.6.1 / AGP 8.7.0

---

## 🚀 Getting Started

### Requirements
- Android Studio (or a shell with JDK 17+)
- Android SDK with compileSdk 36
- A Firebase project (optional, for cloud features)

### Build
```bash
# On a regular machine
./gradlew :app:assembleDebug

# On this device (Termux + proot Ubuntu — gradlew lives on FUSE storage so run via bash)
cd /sdcard/kurostream
bash gradlew :app:assembleDebug          # or just: kbuild
```

**Build tuning on low-RAM devices:** the Gradle daemon is capped at 2 GB heap, Kotlin daemon at 1.5 GB, workers.max=6, configuration cache + build cache enabled, and all build outputs are relocated to internal storage (`/root/.kurostream-build`). An OOM watchdog (`oom-guard.sh`) kills stale daemons when memory runs low.

### Firebase
```bash
firebase deploy --only firestore:rules   # deploy rules
firebase deploy                          # deploy everything (functions, rules)
```
> Deploying functions requires `npm install` inside `marketplace/functions` first.

---

## 📁 Project Structure

```
kurostream/
├── app/               # Android TV app
├── playback/          # KuroVision playback engine
├── domain/            # KMP domain layer
├── data/              # Repositories, APIs, Room DB, subtitles
├── cache/             # Disk caching
├── torrent/           # Torrent streaming
├── extensions/        # Extension runtime
├── marketplace/       # Addon marketplace + Firebase backend
├── plugin-sdk/        # Extension SDK
├── common/ config/ ui/ baseline-profile/ benchmark/
├── firebase.json      # Firebase deploy config
└── gradle.properties  # Build tuning
```

---

## 📜 License

KuroStream is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License v3** (GPL-3.0-only), as published by the Free Software Foundation. See the LICENSE file for details.

---

*KuroStream — your TV, your rules. No ads, no tracking, just streaming.*
