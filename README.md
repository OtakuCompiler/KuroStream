# KuroStream - Premium Android TV Streaming App

A feature-rich, privacy-focused streaming application for Android TV with Trakt.tv integration, voice search, Cast support, and enterprise-grade security.

## 🌟 Features

### Core Streaming
- **Multi-engine playback** - Media3 (ExoPlayer), libVLC, MPV support
- **4K HDR/Dolby Vision** - Full HDR10, HDR10+, Dolby Vision support
- **Offline download** - Encrypted AES-256 downloads with resume support
- **Background playback** - Picture-in-Picture and background audio

### Smart Features
- **Trakt.tv Integration** - Sync watch history, scrobble playback, import watchlist
- **Auto Subtitles** - OpenSubtitles API with smart language detection
- **Voice Search** - Android TV voice search with Alexa-style natural language
- **Watch Party** - Synchronized playback with friends via Firebase
- **New Episode Notifications** - FCM push notifications for tracked shows

### TV-Optimized UI
- **Arctic Fuse Theme** - Custom Material 3 design system for TV
- **Leanback Integration** - Full Leanback launcher, recommendations, voice search
- **Gamepad/Remote Support** - Full D-pad, gamepad, and microphone support
- **Edge-to-Edge & Immersive** - Full screen with system bar control

### Security & Privacy (Fort Knox Grade)
- **Play Integrity API** - Device & app attestation
- **Encrypted Storage** - AES-256 GCM for preferences, SQLCipher for database
- **Certificate Pinning** - TLS 1.3 only with pinned certificates
- **App Check** - Firebase App Check with Play Integrity provider
- **Zero Telemetry** - No analytics, no tracking, no data collection

### Production Ready
- **16KB Page Size** - Android 14+ ready
- **64-bit Only** - ARM64 + x86_64
- **Baseline Profiles** - Optimized startup performance
- **Google Play Compliant** - All 2026 requirements met

## 🏗 Architecture

```
kurostream/
├── app/                    # Main application module
├── domain/                 # Pure Kotlin business logic (multiplatform)
├── data/                   # Repository implementations, database, network
├── playback/               # Playback engines (Media3, VLC, MPV)
├── extensions/             # Stremio, CloudStream, Kodi, TorrServer adapters
├── cache/                  # VOD caching with SQLCipher
├── ui/                     # Shared UI components, Arctic Fuse theme
├── common/                 # Utilities, memory management, optimization
├── config/                 # Build configuration
├── server/                 # Node.js backend (Play Integrity, FCM)
└── baseline-profile/       # Baseline profile generator
```

## 🚀 Quick Start

### Prerequisites
- Android Studio Koala | 2024.1.1+
- JDK 17
- Android SDK 34
- NDK 28.0.13004108
- Firebase project (for FCM, Crashlytics)
- Google Play Console access (for Play Integrity)

### Environment Variables
```bash
# Required for release builds
export UPLOAD_KEYSTORE_PATH=~/keystore.jks
export UPLOAD_KEYSTORE_PASSWORD=your_password
export UPLOAD_KEY_ALIAS=your_alias
export UPLOAD_KEY_PASSWORD=your_password

# Firebase (auto-detected via google-services.json)
# Play Integrity (auto-detected via Play Console)
```

### Build
```bash
# Debug build
./gradlew assembleDebug

# Release build (requires keystore)
./gradlew bundleRelease

# Run tests
./gradlew test

# Lint & static analysis
./gradlew ktlintCheck detekt
```

### Run Server
```bash
cd server
npm install
npm start
# Server runs on http://localhost:3000
```

## 📱 Supported Platforms

| Platform | Min SDK | Target SDK | Status |
|----------|---------|------------|--------|
| Android TV | 24 (7.0) | 34 (14) | ✅ Full Support |
| Google TV | 24 (7.0) | 34 (14) | ✅ Full Support |
| Fire TV | 24 (7.0) | 34 (14) | ✅ Full Support |
| Android Phone/Tablet | 24 (7.0) | 34 (14) | ✅ Basic Support |

## 🔧 Configuration

