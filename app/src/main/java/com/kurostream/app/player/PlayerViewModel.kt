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
import com.kurostream.app.repository.MediaRepository
import com.kurostream.app.repository.WatchProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val error: String? = null
)

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val mediaRepository: MediaRepository,
    private val watchProgressRepository: WatchProgressRepository
) : AndroidViewModel(application) {

    val player: ExoPlayer

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var mediaId: String? = null
    private var episodeId: String? = null

    init {
        player = ExoPlayer.Builder(application)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
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
        }
    }

    fun preparePlayback(mediaId: String, episodeId: String?, startPositionMs: Long) {
        this.mediaId = mediaId
        this.episodeId = episodeId
        viewModelScope.launch {
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
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message, isBuffering = false) }
                }
            )
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

    fun playNextEpisode() {
        val currentMedia = mediaId ?: return
        viewModelScope.launch {
            mediaRepository.getNextEpisode(currentMedia, episodeId)
                .onSuccess { nextEpisode ->
                    preparePlayback(currentMedia, nextEpisode.id, 0L)
                }
        }
    }

    fun saveProgress() {
        val currentMedia = mediaId ?: return
        val currentEpisode = episodeId
        val position = player.currentPosition
        val total = player.duration

        viewModelScope.launch {
            watchProgressRepository.saveProgress(
                mediaId = currentMedia,
                episodeId = currentEpisode,
                positionMs = position,
                durationMs = total
            )
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
