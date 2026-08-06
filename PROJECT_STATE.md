# KuroStream — Project State

**Last updated:** 2026-08-06
**Version:** v1.1.0-alpha (in development)

---

## Architecture

```
kurostream/
├── app/                  Android TV app (Compose UI, Hilt, Media3, Arctic Fuse 3)
│   └── src/main/java/com/kurostream/app/
│       ├── ui/arctic/    Arctic Fuse 3 design system (22 composables)
│       ├── ui/screens/   35 screen composables
│       ├── player/       Media3 PlayerActivity + SubtitleManager
│       ├── security/     AppSecurityManager, PlayIntegrity
│       └── navigation/   TvNavHost, Routes
├── domain/               Kotlin domain models, repository interfaces, enums
│   └── src/commonMain/   KMP-safe models (MediaItem, Profile, Settings)
├── data/                 Repositories, DAOs, Retrofit APIs, subtitle providers
│   ├── metadata/         8 metadata providers (AniList, Kitsu, MAL, TMDB, TVDB, IMDb, ...)
│   ├── subtitle/         4 providers (OpenSubtitles, SubDL, Addic7ed, Podnapisi)
│   └── local/preferences Preferences DataStore + Proto DataStore
├── playback/             KuroVision engine, EnhancedUpscaleEngine, OpenGL renderer
├── torrent/              jlibtorrent session + TorrentStreamServer (NanoHTTPD)
├── extensions/           CloudStream/Kodi/Jellyfin/Plex/Stremio adapters + sandbox
├── plugin-sdk/           Extension API, SafeSystem/SafeRuntime, ExtensionManager
├── marketplace/          Skin marketplace, KuroCloud sync, checkout flow
├── cache/                Disk cache, VOD cache manager
├── common/               Memory governor, BufferPool, ThermalGuard
├── ui/                   Shared UI components library
├── config/               Build config, feature flags
├── kurohub/              Web dashboard (TanStack Start + Cloudflare Workers)
└── server/               DEPRECATED — dead code, 36 broken TS deps, never imported
```

---

## Feature Status

| Feature | Status | Notes |
|---------|--------|-------|
| Android TV launcher | ✅ Done | LEANBACK_LAUNCHER on MainActivity |
| Arctic Fuse 3 UI | ✅ Done | 22 composables, sidebar, hero, widgets, info panel |
| Media3 playback | ✅ Done | ExoPlayer wired, subtitle manager |
| Dolby Atmos passthrough | ✅ Done | Conditional audio sink + logAudioCapabilities |
| GPU upscaling | ✅ Done | Lanczos3, Bicubic, CAS shaders via OpenGL ES 2.0 |
| Multi-source metadata | ✅ Done | 8 providers with fan-out + fallback |
| Subtitle providers | ✅ Done | OpenSubtitles, SubDL, Addic7ed, Podnapisi |
| Torrent streaming | ✅ Done | jlibtorrent + local HTTP server |
| Extension sandbox | ✅ Done | DexClassLoader delegation, package blocklist |
| Marketplace | ✅ Done | Skin purchase + checkout intent |
| Cross-device sync | ✅ Done | Cloudflare Workers + D1 + Firestore |
| Firebase FCM | ✅ Done | App Check, Firestore, Messaging |
| Settings persistence | ✅ Done | Preferences DataStore, 37 fields |
| PIN security | ✅ Done | BCrypt cost 12 |
| Signature verification | ✅ Done | SHA-256 allowlist |
| KuroHub web dashboard | ✅ Done | TanStack Start + Cloudflare Workers |
| Test coverage | ⚠️ Partial | ~1.6%, needs dedicated pass |
| TF Lite + ONNX consolidation | ⏳ TODO | Both included, adds ~6-10 MB |
| Range requests in torrent server | ⏳ TODO | Seeking restarts download |
| Release signing allowlist | ⏳ TODO | ALLOWED_FINGERPRINTS empty |
| Kitsu AniList/TMDB direct filter | ⚠️ Limited | Falls back to text search |

---

## Build Status

| Target | Status | Command |
|--------|--------|---------|
| Kotlin compile (debug) | ✅ Pass | `bash gradlew :app:compileDebugKotlin --no-daemon --max-workers=1` |
| Process resources (aarch64) | ✅ Pass | `bash gradlew :app:processDebugResources -Pandroid.aapt2FromMavenOverride=/opt/aapt2-qemu/aapt2 --no-daemon --max-workers=1` |
| Full debug APK (aarch64) | ⏳ Slow | ~15-20 min with QEMU; use CI for fast verification |
| CI (ubuntu-latest x86_64) | ✅ Pass | https://github.com/OtakuCompiler/KuroStream/actions |
| detekt | ✅ Pass | `bash gradlew detektAll` |

---

## Dependencies (highlights)

| Library | Version | Purpose |
|---------|---------|---------|
| AndroidX Media3 | 1.4.1 | Playback engine |
| Jetpack Compose BOM | 2024.11.00 | UI toolkit |
| Hilt | 2.52 | Dependency injection |
| Retrofit + Kotlinx Serialization | — | Network layer |
| jlibtorrent | latest | Torrent engine |
| NanoHTTPD | 2.3.1 | Local HTTP streaming server |
| BCrypt (jbcrypt) | 0.4 | PIN hashing |
| jsoup | 1.17.2 | HTML scraping (subtitles) |
| Room + KSP | 2.6.1 | Local database |
| Firebase BOM | 33.7.0 | Firestore, FCM, App Check |
| Cloudflare Workers | — | Cross-device sync API |

---

## Known Issues

1. **AAPT2 on aarch64** — needs QEMU wrapper, 5-10× slower than native. Documented in BUILD.md.
2. **Test coverage low** — ~1.6%, needs dedicated pass.
3. **ONNX + TF Lite both included** — adds ~6-10 MB native libs. Consider consolidating.
4. **Torrent server Range requests** — not implemented; seeking restarts download.
5. **Release signing allowlist** — `ALLOWED_FINGERPRINTS` is empty; populates from CI signing config.
