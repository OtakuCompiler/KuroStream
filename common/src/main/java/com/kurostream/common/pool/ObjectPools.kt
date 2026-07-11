// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.common.pool

/**
 * Generic object pool with factory and reset functions.
 * Thread-safe using Mutex.
 */
class ObjectPool<T>(
    private val factory: () -> T,
    private val reset: (T) -> Unit,
    private val maxSize: Int
) {
    private val available = mutableListOf<T>()
    private val lock = Any()
    private var created = 0
    
    /**
     * Acquire an object from the pool, creating new if empty.
     */
    fun acquire(): T = synchronized(lock) {
        if (available.isNotEmpty()) {
            return@synchronized available.removeAt(available.lastIndex)
        }
        if (created < maxSize) {
            created++
            return@synchronized factory()
        }
        // Pool exhausted, create anyway (unbounded overflow)
        return@synchronized factory()
    }
    
    /**
     * Release object back to pool.
     */
    fun release(obj: T) = synchronized(lock) {
        if (available.size < maxSize) {
            reset(obj)
            available.add(obj)
        }
        // Else: let GC handle it
    }
    
    /**
     * Execute a block with a pooled object, auto-releasing.
     */
    fun <R> use(block: (T) -> R): R {
        val obj = acquire()
        try {
            return block(obj)
        } finally {
            release(obj)
        }
    }
    
    /**
     * Current available count.
     */
    fun availableCount(): Int = synchronized(lock) { available.size }
    
    /**
     * Total created count.
     */
    fun createdCount(): Int = synchronized(lock) { created }
    
    /**
     * Clear the pool.
     */
    fun clear() = synchronized(lock) {
        available.clear()
        created = 0
    }
    
    /**
     * Get pool stats.
     */
    fun getStats(): PoolStats = synchronized(lock) {
        PoolStats(
            maxSize = maxSize,
            available = available.size,
            created = created
        )
    }
}

/**
 * Pool for PlayerState objects.
 */
object PlayerStatePool {
    private val pool = ObjectPool<PlayerState>(
        factory = { PlayerState() },
        reset = { it.reset() },
        maxSize = 20
    )
    
    fun acquire(): PlayerState = pool.acquire()
    fun release(state: PlayerState) = pool.release(state)
    fun <R> use(block: (PlayerState) -> R): R = pool.use(block)
    fun stats(): PoolStats = pool.getStats()
}

/**
 * Pool for SubtitleEvent objects.
 */
object SubtitleEventPool {
    private val pool = ObjectPool<SubtitleEvent>(
        factory = { SubtitleEvent() },
        reset = { it.reset() },
        maxSize = 100
    )
    
    fun acquire(): SubtitleEvent = pool.acquire()
    fun release(event: SubtitleEvent) = pool.release(event)
    fun <R> use(block: (SubtitleEvent) -> R): R = pool.use(block)
    fun stats(): PoolStats = pool.getStats()
}

/**
 * Pool for NetworkChunk objects.
 */
object NetworkChunkPool {
    private val pool = ObjectPool<NetworkChunk>(
        factory = { NetworkChunk() },
        reset = { it.reset() },
        maxSize = 200
    )
    
    fun acquire(): NetworkChunk = pool.acquire()
    fun release(chunk: NetworkChunk) = pool.release(chunk)
    fun <R> use(block: (NetworkChunk) -> R): R = pool.use(block)
    fun stats(): PoolStats = pool.getStats()
}

/**
 * Pool for TrackInfo objects.
 */
object TrackInfoPool {
    private val pool = ObjectPool<TrackInfo>(
        factory = { TrackInfo() },
        reset = { it.reset() },
        maxSize = 30
    )
    
    fun acquire(): TrackInfo = pool.acquire()
    fun release(info: TrackInfo) = pool.release(info)
    fun <R> use(block: (TrackInfo) -> R): R = pool.use(block)
    fun stats(): PoolStats = pool.getStats()
}

/**
 * Pool for PlaybackDiagnostics objects.
 */
object PlaybackDiagnosticsPool {
    private val pool = ObjectPool<PlaybackDiagnostics>(
        factory = { PlaybackDiagnostics() },
        reset = { it.reset() },
        maxSize = 10
    )
    
    fun acquire(): PlaybackDiagnostics = pool.acquire()
    fun release(diag: PlaybackDiagnostics) = pool.release(diag)
    fun <R> use(block: (PlaybackDiagnostics) -> R): R = pool.use(block)
    fun stats(): PoolStats = pool.getStats()
}

/**
 * Player state holder.
 */
