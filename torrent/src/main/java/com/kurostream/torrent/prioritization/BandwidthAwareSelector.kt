package com.kurostream.torrent.prioritization

import android.util.Log
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BandwidthAwareSelector @Inject constructor() {

    private val TAG = "BandwidthAwareSelector"

    enum class BandwidthProfile(val label: String) {
        ULTRA_FAST("Ultra Fast (>50Mbps)"),
        FAST("Fast (20-50Mbps)"),
        MEDIUM("Medium (5-20Mbps)"),
        SLOW("Slow (1-5Mbps)"),
        VERY_SLOW("Very Slow (<1Mbps)"),
    }

    data class BandwidthState(
        val profile: BandwidthProfile = BandwidthProfile.MEDIUM,
        val currentSpeedBps: Long = 0,
        val avgSpeedBps: Long = 0,
        val isStable: Boolean = true,
        val fluctuationCount: Int = 0,
    )

    private val speedHistory = ArrayDeque<Long>(30)
    private var lastState = BandwidthState()

    fun detectProfile(currentSpeedBps: Long): BandwidthProfile {
        speedHistory.addLast(currentSpeedBps)
        if (speedHistory.size > 30) speedHistory.removeFirst()

        val avgSpeed = if (speedHistory.isNotEmpty()) speedHistory.average().toLong() else 0
        val variance = if (speedHistory.size > 5) {
            val mean = speedHistory.average()
            speedHistory.map { (it - mean).pow(2) }.average()
        } else 0.0

        val isStable = variance < (avgSpeed * 0.5).pow(2)

        val profile = when {
            avgSpeed > 50_000_000 -> BandwidthProfile.ULTRA_FAST
            avgSpeed > 20_000_000 -> BandwidthProfile.FAST
            avgSpeed > 5_000_000 -> BandwidthProfile.MEDIUM
            avgSpeed > 1_000_000 -> BandwidthProfile.SLOW
            else -> BandwidthProfile.VERY_SLOW
        }

        lastState = BandwidthState(
            profile = profile,
            currentSpeedBps = currentSpeedBps,
            avgSpeedBps = avgSpeed,
            isStable = isStable,
        )

        return profile
    }

    fun getRecommendedPiecePriority(profile: BandwidthProfile): Int {
        return when (profile) {
            BandwidthProfile.ULTRA_FAST -> 7
            BandwidthProfile.FAST -> 6
            BandwidthProfile.MEDIUM -> 4
            BandwidthProfile.SLOW -> 2
            BandwidthProfile.VERY_SLOW -> 1
        }
    }

    fun getRecommendedMaxPieces(profile: BandwidthProfile): Int {
        return when (profile) {
            BandwidthProfile.ULTRA_FAST -> 500
            BandwidthProfile.FAST -> 300
            BandwidthProfile.MEDIUM -> 200
            BandwidthProfile.SLOW -> 100
            BandwidthProfile.VERY_SLOW -> 50
        }
    }

    fun shouldReduceQuality(profile: BandwidthProfile): Boolean {
        return profile == BandwidthProfile.SLOW || profile == BandwidthProfile.VERY_SLOW
    }

    fun applyProfileToTorrent(handle: TorrentHandle, profile: BandwidthProfile) {
        if (!handle.isValid) return

        when (profile) {
            BandwidthProfile.ULTRA_FAST, BandwidthProfile.FAST -> {
                handle.setSequentialDownload(true)
            }
            BandwidthProfile.MEDIUM -> {
                handle.setSequentialDownload(true)
            }
            BandwidthProfile.SLOW, BandwidthProfile.VERY_SLOW -> {
                handle.setSequentialDownload(true)
                val uploadLimit = if (profile == BandwidthProfile.VERY_SLOW) 10 * 1024 else 50 * 1024
                handle.setUploadLimit(uploadLimit)
            }
        }

        Log.d(TAG, "Applied ${profile.label} profile to torrent")
    }

    fun getState(): BandwidthState = lastState

    private fun Double.pow(n: Int): Double {
        var result = 1.0
        repeat(n) { result *= this }
        return result
    }
}
