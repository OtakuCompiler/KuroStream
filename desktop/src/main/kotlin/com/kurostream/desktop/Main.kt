package com.kurostream.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.kurostream.desktop.ui.AppRoot

/**
 * Entry point for KuroStream desktop application.
 * Supports Windows (.exe / .msi), macOS (.dmg), Linux (.deb / .AppImage).
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KuroStream",
        state = rememberWindowState(size = DpSize(1280.dp, 720.dp))
    ) {
        AppRoot()
    }
}
