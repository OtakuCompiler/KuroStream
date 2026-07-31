# KuroStream Issues Ledger

Rules: rows can change status, rows are never deleted. "descoped" is an
honest status. A row that silently disappears between reports is the
one thing this file exists to prevent.

| # | Issue | Found in | Status | Notes |
|---|---|---|---|---|
| 1 | `build-logic/build.gradle.kts` referenced `libs.*` catalog entries that could not be confirmed to resolve inside the included build | build-logic module | **fixed (unverified)** | Hardcoded to pinned versions from `gradle/libs.versions.toml`. Not gradle-verified — no network access to run a real build. |
| 2 | `HomeViewModel.kt` had a duplicate import and referenced `MediaRepository` / `WatchProgressRepository` unqualified with no resolving import (nested inside `object TvRepositories`) | app/ui/screens/home | **fixed (unverified)** | Replaced with nested-type imports, matching the pattern already correct in `TvRepositoryModule.kt`. |
| 3 | `MediaItem.toAppModel()` called at 4 sites in `MediaRepositoryBridge.kt` but never defined anywhere | app/repository | **fixed (unverified)** | Added a mapping extension function. Field mapping is best-effort — needs human review against UI expectations. |
| 4 | Torrent module had ~471 compile errors; screen was commented out of nav | torrent module | **fixed (unverified) — see detail below** | This pass audited all 23 files in the module (not just the 10 originally counted). Multiple root causes found and fixed. Module re-included in `settings.gradle.kts`. Cannot build-verify (no network access for Gradle). |
| 4a | Root cause #1: only backing for `com.frostwire.jlibtorrent.*` was a 34-line local stub | torrent module | **fixed (unverified)** | Deleted stub, added real `com.frostwire:jlibtorrent:2.0.13.6` + 4 arch artifacts + frostwire Maven repo. Package names match. |
| 4b | Root cause #2: `TorrentEngine.kt` used bare `TorrentInfo` for two incompatible types (native jlibtorrent vs domain model) | torrent/engine/TorrentEngine.kt | **fixed (unverified)** | Applied alias pattern already used in `StreamingTorrentManager.kt`. This pass confirmed no other file in the module has this collision — every other file uses only one of the two types. |
| 4c | Root cause #3: `TorrentMetadataCache` and `TorrentPieceCache` imported but neither class existed | torrent/engine/TorrentEngine.kt + torrent/di/TorrentModule.kt | **fixed (unverified)** | Created minimal implementations with only the methods actually called: `TorrentMetadataCache` (ConcurrentHashMap<String, TorrentInfo>, `cacheTorrentInfo()`/`getCachedMetadata()`) and `TorrentPieceCache` (`configure(Int)`/`clearForTorrent(String)`). See `torrent/src/main/java/com/kurostream/torrent/cache/`. |
| 4d | Systematic issue: 15 files used `android.util.Log` (`Log.i`, `Log.d`, `Log.w`, `Log.e`) without importing it — imported `timber.log.Timber` instead, which was never called | 15 files across the module | **fixed (unverified)** | Added `import android.util.Log` to all 15. Files: AdaptiveLimitsCalculator, BandwidthAwareSelector, HttpFallbackManager, LazyVerifier, MetadataFetchManager, PeerWarmupManager, PortMappingMonitor, QuicTorrentProxy, SeederHuntManager, StreamingPiecePrioritizer, StreamingTorrentManager, TorrentEngine, TorrentProcessService, TrackerListProvider, WriteCoalescer. |
| 4e | `TorrentProcessService.kt` passed `TorrentEngine` to `Messenger(Handler)` constructor — `TorrentEngine` is not a `Handler` | torrent/service/TorrentProcessService.kt | **fixed (unverified)** | Changed `onBind` to return `null` since the service doesn't actually need binding. The `Messenger` and related field were unused beyond this line. |
| 4f | `TorrentService.kt` imported `com.kurostream.app.MainActivity` from `:app` module — `:torrent` does not depend on `:app` | torrent/service/TorrentService.kt | **fixed (unverified)** | Removed the cross-module import. Replaced `MainActivity::class.java` with `packageManager.getLaunchIntentForPackage(packageName)` which resolves to the actual launcher activity at runtime. |
| 4g | `TorrentViewModel.kt` called `.onFailure { }` on `TorrentResult` — a sealed interface that has no `onFailure` method | torrent/ui/TorrentViewModel.kt | **fixed (unverified)** | Replaced with `if (result is TorrentResult.Failure) { ... }` pattern, matching how `TorrentsViewModel.kt` already handles the same type. |
| 5 | `DemoMarketplaceRepository` contained fabricated listings/ratings/install counts, not wired into any DI binding or screen | plugin-sdk/marketplace | **fixed — removed** | Deleted. Confirmed via grep nothing else referenced it. |
| 6a | `VlcPlayer.kt` imports `org.videolan.libvlc.*` with no dependency declared anywhere — guaranteed Kotlin compile error | playback module | **fixed (unverified)** | Added the real `org.videolan.android:libvlc-all:3.6.3` Maven Central dependency (confirmed to exist via search this pass). Not build-verified. |
| 6b | Native mpv build (`CMakeLists.txt`) references nonexistent JNI/DSP sources | playback module | **descoped, correcting an earlier overstatement** | CMakeLists.txt is never invoked by Gradle — it's inert dead config, not a build blocker. |
| 7 | **Correction**: ArcticFuseHomeScreen is not dead code; it's a legit runtime skin switcher | app/ui | **retracted — was not a bug** | |
| 8 | Duplicate `PlayerScreen.kt` — unreferenced 28-line stub alongside real 834-line one | app/ui/screens/player | **fixed — removed** | Genuine orphan, confirmed zero references. |
| 9 | Broad TODO/FIXME/not-implemented sweep | whole repo | **updated this pass** | Refined search (word boundaries) finds 3 real hits: `CertificatePinningConfig.kt:11` (placeholder pins), `ExtensionManagerImpl.kt:115` (stub), `TorrentsScreen.kt:7` (stub). Excluded ~80 intentional platform stubs in core-platform jvmMain/jsMain which are correct multi-platform scaffolding. |

