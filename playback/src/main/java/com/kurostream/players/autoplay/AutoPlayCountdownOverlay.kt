package com.kurostream.players.autoplay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AutoPlayCountdownOverlay(
    nextEpisodeTitle: String,
    nextEpisodeThumbnail: String?,
    isVisible: Boolean = true,
    countdownSeconds: Int = 10,
    onAutoPlayCancelled: () -> Unit = {},
    onAutoPlayTriggered: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return

    var timeRemaining by remember { mutableStateOf(countdownSeconds) }

    LaunchedEffect(isVisible) {
        timeRemaining = countdownSeconds
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
        onAutoPlayTriggered()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xCC000000)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                Text(
                    text = "Up Next",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )

                Text(
                    text = nextEpisodeTitle,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 2,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onAutoPlayCancelled,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Cancel", color = Color.White, fontSize = 14.sp)
                    }

                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            progress = timeRemaining.toFloat() / countdownSeconds,
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            strokeWidth = 4.dp,
                        )
                        Text(timeRemaining.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = onAutoPlayTriggered,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Play Now", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
