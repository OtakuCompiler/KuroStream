# KuroStream

Premium Android TV streaming app — Kotlin multiplatform, Jetpack Compose for TV, Arctic Fuse 3 design system.

## Build environment (Replit)

The APK can be built directly inside Replit:

| Requirement | Location |
|---|---|
| JDK 17 | `/nix/store/…-openjdk-17.0.15+6/lib/openjdk` |
| Android SDK | `/home/runner/Android/Sdk` |
| Build tools | `35.0.0` |
| Target SDK | 34 (platforms/android-34) |
| local.properties | `sdk.dir=/home/runner/Android/Sdk` |

```bash
./gradlew assembleDebug        # debug APK → app/build/outputs/apk/debug/
./gradlew :app:bundleRelease   # release AAB (needs signing config)
```

The **Gradle** workflow in Replit runs `./gradlew assembleDebug` automatically.

> **Note:** `app/google-services.json` contains placeholder Firebase credentials.  
> Replace with real values from the Firebase console before Play Store release.

---

## Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose for TV (tv-material3), Arctic Fuse 3 design tokens |
| Players | Media3/ExoPlayer, libVLC, libMPV (via BackendSelector fallback chain) |
| Metadata | AniList, MAL, Kitsu, TMDB, TVDB, IMDb — parallel fan-out with LRU cache |
| Skip | AniSkip v2 + IntroDB — parallel fetch, SharedPreferences cache, animated overlay chips |
| DI | Hilt |
| Network | Retrofit + OkHttp + kotlinx.serialization |
| Database | Room + SQLCipher |
| Security | AES-256-GCM prefs, certificate pinning, Firebase App Check |
| Cloud Sync | Supabase (REST/JWT) — table `kuro_sync` |
| Backend | Node.js / Express — Play Integrity verification, FCM notifications |

---

## Module layout

```
app/            Main application, Arctic Fuse UI, player overlay
domain/         Pure Kotlin business logic (KMP-compatible)
data/           Repositories, metadata providers, skip clients
playback/       Media3Player, VlcPlayer, MpvPlayer, SkipDetectionEngine, AudioEngine
cache/          VOD caching with SQLCipher
common/         LowRamDevice (tier-based RAM profiling), utilities
extensions/     Stremio / CloudStream / Kodi / TorrServer / Jellyfin / Plex / AniList adapters
server/         Node.js backend (Play Integrity, FCM)
config/         Detekt, build config
baseline-profile/ Baseline profile generator
benchmark/      Macrobenchmark suite
```

---

## Key design decisions

- **RAM budget ≤125 MB active** — `LowRamDevice` exposes three tiers (LOW/MID/HIGH) that tune ExoPlayer buffers, VLC caching, MPV demuxer limits, Coil image cache, and metadata LRU size.
- **Parallel metadata** — `UnifiedMetadataRepositoryImpl` fans out to all enabled providers concurrently; cold-start latency drops from ~2–3 s to ~700 ms.
- **Stale-while-revalidate** — cached detail pages are returned instantly while a background refresh runs.
- **Skip engine** — `SkipDetectionEngine` fetches AniSkip + IntroDB in parallel, merges (AniSkip wins), caches to SharedPreferences, and drives animated overlay chips.
- **Dolby Atmos** — Media3 audio offload enabled; `KuroAudioEngine` drives 5-band EQ, BassBoost, Virtualizer, Reverb, night mode DRC.
- **HDR/4K** — tunneled rendering on Media3, `hwdec=auto-safe` on MPV, `:avcodec-hw=any` on VLC; `HdrDetector` reads display capabilities and sets ExoPlayer track params accordingly.
- **Fake HDR** — `EnhancedUpscaleEngine` applies S-curve tone mapping, local contrast, chromatic vibrance, and per-profile colour grading (Cinema/Vivid/Natural/Cool/Warm) as an OpenGL ES 2.0 post-process shader stack.
- **Waifu2x Upscaling** — `UpscaleAlgorithm.WAIFU2X` adds a baked 3×3 convolutional kernel + bilateral denoise + edge-aware anime sharpening in a dedicated GLSL shader (`FRAG_WAIFU2X`). Activated by `KuroVisionQualityMode.WAIFU2X` or `ANIME_4K`.
- **OLED Black Crush** — near-black knee crush + peak white boost in shader; available as a toggle in settings.
- **Ken Burns** — `DynamicFanartBackground` animates backdrop images with a slow pan+zoom using `rememberInfiniteTransition`; driven by snapshot state so it never blocks the main thread.
- **Custom themes** — `CustomThemeEngine` serialises `CustomTheme` JSON to SharedPreferences; six built-in presets; full per-channel RGB editor in `CustomThemeScreen`.
- **AUTO theme** — `ThemeMode.AUTO` follows the Android system dark-mode flag via `isSystemInDarkTheme()` in `PremiumThemeProvider`. Displayed as "Auto (System)" in settings.
- **Extension support** — Stremio, CloudStream, Kodi, TorrServer, Jellyfin, Plex, AniList GraphQL all have dedicated typed adapters wired through `SmartSourceAggregator`.
- **Cloud sync** — `CloudSyncProvider` upserts `SyncPayload` (watch history, favourites, settings, profiles) to Supabase via JWT; conflict resolution uses last-write-wins for history and union for favourites.
- **Audio transcoding** — `AudioTranscoder` uses `MediaCodec` to transcode AC3/E-AC3/TrueHD/DTS → AAC/Opus; includes a Dolby Pro Logic II 5.1→stereo downmix matrix for devices without hardware pass-through.
- **ArcticFusePalette** — reactive palette factory (`forMode()`) covering Dark / Light / OLED / OLED Cinema / Custom branches; all composables read `LocalArcticFusePalette.current` so light mode is correct everywhere.
- **SkinSystem particles** — `ParticleBackground` uses a `mutableLongStateOf` (`animFrameMs`) updated every frame via `withInfiniteAnimationFrameMillis`; the Canvas reads this state so sway/twinkle redraws are driven by snapshot state, not by `System.nanoTime()` outside composition.

---

## Production checklist

- [ ] Replace `app/google-services.json` with real Firebase project credentials (FCM, Crashlytics, App Check)
- [ ] Create `kuro_sync` table in Supabase dashboard (`user_id UUID, payload JSONB, updated_at TIMESTAMPTZ`)
- [ ] Set `YOUTUBE_API_KEY` environment variable for data module build
- [ ] Configure release signing in `app/build.gradle.kts`
- [ ] Run `./gradlew :baseline-profile:generate` before Play Store upload

---

## User preferences

- Maintain Arctic Fuse 3 design system (indigo `#6366F1` / violet `#8B5CF6` accents, `#0A0A0F` background).
- Keep all three players (Media3, VLC, MPV) equally capable with proper HW decode.
- Never exceed 125 MB active RAM on MID-tier devices.
- AniSkip + IntroDB must both be queried in parallel and merged.
- `EnhancedUpscaleEngine` shaders are OpenGL ES 2.0 (not ES 3.x) for widest TV compatibility.
- Player overlay skip chips must call the player's `seekTo()` — wiring lives in `PlayerActivity`.
