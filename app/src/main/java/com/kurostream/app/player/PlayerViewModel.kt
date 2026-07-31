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

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.kurostream.app.model.PlaybackUrl
import com.kurostream.domain.repository.SettingsRepository
import com.kurostream.app.repository.TvRepositories.MediaRepository
import com.kurostream.app.repository.TvRepositories.WatchProgressRepository
import com.kurostream.domain.result.Result as DomainResult
import com.kurostream.common.memory.LowRamDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
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

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val mediaRepository: MediaRepository,
    private val watchProgressRepository: WatchProgressRepository
) : ViewModel() {

    private var player: ExoPlayer? = null
    private var playerReady = false

    /** Exposes the current player instance for UI-only read access. */
    val currentPlayer: ExoPlayer? get() = player

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var mediaId: String? = null
    private var episodeId: String? = null

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "PlayerViewModel coroutine failed")
        _uiState.update { it.copy(error = throwable.message ?: "Unexpected error") }
    }
    private val scope = viewModelScope + exceptionHandler

    init {
        scope.launch(Dispatchers.IO) {
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

        scope.launch {
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

        val isLowRam = LowRamDevice.isLowRamDevice()
        player = PlayerConfig.create(context, lowRam = isLowRam).apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    _uiState.update {
                        it.copy(
                            isBuffering = state == Player.STATE_BUFFERING,
                            duration = if (state == Player.STATE_READY) it.duration.coerceAtLeast(0L) else it.duration
                        )
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    _uiState.update { it.copy(isPlaying = playing) }
                }

                override fun onPlayerError(error: PlaybackException) {
                    _uiState.update { it.copy(error = error.message, isBuffering = false) }
                }
            })
        }
        playerReady = true

        scope.launch {
            try {
                while (isActive) {
                    if (playerReady && player?.isPlaying == true) {
                        _uiState.update {
                            it.copy(
                                currentPosition = player?.currentPosition ?: 0L,
                                bufferedPosition = player?.bufferedPosition ?: 0L
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
        this.mediaId = mediaId
        this.episodeId = episodeId
        scope.launch {
            try {
                _uiState.update { it.copy(isBuffering = true) }

                val result = mediaRepository.getPlaybackUrl(mediaId, episodeId)
                when (result) {
                    is DomainResult.Success<*> -> {
                        val playbackUrl = result.data as? PlaybackUrl
                            ?: throw IllegalStateException("Playback URL resolution returned invalid data")
                        _uiState.update { it.copy(title = playbackUrl.title) }
                        val mediaItem = ExoMediaItem.fromUri(playbackUrl.url)
                        player?.setMediaItem(mediaItem, startPositionMs)
                        player?.prepare()
                        player?.play()
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
        if (player?.isPlaying == true) player?.pause() else player?.play()
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.coerceIn(0, player?.duration?.coerceAtLeast(0) ?: 0L))
    }

    fun seekForward() {
        player?.seekForward()
    }

    fun seekBackward() {
        player?.seekBack()
    }

    fun skipIntro() {
        val targetPosition = ((player?.currentPosition ?: 0L) + _uiState.value.skipIntroDurationMs)
            .coerceAtMost(player?.duration ?: 0L)
        player?.seekTo(targetPosition)
    }

    fun skipOutro() {
        val targetPosition = ((player?.currentPosition ?: 0L) + _uiState.value.skipOutroDurationMs)
            .coerceAtMost(player?.duration ?: 0L)
        player?.seekTo(targetPosition)
    }

    fun setPlaybackSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
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
        scope.launch { settingsRepository.setSubtitleFontSize(size) }
    }

    fun setSubtitleFontColor(hex: String) {
        _uiState.update { it.copy(subtitleFontColorHex = hex) }
        scope.launch { settingsRepository.setSubtitleFontColor(hex) }
    }

    fun setSubtitleBgColor(hex: String) {
        _uiState.update { it.copy(subtitleBgColorHex = hex) }
        scope.launch { settingsRepository.setSubtitleBgColor(hex) }
    }

    fun setSubtitleEnabled(enabled: Boolean) {
        _uiState.update { it.copy(subtitleEnabled = enabled) }
        scope.launch { settingsRepository.setSubtitleEnabled(enabled) }
    }

    fun playNextEpisode() {
        val currentMedia = mediaId ?: return
        scope.launch {
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
        val position = player?.currentPosition ?: 0L
        val total = player?.duration ?: 0L

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
        player?.release()
        player = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
