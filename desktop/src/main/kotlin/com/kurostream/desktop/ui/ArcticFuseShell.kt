package com.kurostream.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Arctic Fuse-style shell for desktop.
 *
 * Mirrors the Android TV layout:
 *   ┌──────┬─────────────────────────────────────┐
 *   │ Side │  Hero + content rows                │
 *   │ bar  │                                     │
 *   │      │                                     │
 *   └──────┴─────────────────────────────────────┘
 *
 * Sidebar uses Arctic Fuse's "vertical line separator" pattern.
 */
@Composable
fun ArcticFuseShell() {
    Row(Modifier.fillMaxSize()) {
        Sidebar(Modifier.fillMaxHeight().width(96.dp))
        Column(Modifier.fillMaxSize()) {
            // Top accent bar (Arctic Fuse's vertical line at the top of content)
            Surface(
                Modifier.fillMaxSize().padding(start = 1.dp),
                color = Color(0xFF1A1A1A)
            ) {
                Column(Modifier.fillMaxSize().padding(24.dp)) {
                    Text(
                        text = "Discover",
                        style = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Featured hero carousel goes here.",
                        style = TextStyle(color = Color(0xFFB0B0B0), fontSize = 14.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Sidebar(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(Color(0xFF0E0E0E), Color(0xFF1A1A1A)))
        ),
        color = Color(0xFF121212)
    ) {
        Column(
            Modifier.fillMaxSize().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            SidebarIcon(Icons.Default.Home, "Home")
            SidebarIcon(Icons.Default.Movie, "Movies")
            SidebarIcon(Icons.Default.Tv, "Series")
            SidebarIcon(Icons.Default.Settings, "Settings")
        }
    }
}

@Composable
private fun SidebarIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFFE94560),
                modifier = Modifier.padding(10.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color(0xFFB0B0B0), fontSize = 10.sp)
    }
}
