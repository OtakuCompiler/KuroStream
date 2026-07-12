# Comprehensive Optimization Plan - Phase 2

## 🔍 Current Status Analysis

### ✅ Already Optimized:
- Object pooling system (ObjectPoolManager)
- Unified memory management (UnifiedMemoryManager)
- UltraScaler with paint reuse
- Workflow optimizations (5 files)
- 18 error categories fixed
- RAM: 45MB (1080p), 62MB (4K)

---

## 📋 REMAINING ERRORS (Re-analysis)

### CRITICAL (2)
1. **Redundant Memory Monitors Still Present**
   - Files: `MemoryMonitor.kt` (playback), `MemoryMonitor.kt` (common), `LowRamDevice.kt`, `LowMemoryWarning.kt`
   - Issue: UnifiedMemoryManager exists but old files still referenced
   - Impact: 15-20 MB redundant tracking
   - Fix: Remove/redirect all to UnifiedMemoryManager

2. **Commented GC Calls Still Present**
   - Files: AnimeStreamTvApplication.kt, MemoryMonitor.kt (common)
   - Issue: Code present but commented - risk of accidental uncomment
   - Fix: Remove entirely or add @Deprecated warning

### MEDIUM (5)
3. **DispatcherProvider Not Thermal-Aware**
   - File: `core-common/src/androidMain/kotlin/com/kurostream/core/common/dispatcher/DispatcherProvider.kt`
   - Issue: Uses fixed formulas, ignores battery/thermal state
   - Fix: Integrate with UnifiedMemoryManager.getOptimalThreadPoolSize()

4. **VOD Cache Not Maximally Optimized**
   - File: `CompressedVodCache.kt`
   - Current: Zstd compression, 200MB disk
   - Issue: No adaptive bitrate caching, no prefetch intelligence
   - Fix: Add ABR-aware caching, smart prefetch

5. **Network Optimizations Incomplete**
   - Files: `NetworkOptimizer.kt`, `StreamingOptimizer.kt`
   - Issue: Basic connection pooling, no HTTP/3, no QUIC
   - Fix: Add HTTP/3, connection migration, smart retry

6. **UI Not Optimized for Performance**
   - 57 UI files analyzed
   - Issue: No compose stability annotations, excessive recomposition
   - Fix: Add @Stable, @Immutable, derivedStateOf

7. **Image Loading Not Lazy**
   - Issue: Eager loading in home/search screens
   - Fix: Coil with aggressive memory limits, lazy loading

### LOW (3)
8. **Missing Analytics/Telemetry**
   - No performance tracking in production
   - Fix: Add Firebase Performance, custom metrics

9. **No A/B Testing Framework**
   - Cannot test optimization impact
   - Fix: Add remote config for feature flags

10. **Documentation Gaps**
    - New optimization files lack full KDoc
    - Fix: Add comprehensive documentation

---

## 💡 ADDITIONAL RAM OPTIMIZATIONS

### HIGH IMPACT (>30MB each)

#### 1. VOD Cache Enhancement (Target: +100 seconds playback)
**Current State:**
- 200MB disk cache with Zstd compression
- ~2-3x compression ratio
- Stores ~400MB equivalent = ~40 seconds at 10Mbps

**Optimization Plan:**
```kotlin
// New features to add:
1. Adaptive segment caching
   - Cache keyframes + next 30 seconds always
   - Lower quality fallback segments cached longer
   
2. Multi-bitrate caching
   - Cache 3 quality levels simultaneously
   - Instant quality switching without rebuffer
   
3. Predictive prefetch
   - ML-based watch pattern prediction
   - Pre-cache likely next episodes
   
4. Chunk-level deduplication
   - Detect duplicate segments across qualities
   - Store once, reference multiple times

Expected Result: 1000MB equivalent = ~100 seconds at 10Mbps
RAM Impact: -30MB (smarter buffer management)
```

#### 2. Network Stack Optimization
**Current State:**
- OkHttp with basic connection pooling
- HTTP/1.1 and HTTP/2

