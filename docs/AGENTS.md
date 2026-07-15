# KuroStream - Agent Guide

## Build & Test Commands

| Command | Purpose |
|---------|---------|
| `./gradlew :app:assembleDebug` | Build debug APK |
| `./gradlew :app:lintDebug` | Run Android lint |
| `./gradlew lint` | Run lint across all modules |
| `./gradlew :domain:test` | Run domain unit tests |
| `./gradlew test` | Run all unit tests |
| `./gradlew :app:assembleRelease` | Build release AAB |
| `./gradlew :app:bundleRelease` | Build release bundle |
| `./gradlew detekt` | Run detekt analysis |
| `./gradlew ktlintCheck` | Run ktlint |
| `./gradlew spotlessCheck` | Run spotless |

## Architecture
- Multi-module Clean Architecture (domain -> data -> presentation)
- 12 modules: app, common, domain, data, cache, playback, extensions, plugin-sdk, launcher, torrent, backup, benchmark
- Jetpack Compose for TV UI with Leanback
- Media3/ExoPlayer, libVLC, MPV for playback
- Hilt DI, Room DB, DataStore, Coil for images
- Coroutines + Flow for async

## Optimization Checklist (100 items)
All 100 optimizations have been implemented across:
- UI/Animation (items 1-50): hero crossfade, parallax, glassmorphism, shimmer, focus animations, gradient borders, page indicators, staggered reveal, press feedback, debounced input
- Deep Performance (items 51-70): startup IdleHandler, MappedByteBuffer, adaptive thread pools, kotlinx-serialization, Room WAL, Compose stability, cache sharding, battery-aware scheduling, ProGuard aggressive, LeakCanary, thermal UI throttling
- Network/Streaming (items 71-100): ABR, adaptive bitrate, failover, P2P config, chunked streaming, ConnectionQuality, NetworkOptimizationConfig, WorkManager coalescing, audio session management
