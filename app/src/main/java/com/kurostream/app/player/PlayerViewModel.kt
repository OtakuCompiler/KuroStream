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

package com.kurostream.app.player

import com.kurostream.players.selector.BackendSelector
import com.kurostream.players.selector.PlaybackEngine
import com.kurostream.app.model.PlaybackUrl
import com.kurostream.domain.repository.SettingsRepository
import com.kurostream.app.repository.TvRepositories.MediaRepository
import com.kurostream.app.repository.TvRepositories.WatchProgressRepository
import com.kurostream.domain.result.Result as DomainResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class PlayerUiState(
    val title: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val error: String? = null,
    val playbackSpeed: Float = 1f,
    val skipIntroDurationMs: Long = 90_000L,
    val skipOutroDurationMs: Long = 90_000L,
    val subtitleFontSize: Float = 24f,
    val subtitleFontColorHex: String = "#FFFFFF",
    val subtitleBgColorHex: String = "#80000000",
    val subtitleEnabled: Boolean = true,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val settingsRepository: SettingsRepository,
    private val mediaRepository: MediaRepository,
    private val watchProgressRepository: WatchProgressRepository,
    private val backendSelector: BackendSelector,
) : androidx.lifecycle.ViewModel() {

    private var engine: PlaybackEngine? = null
    private var playerReady = false
    private val lockedSources = mutableSetOf<String>()

    /** Exposes the current engine instance for UI-only read access. */
    val currentEngine: PlaybackEngine? get() = engine

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var mediaJob: Job? = null

    private var mediaId: String? = null
    private var episodeId: String? = null

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "PlayerViewModel coroutine failed")
        _uiState.update { it.copy(error = throwable.message ?: "Unexpected error") }
    }

    init {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            val settings = settingsRepository.getPlayerSubtitleSettings()
            _uiState.update {
                it.copy(
                    subtitleFontSize = settings.fontSize,
                    subtitleFontColorHex = settings.fontColorHex,
                    subtitleBgColorHex = settings.bgColorHex,
                    subtitleEnabled = settings.enabled,
                )
            }
        }

        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            settingsRepository.observePlayerSubtitleSettings().collect { s ->
                _uiState.update {
                    it.copy(
                        subtitleFontSize = s.fontSize,
                        subtitleFontColorHex = s.fontColorHex,
                        subtitleBgColorHex = s.bgColorHex,
                        subtitleEnabled = s.enabled,
                    )
                }
            }
        }

        viewModelScope.launch(Dispatchers.Main + exceptionHandler) {
            try {
                engine = backendSelector.selectBackend()
                engine?.addListener(object : PlaybackEngine.Listener {
                    override fun onPlaybackStateChanged(state: PlaybackEngine.PlaybackState) {
                        _uiState.update {
                            it.copy(
                                isBuffering = state == PlaybackEngine.PlaybackState.BUFFERING,
                                duration = if (state == PlaybackEngine.PlaybackState.READY) it.duration.coerceAtLeast(0L) else it.duration
                            )
                        }
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        _uiState.update { it.copy(isPlaying = playing) }
                    }

                    override fun onError(message: String?) {
                        _uiState.update { it.copy(error = message, isBuffering = false) }
                    }
                })
                playerReady = true
            } catch (e: Exception) {
                Timber.e(e, "BackendSelector failed")
                _uiState.update { it.copy(error = e.message, isBuffering = false) }
            }
        }

        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                while (isActive) {
                    if (playerReady && engine != null) {
                        _uiState.update {
                            it.copy(
                                currentPosition = engine?.currentPosition?.value ?: 0L,
                                duration = engine?.duration?.value ?: 0L,
                                bufferedPosition = engine?.bufferedPosition?.value ?: 0L,
                                isPlaying = engine?.isPlaying?.value ?: false,
                            )
                        }
                    }
                    delay(500)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun preparePlayback(mediaId: String, episodeId: String?, startPositionMs: Long) {
        if (lockedSources.contains(mediaId)) {
            _uiState.update { it.copy(error = "This source is locked") }
            return
        }
        this.mediaId = mediaId
        this.episodeId = episodeId
        mediaJob?.cancel()
        mediaJob = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                _uiState.update { it.copy(isBuffering = true) }

                val result = mediaRepository.getPlaybackUrl(mediaId, episodeId)
                when (result) {
                    is DomainResult.Success<*> -> {
                        val playbackUrl = result.data as? PlaybackUrl
                            ?: throw IllegalStateException("Playback URL resolution returned invalid data")
                        _uiState.update { it.copy(title = playbackUrl.title) }
                        engine?.setMedia(playbackUrl.url, playbackUrl.title, startPositionMs)
                        engine?.play()
                    }
                    is DomainResult.Error -> {
                        _uiState.update { it.copy(error = result.exception.message, isBuffering = false) }
                    }
                    is DomainResult.Loading -> {
                        _uiState.update { it.copy(isBuffering = true) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isBuffering = false) }
            }
        }
    }

    fun togglePlayPause() {
        val playing = engine?.isPlaying?.value ?: false
        if (playing) {
            engine?.pause()
        } else {
            engine?.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val duration = engine?.duration?.value?.coerceAtLeast(0L) ?: 0L
        engine?.seekTo(positionMs.coerceIn(0, duration))
    }

    fun seekForward() {
        engine?.seekForward()
    }

    fun seekBackward() {
        engine?.seekBack()
    }

    fun skipIntro() {
        val targetPosition = ((engine?.currentPosition?.value ?: 0L) + _uiState.value.skipIntroDurationMs)
            .coerceAtMost(engine?.duration?.value ?: 0L)
        engine?.seekTo(targetPosition)
    }

    fun skipOutro() {
        val targetPosition = ((engine?.currentPosition?.value ?: 0L) + _uiState.value.skipOutroDurationMs)
            .coerceAtMost(engine?.duration?.value ?: 0L)
        engine?.seekTo(targetPosition)
    }

    fun setPlaybackSpeed(speed: Float) {
        engine?.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun setSkipIntroDuration(durationMs: Long) {
        _uiState.update { it.copy(skipIntroDurationMs = durationMs) }
    }

    fun setSkipOutroDuration(durationMs: Long) {
        _uiState.update { it.copy(skipOutroDurationMs = durationMs) }
    }

    fun setSubtitleFontSize(size: Float) {
        _uiState.update { it.copy(subtitleFontSize = size) }
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) { settingsRepository.setSubtitleFontSize(size) }
    }

    fun setSubtitleFontColor(hex: String) {
        _uiState.update { it.copy(subtitleFontColorHex = hex) }
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) { settingsRepository.setSubtitleFontColor(hex) }
    }

    fun setSubtitleBgColor(hex: String) {
        _uiState.update { it.copy(subtitleBgColorHex = hex) }
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) { settingsRepository.setSubtitleBgColor(hex) }
    }

    fun setSubtitleEnabled(enabled: Boolean) {
        _uiState.update { it.copy(subtitleEnabled = enabled) }
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) { settingsRepository.setSubtitleEnabled(enabled) }
    }

    fun playNextEpisode() {
        val currentMedia = mediaId ?: return
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                mediaRepository.getNextEpisode(currentMedia, episodeId)
                    .onSuccess { nextEpisode ->
                        preparePlayback(currentMedia, nextEpisode.id, 0L)
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun saveProgress() {
        val currentMedia = mediaId ?: return
        val currentEpisode = episodeId
        val position = engine?.currentPosition?.value ?: 0L
        val total = engine?.duration?.value ?: 0L

        scope.launch {
            try {
                watchProgressRepository.saveProgress(
                    mediaId = currentMedia,
                    episodeId = currentEpisode,
                    positionMs = position,
                    durationMs = total
                )
            } catch (e: Exception) {
                /* ignore save errors */
            }
        }
    }

    fun releasePlayer() {
        saveProgress()
        engine?.release()
        mediaJob?.cancel()
        mediaJob = null
        engine = null
        playerReady = false
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
