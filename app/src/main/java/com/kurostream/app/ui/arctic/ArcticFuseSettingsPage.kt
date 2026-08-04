// SPDX-License-Identifier: GPL-3.0-only
//
// ArcticFuseSettingsPage — comprehensive settings UI for KuroStream.
//
// Sections:
//   1. Appearance  — theme mode (with custom theme entry), glass cards, blur
//   2. Effects     — blue glow, animation speed
//   3. Playback    — quality, HDR, auto-play, player engine
//   4. Video       — upscaling profile, color profile, fake HDR, OLED mode
//   5. Audio       — EQ preset, Dolby Atmos emulation, night mode, dialogue boost
//   6. Accessibility — high contrast, reduce motion, colour-blind safe
//   7. Hub         — default start hub, max rows
//   8. System      — clear cache, reset, system info
//
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
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
import com.kurostream.app.ui.theme.BlueGlowIntensity
import com.kurostream.app.ui.theme.ThemeMode
import com.kurostream.players.render.ColorProfile

data class ArcticSettingsState(
    // Appearance
    val theme:            ThemeMode          = ThemeMode.DARK,
    val density:          String             = "Normal",
    val blurEffects:      Boolean            = true,
    val glassCards:       Boolean            = true,
    val blueGlow:         BlueGlowIntensity  = BlueGlowIntensity.MEDIUM,
    val animation:        String             = "Normal",
    // Playback
    val defaultQuality:   String             = "Auto",
    val hdrPassthrough:   Boolean            = true,
    val autoPlayNext:     Boolean            = true,
    val playerEngine:     String             = "Auto",
    // Video post-processing
    val upscaleProfile:   String             = "BICUBIC",
    val colorProfile:     ColorProfile       = ColorProfile.NATURAL,
    val fakeHdr:          Boolean            = false,
    val fakeHdrIntensity: Float              = 0.65f,
    val oledMode:         Boolean            = false,
    val animeDetailBoost: Boolean            = false,
    // Audio
    val eqPreset:         String             = "FLAT",
    val dolbyAtmos:       Boolean            = false,
    val nightMode:        Boolean            = false,
    val dialogueBoost:    Boolean            = false,
    // Accessibility
    val highContrast:     Boolean            = false,
    val reduceMotion:     Boolean            = false,
    val colorBlindSafe:   Boolean            = false,
    // Hub
    val defaultHub:       String             = "Home",
    val maxRows:          String             = "5",
    val autoScrollHero:   Boolean            = true,
)

data class ArcticSystemInfo(
    val version: String = BuildConfig_VERSION_NAME,
    val device:  String = "${android.os.Build.MODEL}",
    val storage: String = "—",
    val memory:  String = "—",
    val uptime:  String = "—",
)

private const val BuildConfig_VERSION_NAME = "1.0.0"

