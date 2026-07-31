// This file is part of KuroStream.
//
// ArcticFuseSettingsPage — enhanced with Phase 6-9 additions:
//   - Theme Mode (Light / Dark / AMOLED Black / OLED Cinema)
//   - Glass Cards toggle
//   - Blue Glow (Low / Medium / High)
//   - Animation (Reduced / Normal / Cinema)
//   - Accessibility (High contrast / Reduce motion / Color blindness)
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kurostream.app.ui.theme.AdaptiveProfile
import com.kurostream.app.ui.theme.BlueGlowIntensity
import com.kurostream.app.ui.theme.ThemeMode

data class ArcticSettingsState(
    val theme: ThemeMode = ThemeMode.DARK,
    val density: String = "Normal",
    val blurEffects: Boolean = true,
    val defaultHub: String = "Home",
    val maxRows: String = "5",
    val autoScrollHero: Boolean = true,
    val defaultQuality: String = "Auto",
    val hdrPassthrough: Boolean = true,
    val autoPlayNext: Boolean = true,
    val glassCards: Boolean = true,
    val blueGlow: BlueGlowIntensity = BlueGlowIntensity.MEDIUM,
    val animation: String = "Normal",
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val colorBlindSafe: Boolean = false,
)

@Composable
fun ArcticFuseSettingsPage(
    visible: Boolean,
    state: ArcticSettingsState,
    onToggle: (String) -> Unit,
    onSelect: (String, String) -> Unit,
    onClearCache: () -> Unit,
    onResetDefaults: () -> Unit,
    onClose: () -> Unit,
    systemInfo: ArcticSystemInfo = ArcticSystemInfo(),
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(AFMotion.pageEnter)),
        exit = fadeOut(animationSpec = tween(AFMotion.pageEnter)),
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AFBgDeep)
                .verticalScroll(rememberScrollState())
                .padding(AFSpacing.px8),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "SYSTEM SETTINGS",
                    color = AFText,
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp),
                    ),
                )
                SettingsCloseButton(onClick = onClose)
            }

            Spacer(Modifier.height(AFSpacing.px8))

            SettingsGroup(title = "Appearance") {
                SettingsThemeRow(
                    selected = state.theme,
                    onSelect = { onSelect("theme", it) },
                )
                SettingsSelectRow("Layout Density", state.density, listOf("Compact", "Normal", "Comfortable")) { onSelect("density", it) }
                SettingsToggleRow("Background Effects", state.blurEffects) { onToggle("blurEffects") }
                SettingsToggleRow("Glass Cards", state.glassCards) { onToggle("glassCards") }
            }

            Spacer(Modifier.height(AFSpacing.px4))

            SettingsGroup(title = "Effects") {
                SettingsSelectRow(
                    "Blue Glow",
                    state.blueGlow.name,
                    listOf("LOW", "MEDIUM", "HIGH"),
                ) { onSelect("blueGlow", it) }
                SettingsSelectRow("Animation", state.animation, listOf("Reduced", "Normal", "Cinema")) { onSelect("animation", it) }
            }

            Spacer(Modifier.height(AFSpacing.px4))

            SettingsGroup(title = "Accessibility") {
                SettingsToggleRow("High Contrast", state.highContrast) { onToggle("highContrast") }
                SettingsToggleRow("Reduce Motion", state.reduceMotion) { onToggle("reduceMotion") }
                SettingsToggleRow("Color Blind Safe", state.colorBlindSafe) { onToggle("colorBlindSafe") }
            }

            Spacer(Modifier.height(AFSpacing.px4))

            SettingsGroup(title = "Hub Configuration") {
                SettingsSelectRow("Default Hub", state.defaultHub, listOf("Home", "Movies", "TV Shows")) { onSelect("default", it) }
            }

            Spacer(Modifier.height(AFSpacing.px4))

            SettingsGroup(title = "Playback") {
                SettingsSelectRow("Default Quality", state.defaultQuality, listOf("Auto", "4K", "1080p", "720p")) { onSelect("quality", it) }
                SettingsToggleRow("HDR Passthrough", state.hdrPassthrough) { onToggle("hdr") }
                SettingsToggleRow("Auto-play Next", state.autoPlayNext) { onToggle("next-ep") }
            }

            Spacer(Modifier.height(AFSpacing.px4))

            SettingsGroup(title = "System") {
                SettingsActionRow("Clear Cache", isDestructive = false, onClick = onClearCache)
                SettingsActionRow("Reset to Defaults", isDestructive = true, onClick = onResetDefaults)
            }

            Spacer(Modifier.height(AFSpacing.px4))

            SettingsGroup(title = "System Information") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AFSpacing.px8),
                ) {
                    SystemInfoItem("Version", systemInfo.version)
                    SystemInfoItem("Device", systemInfo.device)
                    SystemInfoItem("Storage", systemInfo.storage)
                    SystemInfoItem("Memory", systemInfo.memory)
                    SystemInfoItem("Uptime", systemInfo.uptime)
                }
            }

            Spacer(Modifier.height(AFSpacing.px16))
        }
    }
}

@Composable
private fun SettingsThemeRow(selected: ThemeMode, onSelect: (String) -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(vertical = AFSpacing.px2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "Theme", color = AFTextSec, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(AFSpacing.px2)) {
            ThemeMode.values().forEach { mode ->
                val isSelected = mode == selected
                Box(
                    modifier = Modifier
                        .background(if (isSelected) AFCyan else AFBgDeep, RoundedCornerShape(AFRadius.md))
                        .border(
                            width = if (isFocused && isSelected) 1.dp else 0.dp,
                            color = if (isFocused && isSelected) AFCyan else Color.Transparent,
                            shape = RoundedCornerShape(AFRadius.md),
                        )
                        .clickable { onSelect(mode.name) }
                        .padding(horizontal = AFSpacing.px3, vertical = AFSpacing.px1),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = mode.displayName,
                        color = if (isSelected) AFBgDeep else AFTextSec,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
