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

package com.kurostream.launcher.state

import androidx.lifecycle.SavedStateHandle
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerStateSaver @Inject constructor(
    private val stateRestorationManager: StateRestorationManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var saveJob: Job? = null

    /**
     * Start periodic state saving while player is active
     */
    fun startSaving(player: ExoPlayer, savedStateHandle: SavedStateHandle) {
        saveJob?.cancel()
        saveJob = scope.launch {
            while (isActive) {
                saveCurrentState(player, savedStateHandle)
                delay(5000) // Save every 5 seconds
            }
        }
    }

    /**
     * Stop periodic saving
     */
    fun stopSaving() {
        saveJob?.cancel()
        saveJob = null
    }

    /**
     * Save current player state immediately
     */
    fun saveCurrentState(player: ExoPlayer, savedStateHandle: SavedStateHandle) {
        val currentMediaItem = player.currentMediaItem ?: return
        val mediaId = currentMediaItem.mediaId
        if (mediaId.isBlank()) return

        val state = PlayerState(
            mediaId = mediaId,
            title = currentMediaItem.mediaMetadata.title?.toString() ?: "",
            currentPositionMs = player.currentPosition,
            durationMs = player.duration.coerceAtLeast(0),
            isPlaying = player.isPlaying,
            playbackSpeed = player.playbackParameters.speed,
            audioTrackIndex = player.currentTrackGroups.length, // Simplified
            subtitleTrackIndex = -1,
            volume = player.volume,
            isMuted = player.volume == 0f
        )

        stateRestorationManager.savePlayerState(savedStateHandle, state)
    }

    /**
     * Restore player state after recreation
     */
    fun restoreState(savedStateHandle: SavedStateHandle): PlayerState? {
        return stateRestorationManager.restorePlayerState(savedStateHandle)
    }

    /**
     * Check if there's a saved player state to restore
     */
    fun hasSavedState(savedStateHandle: SavedStateHandle): Boolean {
        return stateRestorationManager.restorePlayerState(savedStateHandle) != null
    }

    /**
     * Clear saved player state after successful restoration
     */
    fun clearSavedState(savedStateHandle: SavedStateHandle) {
        savedStateHandle.remove<String>(StateRestorationManager.KEY_PLAYER_STATE)
    }
}
