package com.kurostream.torrent.tracker

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

@Singleton
class TrackerListProvider @Inject constructor() {

    private val TAG = "TrackerListProvider"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _trackerCount = MutableStateFlow(0)
    val trackerCount: StateFlow<Int> = _trackerCount.asStateFlow()

    private val trackerHealth = ConcurrentHashMap<String, TrackerHealth>()
    private var refreshJob: Job? = null

    data class TrackerHealth(
        val url: String,
        val isAlive: Boolean = true,
        val responseTimeMs: Long = 0,
        val lastChecked: Long = 0,
        val failCount: Int = 0,
        val peersFound: Int = 0,
    )

    private val builtInTrackers = listOf(
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://open.stealth.si:80/announce",
        "udp://tracker.torrent.eu.org:451/announce",
        "udp://tracker.bittor.pw:1337/announce",
        "udp://public.popcorn-tracker.org:6969/announce",
        "http://tracker.opentrackr.org:1337/announce",
        "udp://tracker.dler.org:6969/announce",
        "udp://exodus.desync.com:6969/announce",
        "udp://explodie.org:6969/announce",
        "udp://tracker.moeking.me:6969/announce",
        "udp://tracker1.bt.moack.co.kr:80/announce",
        "http://tracker.bt4g.com:2095/announce",
        "udp://tracker.tiny-vps.com:6969/announce",
        "http://tracker.openbittorrent.com:80/announce",
        "udp://open.demonii.com:1337/announce",
        "udp://p4p.arenabg.com:1337/announce",
        "udp://tracker.cyberia.is:6969/announce",
        "http://nyaa.tracker.wf:7777/announce",
        "udp://9.rarbg.to:2810/announce",
        "udp://tracker.openbittorrent.com:6969/announce",
        "http://openbittorrent.com:80/announce",
        "udp://opentracker.i2p.rocks:6969/announce",
        "http://tracker.openbittorrent.com:80/announce",
        "udp://www.torrent.eu.org:451/announce",
        "udp://tracker.birkenwald.de:6969/announce",
        "udp://tracker.pirateparty.gr:6969/announce",
        "udp://tracker.jordan.im:6969/announce",
        "udp://tracker.skynetcloud.site:6969/announce",
        "http://tracker.files.fm:6969/announce",
        "udp://tracker.dump.cl:6969/announce",
        "udp://tracker.army:6881/announce",
        "udp://retracker.lanta-net.ru:2710/announce",
        "http://tracker1.bt.moack.co.kr:80/announce",
        "udp://tracker.uw0.xyz:6969/announce",
        "http://tracker.uw0.xyz:6969/announce",
        "udp://tracker.filemail.com:6969/announce",
        "http://tracker.filemail.com:6969/announce",
        "udp://tracker.lelux.fi:6969/announce",
        "http://retracker.netbynet.ru:2710/announce",
        "udp://retracker.netbynet.ru:2710/announce",
        "udp://tracker.zerobytes.xyz:1337/announce",
        "http://tracker.zerobytes.xyz:1337/announce",
        "udp://tracker.monitorit4.me:6969/announce",
        "udp://tracker.leech.ie:1337/announce",
        "http://tracker.leech.ie:1337/announce",
        "udp://tracker.altrosky.nl:6969/announce",
        "http://open.acgnxtracker.com:80/announce",
        "udp://tracker.srv00.com:6969/announce",
        "udp://tracker.leechers-paradise.org:6969/announce",
        "udp://tracker.zoo.team:6969/announce",
        "udp://tracker.moviezoo.org:6969/announce",
        "udp://tracker.0x.tf:6969/announce",
        "udp://retracker1.1pmht.ru:1337/announce",
        "udp://tracker.uw0.xyz:6969/announce",
        "http://tracker01.1vag.com:2710/announce",
        "udp://tracker.filecoin.io:6969/announce",
        "udp://tracker.sjtech.ru:6969/announce",
        "udp://tracker.joebijns.nl:6969/announce",
        "udp://tracker.skynetcloud.site:6969/announce",
        "udp://tracker.4ever.to:1027/announce",
        "udp://open.stealth.si:80/announce",
        "udp://tracker.army:6881/announce",
    )

    private val remoteTrackerUrls = listOf(
        "https://raw.githubusercontent.com/ngosang/trackerslist/master/trackers_best.txt",
        "https://raw.githubusercontent.com/XIU2/TrackersListCollection/master/best.txt",
        "https://raw.githubusercontent.com/TrackersList/trackerslist/main/trackers_best.txt",
    )

    fun getAnnounceUrls(): List<String> {
        return builtInTrackers + trackerHealth.values
            .filter { it.isAlive }
            .sortedByDescending { it.peersFound }
            .map { it.url }
    }

    fun startPeriodicRefresh(scope: CoroutineScope) {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            while (isActive) {
                try {
                    refreshTrackerList()
                } catch (e: Exception) {
                    Log.w(TAG, "Tracker refresh failed", e)
                }
                delay(6 * 60 * 60 * 1000L) // Every 6 hours
            }
        }
    }

    fun stopPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    private suspend fun refreshTrackerList() {
        val allTrackers = mutableSetOf<String>()
        allTrackers.addAll(builtInTrackers)

        withContext(Dispatchers.IO) {
            for (url in remoteTrackerUrls) {
                try {
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string() ?: continue
                    val trackers = body.lines()
                        .map { it.trim() }
                        .filter { it.startsWith("udp://") || it.startsWith("http://") || it.startsWith("https://") }
                    allTrackers.addAll(trackers)
                    Log.i(TAG, "Fetched ${trackers.size} trackers from $url")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch trackers from $url", e)
                }
            }
        }

        _trackerCount.value = allTrackers.size
        Log.i(TAG, "Total trackers available: ${allTrackers.size}")
    }

    fun reportTrackerResult(url: String, alive: Boolean, responseTimeMs: Long, peersFound: Int) {
        val existing = trackerHealth[url] ?: TrackerHealth(url = url)
        val failCount = if (alive) 0 else existing.failCount + 1
        trackerHealth[url] = existing.copy(
            isAlive = alive,
            responseTimeMs = responseTimeMs,
            lastChecked = System.currentTimeMillis(),
            failCount = failCount,
            peersFound = peersFound,
        )
    }

    fun getDeadTrackerBackoff(url: String): Long {
        val health = trackerHealth[url] ?: return 0
        if (health.isAlive) return 0
        val backoffMs = min(30_000L * 2.0.pow(health.failCount.coerceAtMost(6)).toLong(), 3_600_000L)
        val lastCheckAgo = System.currentTimeMillis() - health.lastChecked
        return if (lastCheckAgo >= backoffMs) 0 else backoffMs - lastCheckAgo
    }
}
