package com.kurostream.tv.core.player.datasource

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.kurostream.tv.data.local.TorrentSessionTracker
import com.kurostream.tv.data.local.TorrentSessionStatus
import com.kurostream.tv.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Torrent Proxy Data Source that converts magnet links to HTTP streams.
 * Uses a local torrent streaming proxy to enable ExoPlayer playback.
 * Optimized for 1GB RAM devices with minimal buffering.
 */
@Singleton
class TorrentProxyDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val sessionTracker: TorrentSessionTracker,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BaseDataSource(true) {
    
    companion object {
        private const val TAG = "TorrentProxyDataSource"
        private const val PROXY_PORT = 8888
        private const val CONNECTION_TIMEOUT_MS = 30000
        private const val READ_TIMEOUT_MS = 60000
        
        // Memory optimization settings for 1GB RAM devices
        private const val MAX_BUFFER_SIZE = 8 * 1024 * 1024 // 8MB max buffer
        private const val MIN_BUFFER_SIZE = 2 * 1024 * 1024 // 2MB min buffer
        private const val PIECE_SIZE = 256 * 1024 // 256KB pieces
    }
    
    private var currentDataSpec: DataSpec? = null
    private var inputStream: InputStream? = null
    private var connection: HttpURLConnection? = null
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var torrentSession: TorrentSession? = null

    /** Session ID returned by [TorrentSessionTracker] for the active stream. */
    private var activeSessionId: String? = null
    
    /**
     * Open the data source for a magnet URI.
     */
    override fun open(dataSpec: DataSpec): Long {
        currentDataSpec = dataSpec
        
        val uri = dataSpec.uri
        val scheme = uri.scheme
        
        return when (scheme) {
            "magnet" -> openMagnetLink(dataSpec)
            "torrent-proxy" -> openProxyStream(dataSpec)
            else -> throw IOException("Unsupported scheme: $scheme")
        }
    }
    
    /**
     * Open a magnet link by starting torrent session and streaming.
     */
    private fun openMagnetLink(dataSpec: DataSpec): Long {
        val magnetUri = dataSpec.uri.toString()
        
        // Parse magnet link
        val infoHash = extractInfoHash(magnetUri)
            ?: throw IOException("Invalid magnet link: no info hash")
        
        val fileIndex = dataSpec.uri.getQueryParameter("file_idx")?.toIntOrNull() ?: 0
        
        // Start torrent session and register with session tracker
        runBlocking(ioDispatcher) {
            torrentSession = TorrentSession(
                infoHash = infoHash,
                fileIndex = fileIndex,
                maxBufferSize = MAX_BUFFER_SIZE,
                pieceSize = PIECE_SIZE
            ).also { session ->
                session.start(magnetUri)
            }

            // Register with TorrentSessionTracker for lifecycle persistence.
            // animeId/episodeId are unavailable at the DataSource layer; they can be
            // enriched later by callers via sessionTracker.updateSession().
            sessionTracker.trackSession(
                infoHash = infoHash,
                magnetUri = magnetUri,
                fileIndex = fileIndex,
                animeId = "",
                episodeId = ""
            ).onSuccess { tracked ->
                activeSessionId = tracked.sessionId
                sessionTracker.updateSession(
                    sessionId = tracked.sessionId,
                    status = TorrentSessionStatus.STARTING
                )
                Timber.tag(TAG).d("Torrent session registered id=%s hash=%s", tracked.sessionId, infoHash)
            }.onFailure { e ->
                Timber.tag(TAG).w(e, "Failed to register torrent session for hash=%s", infoHash)
            }
        }
        
        // Get proxy URL for streaming
        val proxyUrl = torrentSession?.getStreamUrl()
            ?: throw IOException("Failed to start torrent stream")
        
        return openHttpConnection(proxyUrl, dataSpec.position)
    }
    
    /**
     * Open an existing torrent proxy stream.
     */
    private fun openProxyStream(dataSpec: DataSpec): Long {
        val proxyUrl = dataSpec.uri.toString().replace("torrent-proxy://", "http://")
        return openHttpConnection(proxyUrl, dataSpec.position)
    }
    