| 10 | `BackupRoute` used in `TvNavHost.kt` (lines 105, 173) but never defined in `Routes.kt` — guaranteed compile error | app/navigation | **open** | `Routes.kt` defines HomeRoute, DetailsRoute, SearchRoute, DownloadsRoute, SettingsRoute, AddonsRoute, PlayerRoute, SourceLockSettingsRoute. No BackupRoute exists. `composable<BackupRoute>` and `navController.navigate(BackupRoute)` will fail with "Unresolved reference". |
| 11 | `SettingsViewModel` calls 28 methods not present on domain `SettingsRepository` interface — guaranteed compile errors | app/ui/screens/settings/SettingsViewModel.kt | **fixed (unverified)** | Expanded domain `SettingsRepository` interface with all missing setter methods (setSkinName, setReduceMotionEnabled, setHighContrastEnabled, setFocusHighlightEnabled, setSourceLock*, setAutoPlayNextEnabled, setHardwareAccelerationEnabled, setBackgroundPlaybackEnabled, setAiUpscalingEnabled, setFrameInterpolationEnabled, setLowLatencyUpscalingEnabled, setVodCacheCompressionEnabled, setDiskBuffer*, setSeed*, setSequentialDownload, setSeedRatioLimit, setGlobalDownloadLimit, setGlobalUploadLimit). Added PlayerSubtitleSettings data class + observe/getPlayerSubtitleSettings + setSubtitle* methods. Implemented all in SettingsRepositoryImpl with MutableStateFlow backing. Fixed SettingsRepositoryAdapter.getSettings() to read subtitle settings from domain repo via getPlayerSubtitleSettings(). Fixed HomeViewModel (uses adapter, not domain — no change needed). Grep proof: zero remaining calls to nonexistent methods. Cannot build-verify. |
| 12 | `PlayerViewModel` calls 6 methods not present on domain `SettingsRepository` interface — guaranteed compile errors | app/player/PlayerViewModel.kt | **fixed (unverified)** | Replaced settingsRepository.getSettings() with settingsRepository.getPlayerSubtitleSettings() (returns PlayerSubtitleSettings data class). Replaced settingsRepository.observeSettings() with settingsRepository.observePlayerSubtitleSettings(). setSubtitleFontSize/FontColor/BgColor/Enabled now match new domain interface methods. Removed duplicate import (line 29). Grep proof: zero remaining calls to nonexistent methods. |
| 13 | `SecurityConfig` (root/Frida/emulator detection) exists but is never instantiated or called anywhere in the app's startup path | app/security/SecurityConfig.kt | **fixed (unverified)** | Added `@Inject lateinit var securityConfig: SecurityConfig` field injection in `AnimeStreamTvApplication` (line 45). Called `securityConfig.logSecurityStatus()` in `onCreate()` right after StrictMode setup (line 71). Grep proof: `SecurityConfig` and `logSecurityStatus` appear in Application file (import, field, call). Cannot build-verify. |
| 14 | `CertificatePinningConfig.applyPinning()` exists but is never called on any OkHttpClient builder | data/network/security/CertificatePinningConfig.kt | **fixed (unverified)** | Added `CertificatePinningConfig` parameter to `provideOkHttpClient()` in `NetworkModule.kt` (line 84). Builder now created via `certificatePinningConfig.applyPinning(OkHttpClient.Builder())` (line 97) instead of bare `OkHttpClient.Builder()`. The `applyPinning()` method itself is still a no-op (placeholder) — real certificate pins need to be added before release. Grep proof: CertificatePinningConfig imported (line 25), passed as param (line 84), applyPinning called on builder (line 97). Cannot build-verify. |
| 15 | `FavoritesRepositoryBridge` is purely in-memory (`mutableListOf`), never persists to DB | app/repository/FavoritesRepositoryBridge.kt | **open** | All favorites are lost on app restart. The bridge exists but stores nothing durably. |
| 16 | `SettingsRepositoryAdapter.getSettings()` hardcodes most values; only `skinName` and `reduceMotionEnabled` are actually read from the domain repo | app/repository/SettingsRepositoryAdapter.kt | **fixed (unverified)** | Now reads `skinName`, `reduceMotionEnabled`, `skipIntroEnabled` from domain flows via `safeFirst()`, and reads subtitle settings from `domainRepo.getPlayerSubtitleSettings()`. Remaining fields (autoPlayNext, hardwareAcceleration, backgroundPlayback, etc.) still hardcoded to defaults — these are now settable via the expanded domain interface but the adapter has no observation flow for them yet. |
| 17 | Phase 1 inventory: 456 .kt files, 64,877 LOC total. Build cannot run (no `gradlew`/Gradle toolchain in this environment). | whole repo | **open** | grep for TODO/FIXME done with word boundaries. Intentional platform stubs excluded. See Phase 1 report for details. |
| 18 | Phase 2 — Missing screens: Favorites, History, Library | whole repo | **fixed (unverified)** | 6 new files created (FavoritesScreen/ViewModel, HistoryScreen/ViewModel, LibraryScreen/ViewModel) totaling 733 LOC. Routes.kt: added FavoritesRoute, HistoryRoute, LibraryRoute, BackupRoute. TvNavHost.kt: wired 3 new composable routes. HomeScreen.kt: added 3 nav callbacks. SidebarNavigation.kt: added 3 items. ArcticFuseHomeScreen.kt: Favourites hub now navigates to FavoritesScreen via onFavoritesClick. Empty states show "Nothing here yet" — no fabricated data. Grep proof: FavoritesRepositoryBridge called from FavoritesViewModel (lines 6,20), WatchProgressRepositoryBridge + MediaRepository called from HistoryViewModel (lines 6,7,22,23), MediaRepository called from LibraryViewModel (lines 6,20). Cannot build-verify (no Gradle). |
| 19 | Phase 3 — UI consistency: AddonsScreen used standard Tv* theme colors (TvBackground, TvPrimary, TvSurface, TvFocusBorder, TvSurfaceHighlight) instead of AF palette; SourceLockSettingsScreen had unused AF imports and no ArcticFuseTheme wrapper; SettingsScreen had unused Tv* imports | app/ui/screens/addons, app/ui/screens/settings | **fixed (unverified)** | AddonsScreen.kt: replaced all 11 Tv* imports with AF* equivalents (AFBg, AFCyan, AFSurface, AFSurfaceHighlight, AFBorder), replaced 14 usage sites. SourceLockSettingsScreen.kt: added ArcticFuseTheme wrapper, removed 3 unused AF imports (AFBg, AFCyan, AFSurface). SettingsScreen.kt: removed 3 unused Tv* imports (TvBackground, TvSurface, TvPrimary). SplashScreen left as-is (transient, shown before theme loads). Grep proof: zero TvBackground/TvPrimary/TvSurface/TvFocusBorder/TvSurfaceHighlight remain in any of the 3 files. |
| 20 | Phase 4 — Settings completeness: SettingsViewModel called 28 methods not on domain interface; PlayerViewModel called 6 methods not on interface; SettingsRepositoryAdapter hardcoded most values | app/ui/screens/settings, app/player, data/repository | **fixed (unverified)** | Expanded domain SettingsRepository interface with all missing setter methods + PlayerSubtitleSettings data class + observe/getPlayerSubtitleSettings + setSubtitle*. Implemented all in SettingsRepositoryImpl with MutableStateFlow backing. Fixed PlayerViewModel (getSettings→getPlayerSubtitleSettings, observeSettings→observePlayerSubtitleSettings, removed duplicate import). Fixed SettingsRepositoryAdapter.getSettings() to read subtitle settings from domain. Fixed HomeViewModel (uses adapter, not domain — no change needed). Fixed SettingsScreen (removed unused Tv* imports). |
| 21 | Phase 5 — Security wiring: SecurityConfig and CertificatePinningConfig were never called | app/security, data/network/security | **fixed (unverified)** | Added @Inject SecurityConfig field injection in AnimeStreamTvApplication, called securityConfig.logSecurityStatus() in onCreate(). Added CertificatePinningConfig param to provideOkHttpClient() in NetworkModule, builder now created via certificatePinningConfig.applyPinning(). applyPinning() is still a no-op — real pins needed before release. |
| 22 | Phase 6 — Marketplace: no implementation of MarketplaceRepository interface; ExtensionManagerImpl.extractManifestFromApk is a stub (line 115); AddonsViewModel hardcodes available addon list instead of querying remote | plugin-sdk, app/ui/screens/addons | **descoped — honest scaffold** | MarketplaceRepository interface and models exist in plugin-sdk. PluginSdkModule provides real SignatureVerifier + ExtensionManifestValidator. SkinManager has full download/verify/install implementation. Missing: a real MarketplaceRepository implementation (requires backend API), proper APK manifest extraction. AddonsScreen works with local AddonDao + hardcoded list — functional for UI but no remote marketplace. A real implementation requires a backend service and API keys before it can be built. |
| 23 | Phase 7 — Performance audit | whole app | **no critical issues found** | Infrastructure solid: Coil with memory+disk cache, OkHttp with cache+connection pooling+HTTP/2, lazy lists (LazyColumn/LazyRow) throughout, deferred startup init via Handler+IdleHandler, memory trimming callbacks, BufferPool/ObjectPools/StringInterner cleanup, proper dispatcher usage (IO/Default/Main). Scopes: viewModelScope for ViewModels, rememberCoroutineScope for composables, SupervisorJob+appScope for Application. No ContentProvider startup tax. Minor: SkinManager scope not cancellable (lives as long as app — acceptable for @Singleton). |
| 24 | Phase 8 — Testing audit | whole project | **minimal coverage — scaffold exists** | 8 test files across 64,877 LOC. Test framework: JUnit4, Compose UI Test (JUnit4), MockK, Turbine, Coroutines Test, Espresso. Existing tests: HomeScreenTest, SearchScreenTest, PlayerScreenTest, DetailsScreenTest (Compose UI), AccessibilityTest, BackendSelectorIntegrationTest, ExtensionManifestValidatorTest, ArchitectureTest. Missing: unit tests for ViewModels (SettingsViewModel, AddonsViewModel, FavoritesViewModel, HistoryViewModel, LibraryViewModel), repository tests (SettingsRepositoryImpl, SettingsRepositoryAdapter, MediaRepositoryBridge), security tests (SecurityConfig, CertificatePinningConfig), Phase 2 screen tests, Phase 4 settings tests. Cannot run tests without build system. |
| 25 | Phase 9 — Release build | app module | **scaffolded — needs real values** | Release config: signingConfigs with env vars (KEYSTORE_PATH / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD), fallback to ../keystore/release.keystore. Build type: minifyEnabled=true, shrinkResources=true, proguard-android-optimize.txt + proguard-rules.pro (42 lines, covers Hilt/kotlinx.serialization/Room/Retrofit/Coil), crunchPngs=true. Debug: applicationIdSuffix=".debug", versionNameSuffix="-debug". network_security_config.xml: cleartext disabled, certificate pinning for AniList/MAL/TMDB/Kitsu, debug-overrides for user certs. Issues: all certificate pins are placeholder (sha256/AAAA... / sha256/BBBB...), keystore doesn't exist, no CI/CD workflow found. Cannot build-verify. |

