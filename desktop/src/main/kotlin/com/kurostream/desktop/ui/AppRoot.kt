package com.kurostream.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Root composable that hosts the entire KuroStream desktop UI.
 * Uses the shared Compose theme tokens from :ui module.
 */
@Composable
fun AppRoot() {
    val darkScheme = darkColorScheme(
        background = Color(0xFF121212),
        surface = Color(0xFF1A1A1A),
        primary = Color(0xFFE94560),
        onPrimary = Color.White,
        onBackground = Color(0xFFE5E5E5),
        onSurface = Color(0xFFE5E5E5),
    )
    val lightScheme = lightColorScheme(
        primary = Color(0xFFE94560),
        onPrimary = Color.White,
    )
    val colorScheme = darkScheme

    MaterialTheme(colorScheme = colorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(Modifier.fillMaxSize().padding(0.dp)) {
                ArcticFuseShell()
            }
        }
    }
}
