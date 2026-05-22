# Kuro Stream — Fire TV App

A Fire TV / Android TV streaming application with universal plugin support (Stremio, CloudStream, AIOMetadata, Nuvio), optimized for 1GB RAM devices with 4K playback capability.

## Build Requirements

- **JDK 17**
- **Android SDK 34** (compileSdk + targetSdk)
- **Gradle 8.9**
- **Minimum SDK:** 22 (Android 5.1 / Fire TV Gen 1+)

## Quick Start

```bash
# Clone and build debug APK
./gradlew assembleDebug

# Install on connected Fire TV (ADB enabled)
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Features

| Feature | Details |
|---|---|
| Plugin system | Stremio addons, CloudStream plugins, AIOMetadata, Nuvio |
| Player | Media3 ExoPlayer with HLS, DASH, HTTP stream support |
| Memory optimization | Adaptive buffer sizes for 1GB RAM (8–16MB cap) |
| Resolution | Auto-adaptive; default 1080p, 4K on capable hardware |
| UI | Compose TV with D-pad navigation, large text, smooth focus |
| Navigation | D-pad / remote friendly — all focus handled |
| Database | Room with watch history and progress tracking |
| DI | Hilt throughout |

## Project Structure

```
app/src/main/kotlin/com/kurostream/
├── KuroStreamApp.kt          # @HiltAndroidApp entry point
├── core/
│   ├── di/                   # Hilt modules (Network, Player, DB, Coroutines)
│   ├── player/               # ExoPlayer controller + MemoryManager
│   └── plugin/               # PluginManager + adapters (Stremio, CloudStream, AIO)
├── data/
│   ├── model/                # ContentItem, Plugin, StreamSource, WatchHistory
│   ├── repository/           # PluginRepository, HistoryRepository
│   └── source/local/         # Room DB, DAOs, entities
└── ui/
    ├── MainActivity.kt
    ├── navigation/           # NavHost with all routes
    ├── screens/              # home, player, search, detail, settings
    ├── components/           # FocusableCard, LoadingScreen, NoPluginsScreen
    └── theme/                # Dark theme (KuroPrimary #6C63FF)
```

## Adding Plugins

Open Settings → Add Plugin → paste any of:

- Stremio manifest URL: `https://addon.example.com/manifest.json`
- CloudStream plugin URL: `https://cs.example.com/plugin.cs3`
- AIOMetadata endpoint: `https://meta.example.com`

## Memory Tuning (1GB RAM)

`MemoryManager` monitors RAM pressure in real time and adapts:

| Pressure | Max Buffer |
|---|---|
| LOW (>35% free) | 32 MB / 30s |
| MODERATE (20–35%) | 16 MB / 16s |
| HIGH (10–20%) | 12 MB / 12s |
| CRITICAL (<10%) | 8 MB / 8s |

## GitHub Actions

The workflow at `.github/workflows/android_build.yml` builds both debug and release APKs and uploads them as artifacts. It uses:

- `platform-tools,build-tools-34.0.0,platforms;android-34,cmdline-tools;latest` (no spaces, no deprecated `tools`)
- `accept-android-sdk-licenses: true`
- Gradle cache for fast subsequent runs

## Release Signing

Create `keystore.properties` (gitignored) at project root:

```properties
storeFile=../release.keystore
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```
