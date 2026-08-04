package com.kurostream.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
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
    var showControls by remember { mutableStateOf(true) }
    var hideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val controlsOpacity by animateFloatAsState(
        targetValue = if (showControls) 1f else 0f,
        animationSpec = tween(200)
    )

    Box(modifier = modifier.fillMaxSize()) {
        content()

        if (showControls) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(controlsOpacity)
                    .pointerInput(isActive) {
                        if (isActive) {
                            while (true) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    // Wait for any pointer event
                                    awaitPointerEventScope {
                                        awaitPointerEvent()
                                    }
                                    showControls = true
                                }
                            }
                        }
                    }
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp)
                ) {
                    androidx.compose.material3.Text("Cinematic Mode - Tap to exit")
                }
            }
        }
    }

    LaunchedEffect(isActive, showControls) {
        hideJob?.cancel()
        if (isActive && showControls) {
            hideJob = launch {
                delay(autoHideDelayMs)
                showControls = false
            }
        }
    }
}
