# KuroStream — Codebase Map

Generated: 2026-08-04

## Module Overview

```
kurostream/
├── app/                    # Android TV app (main entry point)
├── playback/               # KuroVision playback engine (Media3 + OpenGL ES)
├── domain/                 # KMP domain layer (models, repositories, use cases)
├── data/                   # Repositories, APIs, Room DB, subtitle engine, KuroCloud sync
├── cache/                  # Namespaced disk caching (SimpleCache, LRU, low-RAM aware)
├── torrent/                # Torrent streaming (OptimizedTorrentEngine, jlibtorrent)
├── extensions/             # Extension runtime (Stremio, TorrServer, Plex, Kodi, Jellyfin, CloudStream, AniList, Debrid)
├── marketplace/            # Addon marketplace + Firebase backend (DUPLICATE of app's marketplace)
├── plugin-sdk/             # Public SDK for extensions (manifest, sandbox, marketplace, security)
├── common/                 # Shared utilities, theme, memory guards, optimization
├── config/                 # Build config, detekt rules
├── ui/                     # Shared Compose UI components (Log.kt, UiOptimizations.kt)
├── baseline-profile/       # Startup optimization profiles
├── benchmark/              # Performance benchmarks
├── server/                 # Node.js/TypeScript backend (Express, prom-client, Firebase Admin)
├── gradle/                 # Gradle wrapper
└── docs/                   # Documentation (this file)
```

## Module Dependencies (from `settings.gradle.kts` and `build.gradle.kts`)

### App Module Dependencies
```
app → common, domain, data, extensions, plugin-sdk, cache, ui, marketplace, playback
```

### Data Module Dependencies
```
data → common, domain, cache
```

### Domain Module (KMP - no Android deps)
```
domain → (pure Kotlin, kotlinx.serialization, coroutines)
```

### Playback Module Dependencies
```
playback → common, domain (via Media3, libvlc, libmpv, onnx-runtime, tensorflow-lite)
```

### Extensions Module Dependencies
```
extensions → domain, plugin-sdk, common, data (via UnifiedMarketplace, DebridManager)
```

### Marketplace Module (DUPLICATE)
```
marketplace → (has its own MarketplaceScreen, MarketplaceViewModel)
```

### Plugin-SDK Module
```
plugin-sdk → (standalone, provides ExtensionApi, ExtensionProvider, TorrentSource, sandbox, security)
```

### Torrent Module
```
torrent → (OptimizedTorrentEngine, jlibtorrent-android-arm64)
```

### Cache Module
```
cache → common (VodDiskCache, VodCacheManager, KuroCacheManager, DiskAsRamCache, CacheNamespaceManager)
```

### Common Module
```
common → (memory, network, optimization, thermal, pool, io, extension, audio)
```

### UI Module (MINIMAL)
```
ui → Log.kt, UiOptimizations.kt
```

## Key Files by Category

### Profile System (Phase 3 Target)
| File | Purpose |
|------|---------|
| `domain/src/commonMain/kotlin/com/kurostream/domain/model/Profile.kt` | Profile domain model |
| `domain/src/commonMain/kotlin/com/kurostream/domain/model/ProfilePreferences.kt` | Per-profile preferences (JSON) |
| `domain/src/commonMain/kotlin/com/kurostream/domain/repository/ProfileRepository.kt` | Repository interface |
| `data/src/main/java/com/kurostream/data/repository/ProfileRepositoryImpl.kt` | Repository implementation |
| `data/src/main/java/com/kurostream/data/local/entity/ProfileEntity.kt` | Room entity |
| `data/src/main/java/com/kurostream/data/local/dao/ProfileDao.kt` | Room DAO |
| `data/src/main/java/com/kurostream/data/profile/ProfileManager.kt` | Profile management |
| `app/src/main/java/com/kurostream/app/ui/components/ProfileSelector.kt` | UI component |

### Metadata Providers (Phase 3/4 Target)
| File | Purpose |
|------|---------|
| `data/src/main/java/com/kurostream/data/metadata/AniListMetadataProvider.kt` | Anime metadata (AniList GraphQL) |
| `data/src/main/java/com/kurostream/data/metadata/MalMetadataProvider.kt` | Anime metadata (MyAnimeList) |
| `data/src/main/java/com/kurostream/data/metadata/TmdbMetadataProvider.kt` | Movies/Series metadata (TMDB) |
| `data/src/main/java/com/kurostream/data/metadata/ImdbMetadataProvider.kt` | Movies/Series metadata (IMDb) |
| `data/src/main/java/com/kurostream/data/metadata/TvdbMetadataProvider.kt` | TV metadata (TVDB) |
| `data/src/main/java/com/kurostream/data/metadata/KitsuMetadataProvider.kt` | Anime metadata (Kitsu) |
| `data/src/main/java/com/kurostream/data/metadata/MetadataFusionEngine.kt` | Merges multiple providers |
| `data/src/main/java/com/kurostream/data/metadata/UnifiedMetadataRepositoryImpl.kt` | Unified repository |

### Marketplace (DUPLICATE - Phase 1 Item 2)
| Module | File | Purpose |
|--------|------|---------|
| `app` | `app/src/main/java/com/kurostream/app/ui/screens/extensions/MarketplaceScreen.kt` | Extension marketplace UI |
| `app` | `app/src/main/java/com/kurostream/app/ui/screens/extensions/MarketplaceViewModel.kt` | ViewModel for marketplace |
| `marketplace` | `marketplace/src/main/java/com/kurostream/marketplace/ui/MarketplaceScreen.kt` | DUPLICATE marketplace UI |
| `marketplace` | `marketplace/src/main/java/com/kurostream/marketplace/viewmodel/MarketplaceViewModel.kt` | DUPLICATE ViewModel |

