// This file is part of KuroStream.
//
// ThemeModeManager — central authority for theme mode.
// Modes: Light, Dark, AMOLED Black, OLED Cinema
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED_BLACK("AMOLED Black"),
    OLED_CINEMA("OLED Cinema"),
}

class ThemeModeManager(private val context: Context) {

    val currentMode: Flow<ThemeMode> = flowOf(loadMode())

    suspend fun setMode(mode: ThemeMode) {
        context.getSharedPreferences("kurostream_theme", Context.MODE_PRIVATE).edit()
            .putString("theme_mode", mode.name)
            .apply()
    }

    fun loadMode(): ThemeMode {
        val prefs = context.getSharedPreferences("kurostream_theme", Context.MODE_PRIVATE)
        val name = prefs.getString("theme_mode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        return runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.DARK)
    }

    @Composable
    fun rememberThemeMode(): ThemeMode {
        val mode = remember { mutableStateOf(loadMode()) }
        LaunchedEffect(Unit) {
            currentMode.collect { mode.value = it }
        }
        return mode.value
    }
}
