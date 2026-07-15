# Dependency Graph

## Key External Dependencies

### AndroidX / Jetpack
- Activity, AppCompat, Compose (BOM), Core, Fragment, Lifecycle, Navigation, Room, WorkManager, Hilt

### Media
- Media3 (ExoPlayer): exoplayer, dash, hls, smoothstreaming, ui, session, common, decoder
- libVLC (all)
- MPV (native, optional)
- Oboe (audio)

### Networking
- Retrofit + Moshi converter
- OkHttp + Logging
- Ktor Client (Android, Content Negotiation, Serialization)
- WebRTC (Android)

### Serialization
- Kotlinx Serialization (JSON, Protobuf)
- Moshi + Kotlin

### Database
- Room (Runtime, KTX, Compiler via KSP)
- DataStore (Preferences, Core)

### Dependency Injection
- Hilt (Android, Compiler via KSP, Work, Navigation Compose)

### Coroutines & Flow
- Kotlinx Coroutines (Core, Android, Test)

### Image Loading
- Coil (Compose, SVG, Video)

### Testing
- JUnit, Mockito, MockK, Espresso, Compose UI Test, ArchUnit

### Firebase
- BOM, Auth, Firestore

### Other
- Timber (logging)
- DiskLruCache
- TensorFlow Lite
- Vosk (ASR)
- NanoHTTPD (WebSocket)
- PyTorch Android Lite

## Version Catalog
Managed centrally in `gradle/libs.versions.toml`