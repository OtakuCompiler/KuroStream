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

package com.kurostream.app.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.kurostream.app.ui.theme.TvBackground
import com.kurostream.app.ui.theme.TvSurface
import com.kurostream.app.ui.theme.TvPrimary

private val DeepPurple = TvBackground
private val Purple = TvSurface
private val Accent = TvPrimary
private val Gold = Color(0xFFFFD700)

@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation states
    var startAnimation by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "alpha"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2000)
        onTimeout()
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepPurple, Purple, DeepPurple)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated glow effect behind logo
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(scale * 1.2f)
                .alpha(glowAlpha * 0.3f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Accent.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
        ) {
            // Logo text with gradient
            Text(
                text = "KURO",
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFE0E0E0),
                style = MaterialTheme.typography.displayLarge.copy(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFE0E0E0), Gold, Accent)
                    )
                )
            )
            
            Text(
                text = "STREAM",
                fontSize = 36.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFFE0E0E0).copy(alpha = 0.9f),
                letterSpacing = 16.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Loading indicator
            LoadingDots(alpha = alpha)
        }
        
        // Version text at bottom
        Text(
            text = "v1.0.0",
            fontSize = 12.sp,
            color = Color(0xFFE0E0E0).copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

@Composable
private fun LoadingDots(alpha: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.alpha(alpha)
    ) {
        repeat(3) { index ->
            val delay = index * 200
            val dotAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = delay),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(dotAlpha)
                    .background(Accent, shape = androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}