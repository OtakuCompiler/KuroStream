# Kuro Stream

Android TV / Fire TV anime streaming app with comprehensive plugin compatibility (Stremio, CloudStream, Nuvio, AIOMetadata) and player optimization for 1 GB RAM Fire TV devices.

## Run & Operate

- Build: `cd kuro-stream && ./gradlew assembleDebug`
- Typecheck (kapt/KAPT): `cd kuro-stream && ./gradlew kaptDebugKotlin`
- Full build: `cd kuro-stream && ./gradlew build`

## Stack

- Kotlin 2.0, AGP 8.7, compileSdk/targetSdk 35, minSdk 24
- DI: Hilt 2.51
- UI: Jetpack Compose + `androidx.tv.material3`
- Player: ExoPlayer/Media3 1.3.1 (primary), VLC 3.6.0 (fallback)
- DB: Room 2.6 + Drizzle-style typed DAOs
- Networking: Retrofit 2.11 + OkHttp 4.12 + Jikan v4 / AniList GraphQL
- Logging: Timber (planted in `KuroStreamApp.onCreate`)
- Images: Coil 2 (tuned for 15% memory cache, 100 MB disk cache)

## Where things live

```
kuro-stream/app/src/main/java/com/kurostream/tv/
├── KuroStreamApp.kt           — @HiltAndroidApp, Timber.plant, Coil setup
├── navigation/NavGraph.kt     — all screen routes
├── di/
│   ├── AppModule.kt           — OkHttp, Json, dispatchers, DataStore
│   ├── DatabaseModule.kt      — Room DAOs
│   ├── PlayerModule.kt        — ExoPlayerFactory, TorrentRoutingDataSourceFactory
│   └── RepositoryModule.kt    — AnimeRepository binding + service providers
├── domain/
│   ├── model/                 — Anime, Episode, StreamSource, WatchProgress, …
│   ├── provider/              — AnimeProvider interface, ProviderAggregator, StreamQuality/Type
│   └── repository/AnimeRepository.kt   — interface (18 operations)
├── data/
│   ├── repository/AnimeRepositoryImpl.kt  — concrete impl (Jikan + AIO + Room)
│   ├── metadata/AIOMetadataSystem.kt      — AniList + Jikan cross-platform ID resolver
│   ├── local/
│   │   ├── database/KuroDatabase.kt       — Room entities + DAOs
│   │   └── TorrentSessionTracker.kt       — DataStore-backed session persistence
│   └── adapter/
│       ├── stremio/StremioAdapter.kt
│       ├── cloudstream/CloudStreamPluginLoader.kt
│       └── nuvio/NuvioCompatAdapter.kt
├── core/
│   ├── player/
│   │   ├── SmartPlayerRouter.kt            — ExoPlayer / VLC routing
│   │   └── datasource/TorrentProxyDataSource.kt  — magnet→HTTP proxy
│   ├── perf/MemoryAwareQualitySelector.kt  — RAM-tier quality caps
│   └── plugin/UniversalPlugin.kt           — unified plugin interface
└── ui/  — screens + ViewModels (home, detail, player, search, mylist, …)
```

## Architecture decisions

- `AnimeRepository` interface ← `AnimeRepositoryImpl` bound via `@Provides` in `RepositoryModule` (not `@Binds`/abstract class, preserving the existing `object` module pattern).
- Every `CoroutineDispatcher` injection is qualified with `@IoDispatcher` / `@MainDispatcher` / `@DefaultDispatcher` to prevent ambiguous bindings.
- `TorrentProxyDataSource` is `@Singleton`; `TorrentSessionTracker` is injected into it to persist session state to DataStore on `release()`.
- `MemoryAwareQualitySelector` caps at 1080p for ≤ 1.1 GB RAM and only offers 4K when hardware HEVC/VP9/AV1 decode at 2160p is confirmed.
- `PlayerModule.ExoPlayerFactory` is used everywhere instead of injecting `ExoPlayer` directly (ExoPlayer is not a singleton).

## Product

- Browse trending, seasonal, and popular anime via Jikan (MAL) API — no account required.
- Search across all installed Stremio addons and CloudStream plugins simultaneously.
- Play streams through ExoPlayer (direct/HLS/DASH) or fall back to VLC for unsupported formats and torrents.
- Watch progress, favorites, and continue-watching state persist locally in Room.
- Automatic quality selection based on available RAM at playback start.

## User preferences

- Never use `android.util.Log` — Timber only throughout the codebase.
- No placeholder "TODO" stubs without a companion explanation comment.

## Gotchas

- The Gradle project root is `kuro-stream/`, not the repo root. Always `cd kuro-stream` before running Gradle.
- `@Singleton @Inject constructor` classes are auto-provided by Hilt — no `@Provides` needed unless you need to expose them under an interface.
- `TorrentDataSourceFactory` must be `@Singleton` explicitly (see `RepositoryModule`) so it shares the same `TorrentProxyDataSource` instance as `PlayerModule`.
- `StreamQuality` and `StreamType` exist in both `domain.model` and `domain.provider` — use fully-qualified names when both are in scope.

## Pointers

- See the `pnpm-workspace` skill for the surrounding monorepo structure (the kuro-stream Android project lives *inside* a pnpm workspace but is not itself a pnpm package).
- GitHub Actions workflows live in `.github/workflows/` — SDK 35, JDK 17, AGP 8.7.
