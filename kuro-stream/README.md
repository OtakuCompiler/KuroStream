# Kuro Stream 🎌

An anime-only Android TV player built for Fire TV devices, including low-end 1 GB RAM models. Kuro Stream aggregates streams from multiple providers, handles ExoPlayer / VLC switching automatically, and integrates with AniList for tracking.

---

## Features

| Category | Details |
|---|---|
| **Playback** | ExoPlayer (HLS, DASH, direct) with automatic VLC fallback for incompatible codecs |
| **Providers** | Stremio addon ecosystem + CloudStream plugin loader |
| **Metadata** | Cinemeta · Kitsu · AniList OAuth sync |
| **Trailers** | YouTube trailers via embedded API |
| **Skip timestamps** | AniSkip API — auto-skip intro / outro overlays |
| **Subtitles** | Real-time sync offset controls |
| **Scrobbling** | AniList auto-scrobble with offline queue |
| **Profiles** | Multi-profile with PIN lock |
| **Low-end device** | 8 s buffer cap, 720 p max, 1 GB RAM optimised |
| **D-pad navigation** | Full Jetpack Compose for TV with focus management |

---

## Requirements

| Item | Version |
|---|---|
| Android (min) | 7.0 (API 24) |
| Android (target) | 14 (API 34) |
| JDK | 17 |
| AGP | 8.3.2 |
| Kotlin | 2.0.0 |

---

## Project Structure

```
kuro-stream/
├── app/
│   ├── src/main/
│   │   ├── java/com/kurostream/tv/
│   │   │   ├── core/
│   │   │   │   ├── perf/          # AppPerformanceMonitor
│   │   │   │   └── player/        # SmartPlayerRouter, ExoPlayer, VLC, torrent
│   │   │   ├── data/
│   │   │   │   ├── adapter/       # Stremio + CloudStream adapters
│   │   │   │   ├── local/         # Room DB, profiles, scrobble queue
│   │   │   │   └── remote/        # AniList, Kitsu, Cinemeta, AniSkip APIs
│   │   │   ├── di/                # Hilt modules
│   │   │   ├── domain/
│   │   │   │   ├── model/         # Anime, Episode data classes
│   │   │   │   ├── provider/      # AnimeProvider interface, ProviderAggregator
│   │   │   │   └── repository/    # AnimeRepository interface
│   │   │   ├── navigation/        # NavGraph (Compose Navigation)
│   │   │   └── ui/
│   │   │       ├── anilist/       # AniList tab screen + VM
│   │   │       ├── auth/          # PIN lock screen + VM
│   │   │       ├── components/    # Shared TV UI components
│   │   │       ├── detail/        # Anime detail + VM
│   │   │       ├── discover/      # Search/browse + VM
│   │   │       ├── home/          # Home feed + VM
│   │   │       ├── mylist/        # Watchlist + VM
│   │   │       ├── player/        # Player screen + VM
│   │   │       ├── settings/      # Settings + VM
│   │   │       └── theme/         # TV-optimised dark theme
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml         # Version catalog (single source of truth)
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── .github/
    └── workflows/
        ├── ci.yml                 # Lint + build + unit tests on push/PR
        ├── release.yml            # Signed APK + GitHub Release on tag
        ├── pr-check.yml           # Lint + tests + APK size on PRs
        └── nightly.yml            # Nightly debug build from develop
```

---

## Getting Started

### 1. Clone

```bash
git clone https://github.com/your-org/kuro-stream.git
cd kuro-stream
```

### 2. Open in Android Studio

Open the `kuro-stream/` folder in **Android Studio Hedgehog (2023.1.1)** or later. The project uses the Gradle version catalog (`gradle/libs.versions.toml`) for all dependency versions.

### 3. Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing config)
./gradlew assembleRelease
```

### 4. Install on Fire TV via ADB

```bash
# Enable ADB on Fire TV: Settings > My Fire TV > Developer Options > ADB Debugging
adb connect <firetv-ip>:5555
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## GitHub Actions CI/CD

| Workflow | Trigger | What it does |
|---|---|---|
| `ci.yml` | Push to `main` / `develop`, any PR | Lint → unit tests → debug APK |
| `pr-check.yml` | Pull requests | Lint + tests + APK size comment + secret scan |
| `release.yml` | Git tag `v*.*.*` | Signed release APKs → GitHub Release |
| `nightly.yml` | Daily 02:00 UTC + manual | Nightly debug build from `develop` |

### Release secrets required

Set these in **GitHub → Settings → Secrets and variables → Actions**:

| Secret | Description |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded `.jks` keystore file |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias inside the keystore |
| `KEY_PASSWORD` | Key password |

Generate a keystore:

```bash
keytool -genkey -v \
  -keystore kuro-stream.jks \
  -alias kuro-stream \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Encode for GitHub secret
base64 -w 0 kuro-stream.jks
```

### Creating a release

```bash
git tag v1.0.0
git push origin v1.0.0
```

The `release.yml` workflow will build per-ABI APKs and publish a GitHub Release automatically.

---

## Architecture

The app follows **Clean Architecture** with **MVVM** and **Hilt DI**.

```
UI Layer (Compose for TV)
    ↓ observes StateFlow
ViewModel (HiltViewModel)
    ↓ calls use cases / repositories
Domain Layer (interfaces, models)
    ↓ implemented by
Data Layer (Room, Retrofit, adapters)
```

**Key decisions:**

- `SmartPlayerRouter` selects ExoPlayer or VLC at runtime based on stream type and codec hardware support, so the UI never needs to know which player is active.
- `ProviderAggregator` merges streams from Stremio addons and CloudStream plugins behind a single `AnimeProvider` interface, allowing future providers to be added without UI changes.
- Memory is capped at 8 s buffer and 720 p max quality on devices with ≤ 1.5 GB RAM, detected at runtime via `AppPerformanceMonitor`.
- `ScrobbleQueueManager` persists scrobble events in Room when the device is offline and flushes them on next network availability.
- All secrets (AniList token, PIN hash) are stored in `EncryptedSharedPreferences` via `androidx.security.crypto`.

---

## Memory Optimisations (1 GB RAM)

- ExoPlayer buffer: 8 s min / max (vs default 50 s)
- Image cache capped at 50 MB via Coil
- Resource configs limited to `en` and `ja` locales
- ABI splits — only the matching ABI is installed
- `AppPerformanceMonitor` responds to `onTrimMemory` callbacks and clears caches at `TRIM_MEMORY_RUNNING_CRITICAL`
- Coil image loading uses `allowHardware = false` on low-end devices to avoid GPU memory pressure

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

---

## License

```
Copyright 2024 Kuro Stream Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
