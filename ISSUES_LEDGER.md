# Issues Ledger

## Pass 14 — 2026-08-05

### Fixed
- [BUG-01] LAUNCHER category added to MainActivity — app visible on phone
- [BUG-02] LEANBACK_LAUNCHER removed from VoiceSearchActivity and RecommendationService — no duplicate TV icons
- [BUG-03] KSP sqlite-jdbc version override fixed (resolutionStrategy.force) — build succeeds on ARM
- [BUG-04] DolbyAtmosPassthrough wired into Media3Player (logAudioCapabilities + conditional audio sink)
- [BUG-05] UpscalingManager wired into PlayerViewModel and PlayerScreen
- [BUG-07] UnifiedMetadataRepository injected into DetailsViewModel with TMDB/Kitsu/TVDB fallback
- [BUG-08] Anime hub added to ArcticHub enum and home screen routing
- [BUG-09] detekt plugin enabled in app, data, playback modules with CI enforcement
- [BUG-12] kilo.json removed from repo and added to .gitignore
- Orphaned MarketplaceScreen/ViewModel deleted from app/ui/screens/extensions/
- Empty PlaybackModuleStub.kt deleted
- README claims corrected (RAM, Atmos, AF3 fidelity)
- Added CHANGELOG.md and SECURITY.md
- Updated PR template with verification checklist
- Disabled empty baseline-profile and benchmark modules in settings.gradle.kts
- Added TvdbApi and ImdbApi Hilt providers to NetworkModule

### Firebase + Cross-Device Sync
- Updated firebase.json with hosting rewrites and all services
- Updated Firestore rules with user-scoped sync collections (users/{userId}/sync, favorites, watchHistory, settings, profiles)
- Updated Storage rules with user and admin paths
- Added Firebase Functions: onUserCreated, onUserDeleted, sendFCMNotification, onNewEpisode, syncWatchProgress, verifyAppCheck, healthCheck
- Added Cloudflare cross-device sync API: /api/public/v1/sync (GET/PUT bulk), /sync/favorites, /sync/watch-history, /sync/settings, /sync/profiles, /sync/devices
- Added D1 schema for sync tables: favorites, watchHistory, settings, profiles, queue, devices
- Added separate sync worker with Durable Objects for real-time WebSocket sync (kurohub/src/workers/sync-worker.ts, sync-room.ts)
- Added wrangler.sync.jsonc for the sync worker deployment

### Still Open
- Certificate pins: still placeholder (requires live host access to generate)
- google-services.json: must be added manually from Firebase Console
- :extensions module: Stremio wired (Phase 6), CloudStream/Kodi/Jellyfin/Plex not yet connected
- Test coverage: ~3% (was 2.6%) — needs dedicated test-writing pass
- :benchmark and :baseline-profile: commented out — needs real device for generation

### Build Verification
CI passed at commit eca5148:
- CI: https://github.com/OtakuCompiler/KuroStream/actions/runs/31010449801
- Code Quality: https://github.com/OtakuCompiler/KuroStream/actions/runs/31010450013
