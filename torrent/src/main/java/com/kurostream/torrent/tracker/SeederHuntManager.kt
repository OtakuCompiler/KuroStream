package com.kurostream.torrent.tracker

import android.util.Log
import com.frostwire.jlibtorrent.AlertListener
import com.frostwire.jlibtorrent.Session
import com.frostwire.jlibtorrent.TorrentHandle
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeederHuntManager @Inject constructor(
    private val trackerListProvider: TrackerListProvider,
) {

    private val TAG = "SeederHuntManager"

    private val huntJobs = ConcurrentHashMap<String, Job>()
    private val peerDiscoveryHistory = ConcurrentHashMap<String, MutableList<PeerSnapshot>>()

    data class PeerSnapshot(
        val timestamp: Long,
        val seeds: Int,
        val peers: Int,
        val dhtNodes: Int,
    )

    data class HuntStats(
        val infoHash: String,
        val totalPeersDiscovered: Int,
        val seedsFound: Int,
        val announceCount: Int,
        val dhtLookups: Int,
        val lastHuntTime: Long,
    )

    private val _activeHunts = MutableStateFlow(0)
    val activeHunts: StateFlow<Int> = _activeHunts.asStateFlow()

    fun startHunting(infoHash: String, handle: TorrentHandle, scope: CoroutineScope) {
        huntJobs[infoHash]?.cancel()
        peerDiscoveryHistory[infoHash] = mutableListOf()

        huntJobs[infoHash] = scope.launch(Dispatchers.IO) {
            _activeHunts.value = huntJobs.size
            var announceCount = 0
            var dhtLookups = 0
            var interval = 15_000L

            while (isActive) {
                try {
                    val status = handle.status()
                    val seeds = status.numSeeds
                    val peers = status.numPeers

                    peerDiscoveryHistory[infoHash]?.add(
                        PeerSnapshot(
                            timestamp = System.currentTimeMillis(),
                            seeds = seeds,
                            peers = peers,
                            dhtNodes = 0,
                        )
                    )

                    if (seeds < 5 && interval > 10_000L) {
                        interval = 10_000L
                    } else if (seeds >= 20) {
                        interval = 60_000L
                    }

                    handle.forceReannounce()
                    announceCount++

                    if (seeds < 10) {
                        handle.scrapeTracker()
                        dhtLookups++
                    }

                    delay(interval)
                } catch (e: Exception) {
                    Log.w(TAG, "Hunt cycle failed for $infoHash", e)
                    delay(30_000L)
                }
            }
        }
    }

    fun stopHunting(infoHash: String) {
        huntJobs[infoHash]?.cancel()
        huntJobs.remove(infoHash)
        peerDiscoveryHistory.remove(infoHash)
        _activeHunts.value = huntJobs.size
    }

    fun getHuntStats(infoHash: String): HuntStats? {
        val history = peerDiscoveryHistory[infoHash] ?: return null
        val totalPeers = history.sumOf { it.peers }
        val totalSeeds = history.sumOf { it.seeds }
        return HuntStats(
            infoHash = infoHash,
            totalPeersDiscovered = totalPeers,
            seedsFound = totalSeeds,
            announceCount = history.size,
            dhtLookups = 0,
            lastHuntTime = history.lastOrNull()?.timestamp ?: 0L,
        )
    }

    fun stopAll() {
        huntJobs.keys.toList().forEach { stopHunting(it) }
    }
}
