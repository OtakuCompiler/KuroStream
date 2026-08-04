// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Theme modes available to the user.
 *
 * LIGHT       — Bright background, dark text. Good for daytime use.
 * DARK        — Mid-grey background. Standard dark mode.
 * AUTO        — Follows the system dark-mode flag automatically.
 * AMOLED_BLACK— True #000000 backgrounds. Best for OLED panels.
 * OLED_CINEMA — AMOLED Black + Fake-HDR color processing on the player.
 * CUSTOM      — User-defined accent/background palette (see [CustomThemeEngine]).
 */
enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    AUTO("Auto (System)"),
    AMOLED_BLACK("AMOLED Black"),
    OLED_CINEMA("OLED Cinema"),
    CUSTOM("Custom"),
}

/**
 * Central authority for the active theme mode.
 *
 * Exposes a [StateFlow] so any composable can collect updates reactively.
 * Persists the selection to SharedPreferences immediately on [setMode].
 *
 * Thread-safe: [setMode] can be called from any coroutine context.
 */
class ThemeModeManager(private val context: Context) {

    private val prefs get() = context.getSharedPreferences("kurostream_theme", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(loadMode())
    val currentMode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _mode.value = mode
    }

    fun loadMode(): ThemeMode {
        val name = prefs.getString(KEY_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        return runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.DARK)
    }

    /** Use inside a [Composable] to get the current theme mode reactively. */
    @Composable
    fun rememberThemeMode(): ThemeMode {
        val mode by currentMode.collectAsState()
        return mode
    }

    companion object {
        private const val KEY_MODE = "theme_mode"
    }
}