    /**
     * Open HTTP connection to torrent proxy.
     */
    private fun openHttpConnection(url: String, position: Long): Long {
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECTION_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                
                if (position > 0) {
                    setRequestProperty("Range", "bytes=$position-")
                }
                
                connect()
            }
            
            val responseCode = connection?.responseCode ?: -1
            if (responseCode !in 200..299) {
                throw IOException("HTTP error: $responseCode")
            }
            
            // Get content length
            val contentLength = connection?.contentLengthLong ?: C.LENGTH_UNSET.toLong()
            bytesRemaining = if (contentLength != C.LENGTH_UNSET.toLong()) {
                contentLength
            } else {
                C.LENGTH_UNSET.toLong()
            }
            
            inputStream = connection?.inputStream
            
            return bytesRemaining
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to open HTTP connection")
            throw IOException("Failed to connect to torrent proxy: ${e.message}", e)
        }
    }
    
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemaining == 0L) {
            return C.RESULT_END_OF_INPUT
        }
        
        val stream = inputStream ?: throw IOException("Stream not opened")
        
        val bytesToRead = if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            minOf(length.toLong(), bytesRemaining).toInt()
        } else {
            length
        }
        
        val bytesRead = stream.read(buffer, offset, bytesToRead)
        
        if (bytesRead == -1) {
            if (bytesRemaining != C.LENGTH_UNSET.toLong() && bytesRemaining > 0) {
                throw IOException("Unexpected end of stream")
            }
            return C.RESULT_END_OF_INPUT
        }
        
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesRead
        }
        
        bytesTransferred(bytesRead)
        return bytesRead
    }
    
    override fun getUri(): Uri? = currentDataSpec?.uri
    
    override fun close() {
        try {
            inputStream?.close()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error closing input stream")
        }
        
        try {
            connection?.disconnect()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error disconnecting")
        }
        
        inputStream = null
        connection = null
        currentDataSpec = null
        bytesRemaining = C.LENGTH_UNSET.toLong()
    }
    
    /**
     * Clean up resources, stop the torrent session, and deregister it from
     * [TorrentSessionTracker] so DataStore is updated before the process exits.
     */
    fun release() {
        close()

        runBlocking(ioDispatcher) {
            torrentSession?.stop()
            torrentSession = null

            // Deregister from session tracker so DataStore + cache are cleaned up.
            activeSessionId?.let { id ->
                sessionTracker.stopSession(id)
                Timber.tag(TAG).d("Torrent session deregistered id=%s", id)
            }
            activeSessionId = null
        }

        scope.cancel()
    }
    
    /**
     * Get torrent session stats.
     */
    fun getSessionStats(): TorrentSessionStats? {
        return torrentSession?.getStats()
    }
    
    /**
     * Extract info hash from magnet URI.
     */
    private fun extractInfoHash(magnetUri: String): String? {
        val regex = Regex("urn:btih:([a-fA-F0-9]{40}|[a-zA-Z2-7]{32})")
        val match = regex.find(magnetUri)
        return match?.groupValues?.getOrNull(1)
    }
}

/**
 * Torrent session manager for a single torrent.
 */
