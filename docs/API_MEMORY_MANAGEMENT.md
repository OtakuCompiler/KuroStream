# KuroStream Ultra-Low RAM Memory Management API

**Version**: 1.0.0 (Phase 4+)  
**Package**: `com.kurostream.playback.memory`  
**Min SDK**: 28 (Android 9)

---

## Overview

The Ultra-Low RAM memory management system provides a unified, thermal-aware, and network-adaptive memory budgeting framework for media playback. It achieves **<30MB for 1080p P2P** and **<40MB for 4K+upscale** on 2GB devices through aggressive pooling, compression, and intelligent prebuffering.

---

## Core Components

### 1. KuroStreamMemoryManager (Unified Entry Point)

**Purpose**: Single initialization point for all memory subsystems

```kotlin
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val memoryManager: KuroStreamMemoryManager
) : ViewModel() {

    fun startPlayback(streamId: String, quality: VideoQuality) {
        val config = KuroStreamMemoryManager.UnifiedConfig(
            streamId = streamId,
            quality = quality,
            hasUpscaling = true,
            hasTranscoding = false,
            networkSpeedMbps = 15,
            enableP2P = true,
            enableCompressedFrames = true,
            enableDeltaP2P = true,
            maxBitrateKbps = 25000,
            targetFps = 60,
            upscalingEnabled = true,
            atmosEnabled = false
        )
        
        memoryManager.initialize(config)
    }
}
```

#### `UnifiedConfig` Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `streamId` | String | required | Unique stream identifier |
| `quality` | VideoQuality | required | Target quality (LD_360P → UHD_4K) |
| `hasUpscaling` | Boolean | false | Enable upscaler budget |
| `hasTranscoding` | Boolean | false | Enable audio transcoding budget |
| `networkSpeedMbps` | Long | 10 | Current network speed |
| `enableP2P` | Boolean | true | Enable P2P engine |
| `enableCompressedFrames` | Boolean | true | Enable frame compression |
| `enableDeltaP2P` | Boolean | true | Enable delta piece sharing |
| `maxBitrateKbps` | Int | 25000 | Maximum stream bitrate |
| `targetFps` | Int | 60 | Target frame rate |
| `upscalingEnabled` | Boolean | false | Allow upscaling |
| `atmosEnabled` | Boolean | false | Allow Dolby Atmos |

#### Memory Pressure Levels

```kotlin
enum class MemoryPressure {
    NONE,      // >20% available
    LOW,       // 12-20% available
    MODERATE,  // 8-12% available
    HIGH,      // 4-8% available
    CRITICAL   // <4% available
}
```

---

### 2. UltraLowMemoryManagerV3 (Budget Calculator)

**Purpose**: Calculates per-component memory budgets based on device capabilities, thermal state, and network conditions

```kotlin
val memoryManager = hiltViewModel().get<UltraLowMemoryManagerV3>()

val config = memoryManager.getOptimizedConfig(
    quality = VideoQuality.UHD_4K,
    hasUpscaling = true,
    hasTranscoding = false,
    networkSpeedMbps = 15,
    thermalState = ThermalState.NORMAL
)

// Access individual budgets
Log.d(TAG, "P2P Buffer: ${config.p2pBufferSize / 1024 / 1024}MB")
Log.d(TAG, "Decoder Buffer: ${config.decoderBuffer / 1024 / 1024}MB")
Log.d(TAG, "Upscaler Buffer: ${config.upscalerBuffer / 1024 / 1024}MB")
Log.d(TAG, "Max Peers: ${config.maxPeers}")
Log.d(TAG, "Chunk Size: ${config.chunkSize / 1024}KB")
```

#### Thermal State Multipliers

| Thermal State | Quality Multiplier | Max Peers (4K) | Chunk Size |
|--------------|-------------------|----------------|------------|
| NORMAL | 1.0x | 10 | 512KB |
| LIGHT | 0.9x | 8 | 512KB |
| MODERATE | 0.7x | 6 | 512KB |
| SEVERE | 0.5x | 4 | 256KB |
| CRITICAL | 0.3x | 2 | 128KB |