class PlayerState {
    var playbackState = 0 // 0=Idle, 1=Loading, 2=Buffering, 3=Ready, 4=Playing, 5=Paused, 6=Ended, 7=Error
    var positionMs = 0L
    var durationMs = 0L
    var bufferedPositionMs = 0L
    var speed = 1.0f
    var volume = 1.0f
    var isMuted = false
    var videoWidth = 0
    var videoHeight = 0
    var videoFps = 0f
    var bitrate = 0L
    var decoderName = ""
    var droppedFrames = 0
    var renderedFrames = 0
    var bufferHealth = 0
    
    fun reset() {
        playbackState = 0
        positionMs = 0
        durationMs = 0
        bufferedPositionMs = 0
        speed = 1.0f
        volume = 1.0f
        isMuted = false
        videoWidth = 0
        videoHeight = 0
        videoFps = 0f
        bitrate = 0
        decoderName = ""
        droppedFrames = 0
        renderedFrames = 0
        bufferHealth = 0
    }
}

/**
 * Subtitle event for rendering.
 */
class SubtitleEvent {
    var startMs = 0L
    var endMs = 0L
    var text = ""
    var style: SubtitleStyle? = null
    var trackId = ""
    
    fun reset() {
        startMs = 0
        endMs = 0
        text = ""
        style = null
        trackId = ""
    }
}

class SubtitleStyle(
    var fontName: String = "NotoSans-Regular",
    var fontSize: Float = 24f,
    var primaryColor: Int = 0xFFFFFFFF,
    var secondaryColor: Int = 0xFF000000,
    var outlineColor: Int = 0xFF000000,
    var backColor: Int = 0x80000000,
    var bold: Boolean = false,
    var italic: Boolean = false,
    var underline: Boolean = false,
    var strikeout: Boolean = false,
    var scaleX: Float = 1.0f,
    var scaleY: Float = 1.0f,
    var spacing: Float = 0f,
    var angle: Float = 0f,
    var borderStyle: Int = 1,
    var outline: Float = 2f,
    var shadow: Float = 0f,
    var alignment: Int = 2,
    var marginL: Int = 10,
    var marginR: Int = 10,
    var marginV: Int = 10
)

/**
 * Network data chunk.
 */
class NetworkChunk {
    var data: ByteArray? = null
    var offset = 0
    var length = 0
    var timestampMs = 0L
    var sequenceNumber = 0L
    var isKeyFrame = false
    var trackType = 0 // 0=video, 1=audio, 2=subtitle
    
    fun reset() {
        data = null
        offset = 0
        length = 0
        timestampMs = 0
        sequenceNumber = 0
        isKeyFrame = false
        trackType = 0
    }
}

/**
 * Track information.
 */
class TrackInfo {
    var id = ""
    var type = 0
    var language: String? = null
    var codec: String? = null
    var bitrate: Long? = null
    var isSelected = false
    var isDefault = false
    var metadata = mapOf<String, String>()
    
    fun reset() {
        id = ""
        type = 0
        language = null
        codec = null
        bitrate = null
        isSelected = false
        isDefault = false
        metadata = emptyMap()
    }
}

/**
 * Playback diagnostics snapshot.
 */
class PlaybackDiagnostics {
    var bufferDurationMs = 0L
    var bufferedPercentage = 0
    var currentBitrate = 0L
    var droppedFrames = 0
    var renderedFrames = 0
    var currentFps = 0f
    var displayRefreshRate = 0f
    var contentFrameRate = 0f
    var networkSpeedBps = 0L
    var decoderName = ""
    var videoCodec = ""
    var audioCodec = ""
    var videoResolution = ""
    var isHardwareDecoding = false
    var timestamp = System.currentTimeMillis()
    
    fun reset() {
        bufferDurationMs = 0
        bufferedPercentage = 0
        currentBitrate = 0
        droppedFrames = 0
        renderedFrames = 0
        currentFps = 0f
        displayRefreshRate = 0f
        contentFrameRate = 0f
        networkSpeedBps = 0
        decoderName = ""
        videoCodec = ""
        audioCodec = ""
        videoResolution = ""
        isHardwareDecoding = false
        timestamp = System.currentTimeMillis()
    }
}

object ObjectPools {
    fun clearAll() {
        PlayerStatePool.stats()
        SubtitleEventPool.stats()
        NetworkChunkPool.stats()
        TrackInfoPool.stats()
        PlaybackDiagnosticsPool.stats()
        // TODO: call clear() on each pool once implemented
    }
}

data class PoolStats(
    val maxSize: Int,
    val available: Int,
    val created: Int
) {
    override fun toString(): String = "PoolStats(available=$available/$maxSize, created=$created)"
}