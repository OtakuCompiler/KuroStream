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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.kurostream.app.model.PlaybackUrl
import com.kurostream.domain.repository.SettingsRepository
import com.kurostream.app.repository.TvRepositories
import com.kurostream.domain.result.Result
import com.kurostream.common.memory.LowRamDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val mediaRepository: MediaRepository,
    private val watchProgressRepository: WatchProgressRepository
) : AndroidViewModel(application) {

    val player: ExoPlayer

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var mediaId: String? = null
    private var episodeId: String? = null

    init {
        // Load subtitle settings from repository (async to avoid main-thread I/O)
        viewModelScope.launch(Dispatchers.IO) {
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

        // Observe subtitle setting changes from outside (e.g. SettingsScreen)
        viewModelScope.launch {
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
        player = PlayerConfig.create(application, lowRam = isLowRam)
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        _uiState.update {
                            it.copy(
                                isBuffering = state == Player.STATE_BUFFERING,
                                duration = if (state == Player.STATE_READY) duration.coerceAtLeast(0) else it.duration
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

  viewModelScope.launch {
    try {
      while (isActive) {
        if (player.isPlaying) {
          _uiState.update {
            it.copy(
              currentPosition = player.currentPosition.coerceAtLeast(0),
              bufferedPosition = player.bufferedPosition.coerceAtLeast(0)
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
  viewModelScope.launch {
    try {
      _uiState.update { it.copy(isBuffering = true) }

      val result = mediaRepository.getPlaybackUrl(mediaId, episodeId)
    result.fold(
        onSuccess = { playbackUrl ->
            _uiState.update { it.copy(title = playbackUrl.title) }
            val mediaItem = ExoMediaItem.fromUri(playbackUrl.url)
            player.setMediaItem(mediaItem, startPositionMs)
            player.prepare()
            player.play()
        },
        onError = { error ->
            _uiState.update { it.copy(error = error.message, isBuffering = false) }
        },
        onLoading = { _uiState.update { it.copy(isBuffering = true) } }
    )
    } catch (e: Exception) {
      _uiState.update { it.copy(error = e.message, isBuffering = false) }
    }
  }
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceIn(0, player.duration.coerceAtLeast(0)))
    }

    fun seekForward() {
        player.seekForward()
    }

    fun seekBackward() {
        player.seekBack()
    }

    fun skipIntro() {
        val targetPosition = (player.currentPosition + _uiState.value.skipIntroDurationMs)
            .coerceAtMost(player.duration)
        player.seekTo(targetPosition)
    }

    fun skipOutro() {
        val targetPosition = (player.currentPosition + _uiState.value.skipOutroDurationMs)
            .coerceAtMost(player.duration)
        player.seekTo(targetPosition)
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
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
        viewModelScope.launch { settingsRepository.setSubtitleFontSize(size) }
    }

    fun setSubtitleFontColor(hex: String) {
        _uiState.update { it.copy(subtitleFontColorHex = hex) }
        viewModelScope.launch { settingsRepository.setSubtitleFontColor(hex) }
    }

    fun setSubtitleBgColor(hex: String) {
        _uiState.update { it.copy(subtitleBgColorHex = hex) }
        viewModelScope.launch { settingsRepository.setSubtitleBgColor(hex) }
    }

    fun setSubtitleEnabled(enabled: Boolean) {
        _uiState.update { it.copy(subtitleEnabled = enabled) }
        viewModelScope.launch { settingsRepository.setSubtitleEnabled(enabled) }
    }

  fun playNextEpisode() {
    val currentMedia = mediaId ?: return
    viewModelScope.launch {
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
    val position = player.currentPosition
    val total = player.duration

    viewModelScope.launch {
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
        player.release()
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