@Composable
fun ArcticFuseSettingsPage(
    visible:         Boolean,
    state:           ArcticSettingsState,
    onToggle:        (String) -> Unit,
    onSelect:        (String, String) -> Unit,
    onClearCache:    () -> Unit,
    onResetDefaults: () -> Unit,
    onOpenCustomTheme: () -> Unit = {},
    onClose:         () -> Unit,
    systemInfo:      ArcticSystemInfo  = ArcticSystemInfo(),
    modifier:        Modifier          = Modifier,
) {
    val palette = LocalArcticFusePalette.current
    AnimatedVisibility(
        visible  = visible,
        enter    = fadeIn(animationSpec = tween(AFMotion.pageEnter)),
        exit     = fadeOut(animationSpec = tween(AFMotion.pageEnter)),
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AFSpacing.safeZoneH, vertical = AFSpacing.safeZoneV),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Settings", color = palette.text,
                        fontSize = AFTypo.heading, fontWeight = FontWeight.Bold)
                    Text("KuroStream", color = palette.textSec, fontSize = AFTypo.body)
                }
                SettingsCloseButton(onClick = onClose, palette = palette)
            }
            Spacer(Modifier.height(AFSpacing.px8))

            // ── 1. Appearance ─────────────────────────────────────────────────
            SettingsGroup(title = "Appearance", palette = palette) {
                SettingsThemeRow(selected = state.theme, palette = palette, onSelect = { onSelect("theme", it) })
                if (state.theme == ThemeMode.CUSTOM) {
                    SettingsActionRow("Edit Custom Theme…", palette = palette, isDestructive = false, onClick = onOpenCustomTheme)
                }
                SettingsSelectRow("Layout Density", state.density, listOf("Compact", "Normal", "Comfortable"), palette) { onSelect("density", it) }
                SettingsToggleRow("Background Effects",     state.blurEffects,  palette) { onToggle("blurEffects") }
                SettingsToggleRow("Glass Cards",            state.glassCards,   palette) { onToggle("glassCards") }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 2. Effects ────────────────────────────────────────────────────
            SettingsGroup("Effects", palette) {
                SettingsSelectRow("Blue Glow", state.blueGlow.name,
                    listOf("LOW", "MEDIUM", "HIGH"), palette) { onSelect("blueGlow", it) }
                SettingsSelectRow("Animation", state.animation,
                    listOf("Reduced", "Normal", "Cinema"), palette) { onSelect("animation", it) }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 3. Playback ───────────────────────────────────────────────────
            SettingsGroup("Playback", palette) {
                SettingsSelectRow("Default Quality", state.defaultQuality,
                    listOf("Auto", "4K", "1080p", "720p", "480p"), palette) { onSelect("quality", it) }
                SettingsToggleRow("HDR Passthrough",  state.hdrPassthrough, palette) { onToggle("hdr") }
                SettingsToggleRow("Auto-play Next",   state.autoPlayNext,   palette) { onToggle("next-ep") }
                SettingsSelectRow("Player Engine", state.playerEngine,
                    listOf("Auto", "Media3 / ExoPlayer", "libVLC", "libMPV"), palette) { onSelect("playerEngine", it) }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 4. Video Post-Processing ──────────────────────────────────────
            SettingsGroup("Video Post-Processing", palette) {
                SettingsSelectRow("Upscaling Algorithm", state.upscaleProfile,
                    listOf("BILINEAR", "BICUBIC", "LANCZOS3", "ULTRA"), palette) { onSelect("upscale", it) }
                SettingsSelectRow("Colour Profile", state.colorProfile.displayName,
                    ColorProfile.values().map { it.displayName }, palette) { onSelect("colorProfile", it) }
                SettingsToggleRow("Fake HDR (SDR enhancement)", state.fakeHdr,   palette) { onToggle("fakeHdr") }
                SettingsToggleRow("OLED Black Crush",            state.oledMode, palette) { onToggle("oledMode") }
                SettingsToggleRow("Anime Detail Boost",          state.animeDetailBoost, palette) { onToggle("animeBoost") }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 5. Audio ──────────────────────────────────────────────────────
            SettingsGroup("Audio", palette) {
                SettingsSelectRow("EQ Preset", state.eqPreset,
                    listOf("FLAT", "BASS_BOOST", "TREBLE_BOOST", "VOCAL", "CINEMA", "GAMING", "NIGHT", "LOUDNESS"), palette) { onSelect("eqPreset", it) }
                SettingsToggleRow("Dolby Atmos Emulation",  state.dolbyAtmos,     palette) { onToggle("dolbyAtmos") }
                SettingsToggleRow("Night Mode (DRC)",        state.nightMode,      palette) { onToggle("nightMode") }
                SettingsToggleRow("Dialogue Boost",          state.dialogueBoost,  palette) { onToggle("dialogueBoost") }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 6. Accessibility ──────────────────────────────────────────────
            SettingsGroup("Accessibility", palette) {
                SettingsToggleRow("High Contrast",    state.highContrast,  palette) { onToggle("highContrast") }
                SettingsToggleRow("Reduce Motion",    state.reduceMotion,  palette) { onToggle("reduceMotion") }
                SettingsToggleRow("Colour Blind Safe",state.colorBlindSafe,palette) { onToggle("colorBlindSafe") }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 7. Hub ────────────────────────────────────────────────────────
            SettingsGroup("Hub Configuration", palette) {
                SettingsSelectRow("Default Hub", state.defaultHub,
                    listOf("Home", "Movies", "TV Shows", "Anime", "Downloads"), palette) { onSelect("defaultHub", it) }
                SettingsSelectRow("Max Shelf Rows", state.maxRows,
                    listOf("3", "4", "5", "6", "7"), palette) { onSelect("maxRows", it) }
                SettingsToggleRow("Auto-scroll Hero",  state.autoScrollHero, palette) { onToggle("heroScroll") }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 8. System ─────────────────────────────────────────────────────
            SettingsGroup("System", palette) {
                SettingsActionRow("Clear Cache",        palette = palette, isDestructive = false, onClick = onClearCache)
                SettingsActionRow("Reset to Defaults",  palette = palette, isDestructive = true,  onClick = onResetDefaults)
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── System info ───────────────────────────────────────────────────
            SettingsGroup("System Information", palette) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AFSpacing.px8),
                ) {
                    SystemInfoItem("Version",systemInfo.version, palette)
                    SystemInfoItem("Device",  systemInfo.device,  palette)
                    SystemInfoItem("Storage", systemInfo.storage, palette)
                    SystemInfoItem("Memory",  systemInfo.memory,  palette)
                    SystemInfoItem("Uptime",  systemInfo.uptime,  palette)
                }
            }

            Spacer(Modifier.height(AFSpacing.px16))
        }
    }
}

// ── Composable helpers ────────────────────────────────────────────────────────

@Composable
private fun SettingsGroup(
    title:   String,
    palette: ArcticFusePalette,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AFRadius.lg))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(AFRadius.lg))
            .padding(AFSpacing.px4),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text       = title.uppercase(),
            color      = palette.textDim,
            fontSize   = AFTypo.micro,
            fontWeight = FontWeight.Bold,
            letterSpacing = AFTypo.sectionTitleSpacing,
            modifier   = Modifier.padding(bottom = 4.dp),
        )
        content()
    }
}

