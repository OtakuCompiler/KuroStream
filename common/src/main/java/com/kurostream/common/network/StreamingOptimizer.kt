package com.kurostream.common.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class AbrProfile(
    val initialBitrate: Long = 2_000_000,
    val minBitrate: Long = 300_000,
    val maxBitrate: Long = 50_000_000,
    val bandwidthFraction: Double = 0.75,
    val downscaleRatio: Double = 0.5,
    val upscaleRatio: Double = 1.5,
)

data class ChunkSpec(
    val sizeBytes: Int = 256 * 1024,
    val maxParallelChunks: Int = 4,
    val prefetchAheadMs: Long = 30_000,
    val bufferTargetMs: Long = 60_000,
    val bufferMaxMs: Long = 120_000,
)

data class FailoverConfig(
    val retryCount: Int = 3,
    val timeoutMs: Long = 10_000,
    val retryDelayMs: Long = 1_000,
    val failoverSources: List<String> = emptyList(),
)

data class P2PConfig(
    val enabled: Boolean = true,
    val maxPeers: Int = 8,
    val uploadBandwidthKbps: Int = 500,
    val downloadBandwidthKbps: Int = 2000,
)

class StreamingOptimizer {
    val abrProfile = AbrProfile()
    val chunkSpec = ChunkSpec()
    val failoverConfig = FailoverConfig()
    val p2pConfig = P2PConfig()

    fun selectOptimalQuality(
        bandwidthEstimateMbps: Double,
        displayResolution: Pair<Int, Int>,
    ): StreamingQuality {
        return when {
            bandwidthEstimateMbps >= 50 -> StreamingQuality.UHD_4K
            bandwidthEstimateMbps >= 25 -> StreamingQuality.HIGH_1080P
            bandwidthEstimateMbps >= 10 -> StreamingQuality.MEDIUM_720P
            else -> StreamingQuality.LOW_480P
        }
    }

    fun calculateChunkCount(fileSizeBytes: Long, chunkSize: Int = chunkSpec.sizeBytes): Int {
        return (fileSizeBytes / chunkSize + if (fileSizeBytes % chunkSize == 0L) 0 else 1).toInt()
    }

    fun estimateDownloadTime(
        fileSizeBytes: Long,
        bandwidthMbps: Double,
    ): Long {
        if (bandwidthMbps <= 0) return Long.MAX_VALUE
        val bits = fileSizeBytes * 8
        return (bits / (bandwidthMbps * 1_000_000) * 1000).toLong()
    }
}

fun <T> Flow<List<T>>.distinctList(): Flow<List<T>> = distinctUntilChanged { old, new ->
    old.size == new.size && old.zip(new) { a, b -> a == b }.all { it }
}