### API Keys (in `gradle.properties` or environment)
```properties
# Trakt.tv
trakt.client.id=YOUR_CLIENT_ID
trakt.client.secret=YOUR_CLIENT_SECRET

# OpenSubtitles
opensubtitles.api.key=YOUR_API_KEY

# Real-Debrid (optional)
realdebrid.api.key=YOUR_API_KEY

# TMDB
tmdb.api.key=YOUR_API_KEY

# AniList (GraphQL)
anilist.client.id=YOUR_CLIENT_ID
```

### Firebase Setup
1. Create Firebase project
2. Add Android app with package `com.kurostream.app`
3. Download `google-services.json` → `app/`
3. Enable: Cloud Messaging, Crashlytics, App Check (Play Integrity)
4. Copy Server Key for backend

### Play Integrity Setup
1. Go to Play Console → App Integrity
2. Link Cloud Project
3. Enable "Classic" and "Standard" API
4. Add to Cloud project allowed APIs

## 🎮 Voice Search Usage

```
"Play The Office"
"Search for sci-fi movies"
"Show me new episodes of One Piece"
"Skip intro"
"Turn on subtitles"
"Play next episode"
"Add to my list"
```

## 📦 Dependencies

### Core
- Kotlin 2.0.21, Compose BOM 2024.11.00
- Hilt 2.52, KSP 2.0.21-1.0.27
- Media3 1.4.1 (ExoPlayer, Cast, Session)
- libVLC 3.6.0-eap17, MPV 0.38.0

### Security
- Play Integrity 1.4.0
- Firebase App Check (Play Integrity) 17.2.0
- Security Crypto 1.1.0-alpha06
- SQLCipher 4.6.1

### TV & Cast
- Leanback 1.2.0-alpha02
- Cast Framework 21.5.0
- TV Material 1.6.0

### Network
- Retrofit 2.11.0, OkHttp 4.12.0
- Coil 2.7.0 (images)
- Protobuf 3.25.5

## 🧪 Testing

```bash
# Unit tests
./gradlew testDebugUnitTest

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Baseline profile generation
./gradlew :baseline-profile:generateBaselineProfile
```

## 📊 Performance Targets

| Metric | Target |
|--------|--------|
| Cold Start | < 1.5s |
| Frame Drop Rate | < 0.1% |
| Memory (idle) | < 150MB |
| APK Size (64-bit) | < 50MB |
| AAB Size | < 20MB |

## 🛡 Security Checklist

- [x] Play Integrity API integration
- [x] Encrypted SharedPreferences (AES-256-GCM)
- [x] SQLCipher database encryption
- [x] Certificate pinning (TLS 1.3 only)
- [x] Firebase App Check (Play Integrity)
- [x] ProGuard/R8 full obfuscation
- [x] 16KB page size alignment
- [x] 64-bit only native libraries
- [x] FLAG_SECURE on playback
- [x] No debug logging in release
- [x] Network Security Config

## 📋 Google Play 2026 Compliance

| Requirement | Status |
|-------------|--------|
| 16KB Page Size | ✅ |
| 64-bit Only | ✅ |
| Play Integrity | ✅ |
| Target SDK 34 | ✅ |
| Edge-to-Edge | ✅ |
| Predictive Back | ✅ |
| Data Safety Form | ✅ |
| Content Rating | ✅ |
| Privacy Policy | ✅ |

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Run `./gradlew ktlintFormat detektFormat`
4. Commit changes (`git commit -m 'feat: add amazing feature'`)
5. Push to branch (`git push origin feature/amazing-feature`)
6. Open Pull Request

## 📄 License

GNU General Public License v3.0 - see [LICENSE](LICENSE) for details.

## 🙏 Acknowledgments

- [libVLC](https://www.videolan.org/vlc/) - Media playback
- [MPV](https://mpv.io/) - High-quality playback
- [Media3/ExoPlayer](https://github.com/androidx/media) - Streaming engine
- [Trakt.tv](https://trakt.tv/) - Sync API
- [OpenSubtitles](https://www.opensubtitles.com/) - Subtitle API
- [Firebase](https://firebase.google.com/) - Backend services

---

**KuroStream** - Built with ❤️ for the streaming community. No ads, no tracking, just pure streaming.