# Kurostream

> Universal Streaming OS. P2P-native, zero-host, god-tier streaming platform.

## What is Kurostream?

Kurostream is a **self-hosted, zero-backend streaming platform** that combines the best of Stremio, Kodi, CloudStream, Nuvio, and Plex into a single lightweight web app.

## Key Features

- **10 Metadata Providers**: AniList, TMDB, SIMKL, TVDB, MAL, Kitsu, Trakt, OMDB, FanArt, LiveChart
- **Native P2P Streaming**: WebTorrent built-in, no debrid required
- **20+ Extensions**: Stremio addons, CloudStream, Kodi, Nuvio, subtitle providers, themes, widgets
- **God-Tier Player**: Intro/outro skip, A-B repeat, frame stepping, bookmarks, chapters, audio normalization
- **TV Optimized**: D-pad navigation, cursor hiding, focus rings, Fire TV/LG webOS/Samsung Tizen support
- **Privacy First**: Local-only mode, encrypted storage, zero telemetry
- **PWA**: Installable, offline-capable, service worker cached

## Quick Start

```bash
npm install
npm run dev
```

## Build for Production

```bash
npm run build
```

The `dist/` folder is completely static - serve it with any web server or CDN.

## Architecture

```
Kurostream/
├── src/
│   ├── components/       # UI components (Arctic Fuse 3 style)
│   ├── pages/            # Route pages
│   ├── lib/
│   │   ├── metadata/     # 10 provider unified engine
│   │   ├── p2p/          # WebTorrent + WebRTC streaming
│   │   ├── extensions/   # Stremio/CloudStream/Kodi/Nuvio adapters
│   │   ├── subtitles/    # OpenSubtitles, SubDL, ASS/SRT/VTT
│   │   ├── aniskip.ts    # Intro/outro skip detection
│   │   ├── recommendations.ts  # Local ML-free engine
│   │   ├── store.ts      # Zustand state management
│   │   └── types.ts      # TypeScript definitions
│   └── styles/           # Arctic Fuse 3 theme
├── public/               # Static assets, service worker
└── docs/                 # Audit, feature matrix, performance
```

## Production Readiness: 8/10

### Ready
- Full UI/UX (Arctic Fuse 3 hub navigation)
- 10 metadata providers with auto-benchmarking
- Native P2P torrent streaming
- 20+ extension marketplace
- Smart player with all advanced features
- TV mode with remote navigation
- PWA with offline support
- Privacy-first local-only architecture

### Needs Work
- Real API keys for production use
- WebSocket torrent fallback for restricted browsers
- WebCodecs API for hardware decoding on TV browsers
- Tizen/webOS native packaging
- Real-time sync server (optional)
