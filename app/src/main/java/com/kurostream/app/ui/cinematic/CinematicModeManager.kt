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

package com.kurostream.app.ui.cinematic

import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cinematic Mode Manager - handles auto-hide UI after inactivity
 */
@Stable
class CinematicModeManager private constructor() {

    private val _isCinematicMode = MutableStateFlow(false)
    val isCinematicMode = _isCinematicMode.asStateFlow()

    private val _uiOpacity = MutableStateFlow(1f)
    val uiOpacity = _uiOpacity.asStateFlow()

    private var hideJob: kotlinx.coroutines.Job? = null
    private val timeoutMs = 5000L

    fun onUserInteraction() {
        if (_isCinematicMode.value) {
            exitCinematicMode()
        }
        resetHideTimer()
    }

    fun enableCinematicMode() {
        _isCinematicMode.value = true
        startHideTimer()
    }

    fun disableCinematicMode() {
        _isCinematicMode.value = false
        _uiOpacity.value = 1f
        hideJob?.cancel()
    }

    private fun exitCinematicMode() {
        _isCinematicMode.value = false
        animateUiOpacity(1f)
        resetHideTimer()
    }

    private fun startHideTimer() {
        hideJob?.cancel()
        // Note: This class must be used within a coroutine scope
        // The actual timer is managed by the calling scope
    }

    fun startTimer(scope: kotlinx.coroutines.CoroutineScope) {
        hideJob?.cancel()
        hideJob = scope.launch(kotlinx.coroutines.Dispatchers.Main) {
            kotlinx.coroutines.delay(timeoutMs)
            animateUiOpacity(0f)
        }
    }

    private fun resetHideTimer() {
        if (_isCinematicMode.value) {
            animateUiOpacity(1f)
            startHideTimer()
        }
    }

    private fun animateUiOpacity(target: Float) {
        // Animation handled in Compose
        _uiOpacity.value = target
    }

    fun cancel() {
        hideJob?.cancel()
    }
}

@Composable
fun rememberCinematicMode(): CinematicModeState {
    val manager = remember { CinematicModeManager() }
    val scope = rememberCoroutineScope()
    
    androidx.compose.runtime.DisposableEffect(manager) {
        onDispose { manager.cancel() }
    }
    
    val isCinematicMode by manager.isCinematicMode.collectAsStateWithLifecycle()
    val uiOpacity by manager.uiOpacity.collectAsStateWithLifecycle()
    
    return CinematicModeState(
        isCinematicMode = isCinematicMode,
        uiOpacity = uiOpacity,
        onUserInteraction = { manager.onUserInteraction() },
        enableCinematicMode = { manager.enableCinematicMode(); manager.startTimer(scope) },
        disableCinematicMode = { manager.disableCinematicMode() },
    )
}

data class CinematicModeState(
    val isCinematicMode: Boolean,
    val uiOpacity: Float,
    val onUserInteraction: () -> Unit,
    val enableCinematicMode: () -> Unit,
    val disableCinematicMode: () -> Unit,
)