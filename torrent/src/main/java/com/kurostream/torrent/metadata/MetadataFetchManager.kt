package com.kurostream.torrent.metadata

import android.util.Log
import com.frostwire.jlibtorrent.Sha1Hash
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

@Singleton
class MetadataFetchManager @Inject constructor() {

    private val TAG = "MetadataFetchManager"

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val metadataCache = ConcurrentHashMap<String, ByteArray>()
    private val activeFetches = ConcurrentHashMap<String, Job>()

    private val _activeFetchCount = MutableStateFlow(0)
    val activeFetchCount: StateFlow<Int> = _activeFetchCount.asStateFlow()

    private val torcacheUrls = listOf(
        "https://itorrents.org/torrent/{info_hash}.torrent",
        "https://torrage.info/torrent/{info_hash}.torrent",
        "https://torcache.net/torrent/{info_hash}.torrent",
    )

    suspend fun fetchMetadata(
        infoHash: String,
        session: com.frostwire.jlibtorrent.Session,
        scope: CoroutineScope,
    ): Boolean {
        metadataCache[infoHash]?.let { cached ->
            injectMetadata(infoHash, cached, session)
            return true
        }

        val existingJob = activeFetches[infoHash]
        if (existingJob?.isActive == true) return false

        val job = scope.launch(Dispatchers.IO) {
            _activeFetchCount.value = activeFetches.size + 1
            try {
                val results = mutableMapOf<String, ByteArray?>()

                val fetchJobs = torcacheUrls.map { urlTemplate ->
                    async {
                        val url = urlTemplate.replace("{info_hash}", infoHash.uppercase())
                        try {
                            val request = Request.Builder()
                                .url(url)
                                .header("User-Agent", "KuroStream/1.0")
                                .build()
                            val response = client.newCall(request).execute()
                            if (response.isSuccessful) {
                                val bytes = response.body?.bytes()
                                if (bytes != null && bytes.size > 100) {
                                    url to bytes
                                } else null
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                for (job in fetchJobs) {
                    val result = job.await()
                    if (result != null) {
                        val (url, bytes) = result
                        Log.i(TAG, "Fetched metadata for $infoHash from $url (${bytes.size} bytes)")
                        metadataCache[infoHash] = bytes
                        injectMetadata(infoHash, bytes, session)
                        return@launch
                    }
                }
                Log.d(TAG, "No metadata found for $infoHash from any source")
            } finally {
                activeFetches.remove(infoHash)
                _activeFetchCount.value = activeFetches.size
            }
        }

        activeFetches[infoHash] = job
        return false
    }

    private fun injectMetadata(infoHash: String, metadata: ByteArray, session: com.frostwire.jlibtorrent.Session) {
        try {
            val handle = session.findTorrent(Sha1Hash(infoHash))
            if (handle.isValid) {
                handle.addMetadata(metadata)
                Log.i(TAG, "Injected metadata for $infoHash (${metadata.size} bytes)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to inject metadata for $infoHash", e)
        }
    }

    fun getCachedMetadata(infoHash: String): ByteArray? = metadataCache[infoHash]

    fun cacheMetadata(infoHash: String, data: ByteArray) {
        metadataCache[infoHash] = data
        if (metadataCache.size > 1000) {
            val oldest = metadataCache.keys.sorted().take(200)
            oldest.forEach { metadataCache.remove(it) }
        }
    }

    fun cancelAll() {
        activeFetches.values.forEach { it.cancel() }
        activeFetches.clear()
        _activeFetchCount.value = 0
    }
}