---

### 3. ZeroCopyBufferManager

**Purpose**: Direct ByteBuffer pooling with memory-mapped file support

```kotlin
val bufferManager = hiltViewModel().get<ZeroCopyBufferManager>()

// Initialize pool (call once)
bufferManager.initializeBufferPool(chunkSize = 1024 * 1024, capacity = 16)

// Acquire direct buffer (zero-copy)
val buffer = bufferManager.acquireDirectBuffer(1024 * 1024)
try {
    // Use buffer for network I/O, decoder input, etc.
    networkRead(buffer)
    decoder.queueInputBuffer(buffer)
} finally {
    bufferManager.releaseDirectBuffer(buffer)  // Return to pool
}

// Memory-mapped file access (large files)
val mappedBuffer = bufferManager.acquireMappedBuffer(largeFile)
try {
    processFile(mappedBuffer)
} finally {
    bufferManager.releaseMappedBuffer(mappedBuffer)
}
```

#### Stats Monitoring

```kotlin
val stats = bufferManager.getCacheStats()
// {
//   "directBufferPoolMB": 12,
//   "mappedBufferMB": 8,
//   "totalAllocatedMB": 20,
//   "activePools": 3,
//   "hitRate": "96.50%"
// }
```

---

### 4. YuvFramePool

**Purpose**: YUV420 planar frame pooling (avoids ARGB conversion overhead)

```kotlin
val framePool = hiltViewModel().get<YuvFramePool>()

// Acquire YUV frame for rendering
val frame = framePool.acquireFrame(1920, 1080)

try {
    // Fill from decoder
    frame.copyFrom(decoderOutputImage)
    
    // Render via RenderScript/GPU
    upscaler.scaleFrame(frame, 3840, 2160)
    
} finally {
    framePool.releaseFrame(frame)  // Returns to pool
}

// For RenderScript interop
val allocation = framePool.acquireAllocation(1920, 1080, Element.U8_4(rs))
try {
    rsScript.forEach(allocation)
} finally {
    framePool.releaseAllocation(allocation)
}
```

#### Frame Structure (YUV420)

```
Y Plane:  width × height          bytes
U Plane:  width/2 × height/2      bytes  
V Plane:  width/2 × height/2      bytes
Total:    1.5 × width × height    bytes
```

---

### 5. CompressedFrameCache

**Purpose**: Zstd/Deflate compression for YUV frames with key-frame preservation

```kotlin
val frameCache = hiltViewModel().get<CompressedFrameCache>()

// Initialize with budget
frameCache.initialize(CompressedFrameCache.CacheConfig(
    maxSizeBytes = 16 * 1024 * 1024,  // 16MB
    maxFrames = 60,                    // ~2 seconds at 30fps
    enableCompression = true,
    keepKeyFrames = true,
    keyFrameRatio = 0.3
))

// Store frame (async compression)
val success = frameCache.putFrame(
    frameId = System.currentTimeMillis(),
    timestamp = System.currentTimeMillis(),
    yuvFrame = frame,
    isKeyFrame = isKeyFrame
).await()

// Retrieve frame (async decompression)
val decompressed = frameCache.getFrame(frameId).await()
decompressed?.let { frame ->
    renderFrame(frame)
    framePool.releaseFrame(frame)
}
```

#### Compression Ratios (Typical)

| Content Type | Y Plane | UV Planes | Overall |
|-------------|---------|-----------|---------|
| Talking head | 2.5x | 3.2x | **2.7x** |
| Animation | 1.8x | 2.5x | **2.0x** |
| Sports/Action | 1.4x | 1.8x | **1.5x** |
| Static UI | 4.0x | 5.0x | **4.3x** |

---

### 6. AdaptivePrebufferManager

**Purpose**: Network-aware prebuffering with trend detection

