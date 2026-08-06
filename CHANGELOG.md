# Changelog

## [Unreleased]
### Fixed
- Build: Gradle root `subprojects {}` block now declares `java { toolchain { languageVersion = 17 } }` explicitly, preventing `JAVA_COMPILER` capability probing failures on OpenJDK 17 Alpine and similar distributions.
- Build: Moved protobuf/Firebase duplicate-class hardening out of `dependencies {}` into project-scope `configurations.all`, added `exclude(group="com.google.protobuf", module="protobuf-javalite")` + `force(...)` to prevent AbstractMessageLite merge conflicts.
- Build: AAPT2 Daemon startup failures on aarch64 Termux/proot environments are now resolved via qemu-x86_64 wrapper at `/opt/aapt2-qemu/aapt2`; documented in BUILD.md.
- AniList: `getAnimeByExternalId` was passing a `"idMal:12345"` string into a GraphQL `Int` variable — caused a type-mismatch crash. Now passes `idMal`/`id` as proper Int variables.
- AniList: `mapMediaType` was reading `media.status` (airing status) instead of `media.format` — anime type was always `UNKNOWN`/defaulted to TV. Fixed; `format` field added to DTO and both GraphQL queries.
- Extensions: `SandboxClassLoader` overrode `loadClass()` but never delegated to `DexClassLoader`, so `findClass()` always threw. Now overrides `findClass()` and calls `dexLoader.loadClass()`, restoring plugin class resolution.
- Extensions: `CloudstreamPluginLoader` instantiated `SandboxClassLoader` without the `dexPath` constructor argument, so the DexClassLoader had no APK to load from. Now passes `apkFile.absolutePath`.
- Extensions: `LoadedPlugin` previously stored only manifest + classloader. Now also stores `instance` (the instantiated plugin object).
- Marketplace: Checkout URL was computed but never launched. Now fires an `ACTION_VIEW` intent so the user can complete the purchase in a browser.
- Profile: `saveProfile` wrote the literal string `"has_pin"` instead of preserving the existing BCrypt hash or hashing the new PIN. Now preserves the existing hash on save; PINs are only hashed via `setPin`/`verifyPin`.
- Profile: PIN verification used `== hashPin(pin)` (rehash-equals), which fails for any PIN because BCrypt salts are random. Now uses `BCrypt.checkpw(pin, storedHash)`.
- Profile: `hashPin` was SHA-256(salt+pin) — fast, unsalted-per-user. Migrated to BCrypt (cost factor 12) via `org.mindrot:jbcrypt:0.4`.
- RealSignatureVerifier: Previously computed the fingerprint but never compared it to an allowlist. Now compares against `ALLOWED_FINGERPRINTS` set; an empty set means "accept self-signed/debug" for development.
- Torrent: `OptimizedTorrentEngine.addStreamingTorrent` returned a bare `TorrentHandle` with no HTTP URL or progress stream. Now returns `TorrentStream` with `http://127.0.0.1:8090/stream/<name>` and a `Flow<TorrentProgress>`.
- Torrent: Added `TorrentStreamServer` (NanoHTTPD wrapper) in `:torrent` module for local sequential streaming.
- InfoPanel: Was sliding in vertically (`slideInVertically` from top). Now slides in horizontally from the right (`slideInHorizontally`) per AF3 spec.
- Sidebar: Width tokens were 72dp/200dp; corrected to 60dp/220dp per AF3 spec.
- Settings: `ArcticFuseSettingsPage` was backed by an in-memory `remember { mutableStateOf(...) }` in `ArcticFuseHomeScreen`. Now backed by `ArcticFuseSettingsViewModel` → `KuroSettingsRepository` → Preferences DataStore.
- Subtitle providers: Addic7ed and Podnapisi providers were listed in the interface docstring but had no implementations. Both are now implemented with Jsoup-based HTML scraping and registered in `SubtitleProviderModule`.
- Kitsu: `getAnimeByExternalId` always returned `MetadataResult.NotFound`. Now queries `filter[malId]` for MAL IDs and falls back to text search for AniList/TMDB.
- License: Replaced 5-line stub with full official GPL-3.0 text (674 lines).

