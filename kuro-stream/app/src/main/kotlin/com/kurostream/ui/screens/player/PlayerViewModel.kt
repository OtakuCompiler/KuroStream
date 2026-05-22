package com.kurostream.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.core.player.PlayerController
import com.kurostream.core.player.PlayerState
import com.kurostream.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val playerController: PlayerController,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = playerController.playerState

    private var progressJob: Job? = null
    private var currentContentId: String? = null
    private var currentTitle: String? = null

    fun initPlayer(streamUrl: String, contentId: String, title: String) {
        currentContentId = contentId
        currentTitle = title
        playerController.loadAndPlay(streamUrl)
        startProgressTracking()
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                delay(5_000L)
                val contentId = currentContentId ?: continue
                val title = currentTitle ?: continue
                val position = playerController.getCurrentPosition()
                if (position > 0) {
                    historyRepository.recordWatch(contentId, title, null)
                    historyRepository.updateProgress(contentId, position)
                }
            }
        }
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun seekForward() = playerController.seekForward()
    fun seekBackward() = playerController.seekBackward()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    override fun onCleared() {
        progressJob?.cancel()
        playerController.stop()
        super.onCleared()
    }
}