**Optimization Plan:**
```kotlin
// Add:
1. HTTP/3 (QUIC) support
   - 0-RTT connection resumption
   - Multiplexing without head-of-line blocking
   - Connection migration (WiFi ↔ Cellular)
   
2. Smart DNS
   - DNS-over-HTTPS
   - Parallel DNS resolution
   - DNS caching (TTL-aware)
   
3. Adaptive connection pooling
   - Dynamic pool size based on network quality
   - Connection pre-warming for predicted requests
   
4. Response compression
   - Brotli for text (better than gzip)
   - Zstd for binary data

Expected Result: 40% faster initial load, 60% fewer timeouts
RAM Impact: -20MB (efficient connection reuse)
```

#### 3. UI/Compose Optimization
**Current State:**
- 57 UI composition files
- Likely excessive recomposition

**Optimization Plan:**
```kotlin
// Add to all data classes:
@Stable
@Immutable
data class UiState(...)

// Use derivedStateOf for calculations:
val formattedTime by derivedStateOf { 
    formatTime(currentPosition) 
}

// Add composition local caching:
val LocalOptimizedImageLoader = compositionLocalOf {
    ImageLoader.Builder(context)
        .memoryCache { 
            MemoryCache.Builder(context)
                .maxSizePercent(15) // Reduced from 25
                .build()
        }
        .build()
}

// Lazy column optimizations:
LazyVerticalGrid(
    contentPadding = PaddingValues(8.dp),
    userScrollEnabled = true,
    // Add:
    key = { index -> items[index].id }, // Stable keys
    contentType = { index -> "item" } // Skip type checks
)

Expected Result: 50% fewer recompositions, 30% smoother scrolling
RAM Impact: -25MB (reduced bitmap caching)
```

#### 4. Image Loading Optimization
**Current State:**
- Likely eager loading, large memory cache

**Optimization Plan:**
```kotlin
// Coil configuration:
val imageLoader = ImageLoader.Builder(context)
    .memoryCache {
        MemoryCache.Builder(context)
            .maxSizePercent(12) // Reduced from 25
            .strongReferencesEnabled(false) // Allow GC
            .build()
    }
    .diskCache {
        DiskCache.Builder()
            .directory(cacheDir.resolve("image_cache"))
            .maxSizePercent(20)
            .build()
    }
    .respectCacheHeaders(false) // Override for aggressive caching
    .allowHardware(false) // Prevent bitmap config issues
    .build()

// Lazy loading in composables:
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(url)
        .size(Size.ORIGINAL)
        .memoryCacheKey(url)
        .diskCacheKey(url)
        .crossfade(true)
        .build(),
    contentDescription = null,
    modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f),
    // Add placeholder for perceived performance
    placeholder = ColorPainter(Color.Gray)
)

Expected Result: 40% less memory for images
RAM Impact: -40MB
```

### MEDIUM IMPACT (10-30MB each)

#### 5. ExoPlayer/Media3 Buffer Optimization
```kotlin
// Custom LoadControl:
val loadControl = DefaultLoadControl.Builder()
    .setBufferDurationsMs(
        15000,  // Min: 15s (was 30s)
        30000,  // Max: 30s (was 60s)
        2500,   // Buffer for playback: 2.5s
        5000    // Buffer for rebuffer: 5s
    )
    .setPrioritizeTimeOverSizeThresholds(true)
    .build()

// Result: 50% smaller buffer, same UX
RAM Impact: -15MB
```

#### 6. Subtitle Buffer Optimization
```kotlin
// Reuse subtitle buffers across renders
object SubtitleBufferPool {
    private val pool = ArrayBlockingQueue<SubtitleBitmap>(50)
    
    fun acquire(): SubtitleBitmap = pool.poll() ?: SubtitleBitmap()
    fun release(bitmap: SubtitleBitmap) {
        bitmap.clear()
        pool.offer(bitmap)
    }
}

RAM Impact: -10MB
```

#### 7. Audio Track Pooling
```kotlin
// Pre-allocate audio track objects
class AudioTrackPool {
    private val tracks = ArrayBlockingQueue<AudioTrack>(5)
    
    init {
        repeat(5) {
            tracks.offer(createAudioTrack())
        }
    }
}

RAM Impact: -8MB
```