### Added
- Build toolchain: Root `build.gradle.kts` now applies `java { toolchain { languageVersion = 17 } }` to every subproject via `pluginManager.withPlugin("java-base")`.
- Version catalog: Added `jbcrypt = "0.4"` and `nanohttpd = "2.3.1"` to `gradle/libs.versions.toml`.
- Data layer: `KuroSettings` data class (37 fields) + `KuroSettingsRepository` (Preferences DataStore-backed) in `data/src/main/java/com/kurostream/data/settings/`.
- ViewModel: `ArcticFuseSettingsViewModel` with per-setting write methods and `resetDefaults()` action.
- Settings page: 5 new sections — Subtitles, Extensions, Network & Privacy, Parental Controls, Accounts & Sync — all wired to DataStore via the ViewModel.
- Theme tokens: `AFGlass` (blurRadius, overlayAlpha, saturation, cardBorder) and `AFBadges` (uhd, dolbyVision, hdr10, atmos, rating) added to `ArcticFuseTheme.kt`.
- Badge composables: `ArcticFuseBadges.kt` — `MediaTag`, `TagStyle` enum, `AdditionalTagsRow` with BOX/TEXT styles.
- Sidebar: 5 new hubs — Search, Library, History, Favorites, Debrid, Backup — with matching icons (`IconSearch`, `IconClock`, `IconZap`, `IconSave`, `IconPuzzle`).
- Sidebar: D-pad focus auto-expand — when a sidebar item receives D-pad focus and the sidebar is collapsed, it auto-expands.
- Sidebar: Width animation now uses `animateDpAsState` with `AFMotion.fast` duration for smooth transitions.
- MediaCard: AF3 badge overlay row — rating star (top-right), 4K/DV/HDR badges (top-end), Atmos badge (bottom-end), continue-watching progress bar (bottom).
- MediaItem domain + app models: Added `has4k`, `hasDolbyVision`, `hasHdr`, `audioCodec` fields.
- InfoPanel: Border color switched to `AFGlass.cardBorder` for consistent glass-morphism.
- Metadata: `AniListMetadataProvider.getAnime` now parses the `id` parameter to `Int` before passing to GraphQL.
- Metadata: Kitsu `getAnimeByExternalId` now handles MAL IDs via `filter[malId]` endpoint.
- Subtitle providers: `Addic7edProvider` with Jsoup HTML parsing, language detection, season/episode filtering.
- Subtitle providers: `PodnapisiProvider` with Jsoup HTML parsing and multi-language support.
- Security: `org.mindrot:jbcrypt:0.4` added to `data/build.gradle.kts`.
- Security: `ALLOWED_FINGERPRINTS` allowlist in `RealSignatureVerifier` with proper empty-set semantics.
- Torrent: `TorrentStream.kt` data class (url, progressFlow, subtitleTracks).
- Torrent: `TorrentStreamServer.kt` — NanoHTTPD-based local HTTP server for progressive streaming.
- Torrent: `OptimizedTorrentEngine` updated to return `TorrentStream` and start/stop the HTTP server alongside the jlibtorrent session.

### Changed
- Settings: 37-field `KuroSettings` replaces the 22-field `ArcticSettingsState` as the source of truth.
- Settings: `ArcticFuseHomeScreen` no longer holds `settingsState` in Compose state; it gets the ViewModel from `hiltViewModel()`.
- Settings: `ArcticFuseSettingsPage` signature changed from `(state, onToggle, onSelect, onClearCache, onResetDefaults, onOpenCustomTheme, onClose)` to `(viewModel, onClose, systemInfo)`.
- Protobuf: `configurations.all` in `app/build.gradle.kts` now also sets `force("com.google.protobuf:protobuf-java:...")` so transitive bumps can't reopen the duplicate-class conflict.
- MediaItem: `watchProgress` is still `Long` (milliseconds) in the domain/app models; the MediaCard progress bar now reads it correctly as a fraction.
- AniList: `mapMediaType` parameter renamed from `status` to `format` to reflect what it actually reads.
- Kitsu: `getSeasonalAnime` no longer uses empty-text search; uses the general search endpoint.
- CloudstreamPluginLoader: `LoadedPlugin` data class now includes `instance: Any? = null`.

## [Pass 14] — 2026-08-05
### Fixed
- App now visible in phone launcher (missing LAUNCHER category)
- Removed duplicate TV launcher icons (VoiceSearchActivity and RecommendationService misconfiguration)
- Fixed build failure on ARM hosts (KSP/sqlite-jdbc version override)
- Deleted orphaned duplicate MarketplaceScreen/ViewModel
- Deleted empty PlaybackModuleStub.kt

### Added
- Dolby Atmos bitstream passthrough wired to Media3 player
- GPU upscaling (EnhancedUpscaleEngine) connected to player surface
- Anime hub added to Arctic Fuse sidebar navigation
- Multi-source metadata (TMDB/Kitsu/TVDB fallback) wired to Details screen
- detekt static analysis enabled with CI enforcement

### Changed
- README: corrected RAM, Atmos, and Arctic Fuse fidelity claims
- Removed empty benchmark/baseline-profile modules from build