class TorrentSession(
    private val infoHash: String,
    private val fileIndex: Int,
    private val maxBufferSize: Int,
    private val pieceSize: Int
) {
    
    companion object {
        private const val TAG = "TorrentSession"
    }
    
    private var isRunning = false
    private var proxyPort = 0
    private var stats = TorrentSessionStats()
    
    /**
     * Start torrent session.
     */
    suspend fun start(magnetUri: String) {
        if (isRunning) return
        
        Timber.tag(TAG).d("Starting torrent session for: $infoHash")
        
        // In a real implementation, this would:
        // 1. Initialize libtorrent or similar library
        // 2. Add magnet link to session
        // 3. Start piece downloading with prioritization
        // 4. Start local HTTP server for streaming
        
        // For now, use a placeholder implementation
        // Real implementation would use jlibtorrent or similar
        
        isRunning = true
        proxyPort = 8888 + (System.currentTimeMillis() % 1000).toInt()
        
        // Start simulated proxy server
        startProxyServer()
    }
    
    /**
     * Get streaming URL for this session.
     */
    fun getStreamUrl(): String? {
        if (!isRunning) return null
        return "http://127.0.0.1:$proxyPort/stream/$infoHash/$fileIndex"
    }
    
    /**
     * Get session statistics.
     */
    fun getStats(): TorrentSessionStats = stats
    
    /**
     * Stop torrent session.
     */
    suspend fun stop() {
        if (!isRunning) return
        
        Timber.tag(TAG).d("Stopping torrent session for: $infoHash")
        
        isRunning = false
        
        // Clean up resources
        // In real implementation, would stop torrent and HTTP server
    }
    
    private fun startProxyServer() {
        // In a real implementation, this would start a local HTTP server
        // that serves the torrent content as it's downloaded
        
        // The server would:
        // 1. Accept HTTP range requests
        // 2. Prioritize pieces needed for current playback position
        // 3. Buffer ahead while respecting memory limits
        // 4. Return 503 if data isn't available yet (ExoPlayer will retry)
    }
    
    /**
     * Update session stats.
     */
    private fun updateStats() {
        // In real implementation, would query torrent session for stats
        stats = stats.copy(
            downloadSpeed = 0,
            uploadSpeed = 0,
            progress = 0f,
            seeders = 0,
            peers = 0
        )
    }
}

/**
 * Torrent session statistics.
 */
data class TorrentSessionStats(
    val downloadSpeed: Long = 0, // bytes/second
    val uploadSpeed: Long = 0,   // bytes/second
    val progress: Float = 0f,    // 0.0 - 1.0
    val seeders: Int = 0,
    val peers: Int = 0,
    val piecesDownloaded: Int = 0,
    val piecesTotal: Int = 0,
    val bytesDownloaded: Long = 0,
    val bytesTotal: Long = 0
) {
    val downloadSpeedKbps: Float
        get() = downloadSpeed / 1024f
    
    val downloadSpeedMbps: Float
        get() = downloadSpeed / (1024f * 1024f)
    
    val progressPercentage: Int
        get() = (progress * 100).toInt()
}

/**
 * Data source factory for torrent streams.
 */
class TorrentDataSourceFactory @Inject constructor(
    private val torrentProxyDataSource: TorrentProxyDataSource
) : androidx.media3.datasource.DataSource.Factory {
    
    override fun createDataSource(): androidx.media3.datasource.DataSource {
        return torrentProxyDataSource
    }
}

/**
 * Magnet link builder utility.
 */
object MagnetLinkBuilder {
    
    /**
     * Build a magnet link from components.
     */
    fun build(
        infoHash: String,
        displayName: String? = null,
        trackers: List<String> = emptyList()
    ): String {
        val sb = StringBuilder("magnet:?xt=urn:btih:$infoHash")
        
        displayName?.let {
            sb.append("&dn=${Uri.encode(it)}")
        }
        
        trackers.forEach { tracker ->
            sb.append("&tr=${Uri.encode(tracker)}")
        }
        
        return sb.toString()
    }
    
    /**
     * Parse a magnet link into components.
     */
    fun parse(magnetUri: String): MagnetLinkInfo? {
        if (!magnetUri.startsWith("magnet:")) return null
        
        val uri = Uri.parse(magnetUri)
        
        val xt = uri.getQueryParameter("xt") ?: return null
        val infoHashMatch = Regex("urn:btih:([a-fA-F0-9]{40}|[a-zA-Z2-7]{32})").find(xt)
        val infoHash = infoHashMatch?.groupValues?.getOrNull(1) ?: return null
        
        val displayName = uri.getQueryParameter("dn")
        val trackers = uri.getQueryParameters("tr")
        
        return MagnetLinkInfo(
            infoHash = infoHash,
            displayName = displayName,
            trackers = trackers
        )
    }
}

/**
 * Parsed magnet link information.
 */
data class MagnetLinkInfo(
    val infoHash: String,
    val displayName: String?,
    val trackers: List<String>
)
