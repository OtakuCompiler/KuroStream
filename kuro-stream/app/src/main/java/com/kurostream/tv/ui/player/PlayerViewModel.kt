package com.kurostream.tv.ui.player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.kurostream.tv.core.perf.MemoryAwareQualitySelector
import com.kurostream.tv.core.perf.RuntimeConstraint
import com.kurostream.tv.core.player.SkipOverlayController
import com.kurostream.tv.core.player.SkipOverlayState
import com.kurostream.tv.di.IoDispatcher
import com.kurostream.tv.di.PlayerModule
import com.kurostream.tv.domain.model.Episode
import com.kurostream.tv.domain.provider.ProviderAggregator
import com.kurostream.tv.domain.provider.ProviderStream
import com.kurostream.tv.domain.provider.StreamQuality
import com.kurostream.tv.domain.provider.StreamType
import com.kurostream.tv.domain.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// ─── UI State ─────────────────────────────────────────────────────────────────

/**
 * Complete UI state for the player screen.
 *
 * Computed convenience properties ([progress], [bufferedProgress], [position],
 * [duration]) are derived from the raw millisecond values so composables never
 * need to do arithmetic themselves.
 */
data class PlayerUiState(
    val isLoading: Boolean = true,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isBuffering: Boolean = false,
    val error: String? = null,

    // Content info
    val animeId: String? = null,
    val animeTitle: String = "",
    val episodeNumber: Int = 0,
    val episodeTitle: String = "",
    val episode: Episode? = null,
    val episodes: List<Episode> = emptyList(),

    // Episode navigation helpers
    val hasPreviousEpisode: Boolean = false,
    val hasNextEpisode: Boolean = false,

    // Stream sources
    val availableStreams: List<ProviderStream> = emptyList(),
    val currentStream: ProviderStream? = null,
    val isLoadingStreams: Boolean = false,

    // Playback state (raw milliseconds)
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val bufferedPositionMs: Long = 0,
    val playbackSpeed: Float = 1.0f,

    // UI visibility
    val showControls: Boolean = true,
    val showStreamSelector: Boolean = false,
    val showQualitySelector: Boolean = false,
    val showSubtitleSelector: Boolean = false,
    val showEpisodeList: Boolean = false,

    // Player backend
    val useVlcPlayer: Boolean = false
) {
    /** Playback progress in [0, 1]. */
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    /** Buffered progress in [0, 1]. */
    val bufferedProgress: Float
        get() = if (durationMs > 0) (bufferedPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    /** Current playback position in milliseconds (alias for composable convenience). */
    val position: Long get() = currentPositionMs

    /** Content duration in milliseconds (alias for composable convenience). */
    val duration: Long get() = durationMs
}

/**
 * Minimal subtitle track descriptor surfaced to the UI.
 */
data class SubtitleState(
    val availableTracks: List<SubtitleTrack> = emptyList(),
    val selectedTrack: SubtitleTrack? = null
)

data class SubtitleTrack(
    val id: String,
    val language: String,
    val label: String,
    val isDefault: Boolean = false
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

/**
 * ViewModel for the full-screen video player.
 *
 * Owns the [ExoPlayer] lifecycle, loads streams via [ProviderAggregator],
 * forwards skip overlay events from [SkipOverlayController], and handles
 * auto-retry / stream fallback on playback errors.
 *
 * Quality selection adapts to available RAM at runtime via
 * [MemoryAwareQualitySelector] — defaulting to 1080p on 1 GB devices and
 * offering 4K on capable higher-RAM hardware.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val providerAggregator: ProviderAggregator,
    private val animeRepository: AnimeRepository,
    private val exoPlayerFactory: PlayerModule.ExoPlayerFactory,
    private val skipOverlayController: SkipOverlayController,
    private val qualitySelector: MemoryAwareQualitySelector,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    companion object {
        private const val TAG = "PlayerViewModel"
        private const val CONTROLS_HIDE_DELAY_MS = 5_000L
        private const val POSITION_UPDATE_INTERVAL_MS = 500L
        private const val SAVE_PROGRESS_INTERVAL_MS = 30_000L
        private const val MINIMUM_WATCH_PERCENTAGE = 0.9
    }

    // ─── State ────────────────────────────────────────────────────────────────

    private val _playerState = MutableStateFlow(PlayerUiState())

    /** Primary state flow consumed by [PlayerScreen]. */
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    /** Backwards-compat alias so existing internal callers of uiState still compile. */
    val uiState: StateFlow<PlayerUiState> = playerState

    /** Skip overlay state from [SkipOverlayController]. */
    val skipState: StateFlow<SkipOverlayState> = skipOverlayController.skipState

    private val _subtitleState = MutableStateFlow(SubtitleState())

    /** Subtitle track selection state. */
    val subtitleState: StateFlow<SubtitleState> = _subtitleState.asStateFlow()

    // ─── ExoPlayer ────────────────────────────────────────────────────────────

    private var _player: ExoPlayer? = null

    /** Returns the active [ExoPlayer] instance (created lazily on first call). */
    fun getPlayer(): ExoPlayer {
        return _player ?: exoPlayerFactory.create().also {
            _player = it
            setupPlayerListener(it)
            startPositionUpdates()
        }
    }

    // ─── Jobs ─────────────────────────────────────────────────────────────────

    private var controlsHideJob: Job? = null
    private var positionUpdateJob: Job? = null
    private var progressSaveJob: Job? = null

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Load and begin playing [episodeNumber] of [animeId].
     * Called from [PlayerScreen]'s `LaunchedEffect`.
     */
    fun loadEpisode(animeId: String, episodeNumber: Int) {
        Timber.tag(TAG).d("loadEpisode animeId=$animeId ep=$episodeNumber")

        _playerState.update {
            it.copy(isLoading = true, animeId = animeId, episodeNumber = episodeNumber, error = null)
        }

        viewModelScope.launch(ioDispatcher) {
            loadEpisodeInfo(animeId, episodeNumber)
        }

        startProgressSaving()
    }

    /** Explicit play — resumes if paused. */
    fun play() {
        _player?.play()
        showControls()
    }

    /** Explicit pause. */
    fun pause() {
        _player?.pause()
        showControls()
    }

    /** Toggle play / pause. */
    fun togglePlayPause() {
        _player?.let { p ->
            if (p.isPlaying) p.pause() else p.play()
        }
        showControls()
    }

    fun seekTo(positionMs: Long) {
        _player?.seekTo(positionMs)
        showControls()
    }

    fun seekForward(amountMs: Long = 10_000) {
        _player?.let { p ->
            p.seekTo((p.currentPosition + amountMs).coerceAtMost(p.duration))
        }
        showControls()
    }

    fun seekBackward(amountMs: Long = 10_000) {
        _player?.let { p ->
            p.seekTo((p.currentPosition - amountMs).coerceAtLeast(0))
        }
        showControls()
    }

    fun setPlaybackSpeed(speed: Float) {
        _player?.setPlaybackSpeed(speed)
        _playerState.update { it.copy(playbackSpeed = speed) }
        showControls()
    }

    /** Retry the current stream (e.g. after a network hiccup). */
    fun retry() {
        _playerState.value.currentStream?.let { selectStream(it) }
    }

    /** Backwards-compat alias. */
    fun retryCurrentStream() = retry()

    /** Navigate to the previous episode if available. */
    fun previousEpisode() {
        val state = _playerState.value
        val episodes = state.episodes
        val current = state.episode ?: return
        val idx = episodes.indexOfFirst { it.id == current.id }
        if (idx > 0) playEpisodeById(episodes[idx - 1].id)
    }

    /** Navigate to the next episode if available. */
    fun nextEpisode() {
        val state = _playerState.value
        val episodes = state.episodes
        val current = state.episode ?: return
        val idx = episodes.indexOfFirst { it.id == current.id }
        if (idx in 0 until episodes.size - 1) playEpisodeById(episodes[idx + 1].id)
    }

    /** Skip the currently active segment (intro / recap / ending). */
    fun skipCurrentSegment() {
        skipOverlayController.skipCurrentSegment()?.let { targetMs ->
            _player?.seekTo(targetMs)
        }
    }

    /** Dismiss the skip button without skipping. */
    fun dismissSkipButton() {
        skipOverlayController.dismissSkipButton()
    }

    // UI panel toggles

    fun showControls() {
        _playerState.update { it.copy(showControls = true) }
        scheduleControlsHide()
    }

    fun hideControls() {
        _playerState.update { it.copy(showControls = false) }
    }

    fun toggleStreamSelector() =
        _playerState.update { it.copy(showStreamSelector = !it.showStreamSelector) }

    fun toggleQualitySelector() =
        _playerState.update { it.copy(showQualitySelector = !it.showQualitySelector) }

    fun toggleSubtitleSelector() =
        _playerState.update { it.copy(showSubtitleSelector = !it.showSubtitleSelector) }

    fun toggleEpisodeList() =
        _playerState.update { it.copy(showEpisodeList = !it.showEpisodeList) }

    fun clearError() = _playerState.update { it.copy(error = null) }

    /**
     * Select a specific stream source and begin playback.
     */
    fun selectStream(stream: ProviderStream) {
        viewModelScope.launch {
            _playerState.update {
                it.copy(currentStream = stream, isLoading = true, error = null, showStreamSelector = false)
            }

            val needsVlc = stream.type == StreamType.TORRENT || !isExoPlayerCompatible(stream)

            if (needsVlc) {
                _playerState.update { it.copy(useVlcPlayer = true, isLoading = false) }
            } else {
                playWithExoPlayer(stream)
            }
        }
    }

    /**
     * Release the ExoPlayer and cancel background jobs.
     * Called by [PlayerScreen]'s `DisposableEffect`.
     */
    fun release() {
        controlsHideJob?.cancel()
        positionUpdateJob?.cancel()
        progressSaveJob?.cancel()
        skipOverlayController.stopMonitoring()

        viewModelScope.launch(ioDispatcher) { saveCurrentProgress() }

        _player?.release()
        _player = null
    }

    // ─── Private: episode loading ─────────────────────────────────────────────

    private suspend fun loadEpisodeInfo(animeId: String, episodeNumber: Int) {
        try {
            animeRepository.getAnimeById(animeId).collect { result ->
                result.onSuccess { anime ->
                    _playerState.update { it.copy(animeTitle = anime.title) }
                }
            }

            animeRepository.getEpisodes(animeId).collect { result ->
                result.onSuccess { episodes ->
                    val episode = episodes.find { it.number == episodeNumber }
                        ?: episodes.getOrNull(episodeNumber - 1)
                    val currentIdx = episodes.indexOfFirst { it.id == episode?.id }

                    _playerState.update { state ->
                        state.copy(
                            episodes = episodes,
                            episode = episode,
                            episodeTitle = episode?.title ?: "Episode $episodeNumber",
                            hasPreviousEpisode = currentIdx > 0,
                            hasNextEpisode = currentIdx in 0 until episodes.size - 1
                        )
                    }

                    episode?.let { ep ->
                        loadStreamsForEpisode(animeId, ep.id, ep.number)
                    } ?: run {
                        _playerState.update { it.copy(isLoading = false, error = "Episode not found") }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "loadEpisodeInfo failed")
            _playerState.update { it.copy(isLoading = false, error = "Failed to load episode info") }
        }
    }

    private suspend fun loadStreamsForEpisode(animeId: String, episodeId: String, episodeNumber: Int) {
        _playerState.update { it.copy(isLoadingStreams = true) }

        try {
            // Determine preferred quality based on available RAM at this moment
            val preferredQuality = when (qualitySelector.runtimeQualityConstraint()) {
                is RuntimeConstraint.Force720p -> StreamQuality.HD_720
                is RuntimeConstraint.Cap1080p  -> StreamQuality.HD_1080
                is RuntimeConstraint.UseProfile -> {
                    val profile = qualitySelector.profile
                    when {
                        profile.has4kCapability -> StreamQuality.UHD_4K
                        profile.maxHeight >= 1080 -> StreamQuality.HD_1080
                        else -> StreamQuality.HD_720
                    }
                }
            }

            Timber.tag(TAG).d("Loading streams — preferredQuality=$preferredQuality")

            val streams = providerAggregator.getStreams(
                animeId = animeId,
                episodeId = episodeId,
                preferredQuality = preferredQuality
            )

            _playerState.update {
                it.copy(availableStreams = streams, isLoadingStreams = false)
            }

            if (streams.isNotEmpty()) {
                selectStream(streams.first())
                // Load skip timestamps after stream is ready
                loadSkipTimestamps(animeId, episodeNumber)
            } else {
                _playerState.update {
                    it.copy(isLoading = false, error = "No streams available")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "loadStreams failed")
            _playerState.update {
                it.copy(isLoadingStreams = false, isLoading = false, error = "Failed to load streams: ${e.message}")
            }
        }
    }

    private fun loadSkipTimestamps(animeId: String, episodeNumber: Int) {
        viewModelScope.launch(ioDispatcher) {
            runCatching {
                val malId = animeId.toLongOrNull() ?: return@runCatching
                skipOverlayController.loadSkipTimestamps(malId, episodeNumber)
                skipOverlayController.startMonitoring(
                    scope = viewModelScope,
                    getPosition = { _player?.currentPosition ?: 0L }
                )
            }
        }
    }

    // ─── Private: playback ────────────────────────────────────────────────────

    private fun playWithExoPlayer(stream: ProviderStream) {
        val player = getPlayer()
        val mediaItem = buildMediaItem(stream)

        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        _playerState.update { it.copy(isLoading = false, useVlcPlayer = false) }
    }

    private fun buildMediaItem(stream: ProviderStream): MediaItem {
        val builder = MediaItem.Builder().setUri(Uri.parse(stream.url))

        if (stream.subtitles.isNotEmpty()) {
            val configs = stream.subtitles.map { sub ->
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                    .setLanguage(sub.language)
                    .setLabel(sub.label)
                    .setSelectionFlags(
                        if (sub.isDefault) C.SELECTION_FLAG_DEFAULT else 0
                    )
                    .build()
            }
            builder.setSubtitleConfigurations(configs)
            _subtitleState.update {
                it.copy(
                    availableTracks = stream.subtitles.map { sub ->
                        SubtitleTrack(sub.language, sub.language, sub.label, sub.isDefault)
                    },
                    selectedTrack = stream.subtitles.firstOrNull { it.isDefault }
                        ?.let { sub -> SubtitleTrack(sub.language, sub.language, sub.label, true) }
                )
            }
        }

        return builder.build()
    }

    private fun isExoPlayerCompatible(stream: ProviderStream): Boolean = when (stream.type) {
        StreamType.DIRECT, StreamType.HLS, StreamType.DASH -> true
        StreamType.TORRENT, StreamType.EXTERNAL -> false
    }

    private fun playEpisodeById(episodeId: String) {
        val animeId = _playerState.value.animeId ?: return
        val episodes = _playerState.value.episodes
        val targetEp = episodes.firstOrNull { it.id == episodeId } ?: return

        _player?.stop()
        _player?.clearMediaItems()
        _playerState.update { it.copy(isLoading = true, showEpisodeList = false) }

        viewModelScope.launch(ioDispatcher) {
            loadEpisodeInfo(animeId, targetEp.number)
        }
    }

    // ─── Private: player listener ─────────────────────────────────────────────

    private fun setupPlayerListener(player: ExoPlayer) {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING ->
                        _playerState.update { it.copy(isBuffering = true) }

                    Player.STATE_READY ->
                        _playerState.update {
                            it.copy(
                                isLoading = false,
                                isBuffering = false,
                                durationMs = player.duration.takeIf { d -> d > 0 } ?: it.durationMs
                            )
                        }

                    Player.STATE_ENDED -> onPlaybackEnded()
                    Player.STATE_IDLE -> Unit
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update {
                    it.copy(
                        isPlaying = isPlaying,
                        isPaused = !isPlaying && player.playbackState == Player.STATE_READY
                    )
                }
                if (isPlaying) scheduleControlsHide()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                handlePlaybackError(error)
            }
        })
    }

    private fun handlePlaybackError(error: androidx.media3.common.PlaybackException) {
        Timber.tag(TAG).w(error, "Playback error")
        val streams = _playerState.value.availableStreams
        val current = _playerState.value.currentStream
        val nextIdx = streams.indexOf(current) + 1

        if (nextIdx in streams.indices) {
            Timber.tag(TAG).d("Falling back to stream index $nextIdx")
            selectStream(streams[nextIdx])
        } else {
            _playerState.update {
                it.copy(isLoading = false, error = "Playback failed: ${error.message}")
            }
        }
    }

    private fun onPlaybackEnded() {
        val state = _playerState.value

        viewModelScope.launch(ioDispatcher) {
            state.episode?.let { ep ->
                state.animeId?.let { id -> animeRepository.markEpisodeWatched(id, ep.number) }
            }
        }

        // Auto-play next episode
        if (state.hasNextEpisode) {
            nextEpisode()
        }
    }

    // ─── Private: periodic jobs ───────────────────────────────────────────────

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (true) {
                _player?.let { p ->
                    _playerState.update {
                        it.copy(
                            currentPositionMs = p.currentPosition,
                            bufferedPositionMs = p.bufferedPosition
                        )
                    }
                }
                delay(POSITION_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun startProgressSaving() {
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch(ioDispatcher) {
            while (true) {
                delay(SAVE_PROGRESS_INTERVAL_MS)
                saveCurrentProgress()
            }
        }
    }

    private suspend fun saveCurrentProgress() {
        val state = _playerState.value
        val animeId = state.animeId ?: return
        val episode = state.episode ?: return
        val position = state.currentPositionMs
        val duration = state.durationMs

        if (duration > 0) {
            animeRepository.updateWatchProgress(animeId, episode.number, position, duration)
            if (position.toDouble() / duration >= MINIMUM_WATCH_PERCENTAGE) {
                animeRepository.markEpisodeWatched(animeId, episode.number)
            }
        }
    }

    private fun scheduleControlsHide() {
        controlsHideJob?.cancel()
        controlsHideJob = viewModelScope.launch {
            delay(CONTROLS_HIDE_DELAY_MS)
            if (_playerState.value.isPlaying) hideControls()
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        release()
    }
}