```kotlin
val prebufferManager = hiltViewModel().get<AdaptivePrebufferManager>()

// Register stream
prebufferManager.registerStream(
    streamId = "movie_123",
    chunkSize = 1024 * 1024,      // 1MB chunks
    estimatedBitrateKbps = 8000,  // 8 Mbps
    isLive = false
)

// Call on each chunk download
prebufferManager.recordChunkDownload("movie_123", chunkIndex, bytesDownloaded, durationMs)

// Call when chunk consumed by decoder
prebufferManager.recordChunkConsumed("movie_123", chunkIndex, bytesConsumed)

// Get optimal prebuffer count (call before requesting next chunk)
val optimalChunks = prebufferManager.getOptimalPrebufferChunks("movie_123")

// Get next chunks to prebuffer
val nextChunks = prebufferManager.getNextChunksToPrefetch("movie_123", currentChunk)
nextChunks.forEach { chunkIndex ->
    downloadChunk(chunkIndex)
    prebufferManager.markChunkPrefetched("movie_123", chunkIndex)
}
```

#### Network Trend Detection

| Trend | Stability | Action |
|-------|-----------|--------|
| IMPROVING | >0.7 | Reduce prebuffer, increase quality |
| STABLE | 0.5-0.7 | Maintain current strategy |
| DEGRADING | <0.5 | Increase prebuffer, reduce quality |

---

### 7. ThermalQualityController

**Purpose**: Real-time thermal throttling with quality/bitrate/FPS scaling

```kotlin
val thermalController = hiltViewModel().get<ThermalQualityController>()

// Register stream
thermalController.registerStream(ThermalQualityController.QualityConfig(
    streamId = "movie_123",
    maxQuality = ThermalQualityController.VideoQuality.UHD_4K,
    minQuality = ThermalQualityController.VideoQuality.HD_720P,
    currentQuality = ThermalQualityController.VideoQuality.FHD_1080P,
    enableAutoScaling = true,
    upscalingEnabled = true,
    atmosEnabled = true,
    targetFps = 60,
    maxBitrateKbps = 25000
))

// Observe quality changes
thermalController.getCurrentQuality("movie_123")?.let { state ->
    Log.d(TAG, "Current: ${state.currentQuality} @ ${state.currentBitrateKbps}kbps")
    Log.d(TAG, "Upscaling: ${state.upscalingActive}, Atmos: ${state.atmosActive}")
}

// Manual override
thermalController.forceQuality("movie_123", ThermalQualityController.VideoQuality.HD_720P)

// Get thermal stats
val stats = thermalController.getThermalStats()
/*
{
  "currentState": "MODERATE",
  "currentMultiplier": "70%",
  "streams": [
    {
      "streamId": "movie_123",
      "quality": "FHD_1080P",
      "bitrateKbps": 8000,
      "fps": 30,
      "upscaling": false,
      "atmos": false
    }
  ]
}
*/
```

---

### 8. OptimizedP2PEngine

**Purpose**: Delta piece sharing with peer scoring

```kotlin
val p2pEngine = hiltViewModel().get<OptimizedP2PEngine>()

// Initialize
p2pEngine.initialize(
    torrentHash = "movie_123",
    maxPeers = 10,
    chunkSize = 1024 * 1024,
    enableDelta = true
)

// Request piece
val buffer = p2pEngine.requestPiece(pieceIndex)
buffer?.let {
    decoder.queueInputBuffer(it)
    p2pEngine.releasePiece(it)
}

// Observe stats
p2pEngine.stats.collect { stats ->
    Log.d(TAG, "Peers: ${stats.connectedPeers}, Speed: ${stats.downloadSpeedKbps}kbps")
    Log.d(TAG, "Hit Rate: ${String.format("%.1f%%", stats.pieceHitRate * 100)}")
}

// Get peer details
val peerStats = p2pEngine.getPeerStats()
// [
//   {"id": "peer_1", "score": 0.92, "quality": "EXCELLENT", "latencyMs": 23, "downloadedMB": 45},
//   {"id": "peer_2", "score": 0.67, "quality": "GOOD", "latencyMs": 89, "downloadedMB": 12}
// ]
```

