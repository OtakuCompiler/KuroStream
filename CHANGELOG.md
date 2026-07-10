# Changelog

All notable changes to KuroStream will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-01-15

### Added
- **Core App**: Android TV app with Leanback launcher, Compose for TV UI
- **Architecture**: Clean Architecture with Domain/Data separation, Hilt DI, Kotlin Flow
- **Player (KuroEngine)**:
  - Multi-backend: MPV (libmpv+libplacebo) → libVLC → Media3 ExoPlayer fallback
  - Hardware decoding: HEVC, AV1, VP9, H.264 with auto-safe fallback
  - HDR: HDR10, HDR10+, Dolby Vision metadata passthrough
  - Audio: TrueHD/DTS-HD MA/E-AC3/AC3 passthrough, 10-band EQ, EBU R128 loudness norm, night mode DRC
  - Subtitles: libass ASS/SSA/SRT/WebVTT with styling
  - AI Upscaling: ESRGAN 2x (PyTorch Mobile)
  - Frame Interpolation: RIFE 24→60/120fps
  - Codec Pack Manager: Dynamic detection/installation
  - External Intent Handler: Deep links, Plex/Jellyfin/Emby registration
- **Content Discovery**: Home (Hero, Continue, Trending, New, Seasonal), Details, Search
- **Multi-Source Search**: AniList, MyAnimeList, Local DB
- **Offline Downloads**: Background Worker, quality selection, progress tracking
- **Watch History & Sync**: Cloud sync via AniList, MAL, Firebase providers
- **Extensions System**: TorrServer, Stremio addons, Kitsu, Cloudstream plugins
- **Plugin SDK**: QuickJS sandbox for community extensions
- **Watch Party**: WebRTC synchronized playback with chat
- **Community Notes**: Timestamped Danmaku-style annotations
- **Accessibility**: TalkBack, Switch Access, High Contrast (WCAG AAA), Reduce Motion, Focus Highlight
- **Benchmarking**: Startup, 4K playback, AI upscaling latency benchmarks with developer UI
- **CI/CD**: GitHub Actions build, test, benchmark, sign, Firebase App Distribution

### Fixed
- Merged 5 codebases (AnimeStream TV, AniStream TV, Anime Stream, StreamPulse, StreamBox) into unified `com.kurostream` namespace
- Resolved duplicate `MediaRepository`/`ProfileRepository` interfaces (legacy moved to `domain.legacy`)
- Renamed playback module from `com.mediaplayer.playback` to `com.kurostream.playback`
- Fixed JNI symbol names for native libraries (MpvRenderer, AudioDSP)
- Fixed ProGuard/R8 rules for all native libraries
- Fixed PlayerViewModel `getPlaybackUrl` Result handling
- Fixed missing `data/build.gradle.kts` and `cache/build.gradle.kts`
- Fixed version catalog entries for all dependencies

### Changed
- Package namespace unified to `com.kurostream.*`
- Domain entities consolidated to `domain.model` (canonical) + `domain.entity` (legacy)
- App module now wires all modules via Hilt adapters
- Manifest permissions expanded for foreground services, media projection, Bluetooth

### Removed
- Duplicate legacy use cases (kept as reference in `domain.legacy.usecase`)
- Placeholder `:downloader` and `:sync` modules from settings (functionality in `:data`)

## [Unreleased]

### Planned
- Series/Episode model in Domain for proper episode lists
- Unified Watch Party implementation (choose StreamPulse CRDT version)
- Unified Community Notes (choose Firestore-backed version)
- Unified ASR Captions (choose Whisper-based version)
- Launcher module ExoPlayer2 → Media3 migration
- Binary size optimization (consolidate ML runtimes)
- Real launcher icon and banner assets
- Native library AAR publishing pipeline

---

*See [RELEASE.md](RELEASE.md) for build instructions and architecture details.*