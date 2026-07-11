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

package com.kurostream.torrent.engine

import android.app.Application
import android.util.Log
import com.frostwire.jlibtorrent.Alert
import com.frostwire.jlibtorrent.AlertListener
import com.frostwire.jlibtorrent.AddTorrentParams
import com.frostwire.jlibtorrent.Session
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.SessionParams
import com.frostwire.jlibtorrent.SessionSettings
import com.frostwire.jlibtorrent.Sha1Hash
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.swig.add_torrent_params_flags_t
import com.frostwire.jlibtorrent.swig.alert_category_t
import com.kurostream.common.dispatcher.DispatcherProvider
import com.kurostream.common.result.Result
import com.kurostream.torrent.cache.TorrentMetadataCache
import com.kurostream.torrent.cache.TorrentPieceCache
import com.kurostream.torrent.domain.*
import com.kurostream.torrent.metadata.MetadataFetchManager
import com.kurostream.torrent.network.PortMappingMonitor
import com.kurostream.torrent.network.QuicTorrentProxy
import com.kurostream.torrent.prioritization.BandwidthAwareSelector
import com.kurostream.torrent.prioritization.StreamingPiecePrioritizer
import com.kurostream.torrent.prefetch.PeerWarmupManager
import com.kurostream.torrent.prefetch.PredictivePrefetchManager
import com.kurostream.torrent.tracker.SeederHuntManager
import com.kurostream.torrent.tracker.TrackerListProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sharingStarted
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentEngine @Inject constructor(
    private val context: Application,
    private val dispatcherProvider: DispatcherProvider,
    private val trackerListProvider: TrackerListProvider,
    private val metadataFetchManager: MetadataFetchManager,
    private val seederHuntManager: SeederHuntManager,
    private val streamingPiecePrioritizer: StreamingPiecePrioritizer,
    private val peerWarmupManager: PeerWarmupManager,
    private val portMappingMonitor: PortMappingMonitor,
    private val quicTorrentProxy: QuicTorrentProxy,
    private val bandwidthAwareSelector: BandwidthAwareSelector,
    private val predictivePrefetchManager: PredictivePrefetchManager,
    private val writeCoalescer: WriteCoalescer,
    private val lazyVerifier: LazyVerifier,
    private val torrentMetadataCache: TorrentMetadataCache,
    private val torrentPieceCache: TorrentPieceCache,
) : AlertListener, CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    private val TAG = "TorrentEngine"

    private var session: Session? = null
    private var sessionManager: SessionManager? = null
    private var isInitialized = false
    private var engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val torrents = ConcurrentHashMap<String, TorrentHandle>()
    private val torrentStates = ConcurrentHashMap<String, MutableStateFlow<TorrentInfo>>()
    private val _globalStats = MutableStateFlow(GlobalStats(0L, 0L, 0, 0, 0, 0))
    val globalStats = _globalStats

    private val alertChannel = Channel<Alert>(100)
    private val alertProcessor = AtomicBoolean(false)

    private val defaultSavePath: String
        get() = context.getExternalFilesDir("torrents")?.absolutePath
            ?: context.filesDir.absolutePath + "/torrents"

    private val resumeDataPath: File
        get() = File(context.filesDir, "torrent_resume").apply { mkdirs() }

    init {
        initializeSession()
    }

    private fun initializeSession() {
        if (isInitialized) return

        val params = SessionParams().apply {
            settings = SessionSettings().apply {
                setInt(SessionSettings.alert_mask, alert_category_t.all_categories.value)
                setInt(SessionSettings.listen_port, 6881)
                setBool(SessionSettings.enable_dht, true)
                setBool(SessionSettings.enable_lsd, true)
                setBool(SessionSettings.enable_upnp, true)
                setBool(SessionSettings.enable_natpmp, true)
                setInt(SessionSettings.encryption_mode, 1)
                setInt(SessionSettings.max_connections, 200)
                setInt(SessionSettings.max_upload_slots, 50)
                setInt(SessionSettings.max_half_open_connections, 8)
                setInt(SessionSettings.connections_per_torrent, 50)
                setInt(SessionSettings.upload_slots_per_torrent, 10)
                setBool(SessionSettings.enable_utp, true)
                setInt(SessionSettings.utp_target_delay, 50)
                setInt(SessionSettings.utp_gain, 10000)
                setInt(SessionSettings.utp_lost_seed, 10)
                setBool(SessionSettings.announce_to_all_trackers, true)
                setBool(SessionSettings.announce_to_all_tiers, true)
                setBool(SessionSettings.prefer_udp_trackers, true)
                setBool(SessionSettings.strict_end_game_mode, true)
                setInt(SessionSettings.listen_port_range, 6881)
            }
            alertListener = this@TorrentEngine
        }

        session = Session(params)
        sessionManager = SessionManager(session!!)
        sessionManager!!.start()

        isInitialized = true
        startAlertProcessor()
        loadResumeData()
        startPeriodicJobs()
        initializeOptimizations()
    }

    private fun startPeriodicJobs() {
        trackerListProvider.startPeriodicRefresh(engineScope)

        streamingPiecePrioritizer.attachScope(engineScope)

        writeCoalescer.start(engineScope)

        lazyVerifier.startBackgroundVerification(engineScope)

        engineScope.launch {
            while (isActive) {
                updateGlobalStats()
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun initializeOptimizations() {
        torrentPieceCache.configure(64)
        Log.i(TAG, "All torrent optimizations initialized")
    }

    private fun startAlertProcessor() {
        if (alertProcessor.getAndSet(true)) return
        launch {
            while (alertProcessor.get()) {
                val alert = alertChannel.receive()
                processAlert(alert)
            }
        }
    }

    private fun processAlert(alert: Alert) {
        when (alert) {
            is com.frostwire.jlibtorrent.AddTorrentAlert -> handleAddTorrentAlert(alert)
            is com.frostwire.jlibtorrent.TorrentFinishedAlert -> handleTorrentFinishedAlert(alert)
            is com.frostwire.jlibtorrent.TorrentErrorAlert -> handleTorrentErrorAlert(alert)
            is com.frostwire.jlibtorrent.TorrentPausedAlert -> handleTorrentPausedAlert(alert)
            is com.frostwire.jlibtorrent.TorrentResumedAlert -> handleTorrentResumedAlert(alert)
            is com.frostwire.jlibtorrent.TorrentRemovedAlert -> handleTorrentRemovedAlert(alert)
            is com.frostwire.jlibtorrent.MetadataReceivedAlert -> handleMetadataReceivedAlert(alert)
            is com.frostwire.jlibtorrent.SaveResumeDataAlert -> handleSaveResumeDataAlert(alert)
            is com.frostwire.jlibtorrent.SaveResumeDataFailedAlert -> handleSaveResumeDataFailedAlert(alert)
            is com.frostwire.jlibtorrent.StateChangedAlert -> handleStateChangedAlert(alert)
            is com.frostwire.jlibtorrent.TrackerAnnounceAlert -> handleTrackerAnnounceAlert(alert)
            is com.frostwire.jlibtorrent.TrackerReplyAlert -> handleTrackerReplyAlert(alert)
            is com.frostwire.jlibtorrent.DhtAnnounceAlert -> handleDhtAnnounceAlert(alert)
            is com.frostwire.jlibtorrent.PeerBanAlert -> handlePeerBanAlert(alert)
            is com.frostwire.jlibtorrent.PeerUnsnubAlert -> handlePeerUnsnubAlert(alert)
            is com.frostwire.jlibtorrent.PeerSnubbedAlert -> handlePeerSnubbedAlert(alert)
            is com.frostwire.jlibtorrent.PeerConnectAlert -> handlePeerConnectAlert(alert)
            is com.frostwire.jlibtorrent.PeerDisconnectedAlert -> handlePeerDisconnectedAlert(alert)
            is com.frostwire.jlibtorrent.PieceFinishedAlert -> handlePieceFinishedAlert(alert)
            else -> { }
        }
    }

    private fun handleAddTorrentAlert(alert: com.frostwire.jlibtorrent.AddTorrentAlert) {
        val handle = alert.handle
        if (handle.isValid) {
            val infoHash = handle.infoHash().toHex()
            torrents[infoHash] = handle
            initTorrentState(handle)

            launch {
                try {
                    metadataFetchManager.fetchMetadata(infoHash, session!!, engineScope)
                } catch (e: Exception) {
                    Log.d(TAG, "Metadata fetch failed for $infoHash", e)
                }
            }

            launch {
                peerWarmupManager.warmupPeers(infoHash, session!!, engineScope)
            }

            seederHuntManager.startHunting(infoHash, handle, engineScope)
        }
    }

    private fun handleTorrentFinishedAlert(alert: com.frostwire.jlibtorrent.TorrentFinishedAlert) {
        val handle = alert.handle
        if (handle.isValid) {
            val infoHash = handle.infoHash().toHex()
            updateTorrentState(handle)
            saveResumeData(handle)
        }
    }

    private fun handleTorrentErrorAlert(alert: com.frostwire.jlibtorrent.TorrentErrorAlert) {
        val handle = alert.handle
        if (handle.isValid) {
            val infoHash = handle.infoHash().toHex()
            val state = torrentStates[infoHash]?.value?.copy(
                status = TorrentStatus.ERROR,
                error = alert.error.message()
            )
            state?.let { torrentStates[infoHash]?.value = it }
        }
    }

    private fun handleTorrentPausedAlert(alert: com.frostwire.jlibtorrent.TorrentPausedAlert) {
        val handle = alert.handle
        if (handle.isValid) {
            val infoHash = handle.infoHash().toHex()
            updateTorrentState(handle)
            saveResumeData(handle)
        }
    }

    private fun handleTorrentResumedAlert(alert: com.frostwire.jlibtorrent.TorrentResumedAlert) {
        val handle = alert.handle
        if (handle.isValid) {
            updateTorrentState(handle)
        }
    }

    private fun handleTorrentRemovedAlert(alert: com.frostwire.jlibtorrent.TorrentRemovedAlert) {
        val infoHash = alert.infoHash.toHex()
        torrents.remove(infoHash)
        torrentStates.remove(infoHash)
        seederHuntManager.stopHunting(infoHash)
        peerWarmupManager.stopWarmup(infoHash)
        torrentPieceCache.clearForTorrent(infoHash)
        lazyVerifier.clearForTorrent(infoHash)
        val file = File(resumeDataPath, "$infoHash.resume")
        file.delete()
    }

    private fun handleMetadataReceivedAlert(alert: com.frostwire.jlibtorrent.MetadataReceivedAlert) {
        val handle = alert.handle
        if (handle.isValid) {
            val infoHash = handle.infoHash().toHex()
            initTorrentFiles(handle)
            updateTorrentState(handle)

            launch {
                val state = torrentStates[infoHash]?.value
                if (state != null) {
                    torrentMetadataCache.cacheTorrentInfo(state)
                }
            }
        }
    }

    private fun handleSaveResumeDataAlert(alert: com.frostwire.jlibtorrent.SaveResumeDataAlert) {
        val handle = alert.handle
        if (handle.isValid) {
            val infoHash = handle.infoHash().toHex()
            val file = File(resumeDataPath, "$infoHash.resume")
            try {
                val data = alert.resumeData
                val bytes = com.frostwire.jlibtorrent.bencode(data)
                file.writeBytes(bytes)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save resume data for $infoHash", e)
            }
        }
    }

    private fun handleSaveResumeDataFailedAlert(alert: com.frostwire.jlibtorrent.SaveResumeDataFailedAlert) {
        Log.w(TAG, "Failed to save resume data: ${alert.error.message()}")
    }

    private fun handleStateChangedAlert(alert: com.frostwire.jlibtorrent.StateChangedAlert) {
        val handle = alert.handle
        if (handle.isValid) {
            updateTorrentState(handle)
        }
    }

    private fun handleTrackerAnnounceAlert(alert: com.frostwire.jlibtorrent.TrackerAnnounceAlert) {
        val startTime = System.currentTimeMillis()
        trackerListProvider.reportTrackerResult(
            url = alert.tracker(),
            alive = true,
            responseTimeMs = 0,
            peersFound = 0,
        )
    }

    private fun handleTrackerReplyAlert(alert: com.frostwire.jlibtorrent.TrackerReplyAlert) {
        trackerListProvider.reportTrackerResult(
            url = alert.tracker(),
            alive = true,
            responseTimeMs = 0,
            peersFound = alert.numPeers(),
        )
    }

    private fun handleDhtAnnounceAlert(alert: com.frostwire.jlibtorrent.DhtAnnounceAlert) { }
    private fun handlePeerBanAlert(alert: com.frostwire.jlibtorrent.PeerBanAlert) { }
    private fun handlePeerUnsnubAlert(alert: com.frostwire.jlibtorrent.PeerUnsnubAlert) { }
    private fun handlePeerSnubbedAlert(alert: com.frostwire.jlibtorrent.PeerSnubbedAlert) { }

    private fun handlePeerConnectAlert(alert: com.frostwire.jlibtorrent.PeerConnectAlert) {
        val handle = alert.handle
        if (handle.isValid) {
            val infoHash = handle.infoHash().toHex()
            peerWarmupManager.recordPeers(infoHash, listOf(
                "${alert.peer().address().address().hostAddress}" to alert.peer().listenPort()
            ))
        }
    }

    private fun handlePeerDisconnectedAlert(alert: com.frostwire.jlibtorrent.PeerDisconnectedAlert) { }

    private fun handlePieceFinishedAlert(alert: com.frostwire.jlibtorrent.PieceFinishedAlert) {
        val handle = alert.handle
        if (handle.isValid) {
            val infoHash = handle.infoHash().toHex()
            val pieceIndex = alert.pieceIndex()

            lazyVerifier.enqueueVerification(infoHash, pieceIndex, LazyVerifier.VerifyPriority.HIGH)

            if (pieceIndex < 3) {
                val state = torrentStates[infoHash]?.value
                if (state != null && state.files.isNotEmpty()) {
                    com.kurostream.players.buffer.ZeroSeekPlaybackManager().onFirstPieceAvailable(
                        infoHash, 0, pieceIndex
                    )
                }
            }
        }
    }

    override fun alert(alert: Alert) {
        alertChannel.trySend(alert)
    }

    fun addMagnet(magnetUri: String, savePath: String = defaultSavePath, sequential: Boolean = false): Result<TorrentInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val params = AddTorrentParams()
                params.setUrl(magnetUri)
                params.setSavePath(savePath)
                params.setSequentialDownload(sequential)
                params.setAutoManaged(true)
                params.setFlags(
                    add_torrent_params_flags_t.flag_auto_managed.value or
                            add_torrent_params_flags_t.flag_sequential_download.value
                )

                val handle = session!!.addTorrent(params)
                if (!handle.isValid) {
                    Result.failure(Exception("Invalid magnet URI or failed to add torrent"))
                } else {
                    val infoHash = handle.infoHash().toHex()
                    torrents[infoHash] = handle
                    initTorrentState(handle)

                    launch {
                        val cached = torrentMetadataCache.getCachedMetadata(infoHash)
                        if (cached != null) {
                            Log.i(TAG, "Cache hit for $infoHash: ${cached.name}")
                        }
                    }

                    Result.success(torrentStates[infoHash]?.value!!)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding magnet", e)
                Result.failure(e)
            }
        }
    }

    fun addTorrentFile(filePath: String, savePath: String = defaultSavePath, sequential: Boolean = false): Result<TorrentInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val torrentInfo = TorrentInfo(filePath)
                val params = AddTorrentParams()
                params.setTi(torrentInfo)
                params.setSavePath(savePath)
                params.setSequentialDownload(sequential)
                params.setAutoManaged(true)

                val handle = session!!.addTorrent(params)
                if (!handle.isValid) {
                    Result.failure(Exception("Invalid torrent file or failed to add torrent"))
                } else {
                    val infoHash = handle.infoHash().toHex()
                    torrents[infoHash] = handle
                    initTorrentFiles(handle)
                    initTorrentState(handle)
                    Result.success(torrentStates[infoHash]?.value!!)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding torrent file", e)
                Result.failure(e)
            }
        }
    }

    fun removeTorrent(infoHash: String, deleteFiles: Boolean) {
        val handle = torrents.remove(infoHash) ?: return
        if (handle.isValid) {
            session!!.removeTorrent(handle, if (deleteFiles) 1 else 0)
            val file = File(resumeDataPath, "$infoHash.resume")
            file.delete()
        }
        torrentStates.remove(infoHash)
    }

    fun pauseTorrent(infoHash: String) {
        torrents[infoHash]?.pause()
    }

    fun resumeTorrent(infoHash: String) {
        torrents[infoHash]?.resume()
    }

    fun setTorrentPriority(infoHash: String, priority: Int) {
        torrents[infoHash]?.setQueuePosition(priority)
    }

    fun setFilePriorities(infoHash: String, priorities: Map<String, FilePriority>) {
        val handle = torrents[infoHash] ?: return
        val torrentInfo = handle.torrentFile() ?: return
        val files = torrentInfo.files()
        val filePriorities = IntArray(files.numFiles())
        files.forEachIndexed { index, file ->
            priorities[file.path]?.let { filePriorities[index] = mapPriorityToInt(it) }
        }
        handle.setFilePriorities(filePriorities)
    }

    fun setSequentialDownload(infoHash: String, enabled: Boolean) {
        torrents[infoHash]?.setSequentialDownload(enabled)
    }

    fun setSeedLimits(infoHash: String, ratioLimit: Float, timeLimitMinutes: Long) {
        val handle = torrents[infoHash] ?: return
        handle.setSeedRatioLimit(ratioLimit)
        handle.setSeedTimeLimit(timeLimitMinutes * 60)
    }

    fun pauseAll() {
        torrents.values.forEach { it.pause() }
    }

    fun resumeAll() {
        torrents.values.forEach { it.resume() }
    }

    fun removeAll(deleteFiles: Boolean) {
        torrents.keys.toList().forEach { removeTorrent(it, deleteFiles) }
    }

    fun setGlobalSpeedLimits(downloadKbps: Long, uploadKbps: Long) {
        val settings = session!!.settings()
        settings.setInt(
            SessionSettings.download_rate_limit,
            if (downloadKbps > 0) downloadKbps * 1024 else -1
        )
        settings.setInt(
            SessionSettings.upload_rate_limit,
            if (uploadKbps > 0) uploadKbps * 1024 else -1
        )
    }

    fun setGlobalSeedLimits(ratioLimit: Float, timeLimitMinutes: Long) {
        torrents.values.forEach {
            it.setSeedRatioLimit(ratioLimit)
            it.setSeedTimeLimit(timeLimitMinutes * 60)
        }
    }

    fun prioritizeForStreaming(infoHash: String, playbackPositionBytes: Long = 0, totalFileSize: Long = 0) {
        val handle = torrents[infoHash] ?: return
        val torrentInfo = handle.torrentFile() ?: return
        streamingPiecePrioritizer.prioritizeForStreaming(handle, torrentInfo, playbackPositionBytes, totalFileSize)
    }

    fun applyBandwidthProfile(infoHash: String) {
        val handle = torrents[infoHash] ?: return
        val profile = bandwidthAwareSelector.detectProfile(_globalStats.value.totalDownloadSpeed)
        bandwidthAwareSelector.applyProfileToTorrent(handle, profile)
    }

    fun observeTorrents(): kotlinx.coroutines.flow.Flow<List<TorrentInfo>> {
        return combine(torrentStates.values.map { it.asStateFlow() }.toTypedArray()) { states ->
            states.mapNotNull { it }.sortedByDescending { it.addedAt }
        }.distinctUntilChanged()
            .stateIn(this.coroutineContext, sharingStarted.WhileSubscribed(), emptyList())
    }

    fun observeTorrent(infoHash: String): kotlinx.coroutines.flow.Flow<TorrentInfo?> {
        return torrentStates[infoHash]?.asStateFlow()?.stateIn(
            this.coroutineContext,
            sharingStarted.WhileSubscribed(),
            null
        ) ?: flowOf(null).stateIn(
            this.coroutineContext,
            sharingStarted.WhileSubscribed(),
            null
        )
    }

    fun getTorrent(infoHash: String): TorrentInfo? = torrentStates[infoHash]?.value

    fun getAllTorrents(): List<TorrentInfo> = torrentStates.values.mapNotNull { it.value }.sortedByDescending { it.addedAt }

    fun observeGlobalStats(): kotlinx.coroutines.flow.Flow<GlobalStats> {
        return _globalStats.distinctUntilChanged()
            .stateIn(this.coroutineContext, sharingStarted.WhileSubscribed(), GlobalStats(0L, 0L, 0, 0, 0, 0))
    }

    fun getSessionStats(): GlobalStats = _globalStats.value

    fun moveTorrentData(infoHash: String, newPath: String): Result<TorrentInfo> {
        val handle = torrents[infoHash] ?: return Result.failure(Exception("Torrent not found"))
        return withContext(Dispatchers.IO) {
            try {
                handle.moveStorage(newPath)
                Result.success(buildTorrentInfo(handle))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun reannounceTorrent(infoHash: String) {
        torrents[infoHash]?.forceReannounce()
    }

    fun scrapeTracker(infoHash: String) {
        torrents[infoHash]?.scrapeTracker()
    }

    fun getTorrentStreamUrl(infoHash: String, fileIndex: Int): String? {
        val handle = torrents[infoHash] ?: return null
        val torrentInfo = handle.torrentFile() ?: return null
        val files = torrentInfo.files()
        if (fileIndex >= files.numFiles()) return null
        val file = files.fileAt(fileIndex)
        val filePath = File(handle.savePath(), file.path)
        if (filePath.exists()) {
            return "file://${filePath.absolutePath}"
        }
        return "torrent://$infoHash/$fileIndex"
    }

    fun setSessionSettings(settings: TorrentSessionSettings) {
        val s = session!!.settings()
        s.setInt(SessionSettings.listen_port, settings.listenPort)
        s.setBool(SessionSettings.enable_dht, settings.enableDht)
        s.setBool(SessionSettings.enable_lsd, settings.enableLsd)
        s.setBool(SessionSettings.enable_upnp, settings.enableUpnp)
        s.setBool(SessionSettings.enable_natpmp, settings.enableNatpmp)
        s.setInt(SessionSettings.encryption_mode, settings.encryptionMode.ordinal)
        s.setInt(SessionSettings.max_connections, settings.maxConnections)
        s.setInt(SessionSettings.max_upload_slots, settings.maxUploadSlots)
        s.setInt(SessionSettings.max_half_open_connections, settings.maxHalfOpenConnections)
        s.setInt(SessionSettings.connections_per_torrent, settings.maxConnectionsPerTorrent)
        s.setInt(SessionSettings.upload_slots_per_torrent, settings.maxUploadSlotsPerTorrent)
        if (settings.downloadRateLimit > 0) {
            s.setInt(SessionSettings.download_rate_limit, settings.downloadRateLimit * 1024)
        }
        if (settings.uploadRateLimit > 0) {
            s.setInt(SessionSettings.upload_rate_limit, settings.uploadRateLimit * 1024)
        }
    }

    fun getSessionSettings(): TorrentSessionSettings {
        val s = session!!.settings()
        return TorrentSessionSettings(
            listenPort = s.getInt(SessionSettings.listen_port),
            enableDht = s.getBool(SessionSettings.enable_dht),
            enableLsd = s.getBool(SessionSettings.enable_lsd),
            enableUpnp = s.getBool(SessionSettings.enable_upnp),
            enableNatpmp = s.getBool(SessionSettings.enable_natpmp),
            enablePeX = true,
            enableUtp = true,
            encryptionMode = EncryptionMode.values()[s.getInt(SessionSettings.encryption_mode)],
            maxConnections = s.getInt(SessionSettings.max_connections),
            maxUploadSlots = s.getInt(SessionSettings.max_upload_slots),
            maxHalfOpenConnections = s.getInt(SessionSettings.max_half_open_connections),
            connectionsPerTorrent = s.getInt(SessionSettings.connections_per_torrent),
            maxUploadSlotsPerTorrent = s.getInt(SessionSettings.upload_slots_per_torrent),
            downloadRateLimit = s.getInt(SessionSettings.download_rate_limit).toLong() / 1024,
            uploadRateLimit = s.getInt(SessionSettings.upload_rate_limit).toLong() / 1024,
        )
    }

    fun start() {
        if (sessionManager != null) {
            sessionManager!!.start()
        }
    }

    fun stop() {
        alertProcessor.set(false)
        alertChannel.close()
        torrents.values.forEach { saveResumeData(it) }
        trackerListProvider.stopPeriodicRefresh()
        seederHuntManager.stopAll()
        peerWarmupManager.stopAll()
        writeCoalescer.stop()
        lazyVerifier.stopBackgroundVerification()
        metadataFetchManager.cancelAll()
        sessionManager?.stop()
        sessionManager = null
        session = null
        isInitialized = false
    }

    private fun initTorrentState(handle: TorrentHandle) {
        val infoHash = handle.infoHash().toHex()

        val state = MutableStateFlow(buildTorrentInfo(handle))
        torrentStates[infoHash] = state

        updateGlobalStats()
    }

    private fun initTorrentFiles(handle: TorrentHandle) {
        val infoHash = handle.infoHash().toHex()
        val torrentInfo = handle.torrentFile() ?: return
        val files = torrentInfo.files()
        val fileList = mutableListOf<TorrentFile>()
        files.forEachIndexed { index, file ->
            fileList.add(TorrentFile(
                path = file.path,
                size = file.size,
                priority = mapIntToPriority(file.priority),
                progress = 0f,
                isMediaFile = isMediaFile(file.path),
                index = index,
            ))
        }
        torrentStates[infoHash]?.value?.let { oldState ->
            torrentStates[infoHash]?.value = oldState.copy(files = fileList)
        }
    }

    private fun buildTorrentInfo(handle: TorrentHandle): TorrentInfo {
        val status = handle.status()
        val torrentInfo = handle.torrentFile()
        val infoHash = handle.infoHash().toHex()

        val files = if (torrentInfo != null) {
            val fileList = mutableListOf<TorrentFile>()
            val files = torrentInfo.files()
            files.forEachIndexed { index, file ->
                fileList.add(TorrentFile(
                    path = file.path,
                    size = file.size,
                    priority = mapIntToPriority(file.priority),
                    progress = status.fileProgress?.get(index) ?: 0f,
                    isMediaFile = isMediaFile(file.path),
                    index = index,
                ))
            }
            fileList
        } else emptyList()

        val swarmHealth = computeSwarmHealth(status)

        return TorrentInfo(
            infoHash = infoHash,
            name = torrentInfo?.name() ?: status.name,
            totalSize = status.totalWanted,
            downloadDir = handle.savePath(),
            status = mapStatus(status.state),
            progress = status.progress * 100,
            downloadSpeed = status.downloadRate,
            uploadSpeed = status.uploadRate,
            peers = status.numPeers,
            seeds = status.numSeeds,
            eta = status.eta.toLong(),
            priority = status.queuePosition,
            isSequentialDownload = status.sequentialDownload,
            seedRatioLimit = status.seedRatioLimit,
            seedTimeLimitMinutes = status.seedTimeLimit / 60,
            addedAt = System.currentTimeMillis() - status.activeTime * 1000L,
            magnetUri = status.url,
            torrentFilePath = null,
            error = if (status.error != 0) status.errorMsg else null,
            files = files,
            savePath = handle.savePath(),
            downloadLimit = status.downloadRateLimit.toLong(),
            uploadLimit = status.uploadRateLimit.toLong(),
            ratio = status.allTimeUpload.toDouble() / status.allTimeDownload.toDouble().coerceAtLeast(1),
            totalDownloaded = status.allTimeDownload,
            totalUploaded = status.allTimeUpload,
            activeTime = status.activeTime * 1000L,
            seedingTime = status.seedingTime * 1000L,
            numPieces = torrentInfo?.numPieces() ?: 0,
            pieceSize = torrentInfo?.pieceLength() ?: 0,
            peersConnected = status.numPeers,
            trackerCount = status.numTrackers,
            swarmHealthScore = swarmHealth,
            dhtNodes = session?.dhtState()?.nodes ?: 0,
        )
    }

    private fun computeSwarmHealth(status: com.frostwire.jlibtorrent.TorrentStatus): Int {
        val seeds = status.numSeeds
        val peers = status.numPeers
        val progress = status.progress

        val seedScore = when {
            seeds >= 50 -> 30
            seeds >= 20 -> 25
            seeds >= 10 -> 20
            seeds >= 5 -> 15
            seeds >= 1 -> 10
            else -> 0
        }

        val peerScore = when {
            peers >= 100 -> 30
            peers >= 50 -> 25
            peers >= 20 -> 20
            peers >= 10 -> 15
            peers >= 1 -> 10
            else -> 0
        }

        val progressScore = (progress * 40).toInt()

        return (seedScore + peerScore + progressScore).coerceIn(0, 100)
    }

    private fun updateTorrentState(handle: TorrentHandle) {
        val infoHash = handle.infoHash().toHex()
        torrentStates[infoHash]?.value = buildTorrentInfo(handle)
    }

    private fun updateGlobalStats() {
        var totalDownload: Long = 0
        var totalUpload: Long = 0
        var active = 0
        var paused = 0
        var total = torrents.size
        var totalPeers = 0

        torrents.values.forEach { handle ->
            val status = handle.status()
            totalDownload += status.downloadRate
            totalUpload += status.uploadRate
            totalPeers += status.numPeers
            if (status.state == com.frostwire.jlibtorrent.TorrentStatus.downloading.ordinal ||
                status.state == com.frostwire.jlibtorrent.TorrentStatus.seeding.ordinal) {
                active++
            } else if (status.state == com.frostwire.jlibtorrent.TorrentStatus.paused.ordinal) {
                paused++
            }
        }

        _globalStats.value = GlobalStats(
            totalDownloadSpeed = totalDownload,
            totalUploadSpeed = totalUpload,
            activeTorrents = active,
            pausedTorrents = paused,
            totalTorrents = total,
            dhtNodes = session?.dhtState()?.nodes ?: 0,
            totalDownload = torrents.values.sumOf { it.status().allTimeDownload },
            totalUpload = torrents.values.sumOf { it.status().allTimeUpload },
            sessionTime = System.currentTimeMillis() / 1000,
            trackerCount = trackerListProvider.trackerCount.value,
            totalPeersConnected = totalPeers,
            portMapped = portMappingMonitor.mappingState.value?.isSuccessful == true,
        )
    }

    fun writeCoalescer(): WriteCoalescer = writeCoalescer
    fun lazyVerifier(): LazyVerifier = lazyVerifier
    fun metadataCache(): TorrentMetadataCache = torrentMetadataCache
    fun pieceCache(): TorrentPieceCache = torrentPieceCache
    fun portMonitor(): PortMappingMonitor = portMappingMonitor
    fun bandwidthSelector(): BandwidthAwareSelector = bandwidthAwareSelector
    fun prefetchManager(): PredictivePrefetchManager = predictivePrefetchManager
    fun seederHunt(): SeederHuntManager = seederHuntManager
    fun trackerList(): TrackerListProvider = trackerListProvider
    fun metadataFetch(): MetadataFetchManager = metadataFetchManager

    private fun saveResumeData(handle: TorrentHandle) {
        if (handle.isValid && handle.needSaveResumeData()) {
            handle.saveResumeData()
        }
    }

    private fun loadResumeData() {
        val files = resumeDataPath.listFiles() ?: return
        files.forEach { file ->
            try {
                val bytes = file.readBytes()
                val resumeData = com.frostwire.jlibtorrent.bdecode(bytes)
                val params = AddTorrentParams()
                params.setResumeData(resumeData)
                val handle = session!!.addTorrent(params)
                if (handle.isValid) {
                    val infoHash = handle.infoHash().toHex()
                    torrents[infoHash] = handle
                    initTorrentState(handle)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load resume data from ${file.name}", e)
            }
        }
    }

    private fun mapStatus(state: Int): TorrentStatus {
        return when (state) {
            com.frostwire.jlibtorrent.TorrentStatus.queued_for_checking.ordinal -> TorrentStatus.QUEUED
            com.frostwire.jlibtorrent.TorrentStatus.checking_files.ordinal -> TorrentStatus.CHECKING
            com.frostwire.jlibtorrent.TorrentStatus.downloading_metadata.ordinal -> TorrentStatus.DOWNLOADING_METADATA
            com.frostwire.jlibtorrent.TorrentStatus.downloading.ordinal -> TorrentStatus.DOWNLOADING
            com.frostwire.jlibtorrent.TorrentStatus.finished.ordinal -> TorrentStatus.FINISHED
            com.frostwire.jlibtorrent.TorrentStatus.seeding.ordinal -> TorrentStatus.SEEDING
            com.frostwire.jlibtorrent.TorrentStatus.allocating.ordinal -> TorrentStatus.CHECKING
            com.frostwire.jlibtorrent.TorrentStatus.checking_resume_data.ordinal -> TorrentStatus.CHECKING_RESUME_DATA
            else -> TorrentStatus.PAUSED
        }
    }

    private fun mapIntToPriority(priority: Int): FilePriority {
        return when (priority) {
            0 -> FilePriority.DONT_DOWNLOAD
            1 -> FilePriority.LOW
            2 -> FilePriority.NORMAL
            3 -> FilePriority.HIGH
            4 -> FilePriority.DONT_DOWNLOAD
            else -> FilePriority.NORMAL
        }
    }

    private fun mapPriorityToInt(priority: FilePriority): Int {
        return when (priority) {
            FilePriority.DONT_DOWNLOAD -> 0
            FilePriority.LOW -> 1
            FilePriority.NORMAL -> 2
            FilePriority.HIGH -> 3
        }
    }

    private fun isMediaFile(path: String): Boolean {
        val ext = path.substringAfterLast(".", "").lowercase()
        return ext in setOf("mp4", "mkv", "avi", "mov", "flv", "wmv", "webm", "m4v", "ts", "mp3", "m4a", "flac", "aac", "ogg", "wav")
    }
}
