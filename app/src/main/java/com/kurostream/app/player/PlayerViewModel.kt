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

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.kurostream.app.sync.TraktSyncManager
import com.kurostream.data.subtitle.SubtitleDownloadManager
import com.kurostream.domain.entity.Episode
import com.kurostream.domain.model.PlaybackUrl
import com.kurostream.domain.result.Result
import com.kurostream.domain.usecase.GetPlaybackUrlUseCase
import com.kurostream.players.selector.PlaybackEngine
import com.kurostream.players.selector.PlaybackEngine.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPlaybackUrl: GetPlaybackUrlUseCase,
    private val watchProgressRepository: com.kurostream.domain.repository.WatchProgressRepository,
    private val mediaRepository: com.kurostream.domain.repository.MediaRepository,
    private val settingsRepository: com.kurostream.domain.repository.SettingsRepository,
    private val traktSyncManager: TraktSyncManager,
    private val subtitleDownloadManager: SubtitleDownloadManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _bufferedPosition = MutableStateFlow(0L)
    val bufferedPosition: StateFlow<Long> = _bufferedPosition.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String?>()
    val errorMessage: SharedFlow<String?> = _errorMessage.asSharedFlow()

    private val _currentEngine = MutableStateFlow<PlaybackEngine?>(null)
    val currentEngine: StateFlow<PlaybackEngine?> = _currentEngine.asStateFlow()

    private val _subtitleFile = MutableStateFlow<java.io.File?>(null)
    val subtitleFile: StateFlow<java.io.File?> = _subtitleFile.asStateFlow()

    private var engine: PlaybackEngine? = null
    private var mediaId: String? = null
    private var episodeId: String? = null
    private var progressUpdateJob: Job? = null
    private var lastReportedProgress: Float = 0f

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "PlayerViewModel coroutine error")
        _errorMessage.tryEmit(throwable.message)
    }

    private val restoredPosition = savedStateHandle.get<Long>("player_position") ?: 0L
    private val restoredMediaId = savedStateHandle.get<String>("media_id")

    init {
        savedStateHandle["player_position"] = 0L
        if (restoredMediaId != null) {
            viewModelScope.launch { preparePlayback(restoredMediaId, null, restoredPosition) }
        }
    }

    fun initialize() {
        viewModelScope.launch(Dispatchers.Main + exceptionHandler) {
            try {
                val engineInstance = com.kurostream.players.selector.BackendSelector.selectEngine()
                engineInstance.initialize()
                engine = engineInstance
                _currentEngine.value = engineInstance
                setupEngineListeners(engineInstance)
                _playbackState.value = PlaybackState.IDLE
            } catch (e: Exception) {
                Timber.e(e, "Player initialization failed")
                _errorMessage.emit("Failed to initialize player: ${e.message}")
                _playbackState.value = PlaybackState.ERROR
            }
        }
    }

    private fun setupEngineListeners(engineInstance: PlaybackEngine) {
        engineInstance.addListener(object : PlaybackEngine.Listener {
            override fun onPlaybackStateChanged(state: PlaybackState) {
                _playbackState.value = state
                when (state) {
                    PlaybackState.READY -> _isPlaying.value = true
                    PlaybackState.ENDED -> {
                        _isPlaying.value = false
                        onPlaybackCompleted()
                    }
                    PlaybackState.ERROR -> _isPlaying.value = false
                    else -> Unit
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    startProgressTracking()
                    reportTraktScrobble("start")
                } else {
                    stopProgressTracking()
                    reportTraktScrobble("pause")
                }
            }

            override fun onError(message: String?) {
                _errorMessage.tryEmit(message)
                _playbackState.value = PlaybackState.ERROR
            }
        })
    }

    fun preparePlayback(mediaId: String, episodeId: String?, startPositionMs: Long) {
        this.mediaId = mediaId
        this.episodeId = episodeId

        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                _playbackState.value = PlaybackState.BUFFERING
                val result = getPlaybackUrl(mediaId, episodeId)

                when (result) {
                    is Result.Success<PlaybackUrl> -> {
                        val playbackUrl = result.data
                        withContext(Dispatchers.Main) {
                            engine?.setMedia(playbackUrl.url, playbackUrl.title, startPositionMs)
                            _uiState.update { it.copy(currentTitle = playbackUrl.title) }
                        }
                        // Auto-download subtitles
                        downloadSubtitles(playbackUrl.title)
                    }
                    is Result.Error -> {
                        _errorMessage.emit(result.exception?.message ?: "Playback error")
                        _playbackState.value = PlaybackState.ERROR
                    }
                    is Result.Loading -> {
                        _playbackState.value = PlaybackState.BUFFERING
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Prepare playback failed")
                _errorMessage.emit(e.message)
                _playbackState.value = PlaybackState.ERROR
            }
        }
    }

    private fun downloadSubtitles(title: String) {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            try {
                val file = subtitleDownloadManager.searchAndDownloadBest(title)
                if (file != null) {
                    _subtitleFile.value = file
                }
            } catch (e: Exception) {
                Timber.e(e, "Subtitle download failed")
            }
        }
    }

    fun play() {
        engine?.play()
        reportTraktScrobble("start")
    }

    fun pause() {
        engine?.pause()
        reportTraktScrobble("pause")
    }

    fun seekTo(positionMs: Long) {
        engine?.seekTo(positionMs)
    }

    fun seekForward() {
        engine?.seekForward()
    }

    fun seekBack() {
        engine?.seekBack()
    }

    fun setPlaybackSpeed(speed: Float) {
        engine?.setPlaybackSpeed(speed)
    }

    fun saveProgress() {
        val currentMedia = mediaId ?: return
        val currentEpisode = episodeId
        val position = engine?.currentPosition?.value ?: 0L
        val total = engine?.duration?.value ?: 0L

        runBlocking(Dispatchers.IO) {
            try {
                watchProgressRepository.saveProgress(currentMedia, currentEpisode, position, total)
            } catch (e: Exception) {
                Timber.e(e, "Save progress failed")
            }
        }
    }

    fun saveState() {
        val position = engine?.currentPosition?.value ?: 0L
        // In a real app, save to SavedStateHandle or persistent storage
    }

    fun releasePlayer() {
        stopProgressTracking()
        saveProgress()
        reportTraktScrobble("stop")
        engine?.release()
        engine = null
        _currentEngine.value = null
    }

    override fun onCleared() {
        super.onCleared()
        saveProgress()
        reportTraktScrobble("stop")
        engine?.release()
        progressUpdateJob?.cancel()
    }

    private fun startProgressTracking() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            while (isActive) {
                delay(10000)
                val position = engine?.currentPosition?.value ?: 0L
                val total = engine?.duration?.value ?: 0L
                if (total > 0) {
                    val progress = (position.toFloat() / total * 100).roundToInt()
                    if (progress != lastReportedProgress) {
                        lastReportedProgress = progress.toFloat()
                        reportTraktScrobbleProgress(progress)
                    }
                }
            }
        }
    }

    private fun stopProgressTracking() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    private fun reportTraktScrobble(action: String) {
        val mediaId = this.mediaId ?: return
        val position = engine?.currentPosition?.value ?: 0L
        val duration = engine?.duration?.value ?: 0L
        val progress = if (duration > 0) (engine?.currentPosition?.value?.toFloat() ?: 0f) / duration * 100 else 0f
        
        // This would integrate with TraktSyncManager
        // traktSyncManager.onPlaybackStarted/Stopped/Paused etc.
    }

    private fun reportTraktScrobbleProgress(progress: Int) {
        // Report progress to Trakt every 10%
    }

    private fun onPlaybackCompleted() {
        // Mark episode as watched on Trakt
    }
}
