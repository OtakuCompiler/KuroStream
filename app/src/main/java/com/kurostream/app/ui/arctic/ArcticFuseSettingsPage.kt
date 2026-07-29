// This file is part of KuroStream.
//
// ArcticFuseSettingsPage — full-screen settings overlay matching Arctic Fuse
// SettingsPage.jsx: grouped settings cards (Appearance, Hub, Widgets,
// Playback, System) with toggle/select/button rows and system info footer.
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

data class ArcticSettingsState(
    val theme: String = "Dark",
    val density: String = "Normal",
    val blurEffects: Boolean = true,
    val defaultHub: String = "Home",
    val maxRows: String = "5",
    val autoScrollHero: Boolean = true,
    val defaultQuality: String = "Auto",
    val hdrPassthrough: Boolean = true,
    val autoPlayNext: Boolean = true,
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
            // Header
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

            // Appearance
            SettingsGroup(title = "Appearance") {
                SettingsSelectRow("Theme", state.theme, listOf("Dark", "OLED Black", "Midnight Blue")) { onSelect("theme", it) }
                SettingsSelectRow("Layout Density", state.density, listOf("Compact", "Normal", "Comfortable")) { onSelect("density", it) }
                SettingsToggleRow("Background Effects", state.blurEffects) { onToggle("blurEffects") }
            }

            Spacer(Modifier.height(AFSpacing.px4))

            // Hub Configuration
            SettingsGroup(title = "Hub Configuration") {
                SettingsSelectRow("Default Hub", state.defaultHub, listOf("Home", "Movies", "TV Shows")) { onSelect("default", it) }
            }

            Spacer(Modifier.height(AFSpacing.px4))

            // Widget Settings
            SettingsGroup(title = "Widget Settings") {
                SettingsSelectRow("Max Rows", state.maxRows, listOf("3", "4", "5", "6")) { onSelect("rows", it) }
                SettingsToggleRow("Auto-scroll Hero", state.autoScrollHero) { onToggle("autoScrollHero") }
            }

            Spacer(Modifier.height(AFSpacing.px4))

            // Playback
            SettingsGroup(title = "Playback") {
                SettingsSelectRow("Default Quality", state.defaultQuality, listOf("Auto", "4K", "1080p", "720p")) { onSelect("quality", it) }
                SettingsToggleRow("HDR Passthrough", state.hdrPassthrough) { onToggle("hdr") }
                SettingsToggleRow("Auto-play Next", state.autoPlayNext) { onToggle("next-ep") }
            }

            Spacer(Modifier.height(AFSpacing.px4))

            // System
            SettingsGroup(title = "System") {
                SettingsActionRow("Clear Cache", isDestructive = false, onClick = onClearCache)
                SettingsActionRow("Reset to Defaults", isDestructive = true, onClick = onResetDefaults)
            }

            Spacer(Modifier.height(AFSpacing.px4))

            // System Info
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

data class ArcticSystemInfo(
    val version: String = "1.0.0",
    val device: String = "Android TV",
    val storage: String = "12.4 GB free",
    val memory: String = "3.2 GB available",
    val uptime: String = "2h 34m",
)

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AFSurface, RoundedCornerShape(AFRadius.lg))
            .padding(AFSpacing.px5),
    ) {
        Text(
            text = title,
            color = AFText,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(AFSpacing.px3))
        content()
    }
}

@Composable
private fun SettingsToggleRow(label: String, value: Boolean, onToggle: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) { onToggle(); true } else false
            }
            .clickable(onClick = onToggle)
            .padding(vertical = AFSpacing.px2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = AFTextSec,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )
        ToggleSwitch(isOn = value, isFocused = isFocused)
    }
}

@Composable
private fun ToggleSwitch(isOn: Boolean, isFocused: Boolean) {
    val trackColor = if (isOn) AFCyan else AFBgDeep
    val thumbColor = Color.White
    val thumbOffset = if (isOn) 20.dp else 2.dp

    Box(
        modifier = Modifier
            .width(40.dp)
            .height(20.dp)
            .background(trackColor, RoundedCornerShape(10.dp))
            .border(width = if (isFocused) 1.dp else 0.dp, color = AFCyan, shape = RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .width(16.dp)
                .height(16.dp)
                .background(thumbColor, RoundedCornerShape(8.dp)),
        )
    }
}

@Composable
private fun SettingsSelectRow(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
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
        Text(
            text = label,
            color = AFTextSec,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AFSpacing.px2)) {
            options.forEach { opt ->
                val isSelected = opt == value
                Box(
                    modifier = Modifier
                        .background(if (isSelected) AFCyan else AFBgDeep, RoundedCornerShape(AFRadius.md))
                        .border(
                            width = if (isFocused && opt == value) 1.dp else 0.dp,
                            color = if (isFocused && opt == value) AFCyan else Color.Transparent,
                            shape = RoundedCornerShape(AFRadius.md),
                        )
                        .clickable { onSelect(opt) }
                        .padding(horizontal = AFSpacing.px3, vertical = AFSpacing.px1),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = opt,
                        color = if (isSelected) AFBgDeep else AFTextSec,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsActionRow(label: String, isDestructive: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) { onClick(); true } else false
            }
            .clickable(onClick = onClick)
            .background(AFBgDeep, RoundedCornerShape(AFRadius.md))
            .border(
                width = if (isFocused) 1.dp else 0.dp,
                color = if (isDestructive) AFDanger else AFCyan,
                shape = RoundedCornerShape(AFRadius.md),
            )
            .padding(horizontal = AFSpacing.px3, vertical = AFSpacing.px2),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isDestructive) AFDanger else AFTextSec,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SystemInfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = AFTextDim,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        )
        Text(
            text = value,
            color = AFText,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SettingsCloseButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .background(AFSurface, RoundedCornerShape(AFRadius.lg))
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) { onClick(); true } else false
            }
            .clickable(onClick = onClick)
            .border(width = if (isFocused) 1.dp else 0.dp, color = AFCyan, shape = RoundedCornerShape(AFRadius.lg))
            .padding(horizontal = AFSpacing.px4, vertical = AFSpacing.px2),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Close",
            color = AFTextSec,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )
    }
}