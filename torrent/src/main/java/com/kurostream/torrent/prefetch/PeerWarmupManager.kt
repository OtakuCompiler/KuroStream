package com.kurostream.torrent.prefetch

import android.util.Log
import com.frostwire.jlibtorrent.Session
import com.frostwire.jlibtorrent.Sha1Hash
import com.frostwire.jlibtorrent.TorrentHandle
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeerWarmupManager @Inject constructor() {

    private val TAG = "PeerWarmupManager"

    private val knownPeers = ConcurrentHashMap<String, MutableSet<KnownPeer>>()
    private val warmupJobs = ConcurrentHashMap<String, Job>()
    private val warmedUpCount = AtomicInteger(0)

    data class KnownPeer(
        val address: String,
        val port: Int,
        val sourceInfoHash: String,
        val lastSeen: Long,
        val connectionQuality: Float = 1.0f,
    )

    private val _warmedPeersCount = MutableStateFlow(0)
    val warmedPeersCount: StateFlow<Int> = _warmedPeersCount.asStateFlow()

    fun recordPeers(infoHash: String, peers: List<Pair<String, Int>>) {
        val peerSet = knownPeers.getOrPut(infoHash) { mutableSetOf() }
        val now = System.currentTimeMillis()
        peers.forEach { (addr, port) ->
            peerSet.add(KnownPeer(addr, port, infoHash, now))
        }
        if (peerSet.size > 200) {
            val sorted = peerSet.sortedBy { it.lastSeen }
            peerSet.removeAll(sorted.take(50).toSet())
        }
    }

    suspend fun warmupPeers(
        infoHash: String,
        session: Session,
        scope: CoroutineScope,
    ) {
        warmupJobs[infoHash]?.cancel()

        val peers = knownPeers[infoHash] ?: return
        if (peers.isEmpty()) return

        warmupJobs[infoHash] = scope.launch(Dispatchers.IO) {
            Log.i(TAG, "Warming up ${peers.size} peers for $infoHash")
            val sortedPeers = peers.sortedByDescending { it.connectionQuality }.take(30)

            for (peer in sortedPeers) {
                if (!isActive) break
                try {
                    val handle = session.findTorrent(Sha1Hash(infoHash))
                    if (handle.isValid) {
                        handle.connectPeer(
                            com.frostwire.jlibtorrent.TorrentHandle.PeerInfo(
                                peer.address,
                                peer.port,
                            )
                        )
                        warmedUpCount.incrementAndGet()
                        _warmedPeersCount.value = warmedUpCount.get()
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Failed to warm up peer ${peer.address}:${peer.port}")
                }
                delay(50)
            }
        }
    }

    fun stopWarmup(infoHash: String) {
        warmupJobs[infoHash]?.cancel()
        warmupJobs.remove(infoHash)
    }

    fun getKnownPeerCount(infoHash: String): Int {
        return knownPeers[infoHash]?.size ?: 0
    }

    fun stopAll() {
        warmupJobs.values.forEach { it.cancel() }
        warmupJobs.clear()
    }
}