## Per-file audit results (torrent module, this pass)

| File | Status | Notes |
|---|---|---|
| `TorrentEngine.kt` | **fixed** | Had TorrentInfo alias from prior pass; added missing `android.util.Log` import. |
| `StreamingTorrentManager.kt` | **clean** | Already had correct TorrentInfo alias; added missing `android.util.Log` import. |
| `TorrentModule.kt` | **clean** | Imports non-existent caches — now resolved. |
| `TorrentModels.kt` | **clean** | Defines domain models. |
| `AdaptiveLimitsCalculator.kt` | **fixed** | Added missing `android.util.Log` import. |
| `LazyVerifier.kt` | **fixed** | Added missing `android.util.Log` import. Unused native TorrentInfo import (warning only). |
| `WriteCoalescer.kt` | **fixed** | Added missing `android.util.Log` import. |
| `MetadataFetchManager.kt` | **fixed** | Added missing `android.util.Log` import. |
| `PortMappingMonitor.kt` | **fixed** | Added missing `android.util.Log` import. |
| `QuicTorrentProxy.kt` | **fixed** | Added missing `android.util.Log` import. |
| `PeerWarmupManager.kt` | **fixed** | Added missing `android.util.Log` import. |
| `PredictivePrefetchManager.kt` | **clean** | Unused domain TorrentInfo import (warning only). |
| `BandwidthAwareSelector.kt` | **fixed** | Added missing `android.util.Log` import. Unused native TorrentInfo import (warning only). |
| `StreamingPiecePrioritizer.kt` | **fixed** | Added missing `android.util.Log` import. |
| `TorrentRepositoryImpl.kt` | **clean** | Verified: domain Result type has `fold(onSuccess, onError, onLoading)`. |
| `TorrentProcessService.kt` | **fixed** | Fixed `Messenger(engine)` → `onBind` returns null. Added Log import. |
| `TorrentService.kt` | **fixed** | Removed `:app` module dependency (`MainActivity` import). Replaced with `packageManager.getLaunchIntentForPackage()`. |
| `SeederHuntManager.kt` | **fixed** | Added missing `android.util.Log` import. |
| `TrackerListProvider.kt` | **fixed** | Added missing `android.util.Log` import. |
| `TorrentViewModel.kt` | **fixed** | Replaced `.onFailure` on `TorrentResult` with proper `when` matching. |
| `TorrentsViewModel.kt` | **clean** | Uses correct `when (result)` pattern for TorrentResult. |
| `HttpFallbackManager.kt` | **fixed** | Added missing `android.util.Log` import. |
| `TorrentsScreen.kt` | **clean** | Compose stub, no compilation issues. |

