# KuroStream — Performance Report
**Date:** 2026-08-03  
**Note:** This is a static code-analysis performance report. Real runtime benchmarks require Android hardware. Numbers marked "estimated" are derived from code analysis and known library behavior, not measured profiling runs.

---

## Important Disclaimer

> Real performance numbers cannot be measured without an Android device or emulator.  
> Replit runs x86_64 Linux — no Android runtime is available.  
> All figures below are **code-analysis estimates** and must be verified on target hardware before release.

---

## Startup Performance

### Code Analysis

| Metric | Target | Estimated | Evidence |
|--------|--------|-----------|----------|
| Cold start to first frame | < 2 s | ~2.5–3.5 s | Hilt component graph is large; 13 Room entities; KuroCloud sync on init |
| Warm start | < 1 s | ~0.8–1.2 s | Activity not recreated; ViewModel cached |
| Baseline profile | ✅ Present | Reduces cold start ~20–30% | `baseline-profile/` module present; profile generator task configured |

### Startup Risk Factors

1. **Hilt initialization depth** — `app/` depends on 8 Gradle modules all with Hilt bindings. Component graph resolution runs on first inject.  
2. **Room database open** — `KuroStreamDatabase` has 13 entities + converters + WAL mode. SQLCipher decryption adds ~50–150 ms.  
3. **KuroCloud auth check** — `KuroSyncRepository` launches coroutines on first access. If this runs on main thread, expect ANR risk.  
4. **Coil image preloading** — `CoilCacheConfig` in `common/optimization/` configures memory/disk limits. Excessive preload can spike RAM.  
5. **Extension health monitor** — `ExtensionHealthMonitorImpl.autoEnableFixed()` may trigger network calls at startup.

### Recommendation
- Profile startup with Android Studio Profiler before release.  
- Ensure all DB/network calls are off the main thread (check `@WorkerThread` annotations).  
- Consider lazy DI initialization for non-critical modules (marketplace, extensions).

---

## Memory Usage

### Code Analysis

| Scenario | Target | Estimated | Evidence |
|----------|--------|-----------|----------|
| Idle (home screen) | < 100 MB | ~120–180 MB | Hilt + Room + Coil memory cache + extension repos |
| Playback (1080p) | < 300 MB | ~220–280 MB | Media3 codec buffers + KuroVision frame pool |
| Playback (4K) | < 500 MB | ~350–500 MB | Larger codec buffers + potential OpenGL textures |

### Memory Management Code

- **`AdaptiveMemoryGovernor`** (`common/memory/`) — real implementation that adjusts behavior under memory pressure.  
- **`UnifiedMemoryManager`** — coordinates codec, image, and frame pool memory.  
- **`NativeFramePool`** — manages native frame buffers for KuroVision pipeline.  
- **`RamEnforcer`** — enforces per-process RAM budget.  
- **`ThermalGuard`** — throttles work under thermal pressure.

### Risk: `gradle.properties` JVM settings
```
org.gradle.jvmargs=-Xmx2048m
kotlin.daemon.jvmargs=-Xmx1536m
```
These are tuned for a Snapdragon 680 / 6 GB device (Termux proot). Replit builds may OOM with these limits. Recommend raising to `-Xmx4g` for CI builds.

---

## UI Rendering

### Code Analysis

| Metric | Target | Estimated | Evidence |
|--------|--------|-----------|----------|
| List scrolling FPS | 60 FPS | Likely 60 FPS on mid/high-end TV | Compose LazyRow/Column used; no custom layout measurement |
| Image loading | — | Coil async with memory cache | `CoilCacheConfig` configured |
| Compose recomposition | — | Risk in `MarketplaceScreen` — state hoisting unclear | `MarketplaceScreen.kt` has nested state |

### `UiOptimizations.kt` (real implementation)
- Texture atlas for small UI assets — reduces draw calls.  
- Background Compose computation cache — avoids redundant measure passes.

---

## Network Performance

### Code Analysis

| Feature | Implementation | Evidence |
|---------|----------------|----------|
| OkHttp connection pooling | ✅ Real | `UltraNetworkManager` + `NetworkOptimizer` in `common/network/` |
| Brotli compression | ✅ Real | `okhttp3-brotli` in data deps |
| Adaptive quality selection | ✅ Real | `SmartPlayerSelector.selectQuality()` uses network Mbps |
| Pre-buffering / zero-start | ✅ Real | `ZeroStartBuffer.kt` in playback module |
| Streaming optimizer | ✅ Real | `StreamingOptimizer.kt` in common module |

### Caveats
- `SmartPlayerSelector` uses `networkMbps` parameter but there is no real-time bandwidth probe feeding this value at call sites. Caller is responsible for passing an accurate current measurement.

---

## Fire TV Stick HD Specific Analysis

The Fire TV Stick HD has a quad-core 1.7 GHz ARM Cortex-A53 CPU and 1.5 GB RAM.

| Risk | Severity | Analysis |
|------|----------|----------|
| KuroVision OpenGL pipeline | HIGH | A53 + Mali GPU will struggle with real-time shader processing at 1080p. Hardware passthrough mode must be the default. |
| SQLCipher overhead | MEDIUM | ~30–50 ms per DB open on slow storage |
| Hilt component graph | MEDIUM | Large DI graph on slow CPU adds 200–500 ms to cold start |
| Extension health monitor at boot | MEDIUM | Network checks on weak WiFi can block UI |
| 1.5 GB RAM with all modules loaded | HIGH | Risk of LMK killing the app. `AdaptiveMemoryGovernor` must aggressively reduce cache on Fire TV |

**Recommendation:** Add a `DeviceClass.LOW_POWER_TV` code path that disables KuroVision pipeline, reduces Coil memory cache to 32 MB, and limits concurrent extension queries to 1.

---

## Benchmark Module

A `benchmark/` Gradle module exists. This suggests the team intended to use Macrobenchmark for startup and frame-time measurement. The module was not built or run as part of this audit (requires connected Android device). **Run benchmarks on a Fire TV Stick HD and a mid-range Android TV before release.**

---

## Lighthouse / Website

No web frontend was found in the repository. Website performance cannot be assessed.  
The Node.js backend has no load test or response-time benchmarks in the codebase.

---

## Summary

| Platform | Estimated Readiness | Biggest Risk |
|----------|---------------------|--------------|
| Android TV (high-end) | ⚠️ Close | Startup time, needs profiling |
| Fire TV Stick HD | ❌ Not ready | RAM pressure + KuroVision GPU cost |
| Mobile | ⚠️ Unknown | TV-optimized UI, no mobile layout tests |
| LG webOS | ❌ Not implemented | No webOS module found |
| Samsung Tizen | ❌ Not implemented | No Tizen module found |
| Website | ❌ Not implemented | No web frontend found |
