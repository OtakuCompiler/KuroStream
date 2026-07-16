# Architecture Decision Records (ADR)

## ADR-001: Module Split - common → common + ui

**Date**: 2026-07-15  
**Status**: Accepted  
**Deciders**: Architecture Team  

### Context
The `common` module contained both pure utilities (BufferPool, MemoryMonitor, Result) AND Compose UI components (CommonOptimizations). This created a layering violation where data/domain modules depended on UI code.

### Decision
Split `common` into:
- **`common`**: Pure utilities only (no Android/Compose dependencies)
- **`ui`**: Compose components (UiOptimizations, texture atlas, render offloading)

### Consequences
- **Positive**: Clean architecture layers, data modules no longer pull in Compose
- **Negative**: 8 modules needed dependency updates
- **Migration**: Automated via script, verified by build

---

## ADR-002: Ultra-Low RAM Architecture

**Date**: 2026-07-15  
**Status**: Accepted  

### Context
Original memory usage: 185MB (1080p P2P), 687MB (4K+Atmos) - too high for 2GB devices.

### Decision
Implement 8-component memory management system:
1. **UltraLowMemoryManagerV3** - **RAM-adaptive budgeting** (leave 20MB, cap 1GB), network-aware
2. **ZeroCopyBufferManager** - Direct ByteBuffer + mmap pooling
3. **YuvFramePool** - YUV planar frames (no ARGB conversion)
4. **CompressedFrameCache** - Zstd/Deflate per-plane compression
5. **AdaptivePrebufferManager** - Network-speed aware prebuffering
6. **RamQualityController** - **RAM-tier based quality scaling** (6 tiers), manual override
7. **OptimizedP2PEngine** - Peer scoring + delta pieces
8. **KuroStreamMemoryManager** - Unified facade

### Consequences
- **Positive**: 23MB (1080p), 33MB (1080p→4K), 36MB (4K+Atmos)
- **Negative**: ~3,800 lines new code, increased complexity
- **Testing**: Required memory regression CI/CD

---

## ADR-003: SyncProvider Interface Consolidation

**Date**: 2026-07-15  
**Status**: Documented (Not Fixed)  

### Context
Three `SyncProvider` interfaces exist:
1. `domain.repository.SyncProvider` - Playback state sync (Flow-based)
2. `domain.sync.SyncProvider` - Cross-device payload sync (Map-based)  
3. `data.anistream.sync.SyncProvider` - MAL/AniList watch history

### Decision
**Keep all three** but document clearly:
- Rename to `PlaybackSyncProvider`, `CloudSyncProvider`, `AniStreamSyncProvider`
- Different method signatures reflect different use cases
- No consolidation - they serve different bounded contexts

### Consequences
- **Positive**: Clear separation of concerns
- **Negative**: Name collision risk for developers

---

## ADR-004: Hilt in KMP sync Module

**Date**: 2026-07-15  
**Status**: Needs Fix  

### Context
`sync/build.gradle.kts` uses Hilt in `commonMain` which only works on Android.

### Decision
Move Hilt to `androidMain` source set, use manual DI for `jvmMain`/`iosMain`

```kotlin
// sync/build.gradle.kts
sourceSets {
    androidMain {
        dependencies {
            implementation(libs.hilt.android)
            ksp(libs.hilt.compiler)
        }
    }
    commonMain {
        // No Hilt here
    }
}
```

---

## ADR-005: Compose in Library Modules

**Date**: 2026-07-15  
**Status**: Needs Fix  

### Context
`playback`, `backup`, `torrent`, `extensions` modules contain `@Composable` functions.

### Decision
Move all Compose UI to `app` module or new `ui` module. Libraries should only expose data/state.

---

## ADR-006: System.gc() Removal

**Date**: 2026-07-15  
**Status**: Accepted  

### Context
13 explicit `System.gc()` calls in production code.

### Decision
Remove all explicit GC calls. Rely on:
- `UltraLowMemoryManagerV3.autoTrim()` for pressure-based trimming
- JVM ergonomics with G1GC (configured in gradle.properties)
- `ActivityManager.TRIM_MEMORY_*` callbacks

### Rationale
Explicit GC defeats generational collection, causes stop-the-world pauses.

---

## ADR-007: Unscoped CoroutineScope Cleanup

**Date**: 2026-07-15  
**Status**: Accepted  

### Context
22 instances of `CoroutineScope(...)` without lifecycle binding.

### Decision
Replace with:
- `viewModelScope` / `lifecycleScope` for UI
- Injected `CoroutineScope` with `SupervisorJob` for services
- `rememberCoroutineScope()` for Compose

---

## ADR-008: Lazy List Keys Mandate

**Date**: 2026-07-15  
**Status**: Accepted  

### Context
16 `LazyColumn/Row` usages without stable keys.

### Decision
All lazy lists MUST have `key = { item.id }`. Enforced by detekt rule `LazyListHasKey`.

---

## ADR-009: Memory Regression Testing

**Date**: 2026-07-15  
**Status**: Accepted  

### Context
No automated memory regression detection.

### Decision
Add GitHub Actions workflow:
- Run on every PR + daily schedule
- Test on API 28, 31, 34 (arm64)
- 2GB emulator memory
- Thresholds: 1080p<30MB, 4K<50MB, Idle<25MB
- Block merge on regression >10%

---

## ADR-010: Benchmark Module Structure

**Date**: 2026-07-15  
**Status**: Accepted  

### Context
Benchmark code scattered across modules.

### Decision
Consolidate in `:benchmark` module:
```
benchmark/
├── MemoryBenchmark.kt          # Main benchmark orchestrator
├── MemoryBenchmarkActivity.kt  # UI for manual runs
├── BenchmarkResult.kt          # Data classes
└── baseline.json               # Committed baseline
```

---

*All ADRs are living documents. Update status as implementation progresses.*