### Server (Phase 1 Item 3 - prom-client issue)
| File | Issue |
|------|-------|
| `server/src/index.ts:4` | `import { metricsMiddleware, metricsEndpoint } from 'prom-client';` — these don't exist |
| `server/src/index.ts:59` | `app.get('/metrics', metricsEndpoint);` — uses non-existent export |

### Build Configuration (Phase 0)
| File | Purpose |
|------|---------|
| `gradle.properties` | JVM args, workers, aapt2 path, caching |
| `local.properties` | SDK path (`/root/Android/Sdk`) |
| `app/build.gradle.kts` | ABI filters, dependencies, build types |
| `data/build.gradle.kts` | Room/KSP, sqlite-jdbc on wrong config |
| `gradle/libs.versions.toml` | Version catalog |

### Orphaned/Unused Modules (Phase 1 Item 2)
| Module | Files | Status |
|--------|-------|--------|
| `torrent` | 1 file (`OptimizedTorrentEngine.kt`) | Not a dependency of `:app` |
| `plugin-sdk` | 15 files | Not used by `:app` directly |
| `extensions` | 28 files | Compiled into `:app` but unclear if all used |
| `marketplace` | 2 files | **Duplicate** of app's marketplace |
| `ui` | 2 files (`Log.kt`, `UiOptimizations.kt`) | Only `Log.kt` used |
| `playback` KuroVision | 15 files | Compiled but unclear if imported by `:app` |

## Data Flow Overview

```
User Interaction (TV Remote)
    ↓
App Module (Compose UI, Navigation)
    ↓
ViewModels (Hilt) → UseCases (Domain) → Repositories (Data)
    ↓                              ↓
                     Room DB / Network APIs / Cache / Extensions
    ↓
Playback Engine (Media3 + KuroVision) → ExoPlayer / VLC / MPV
```

## Known Issues (from static analysis + ISSUES_LEDGER.md)

### Phase 0 (SDK/Toolchain) - DONE
- ✅ JDK 17 installed
- ✅ SDK at `/root/Android/Sdk` with platforms;android-34, platforms;android-36, build-tools;35.0.0
- ✅ `local.properties` fixed to `/root/Android/Sdk`
- ✅ `gradle.properties` aapt2 path fixed to `/root/Android/Sdk/build-tools/35.0.0/aapt2`
- ✅ ABI override for debug: `arm64-v8a` only
- ✅ RAM caps reduced (Gradle 1536m, Kotlin 1024m)
- ✅ Cleanup: removed 46MB of zip snapshots

### Phase 1 (Build Fixes) - PENDING
1. **Room/KSP sqlite-jdbc**: `data/build.gradle.kts:65` has `implementation(libs.sqlite.jdbc)` — should be `ksp(libs.sqlite.jdbc)` or forced in ksp configurations
2. **Orphaned modules**: `:torrent`, `:plugin-sdk`, `:extensions`, `:marketplace`, `:ui`, `:playback` KuroVision — need wiring or removal
3. **Server prom-client**: `server/src/index.ts` imports non-existent `metricsMiddleware`, `metricsEndpoint`
4. **Full build not yet run** — need `bash gradlew assembleDebug` output

### Phase 2 (Social/Parental Controls) - PENDING
- Need to grep for: `parental`, `kidsMode`, `KidsProfile`, `maturityLevel`, `FriendRequest`, `FollowUser`, `ActivityFeed`, `SocialFeed`, `Leaderboard`
- **Preserve**: `Profile.hasPin`, `ProfileManager.verifyPin`, `ProfileEntity` PIN fields (legitimate profile lock)

### Phase 3 (Profile Content Scope) - PENDING
- Add `ContentCategory` enum and `ProfileContentScope` sealed interface to `Profile.kt`
- Room migration for `ProfileEntity`
- Wire into metadata provider selection (AniList/MAL for ANIME, TMDB/IMDb for MOVIES/SERIES)
- Profile creation flow with 3 presets + Custom

### Phase 4 (Region/Country Filtering) - PENDING
- New `Region`/`Country` setting (account-level, not per-profile)
- TMDB `region` parameter integration
- 30 major markets list
- Fallback to global when no regional data

## Verification Commands

```bash
# Toolchain
bash gradlew --version
bash gradlew :app:tasks

# Build
bash gradlew :data:kspDebugKotlin
bash gradlew :app:compileDebugKotlin
bash gradlew :app:assembleDebug

# Check APK ABI
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep lib/

# Server deps
cd server && npm install && npm run build

# Search for social/parental
grep -rliE "parental|kidsMode|KidsProfile|maturityLevel|FriendRequest|FollowUser|ActivityFeed|SocialFeed|Leaderboard" --include="*.kt" .
```

## Memory/Performance Constraints (from AGENTS.md)
- Device: Moto G52 (Snapdragon 680, 4-6 GB RAM, arm64-v8a)
- Gradle heap: 1536m (was 2048m)
- Kotlin daemon heap: 1024m (was 1536m)
- Workers: 2 (`org.gradle.workers.max=2`)
- Build outputs on internal storage: `/root/.kurostream-build/`
- Gradle cache on internal storage: `/root/.kurostream-gradle/`
- OOM watchdog: `oom-guard.sh` kills daemons below 250 MB free
- `org.gradle.vfs.watch=false` (FUSE inotify expensive)
- `android.aapt2daemonMode=outofprocess` (in-process fails under proot)