## Open items (not attempted this pass)
- Build verification: cannot run `./gradlew :torrent:compileDebugKotlin` — no network access in this environment. Every fix is static-text only.
- jlibtorrent API compatibility: unknown whether the 2.0.13.6 API matches every call site. Known risky patterns: `SessionManager.start()`, `handle.addMetadata()`, `TorrentHandle.connectPeer(PeerInfo(...))`, `session.dhtState()?.nodes` — plausible based on library docs but unverified.
- Unused imports across the module (e.g., `timber.log.Timber` in files that now use `android.util.Log`, native `TorrentInfo` import in files that don't use the type) — these are warnings, not errors.# Phase 0 — Baseline

Date: 2026-07-31

## Compile attempt
Command: `bash gradlew :data:compileDebugKotlin :app:compileDebugKotlin --no-daemon --stacktrace --console=plain`

First failure (before fix): `:tizenApp` directory missing from workspace, Gradle configuration error.
Fix applied: removed `include(":tizenApp")` from `settings.gradle.kts` (no code references to tizenApp found anywhere in the project).

Second failure: `SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path...`
Root cause: `/usr/local/android-sdk` exists but contains no platforms/build-tools/cmdline-tools. Local environment does not have a functional Android SDK installed.

**Phase 0 verification status: UNVERIFIED — local Gradle build is blocked by missing Android SDK.**

## Module/folder check
Modules listed in `settings.gradle.kts` vs filesystem:

| Module | settings.gradle.kts | Directory exists | build.gradle.kts exists |
|--------|---------------------|------------------|------------------------|
| :app | yes | YES | YES |
| :benchmark | yes | YES | YES |
| :cache | yes | YES | YES |
| :common | yes | YES | YES |
| :config | yes | YES | YES |
| :core-common | yes | YES | YES |
| :core-platform | yes | YES | YES |
| :data | yes | YES | YES |
| :domain | yes | YES | YES |
| :extensions | yes | YES | YES |
| :launcher | yes | YES | YES |
| :marketplace | yes | YES | YES |
| :playback | yes | YES | YES |
| :plugin-sdk | yes | YES | YES |
| :tizenApp | yes | NO | NO |
| :ui | yes | YES | YES |

Action taken: removed `:tizenApp` from `settings.gradle.kts` with a comment.

## LOC baseline
- Kotlin source files: 366
- Total LOC: 41,862

---

# Phase 1 — Room/KSP sqlite-jdbc crash fix

Problem statement from issue: Room 2.6.1's KSP compiler pulls an old `org.xerial:sqlite-jdbc` with no linux-aarch64 native binary, causing `NoClassDefFoundError: org/sqlite/SQLiteJDBCLoader`.

Fix applied:
- Added `sqliteJdbc = "3.49.1.0"` to `gradle/libs.versions.toml` (latest stable as of 2026-07-31, confirmed on Maven Central).
- Added `sqlite-jdbc` library alias in version catalog.
- Added `ksp(libs.sqlite.jdbc)` to both `app/build.gradle.kts` and `data/build.gradle.kts`.

Proof status: **fixed (unverified)** — local Gradle build is blocked by missing Android SDK (`/usr/local/android-sdk` has no platforms/build-tools). Cannot run KSP task or dependency-tree check locally. CI verification pending on push to GitHub Actions.

---

# Phase 2 — Playback engine decision

Decision: **Option B — Formally descope to Media3-only.**

Rationale:
- `MpvPlayer.kt` and `VlcPlayer.kt` are deleted from the repo.
- `BackendSelector.kt` is a 3-line enum (`MPV, VLC, MEDIA3, AUTO, TORRENT`) with zero routing/health-monitor logic.
- `PlayerViewModel` directly instantiates `ExoPlayer`; `BackendSelector`/`PlayerBackend` are referenced nowhere in production code.
- Restoring three engines would require substantial JNI/LibVLC/libmpv wrappers, separate process sandboxing, and scoring logic — out of scope for this pass.

Changes made:
- Updated `BackendSelector.kt` with a doc comment stating that only `MEDIA3` is implemented and the other values are aspirational.
- Updated `README.md`:
  - Changed libmpv/libVLC/Auto backend selection from ✅ to 🚧 Not implemented.
  - Removed "NDK (for MPV/VLC native libraries)" from prerequisites.
  - Updated architecture diagram to show only `Media3 · Extension SDK` in platform layer.
  - Updated module list to show `playback/ — Media3 player, renderers, buffers` (removed MPV/VLC).
  - Updated credits section to mark MPV/libVLC as "Planned native playback engines (not yet implemented)".
- Grep for `PlayerBackend` and `BackendSelector` usage in `.kt` files returns zero hits outside the enum file itself.

Proof: `grep -rn "import com.kurostream.players.selector.BackendSelector\|import com.kurostream.players.selector.PlayerBackend\|BackendSelector\.\|PlayerBackend\." app/src/main/java/ — returned no matches. Diff shows README and BackendSelector.kt comment changes only.

---

# Phase 3 — Torrent module decision

Decision: **Option B — Formally descope and leave excluded.**

Rationale:
- `:torrent` is already commented out in `settings.gradle.kts` with a note that `dl.frostwire.com/maven` returns 404.
- No resolvable Maven coordinate for `com.frostwire:jlibtorrent` was found on a live listing that matches the project's current repository configuration.
- Re-enabling the module without a confirmed resolvable dependency would create a build that "merely looks fixed."

Changes made:
- Confirmed `:torrent` remains excluded in `settings.gradle.kts`.
- Updated `README.md` with an "Excluded modules" section explicitly stating:
  - `:torrent` — excluded from `settings.gradle.kts`; `dl.frostwire.com/maven` is dead (404), `jlibtorrent` cannot be resolved. Torrent-related UI/navigation is disabled.
- Confirmed torrent UI screens (`TorrentsScreen.kt`) are disabled in `TvNavHost.kt` with `onTorrentsClick = {}`.

Proof: `settings.gradle.kts` still has `// include(":torrent")` with explanatory comment. README diff shows added excluded-modules section. No build.run attempt needed because module is already excluded.

---

# Phase 4 — Repo hygiene cleanup

Completed items:
1. **kurostream_final_analysis.json**: Already deleted in prior cleanup (confirmed `ls` returns "GONE").
2. **`.gitignore`**: Verified to cover `.gradle/`, `build/`, `*.apk`, `*.aab`, `*.keystore`, `*.jks`, `.kilo/`, `.kotlin/`. `local.properties` is also ignored.
3. **TrailerRepositoryImpl.kt**: Fixed hardcoded `"YOUTUBE_API_KEY"` strings at 2 call sites.
   - Replaced with constructor-injected `youTubeApiKey: String`.
   - Added `ksp(libs.sqlite.jdbc)` to both module build files (Phase 1).
   - Added `provideYouTubeApiKey()` provider in `NetworkModule.kt` that reads from `BuildConfig.YOUTUBE_API_KEY` with `System.getenv("YOUTUBE_API_KEY")` fallback.
   - Added `buildConfigField("String", "YOUTUBE_API_KEY", System.getenv("YOUTUBE_API_KEY") ?: "\"\"")` to `data/build.gradle.kts`.

Proof: `git diff` shows TrailerRepositoryImpl.kt lines changed from literal string to `youTubeApiKey` parameter. `git diff` shows NetworkModule.kt added `provideYouTubeApiKey()`. `.gitignore` contents confirmed by read.

---

# Phase 0 — Baseline (recap)

- Compile command: `bash gradlew :data:compileDebugKotlin :app:compileDebugKotlin --no-daemon --stacktrace --console=plain`
- First failure: `:tizenApp` directory missing → removed from `settings.gradle.kts`.
- Second failure: `SDK location not found` → `/usr/local/android-sdk` exists but has no platforms/build-tools.
- Module/folder check: 14 of 15 settings entries have matching dirs/build.gradle.kts; `:tizenApp` was the missing one.
- LOC baseline: 366 Kotlin files, 41,862 total LOC.

**Note:** Local Android SDK is incomplete in this environment. All build-dependent verification items in subsequent phases are marked "unverified" with explicit reasoning. CI on GitHub Actions has Android SDK 35 configured and was previously passing before this session's changes; post-push CI status will be the authoritative verification source.
