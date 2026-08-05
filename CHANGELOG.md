# Changelog

## [Unreleased]
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
