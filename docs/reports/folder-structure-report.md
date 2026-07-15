# Folder Structure Report

## Root Structure
```
kurostream/
├── app/                 # Main Android application
├── domain/              # Domain layer (Kotlin Multiplatform)
├── data/                # Data layer (Android)
├── common/              # Common utilities (Android)
├── core-common/         # Core common (Kotlin Multiplatform)
├── core-platform/       # Core platform (Kotlin Multiplatform)
├── playback/            # Media playback (Android)
├── plugin-sdk/          # Plugin SDK (Android)
├── sync/                # Sync module (Kotlin Multiplatform)
├── backup/              # Backup module (Android)
├── benchmark/           # Benchmarking (Android)
├── extensions/          # Extensions (Android)
├── launcher/            # Launcher (Android)
├── torrent/             # Torrent/P2P (Android)
├── webosApp/            # WebOS app (Kotlin/JS)
├── tizenApp/            # Tizen app (Kotlin/JS)
├── lint-checks/         # Custom lint rules (Android)
├── build-logic/         # Gradle convention plugins
├── gradle/              # Gradle wrapper & version catalog
├── .github/             # GitHub workflows
├── config/              # Detekt configuration
├── docs/                # Documentation
├── scripts/             # Build/deployment scripts
├── tools/               # Development tools
└── README.md
```

## Module Types
| Module | Type | Platforms | Description |
|--------|------|-----------|-------------|
| app | Android App | Android | Main application |
| domain | KMP Library | JVM, JS, Android | Business logic |
| data | Android Library | Android | Data repositories |
| common | Android Library | Android | Shared utilities |
| core-common | KMP Library | JVM, JS, Android | Core utilities |
| core-platform | KMP Library | JVM, JS, Android, iOS | Platform abstractions |
| playback | Android Library | Android | Media playback |
| plugin-sdk | Android Library | Android | Plugin SDK |
| sync | KMP Library | JVM, Android, iOS | Data synchronization |
| backup | Android Library | Android | Backup/restore |
| benchmark | Android Library | Android | Performance benchmarks |
| extensions | Android Library | Android | Feature extensions |
| launcher | Android Library | Android | Universal launcher |
| torrent | Android Library | Android | P2P torrent |
| webosApp | KMP Library | JS | WebOS target |
| tizenApp | KMP Library | JS | Tizen target |
| lint-checks | Android Library | Android | Custom lint rules |
| build-logic | Gradle Plugin | - | Convention plugins |