---

## Integration Patterns

### Pattern 1: ExoPlayer Integration (App Module)

```kotlin
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val memoryManager: KuroStreamMemoryManager,
    private val playbackController: MemoryAwarePlaybackController
) : ViewModel() {

    private val exoPlayer = ExoPlayer.Builder(context).build()

    fun play(streamId: String, quality: VideoQuality) {
        // 1. Initialize memory subsystem
        val config = KuroStreamMemoryManager.UnifiedConfig(
            streamId = streamId,
            quality = quality,
            hasUpscaling = true,
            networkSpeedMbps = networkMonitor.currentSpeedMbps,
            enableP2P = true,
            enableCompressedFrames = true,
            maxBitrateKbps = 25000,
            targetFps = 60,
            upscalingEnabled = true
        )
        
        memoryManager.initialize(config)
        
        // 2. Create media source
        val mediaSource = createMediaSource(streamId)
        
        // 3. Attach to playback controller
        playbackController.attachExoPlayer(exoPlayer, streamId, mediaSource)
        
        // 4. Start playback
        exoPlayer.playWhenReady = true
    }

    fun onChunkDownloaded(chunkIndex: Long, bytes: Int, durationMs: Long) {
        playbackController.recordChunkDownload(chunkIndex, bytes, durationMs)
    }

    fun onChunkConsumed(chunkIndex: Long, bytes: Int) {
        playbackController.recordChunkConsumed(chunkIndex, bytes)
    }

    override fun onCleared() {
        playbackController.shutdown()
        exoPlayer.release()
        super.onCleared()
    }
}
```

### Pattern 2: KuroEngine Integration (Playback Module)

```kotlin
class KuroEngineWithMemory @Inject constructor(
    private val memoryManager: KuroStreamMemoryManager,
    private val framePool: YuvFramePool,
    private val frameCache: CompressedFrameCache,
) : PlayerInterface {

    private var currentConfig: UnifiedConfig? = null

    override fun loadMedia(mediaItem: CoreMediaItem) {
        val config = UnifiedConfig(
            streamId = mediaItem.uri,
            quality = detectQuality(mediaItem),
            hasUpscaling = mediaItem.requiresUpscaling,
            hasTranscoding = mediaItem.requiresAudioTranscode,
            networkSpeedMbps = networkMonitor.currentSpeedMbps,
            enableP2P = mediaItem.supportsP2P,
            enableCompressedFrames = true,
            enableDeltaP2P = true,
            maxBitrateKbps = mediaItem.maxBitrateKbps,
            targetFps = 60,
            upscalingEnabled = mediaItem.requiresUpscaling,
            atmosEnabled = mediaItem.hasAtmos
        )
        
        memoryManager.initialize(config)
        currentConfig = config
        
        // Initialize disk buffer with memory-aware config
        val memConfig = memoryManager.getCurrentConfig()
        diskBufferManager = DiskBufferManager(
            DiskBufferConfig(
                bufferSizeMb = memConfig?.p2pBufferSize?.let { it / 1024 / 1024 } ?: 50,
                maxReadAheadMb = 4
            )
        )
        
        // Start native playback
        nativePlaybackLoop()
    }

    private fun nativePlaybackLoop() {
        scope.launch {
            while (isPlaying) {
                // Acquire YUV frame from pool
                val frame = framePool.acquireFrame(width, height)
                
                // Decode into frame (zero-copy)
                nativeDecodeIntoFrame(ffmpegHandle, frame.buffer)
                
                // Cache compressed if enabled
                if (currentConfig?.enableCompressedFrames == true && isKeyFrame) {
                    frameCache.putFrame(
                        frameId = System.currentTimeMillis(),
                        timestamp = currentPositionMs,
                        yuvFrame = frame,
                        isKeyFrame = true
                    )
                }
                
                // Render frame
                renderFrame(frame)
                
                // Release back to pool
                framePool.releaseFrame(frame)
            }
        }
    }
}
```

---

## Memory Budget Formula