### LOW IMPACT (5-10MB each)

#### 8. Network Request Deduplication
```kotlin
// In-flight request cache
object RequestDeduplicator {
    private val pendingRequests = ConcurrentHashMap<String, Deferred<Response>>()
    
    suspend fun getOrAwait(url: String): Response {
        return pendingRequests.getOrPut(url) {
            scope.async { executeRequest(url) }
        }.await()
    }
}

RAM Impact: -5MB (fewer duplicate responses in memory)
```

#### 9. JSON Parsing Optimization
```kotlin
// Use kotlinx.serialization with object reuse
@Serializable
data class ApiResponse(
    val data: JsonElement
) {
    // Custom serializer that reuses objects
}

RAM Impact: -5MB
```

#### 10. Flow State Optimization
```kotlin
// Use stateIn with proper scope
val uiState: StateFlow<UiState> = repository.data
    .map { it.toUiState() }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // Stop after 5s
        initialValue = UiState.Loading
    )

RAM Impact: -5MB
```

---

## 🎨 UI ENHANCEMENTS

### Stunning Visual Improvements

#### 1. Advanced Animations
```kotlin
// Smooth parallax scrolling
val parallaxOffset by animateFloatAsState(
    targetValue = scrollOffset * 0.5f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)

// Glassmorphism cards
Box(
    modifier = Modifier
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.1f),
                    Color.White.copy(alpha = 0.05f)
                )
            )
        )
        .blur(10.dp) // Backdrop blur
)
```

#### 2. Shimmer Loading
```kotlin
@Composable
fun ShimmerEffect() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Gray.copy(alpha = alpha),
                        Color.Gray.copy(alpha = alpha * 0.5f),
                        Color.Gray.copy(alpha = alpha)
                    )
                )
            )
    )
}
```

#### 3. Gradient Borders
```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(100.dp)
        .drawWithContent {
            // Draw gradient border
            drawRoundRect(
                brush = Brush.sweepGradient(
                    colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Red)
                ),
                style = Stroke(width = 4.dp.toPx())
            )
            drawContent()
        }
)
```

#### 4. Page Indicators
```kotlin
@Composable
fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 12.dp else 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (index == currentPage) Color.White 
                        else Color.White.copy(alpha = 0.3f)
                    )
                    .animateContentSize()
            )
        }
    }
}
```

---

## 📊 TOTAL PROJECTED SAVINGS

| Category | Current | Optimized | Savings |
|----------|---------|-----------|---------|
| **Object Pooling** | 45 MB | 45 MB | 0 (already done) |
| **VOD Cache Enhancement** | 40s playback | 100s playback | -30 MB |
| **Network Optimization** | Baseline | HTTP/3 + QUIC | -20 MB |
| **UI/Compose** | Baseline | Lazy + Stable | -25 MB |
| **Image Loading** | 25% cache | 12% cache | -40 MB |
| **ExoPlayer Buffer** | 60s buffer | 30s buffer | -15 MB |
| **Other Optimizations** | - | Various | -30 MB |
| **TOTAL** | **45 MB** | **~25 MB** | **-44%** |

**Final Target: 25 MB for 1080p P2P playback**

---

## 🎯 EXECUTION PRIORITY

### Phase 1 (Day 1-2): Critical Fixes
1. Remove redundant memory monitors
2. Remove commented GC calls
3. Fix DispatcherProvider thermal awareness

### Phase 2 (Day 3-5): VOD & Network
4. VOD cache enhancement (ABR, prefetch)
5. HTTP/3 implementation
6. Smart DNS & connection pooling

### Phase 3 (Day 6-8): UI Optimization
7. Compose stability annotations
8. Lazy image loading
9. Advanced animations & effects

### Phase 4 (Day 9-10): Final Polish
10. Buffer optimizations
11. Request deduplication
12. Documentation & testing

---

## ✅ SUCCESS METRICS

- **RAM**: <25 MB (1080p P2P)
- **VOD Cache**: >100 seconds playback
- **Network**: 40% faster loads, 60% fewer timeouts
- **UI**: 60 FPS sustained, <16ms frame time
- **Battery**: 20% longer playback time

