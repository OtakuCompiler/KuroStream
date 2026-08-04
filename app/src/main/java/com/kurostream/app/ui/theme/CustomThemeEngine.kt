// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.theme

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * User-editable custom color theme.
 *
 * All colors are stored as ARGB hex strings so they survive serialization.
 * [CustomThemeEngine] persists the active theme to SharedPreferences and
 * exposes it via a [StateFlow] so the UI rebuilds automatically when the
 * user edits any color.
 *
 * Built-in presets are offered as starting points — the user can fork any
 * preset and adjust individual colors.
 */
@Immutable
@Serializable
data class CustomTheme(
    val name: String = "My Theme",
    val accentPrimary:    Long = 0xFF6366F1,  // indigo
    val accentSecondary:  Long = 0xFF8B5CF6,  // violet
    val background:       Long = 0xFF0A0A0F,
    val surface:          Long = 0xFF16161F,
    val surfaceVariant:   Long = 0xFF1A1A2E,
    val textPrimary:      Long = 0xFFFFFFFF,
    val textSecondary:    Long = 0xFF9CA3AF,
    val danger:           Long = 0xFFEF4444,
    val starGold:         Long = 0xFFFBBF24,
    val border:           Long = 0xFF1F1F2E,
) {
    fun primaryColor()    = Color(accentPrimary)
    fun secondaryColor()  = Color(accentSecondary)
    fun bgColor()         = Color(background)
    fun surfaceColor()    = Color(surface)
    fun surfaceVarColor() = Color(surfaceVariant)
    fun textColor()       = Color(textPrimary)
    fun textSecColor()    = Color(textSecondary)
    fun dangerColor()     = Color(danger)
    fun starColor()       = Color(starGold)
    fun borderColor()     = Color(border)

    companion object {
        val ARCTIC_FUSE = CustomTheme(
            name = "Arctic Fuse 3",
            accentPrimary = 0xFF6366F1, accentSecondary = 0xFF8B5CF6,
            background = 0xFF0A0A0F, surface = 0xFF16161F, surfaceVariant = 0xFF1A1A2E,
        )
        val DEEP_SPACE = CustomTheme(
            name = "Deep Space",
            accentPrimary = 0xFF0EA5E9, accentSecondary = 0xFF38BDF8,
            background = 0xFF020817, surface = 0xFF0F172A, surfaceVariant = 0xFF1E293B,
        )
        val CHERRY_NIGHT = CustomTheme(
            name = "Cherry Night",
            accentPrimary = 0xFFE91E8C, accentSecondary = 0xFFFF6B9D,
            background = 0xFF0F0009, surface = 0xFF1A0014, surfaceVariant = 0xFF280020,
        )
        val FOREST = CustomTheme(
            name = "Forest",
            accentPrimary = 0xFF22C55E, accentSecondary = 0xFF86EFAC,
            background = 0xFF021005, surface = 0xFF0A1F0D, surfaceVariant = 0xFF122B16,
        )
        val AMBER_GLOW = CustomTheme(
            name = "Amber Glow",
            accentPrimary = 0xFFF59E0B, accentSecondary = 0xFFFBBF24,
            background = 0xFF0A0800, surface = 0xFF1A1400, surfaceVariant = 0xFF261E00,
        )
        val ARCTIC_DAY = CustomTheme(
            name = "Arctic Day (Light)",
            accentPrimary = 0xFF4F46E5, accentSecondary = 0xFF7C3AED,
            background = 0xFFF8F9FF, surface = 0xFFFFFFFF, surfaceVariant = 0xFFE8EAFF,
            textPrimary = 0xFF111827, textSecondary = 0xFF6B7280,
            border = 0xFFE5E7EB,
        )
        val PRESETS = listOf(ARCTIC_FUSE, DEEP_SPACE, CHERRY_NIGHT, FOREST, AMBER_GLOW, ARCTIC_DAY)
    }
}

class CustomThemeEngine(private val context: Context) {

    private val prefs = context.getSharedPreferences("kurostream_custom_theme", Context.MODE_PRIVATE)
    private val json  = Json { ignoreUnknownKeys = true }

    private val _theme = MutableStateFlow(load())
    val theme: StateFlow<CustomTheme> = _theme.asStateFlow()

    fun save(theme: CustomTheme) {
        prefs.edit().putString(KEY_THEME, json.encodeToString(theme)).apply()
        _theme.value = theme
    }

    fun applyPreset(preset: CustomTheme) = save(preset)

    fun updateAccentPrimary(color: Color) = save(_theme.value.copy(accentPrimary = color.value.toLong()))
    fun updateAccentSecondary(color: Color) = save(_theme.value.copy(accentSecondary = color.value.toLong()))
    fun updateBackground(color: Color) = save(_theme.value.copy(background = color.value.toLong()))
    fun updateSurface(color: Color) = save(_theme.value.copy(surface = color.value.toLong()))
    fun updateName(name: String) = save(_theme.value.copy(name = name))
    fun reset() = save(CustomTheme.ARCTIC_FUSE)

    private fun load(): CustomTheme {
        val str = prefs.getString(KEY_THEME, null) ?: return CustomTheme()
        return runCatching { json.decodeFromString<CustomTheme>(str) }.getOrDefault(CustomTheme())
    }

    companion object {
        private const val KEY_THEME = "custom_theme_json"
    }
}