```
Total Budget = Available RAM × Thermal Multiplier × Network Multiplier × Device Factor

Device Factor:
- <512MB: 0.35
- 512MB-1GB: 0.45
- 1GB-1.5GB: 0.55
- 1.5GB-2GB: 0.60
- 2GB-3GB: 0.65
- >3GB: 0.70

Component Allocation:
- P2P Buffer:      10-15% of budget
- Decoder Buffer:  15-20% of budget
- Upscaler:        15-20% of budget (if enabled)
- Frame Pool:      5-8% of budget
- Compressed Cache: 6-10% of budget
- Zero-Copy:       3-5% of budget
- Audio:           3-5% of budget
- Network:         2-4% of budget
- UI/Compose:      4-8% of budget
```

---

## ProGuard/R8 Rules

```proguard
# KuroStream Memory Management
-keep class com.kurostream.playback.memory.** { *; }
-keepclassmembers class com.kurostream.playback.memory.** { *; }

# Hilt generated
-keep class dagger.hilt.** { *; }
-keepclassmembers class dagger.hilt.** { *; }

# Coroutines
-keep class kotlinx.coroutines.** { *; }

# Flows
-keep class kotlinx.coroutines.flow.** { *; }
```

---

## Performance Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| 1080p P2P Peak RAM | ≤30 MB | `adb shell dumpsys meminfo` |
| 1080p→4K Peak RAM | ≤40 MB | `adb shell dumpsys meminfo` |
| Frame Pool Hit Rate | ≥90% | `YuvFramePool.getStats()` |
| Zero-Copy Hit Rate | ≥95% | `ZeroCopyBufferManager.getCacheStats()` |
| Frame Compression Ratio | ≥1.5x | `CompressedFrameCache.getStats()` |
| Prebuffer Efficiency | ≥85% | `AdaptivePrebufferManager.getGlobalStats()` |
| Thermal Response Time | <1s | `ThermalQualityController` logs |
| Memory Trim Latency | <50ms | `MemoryManager.trimMemory()` |

---

## Migration Guide (Legacy → Phase 4+)

| Legacy Component | Replacement |
|-----------------|-------------|
| `MemoryManager` | `UltraLowMemoryManagerV3` |
| `AdaptiveMemoryManager` | `UltraLowMemoryManagerV3` |
| `UltraLowMemoryManager` | `UltraLowMemoryManagerV3` |
| `BufferPool` | `ZeroCopyBufferManager` |
| Manual ` | `BitmapPool` | `YuvFramePool` + `CompressedFrameCache` |
| `DiskBackedLoadControl` | `AdaptivePrebufferManager` |
| `ThermalGuard` | `ThermalQualityController` |
| `UltraLightP2P` | `OptimizedP2PEngine` |

---

## Testing

```kotlin
@Test
fun test1080pMemoryBudget() {
    val memoryManager = UltraLowMemoryManagerV3(context)
    
    val config = memoryManager.getOptimizedConfig(
        quality = VideoQuality.FHD_1080P,
        hasUpscaling = false,
        hasTranscoding = false,
        networkSpeedMbps = 10,
        thermalState = ThermalState.NORMAL
    )
    
    val totalBudget = config.p2pBufferSize + config.decoderBuffer + 
                     config.framePoolSize + config.compressedCacheSize + 
                     config.networkBuffer + config.uiBuffer
    
    assertTrue("1080p budget should be <30MB", totalBudget < 30 * 1024 * 1024)
}

@Test
fun testFramePoolRecycling() {
    val framePool = YuvFramePool()
    val frame1 = framePool.acquireFrame(1920, 1080)
    framePool.releaseFrame(frame1)
    
    val frame2 = framePool.acquireFrame(1920, 1080)
    
    // Should reuse same buffer
    assertTrue("Frame pool should recycle", framePool.getStats().hitRate > 0.8)
}
```

---

*Generated: 2026-07-15*  
*API Version: 1.0.0*  
*KuroStream Ultra-Low RAM Phase 4+*