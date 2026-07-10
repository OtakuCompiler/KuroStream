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

package com.kurostream.app.ui.components

import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.awaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.ms
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CinematicModeOverlay(
    isActive: Boolean,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    autoHideDelayMs: Long = 5000,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val uiOpacity by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(300)
    )
    val controlsOpacity by animateFloatAsState(
        targetValue = if (showControls) 1f else 0f,
        animationSpec = tween(200)
    )
    var showControls by remember { mutableStateOf(true) }
    var hideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        content()

        // Auto-hide timer
        LaunchedEffect(isActive, showControls) {
            hideJob?.cancel()
            if (isActive && showControls) {
                hideJob = launch {
                    delay(autoHideDelayMs)
                    showControls = false
                }
            }
        }

        // Touch/key listener to show controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                            if (isActive && !showControls) {
                                showControls = true
                            }
                        }
                    }
                }
        ) { }

        // UI Controls
        if (isActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = controlsOpacity }
                    .padding(48.dp)
            ) {
                // Top bar - minimal
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.TopCenter)
                ) {
                    // Episode info, time, etc.
                }

                // Bottom bar - playback controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    // Playback controls
                }
            }
        }
    }
}

@Composable
fun CinematicModeButton(
    isActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = tween(200)
    )
    val color by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.5f,
        animationSpec = tween(200)
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(48.dp)
            .onClick(onToggle)
            .padding(8.dp)
    ) {
        // Cinema icon
    }
}

@Composable
fun AmbientModeOverlay(
    isActive: Boolean,
    dominantColor: androidx.compose.ui.graphics.Color?,
    modifier: Modifier = Modifier,
) {
    val opacity by animateFloatAsState(
        targetValue = if (isActive) 0.15f else 0f,
        animationSpec = tween(500)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = opacity }
            .background(
                dominantColor?.copy(alpha = 0.3f) ?? androidx.compose.ui.graphics.Color.Transparent
            )
    )
}