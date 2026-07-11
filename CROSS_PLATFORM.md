# KuroStream Cross-Platform Architecture

## Overview

KuroStream is architected as a Kotlin Multiplatform (KMP) project to support Android TV, LG webOS, Samsung Tizen, and future platforms from a single shared codebase.

## Project Structure

```
KuroStream/
├── app/                 # Android TV application (Compose for TV)
├── domain/              # KMP module - shared business logic
│   ├── commonMain/      # Platform-agnostic code
│   ├── androidMain/     # Android implementations
│   └── jsMain/          # webOS/Tizen stub implementations
├── data/                # Android-only data layer (Room, DataStore, Retrofit)
├── playback/            # Android playback engine (KuroEngine, Media3, libVLC, MPV)
├── cache/               # Android caching layer
├── common/              # Android utilities
├── extensions/          # Android extension system
├── launcher/            # Android TV launcher integration
├── benchmark/           # Performance benchmarks
├── torrent/             # Torrent client (libtorrent4j)
├── backup/              # Backup/restore functionality
├── webosApp/            # webOS placeholder (Kotlin/JS)
└── tizenApp/            # Tizen placeholder (Kotlin/JS)
```

## Platform Abstraction Layer

### Core Interfaces (domain/commonMain)

Located in `domain/src/commonMain/kotlin/com/kurostream/domain/platform/`:

| Interface | Purpose |
|-----------|---------|
| `PlatformPlayer` | Video playback control (play, pause, seek, speed, volume) |
| `PlatformStorage` | File I/O, preferences, key-value storage |
| `PlatformNetwork` | HTTP client, WebSocket, downloads, connection quality |
| `PlatformUI` | Navigation, toasts, focus, display refresh rate, immersive mode |
| `PlatformFactory` | Factory for creating platform-specific implementations |

### Android Implementations (domain/androidMain)

Located in `domain/src/androidMain/kotlin/com/kurostream/domain/platform/`:

| Class | Implementation |
|-------|----------------|
| `AndroidPlayer` | MediaPlayer-based with ExoPlayer fallback |
| `AndroidStorage` | File I/O + DataStore/SharedPreferences |
| `AndroidNetwork` | OkHttp-based with WebSocket support |
| `AndroidUI` | Android TV framework APIs |
| `PlatformFactory` | Initializes all Android implementations |

### JS Stubs (domain/jsMain)

Located in `domain/src/jsMain/kotlin/com/kurostream/domain/platform/`:

- `WebPlayer`, `WebStorage`, `WebNetwork`, `WebUI` - Throw `UnsupportedOperationException` with clear messages
- `PlatformFactory` - Returns stub implementations

## Adding a New Platform

### 1. Create Platform Module

```kotlin
// newPlatform/build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    js(IR) {  // or native for native platforms
        browser()
        nodejs()
        binaries.executable()
    }
    
    sourceSets {
        val main by getting {
            dependencies {
                implementation(project(":domain"))
            }
        }
    }
}
```

### 2. Implement Platform Services

Create implementations in `newPlatform/src/main/kotlin/`:

```kotlin
// newPlatform/src/main/kotlin/com/kurostream/newplatform/NewPlatformPlayer.kt
package com.kurostream.newplatform

import com.kurostream.domain.platform.PlatformPlayer
import com.kurostream.domain.platform.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow

class NewPlatformPlayer : PlatformPlayer {
    // Implement using platform's native APIs
    // webOS: webOS Media API or HTML5 Video
    // Tizen: Tizen TV Media API or HTML5 Video
    // Native: Platform-specific media framework
}
```

### 3. Create Platform Factory

```kotlin
// newPlatform/src/main/kotlin/com/kurostream/newplatform/PlatformFactory.kt
package com.kurostream.newplatform

import com.kurostream.domain.platform.PlatformFactory

actual class PlatformFactory {
    actual fun createPlayer(): PlatformPlayer = NewPlatformPlayer()
    actual fun createStorage(): PlatformStorage = NewPlatformStorage()
    actual fun createNetwork(): PlatformNetwork = NewPlatformNetwork()
    actual fun createUI(): PlatformUI = NewPlatformUI()
    
    actual companion object {
        actual fun getInstance(): PlatformFactory = PlatformFactory()
    }
}
```

### 4. Update Settings

Add to `settings.gradle.kts`:
```kotlin
include(":newPlatform")
```

### 5. Platform-Specific Entry Point