@Composable
private fun SettingsToggleRow(
    label:   String,
    value:   Boolean,
    palette: ArcticFusePalette,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val fr      = remember { FocusRequester() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AFRadius.sm))
            .background(if (focused) palette.surfaceVariant else Color.Transparent)
            .focusRequester(fr)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyUp &&
                    (ev.key == Key.Enter || ev.key == Key.DirectionCenter)) { onClick(); true }
                else false
            }
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = palette.text, fontSize = AFTypo.body)
        // Toggle pill
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (value) palette.cyan else palette.surfaceVariant),
            contentAlignment = if (value) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (value) palette.bg else palette.textDim),
            )
        }
    }
}

@Composable
private fun SettingsSelectRow(
    label:   String,
    current: String,
    options: List<String>,
    palette: ArcticFusePalette,
    onPick:  (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = palette.textSec, fontSize = AFTypo.body)
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { opt ->
                val selected = opt == current
                val fr2 = remember { FocusRequester() }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AFRadius.sm))
                        .background(if (selected) palette.cyan else palette.surfaceVariant)
                        .border(
                            width = if (focused && selected) 1.dp else 0.dp,
                            color = palette.cyan,
                            shape = RoundedCornerShape(AFRadius.sm),
                        )
                        .focusRequester(fr2)
                        .onFocusChanged { focused = it.isFocused }
                        .focusable()
                        .onKeyEvent { ev ->
                            if (ev.type == KeyEventType.KeyUp && (ev.key == Key.Enter || ev.key == Key.DirectionCenter)) {
                                onPick(opt); true
                            } else false
                        }
                        .clickable { onPick(opt) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = opt,
                        color      = if (selected) palette.bg else palette.textSec,
                        fontSize   = AFTypo.meta,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines   = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    label:        String,
    palette:      ArcticFusePalette,
    isDestructive: Boolean,
    onClick:      () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val fr      = remember { FocusRequester() }
    val color   = if (isDestructive) palette.danger else palette.cyan
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AFRadius.sm))
            .background(if (focused) color.copy(alpha = 0.12f) else Color.Transparent)
            .border(if (focused) 1.dp else 0.dp, color, RoundedCornerShape(AFRadius.sm))
            .focusRequester(fr)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyUp && (ev.key == Key.Enter || ev.key == Key.DirectionCenter)) {
                    onClick(); true
                } else false
            }
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = color, fontSize = AFTypo.body, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingsThemeRow(
    selected: ThemeMode,
    palette:  ArcticFusePalette,
    onSelect: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(fr)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(vertical = AFSpacing.px2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Theme", color = palette.textSec, fontSize = AFTypo.body)
        Row(horizontalArrangement = Arrangement.spacedBy(AFSpacing.px2)) {
            ThemeMode.values().forEach { mode ->
                val isSelected = mode == selected
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) palette.cyan else palette.surfaceVariant,
                            RoundedCornerShape(AFRadius.md),
                        )
                        .border(
                            width = if (focused && isSelected) 1.dp else 0.dp,
                            color = palette.cyan,
                            shape = RoundedCornerShape(AFRadius.md),
                        )
                        .clickable { onSelect(mode.name) }
                        .padding(horizontal = AFSpacing.px3, vertical = AFSpacing.px1),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = mode.displayName,
                        color      = if (isSelected) palette.bg else palette.textSec,
                        fontSize   = AFTypo.meta,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCloseButton(onClick: () -> Unit, palette: ArcticFusePalette) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (focused) palette.surfaceVariant else Color.Transparent)
            .border(if (focused) 1.dp else 0.dp, palette.cyan, CircleShape)
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { IconClose(tint = palette.text, iconSize = 20.dp) }
}

@Composable
private fun SystemInfoItem(label: String, value: String, palette: ArcticFusePalette) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), color = palette.textDim, fontSize = AFTypo.micro, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(value, color = palette.text, fontSize = AFTypo.meta)
    }
}
