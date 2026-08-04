package com.kurostream.common.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * Device capability tier used to tune every subsystem (player buffers, image
 * cache, GPU pool, etc.) toward the <125 MB active RAM budget.
 *
 * Tier thresholds (physical RAM):
 *   LOW  — ≤2 GB   (Fire Stick Lite, budget tablets)
 *   MID  — ≤4 GB   (Fire TV Stick 4K, Shield TV compact)
 *   HIGH — >4 GB   (Shield TV Pro, high-end phones)
 */
enum class RamTier { LOW, MID, HIGH }

object LowRamDevice {
    private var _context: Context? = null

    fun init(context: Context) {
        _context = context.applicationContext
    }

    val ramTier: RamTier by lazy {
        val mb = totalMemoryMb
        when {
            mb <= 2048 -> RamTier.LOW
            mb <= 4096 -> RamTier.MID
            else       -> RamTier.HIGH
        }
    }

    val isLowRamDevice: Boolean by lazy {
        val ctx = _context ?: return@lazy true
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return@lazy true
        am.isLowRamDevice || ramTier == RamTier.LOW
    }

    val totalMemoryMb: Int by lazy {
        val ctx = _context ?: return@lazy 1024
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return@lazy 1024
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        (mi.totalMem / (1024 * 1024)).toInt()
    }

    // ── Image cache ─────────────────────────────────────────────────────────

    val maxImageCacheBytes: Int
        get() = when (ramTier) {
            RamTier.LOW  -> 2  * 1024 * 1024
            RamTier.MID  -> 6  * 1024 * 1024
            RamTier.HIGH -> 12 * 1024 * 1024
        }

    val coilMemoryCacheSize: Int get() = maxImageCacheBytes

    val coilDiskCacheSize: Long
        get() = when (ramTier) {
            RamTier.LOW  -> 25L * 1024 * 1024
            RamTier.MID  -> 50L * 1024 * 1024
            RamTier.HIGH -> 100L * 1024 * 1024
        }

    val maxSimultaneousImages: Int
        get() = when (ramTier) {
            RamTier.LOW  -> 2
            RamTier.MID  -> 4
            RamTier.HIGH -> 8
        }

    // ── Player buffers — tuned to keep active heap under 125 MB ─────────────

    /** ExoPlayer DefaultLoadControl min buffer (ms). */
    val exoMinBufferMs: Int
        get() = when (ramTier) {
            RamTier.LOW  -> 8_000
            RamTier.MID  -> 12_000
            RamTier.HIGH -> 15_000
        }

    /** ExoPlayer DefaultLoadControl max buffer (ms). */
    val exoMaxBufferMs: Int
        get() = when (ramTier) {
            RamTier.LOW  -> 20_000
            RamTier.MID  -> 35_000
            RamTier.HIGH -> 50_000
        }

    /** ExoPlayer target buffer bytes. */
    val exoTargetBufferBytes: Int
        get() = when (ramTier) {
            RamTier.LOW  -> 10 * 1024 * 1024
            RamTier.MID  -> 20 * 1024 * 1024
            RamTier.HIGH -> 32 * 1024 * 1024
        }

    /** VLC network-caching ms. */
    val vlcNetworkCacheMs: Int
        get() = when (ramTier) {
            RamTier.LOW  -> 1500
            RamTier.MID  -> 3000
            RamTier.HIGH -> 5000
        }

    // ── Decoder / GPU ────────────────────────────────────────────────────────

    val maxDecoderFrameBuffers: Int
        get() = when (ramTier) {
            RamTier.LOW  -> 1
            RamTier.MID  -> 2
            RamTier.HIGH -> 3
        }

    val maxGpuPoolTextures: Int
        get() = when (ramTier) {
            RamTier.LOW  -> 2
            RamTier.MID  -> 4
            RamTier.HIGH -> 6
        }

    val upscaleRingBufferSeconds: Int
        get() = when (ramTier) {
            RamTier.LOW  -> 0
            RamTier.MID  -> 2
            RamTier.HIGH -> 4
        }

    // ── Subtitle cache ───────────────────────────────────────────────────────

    val maxSubtitleCacheBytes: Int
        get() = when (ramTier) {
            RamTier.LOW  -> 2  * 1024 * 1024
            RamTier.MID  -> 4  * 1024 * 1024
            RamTier.HIGH -> 8  * 1024 * 1024
        }

    // ── Metadata in-memory LRU ───────────────────────────────────────────────

    /** Max entries in the metadata in-memory LRU cache. */
    val metadataLruSize: Int
        get() = when (ramTier) {
            RamTier.LOW  -> 20
            RamTier.MID  -> 60
            RamTier.HIGH -> 150
        }
}
