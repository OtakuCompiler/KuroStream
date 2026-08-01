# Changelog

All notable changes to KuroStream will be documented in this file.

## [1.0.0] - 2026-08-01
### Added
- Arctic Fuse 3 inspired UI
- 4K/HDR/Dolby Vision support with <125MB RAM target
- 500MB VOD disk cache + 125MB RAM disk
- Optimized torrent streaming engine (DHT/PEX/LSD)
- PlayerProcessInfo overlay
- Low-RAM ExoPlayer with tunneling support

### Fixed
- PlayerViewModel scope crash
- PlayerActivity memory leaks (receiver, wakelock, audiofocus)
- Manifest incomplete queries section
- Coil cache misconfiguration

### Security
- FLAG_SECURE on player window
- Network security config enforced
