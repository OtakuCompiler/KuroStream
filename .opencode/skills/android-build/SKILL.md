# Android Build Skill

Optimized Gradle builds for KuroStream Android TV app.

## Build Commands
- `./gradlew assembleDebug -PabiFilters=arm64-v8a --no-daemon -x lint -x test` — Debug APK
- `./gradlew :app:bundleRelease --no-daemon` — Release AAB
- `./gradlew :domain:test --no-daemon -q` — Unit tests
- `./gradlew spotlessCheck --no-daemon -q` — Code style
- `./gradlew detekt --no-daemon -q` — Static analysis

## Memory Optimization
- Gradle heap: `-Xmx1536m -XX:MaxMetaspaceSize=512m -XX:+UseG1GC`
- Kotlin daemon: `-Xmx512m`
- Disable parallel: `-Dorg.gradle.parallel=false`
- Enable caching: `-Dorg.gradle.caching=true`

## Architecture
- Clean Architecture: domain → data → presentation
- Modules: app, common, domain, data, cache, playback, extensions, plugin-sdk, launcher, torrent, backup, benchmark
- Jetpack Compose for TV UI with Leanback
- Hilt DI, Room DB, DataStore, Coil

## Common Fixes
- Torrent module needs GitHub Packages auth
- LibVLC backend requires manual AAR
- LibMPV native build needs manual compilation
