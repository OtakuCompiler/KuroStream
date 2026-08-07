/*
 * KuroStream Desktop — native entry point.
 *
 * Builds a single binary per OS via Compose Multiplatform + jpackage:
 *   - Linux:   .deb / .rpm / .AppImage
 *   - Windows: .exe / .msi
 *   - macOS:   .dmg / .pkg
 *
 * The actual UI logic is shared with the Android app via the common `:domain`
 * module. Android-specific UI (Compose-for-TV, leanback launcher, etc.) lives
 * in `:app` and is *not* included here — desktop uses a separate Compose
 * renderer that runs on the JVM.
 */
package com.kurostream.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.kurostream.desktop.playback.DesktopPlayerFactory
import com.kurostream.desktop.ui.DesktopAppShell
import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.Dimension
import java.io.File

// KuroStream brand palette (matches Android Arctic Fuse 3 theme)
private object Palette {
    val Background = Color(0xFF121212)
    val Surface = Color(0xFF1E1E2E)
    val SurfaceVariant = Color(0xFF2A2A3E)
    val Accent = Color(0xFFE94560)
    val AccentLight = Color(0xFFFF6B81)
    val Cyan = Color(0xFF00E5FF)
    val Text = Color(0xFFFFFFFF)
    val TextDim = Color(0xFFB0B0C0)
}

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(1280.dp, 720.dp),
        position = androidx.compose.ui.window.WindowPosition.PlatformDefault
    )

    // Application-wide state holder (replaces Android Hilt singleton).
    val appState = remember { DesktopAppState.create() }

    Window(
        onCloseRequest = { appState.shutdown(); exitApplication() },
        state = windowState,
        title = "KuroStream",
        icon = loadAppIcon(),
    ) {
        // Set initial window size (in pixels since AWT is what sizes the window on Linux/Win)
        LaunchedEffect(Unit) {
            window.minimumSize = Dimension(960, 540)
        }

        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Palette.Accent,
                onPrimary = Palette.Text,
                secondary = Palette.Cyan,
                onSecondary = Palette.Text,
                background = Palette.Background,
                onBackground = Palette.Text,
                surface = Palette.Surface,
                onSurface = Palette.Text,
                surfaceVariant = Palette.SurfaceVariant,
                onSurfaceVariant = Palette.TextDim,
                error = Color(0xFFFF5252),
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                DesktopAppShell(
                    state = appState,
                    playerFactory = DesktopPlayerFactory(),
                    onExit = { appState.shutdown(); exitApplication() },
                )
            }
        }
    }
}

private fun loadAppIcon(): androidx.compose.ui.graphics.painter.Painter? {
    // Try loading the bundled PNG; fall back to null so the OS uses its default.
    return try {
        val resource = object {}.javaClass.getResourceAsStream("/desktop/icon.png")
        resource?.use { stream ->
            val bytes = stream.readAllBytes()
            val tmp = File.createTempFile("kurostream-icon", ".png")
            tmp.writeBytes(bytes)
            tmp.deleteOnExit()
            androidx.compose.ui.graphics.painter.BitmapPainter(
                androidx.compose.ui.graphics.asImageBitmap(
                    javax.imageio.ImageIO.read(tmp)
                )
            )
        }
    } catch (_: Throwable) {
        null
    }
}
