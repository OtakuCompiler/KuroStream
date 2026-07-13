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
class CinematicModeManager {

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
        hideJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
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
}

@Composable
fun rememberCinematicMode(): CinematicModeState {
    val manager = remember { CinematicModeManager() }
    val isCinematic by manager.isCinematicMode.collectAsStateWithLifecycle()
    val opacity by manager.uiOpacity.collectAsStateWithLifecycle()
    
    return CinematicModeState(
        isCinematicMode = isCinematic,
        uiOpacity = opacity,
        onUserInteraction = { manager.onUserInteraction() },
        enable = { manager.enableCinematicMode() },
        disable = { manager.disableCinematicMode() },
    )
}

@Stable
data class CinematicModeState(
    val isCinematicMode: Boolean,
    val uiOpacity: Float,
    val onUserInteraction: () -> Unit,
    val enable: () -> Unit,
    val disable: () -> Unit,
)

@Composable
fun CinematicModeWrapper(
    content: @Composable () -> Unit,
    cinematicState: CinematicModeState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        content()
        
        // Touch/key listener to detect user interaction
        androidx.compose.ui.input.pointer.pointerInput(Unit) {
            androidx.compose.ui.input.pointer.detectTapGestures(
                onTap = { cinematicState.onUserInteraction() },
                onDoubleTap = { cinematicState.onUserInteraction() },
                onLongPress = { cinematicState.onUserInteraction() },
            )
        }
        
        // Key event listener for remote
        androidx.compose.ui.input.key.onKeyEvent { event ->
            if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                cinematicState.onUserInteraction()
            }
            false
        }
    }
}

@Composable
fun AnimatedOpacityWrapper(
    opacity: Float,
    content: @Composable () -> Unit,
) {
    val animatedOpacity by animateFloatAsState(
        targetValue = opacity,
        animationSpec = tween(300)
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = animatedOpacity }
    ) {
        content()
    }
}