```kotlin
// newPlatform/src/main/kotlin/com/kurostream/newplatform/Main.kt
fun main() {
    // Initialize platform factory
    val factory = PlatformFactory.getInstance()
    
    // Initialize shared business logic
    val repository = MediaRepository(factory.createNetwork(), factory.createStorage())
    val useCase = GetHomeRows(repository)
    
    // Start UI
    launchUI()
}
```

## Platform-Specific Notes

### LG webOS
- **Language**: JavaScript/TypeScript (Enact framework) or Kotlin/JS
- **Player**: HTML5 `<video>` with MSE/EME or webOS Media API
- **Storage**: webOS File API, localStorage, IndexedDB
- **Network**: Fetch API, WebSocket API
- **UI**: Enact (React-based) or plain HTML/CSS/JS
- **Packaging**: `.ipk` via `ares-package`

### Samsung Tizen
- **Language**: JavaScript/TypeScript (TAU framework) or Kotlin/JS
- **Player**: HTML5 `<video>` with MSE/EME or Tizen AVPlay API
- **Storage**: Tizen FileSystem API, localStorage
- **Network**: Fetch API, WebSocket API, Tizen Network API
- **UI**: TAU (jQuery-based) or React
- **Packaging**: `.wgt` via Tizen Studio CLI

### Future Platforms
- **Apple tvOS**: Kotlin/Native + SwiftUI bridge
- **Fire TV**: Same as Android TV (already supported)
- **Android Automotive**: Same as Android TV
- **Desktop**: Kotlin/JVM with Compose for Desktop

## Shared Code Guidelines

### ✅ DO put in commonMain:
- Domain entities (`MediaItem`, `AnimeDetails`, etc.)
- Repository interfaces (`MediaRepository`, `ProfileRepository`)
- Use cases (`GetHomeRows`, `SearchAnime`, `GetAnimeDetails`)
- Platform interfaces (`PlatformPlayer`, `PlatformStorage`, etc.)
- Business logic, validation, transformations
- Serialization models (`@Serializable` data classes)

### ❌ DON'T put in commonMain:
- Android-specific APIs (Context, Activity, View, Room, DataStore)
- UI frameworks (Compose, Views, Jetpack libraries)
- Platform-specific implementations
- Native libraries (JNI, .so files)

## Build Commands

```bash
# Build all targets
./gradlew build

# Build Android only
./gradlew :app:assembleDebug

# Build domain for all targets
./gradlew :domain:compileKotlinAndroid :domain:compileKotlinJs

# Run tests
./gradlew :domain:test

# Check JS compilation
./gradlew :domain:compileKotlinJs :webosApp:compileKotlinJs :tizenApp:compileKotlinJs
```

## CI/CD Integration

The GitHub Actions workflow (`.github/workflows/ci.yml`) builds and tests all targets:

```yaml
jobs:
  android:
    runs-on: ubuntu-latest
    steps:
      - ./gradlew :app:assembleDebug :domain:test
      
  js:
    runs-on: ubuntu-latest
    steps:
      - ./gradlew :domain:compileKotlinJs :webosApp:compileKotlinJs :tizenApp:compileKotlinJs
```

## Migration Checklist for New Platform

- [ ] Create platform module with KMP build config
- [ ] Implement `PlatformPlayer` with native media APIs
- [ ] Implement `PlatformStorage` with platform file/prefs APIs
- [ ] Implement `PlatformNetwork` with platform HTTP/WebSocket
- [ ] Implement `PlatformUI` with platform UI/navigation
- [ ] Create `PlatformFactory` returning implementations
- [ ] Add module to `settings.gradle.kts`
- [ ] Create entry point (`main` function)
- [ ] Add CI/CD job for platform
- [ ] Test shared domain logic works correctly
- [ ] Document platform-specific quirks/limitations

## Performance Targets (Cross-Platform)

| Metric | Target |
|--------|--------|
| Cold start | < 1.0s |
| 4K playback start | < 1.0s |
| Seek latency | < 50ms |
| RAM (4K P2P) | < 80MB |
| CPU avg | < 20% |
| Frame drops | 0.0% |
| Quality switch | < 20ms |

## Resources

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [webOS Developer Guide](https://webostv.developer.lge.com/)
- [Tizen TV Developer Guide](https://developer.tizen.org/development/tv-development)
- [KMP Sample Projects](https://github.com/kotlin-hands-on/multiplatform-mobile)

---

*Last updated: 2026*
*KuroStream v1.0.0+*