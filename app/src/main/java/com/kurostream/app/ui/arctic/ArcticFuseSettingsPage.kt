// SPDX-License-Identifier: GPL-3.0-only
//
// ArcticFuseSettingsPage — DataStore-backed settings UI.
//
// All writes go through ArcticFuseSettingsViewModel → KuroSettingsRepository
// → Preferences DataStore. The old in-memory ArcticSettingsState is only used
// as a projection for the composable row widgets.
//
// Sections:
//   1.  Appearance       — theme, density, glass, blur, tag style
//   2.  Effects          — blue glow, animation
//   3.  Playback         — quality, HDR, auto-play, player engine
//   4.  Video            — upscaling, color profile, CAS, fake-HDR, OLED
//   5.  Audio            — passthrough, delay, night-DRC, dialogue boost
//   6.  Accessibility    — high contrast, reduce motion
//   7.  Hub              — default hub, max rows, hero auto-scroll
//   8.  Subtitles        — providers, size, sync offset
//   9.  Extensions       — auto-update, strict sandbox
//  10.  Network         — DoH, certificate pinning
//  11.  Parental        — kids mode, PIN, rating limit
//  12.  Accounts        — Trakt, AniList, MAL sync
//  13.  System          — clear cache, reset defaults, system info
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kurostream.app.ui.theme.BlueGlowIntensity
import com.kurostream.app.ui.theme.ThemeMode
import com.kurostream.data.settings.KuroSettings
import com.kurostream.playback.kurovision.ColorProfile
import com.kurostream.domain.repository.AppTheme

// ── Projection ───────────────────────────────────────────────────────────────
// Legacy projection kept so all existing Settings*Row composables keep their
// current signatures. Populated from the live KuroSettings snapshot.

data class ArcticSettingsState(
    val theme:            ThemeMode         = ThemeMode.DARK,
    val density:          String            = "Normal",
    val blurEffects:      Boolean           = true,
    val glassCards:       Boolean           = true,
    val blueGlow:         BlueGlowIntensity = BlueGlowIntensity.MEDIUM,
    val animation:        String            = "Normal",
    val defaultQuality:   String            = "Auto",
    val hdrPassthrough:   Boolean           = true,
    val autoPlayNext:     Boolean           = true,
    val playerEngine:     String            = "Auto",
    val upscaleProfile:   String            = "BICUBIC",
    val colorProfile:     ColorProfile       = ColorProfile.NATURAL,
    val fakeHdr:          Boolean           = false,
    val fakeHdrIntensity: Float             = 0.65f,
    val oledMode:         Boolean           = false,
    val animeDetailBoost: Boolean           = false,
    val eqPreset:         String            = "FLAT",
    val dolbyAtmos:       Boolean           = false,
    val nightMode:        Boolean           = false,
    val dialogueBoost:    Boolean           = false,
    val highContrast:     Boolean           = false,
    val reduceMotion:     Boolean           = false,
    val colorBlindSafe:   Boolean           = false,
    val defaultHub:       String            = "Home",
    val maxRows:          String            = "5",
    val autoScrollHero:   Boolean           = true,
)

private fun KuroSettings.toArcticSettingsState(): ArcticSettingsState {
    fun AppTheme.toThemeMode(): ThemeMode = when (this) {
        AppTheme.LIGHT -> ThemeMode.LIGHT
        AppTheme.DARK  -> ThemeMode.DARK
        AppTheme.OLED  -> ThemeMode.AMOLED_BLACK
        AppTheme.SYSTEM -> ThemeMode.AUTO
    }
    return ArcticSettingsState(
        theme            = themeMode.toThemeMode(),
        density          = "Normal",
        blurEffects      = blurEffects,
        glassCards       = glassCards,
        blueGlow         = BlueGlowIntensity.MEDIUM,
        animation        = "Normal",
        defaultQuality   = defaultQuality,
        hdrPassthrough   = true,
        autoPlayNext     = autoPlayNext,
        playerEngine     = defaultEngine,
        upscaleProfile   = upscaleAlgorithm,
        colorProfile     = runCatching { ColorProfile.valueOf(colorProfile) }.getOrDefault(ColorProfile.NATURAL),
        fakeHdr          = fakeHdr,
        fakeHdrIntensity = 0.65f,
        oledMode         = oledMode,
        animeDetailBoost = false,
        eqPreset         = "FLAT",
        dolbyAtmos       = false,
        nightMode        = nightModeDrc,
        dialogueBoost    = dialogueBoost,
        highContrast     = false,
        reduceMotion     = false,
        colorBlindSafe   = false,
        defaultHub       = defaultHub,
        maxRows          = maxRows.toString(),
        autoScrollHero   = heroAutoScroll,
    )
}

// ── Composable ───────────────────────────────────────────────────────────────

@Composable
fun ArcticFuseSettingsPage(
    visible:      Boolean,
    viewModel:    ArcticFuseSettingsViewModel,
    onClose:      () -> Unit,
    systemInfo:   ArcticSystemInfo  = ArcticSystemInfo(),
    modifier:     Modifier          = Modifier,
) {
    val s by viewModel.settings.collectAsStateWithLifecycle(KuroSettings())
    val state = s.toArcticSettingsState()
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
                SettingsThemeRow(
                    selected = state.theme,
                    palette  = palette,
                    onSelect = { name -> viewModel.setThemeMode(AppTheme.valueOf(name)) },
                )
                SettingsSelectRow("Layout Density", state.density,
                    listOf("Compact", "Normal", "Comfortable"), palette) { }
                SettingsToggleRow("Background Effects",  state.blurEffects,  palette) { viewModel.setBlurEffects(!state.blurEffects) }
                SettingsToggleRow("Glass Cards",         state.glassCards,   palette) { viewModel.setGlassCards(!state.glassCards) }
                SettingsToggleRow("OLED Black",          s.oledBlack,        palette) { viewModel.setOledBlack(!s.oledBlack) }
                SettingsSelectRow("Tag Style", s.tagStyle,
                    listOf("BOX", "TEXT"), palette) { viewModel.setTagStyle(it) }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 2. Effects ────────────────────────────────────────────────────
            SettingsGroup("Effects", palette) {
                SettingsSelectRow("Blue Glow", state.blueGlow.name,
                    listOf("LOW", "MEDIUM", "HIGH"), palette) { }
                SettingsSelectRow("Animation", state.animation,
                    listOf("Reduced", "Normal", "Cinema"), palette) { }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 3. Playback ───────────────────────────────────────────────────
            SettingsGroup("Playback", palette) {
                SettingsSelectRow("Default Quality", state.defaultQuality,
                    listOf("Auto", "4K", "1080p", "720p", "480p"), palette) { viewModel.setDefaultQuality(it) }
                SettingsToggleRow("HDR Passthrough",  state.hdrPassthrough, palette) { }
                SettingsToggleRow("Auto-play Next",   state.autoPlayNext,   palette) { viewModel.setAutoPlayNext(!state.autoPlayNext) }
                SettingsSelectRow("Player Engine", state.playerEngine,
                    listOf("Auto", "Media3 / ExoPlayer", "libVLC", "libMPV"), palette) { viewModel.setDefaultEngine(it) }
                SettingsToggleRow("Refresh Rate Switching", s.refreshRateSwitching, palette) { viewModel.setRefreshRateSwitching(!s.refreshRateSwitching) }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 4. Video Post-Processing ──────────────────────────────────────
            SettingsGroup("Video Post-Processing", palette) {
                SettingsSelectRow("Upscaling Algorithm", state.upscaleProfile,
                    listOf("BILINEAR", "BICUBIC", "LANCZOS3", "ULTRA"), palette) { viewModel.setUpscaleAlgorithm(it) }
                SettingsSelectRow("Colour Profile", state.colorProfile.displayName,
                    ColorProfile.values().map { it.displayName }, palette) { viewModel.setColorProfile(it) }
                SettingsToggleRow("Fake HDR (SDR enhancement)", state.fakeHdr,   palette) { viewModel.setFakeHdr(!state.fakeHdr) }
                SettingsToggleRow("OLED Black Crush",            state.oledMode, palette) { viewModel.setOledMode(!state.oledMode) }
                SettingsToggleRow("Contrast Adaptive Sharpening",  s.contrastAdaptiveSharpening, palette) { viewModel.setContrastAdaptiveSharpening(!s.contrastAdaptiveSharpening) }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 5. Audio ──────────────────────────────────────────────────────
            SettingsGroup("Audio", palette) {
                SettingsSelectRow("Passthrough Mode", s.passthroughMode,
                    listOf("AUTO", "ENABLED", "DISABLED"), palette) { viewModel.setPassthroughMode(it) }
                SettingsSelectRow("Audio Delay (ms)", s.audioDelayMs.toString(),
                    listOf("0", "50", "100", "200", "500"), palette) { viewModel.setAudioDelayMs(it.toIntOrNull() ?: 0) }
                SettingsToggleRow("Night Mode (DRC)",      state.nightMode,    palette) { viewModel.setNightModeDrc(!state.nightMode) }
                SettingsToggleRow("Dialogue Boost",        state.dialogueBoost, palette) { viewModel.setDialogueBoost(!state.dialogueBoost) }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 6. Accessibility ──────────────────────────────────────────────
            SettingsGroup("Accessibility", palette) {
                SettingsToggleRow("High Contrast",    state.highContrast,  palette) { }
                SettingsToggleRow("Reduce Motion",    state.reduceMotion,  palette) { }
                SettingsToggleRow("Colour Blind Safe",state.colorBlindSafe,palette) { }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 7. Hub ────────────────────────────────────────────────────────
            SettingsGroup("Hub Configuration", palette) {
                SettingsSelectRow("Default Hub", state.defaultHub,
                    listOf("Home", "Movies", "TV Shows", "Anime", "Downloads"), palette) { viewModel.setDefaultHub(it) }
                SettingsSelectRow("Max Shelf Rows", state.maxRows,
                    listOf("3", "4", "5", "6", "7"), palette) { viewModel.setMaxRows(it.toIntOrNull() ?: 5) }
                SettingsToggleRow("Auto-scroll Hero",  state.autoScrollHero, palette) { viewModel.setHeroAutoScroll(!state.autoScrollHero) }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 8. Subtitles ──────────────────────────────────────────────────
            SettingsGroup("Subtitles", palette = palette) {
                SettingsSelectRow("Providers", s.subtitleProviders.firstOrNull() ?: "opensubtitles",
                    listOf("opensubtitles", "subdl"), palette) { viewModel.setSubtitleProviders(listOf(it)) }
                SettingsSelectRow("Size", "%.1f".format(s.subtitleSize),
                    listOf("0.5", "0.75", "1.0", "1.25", "1.5", "2.0"), palette) { viewModel.setSubtitleSize(it.toFloatOrNull() ?: 1.0f) }
                SettingsSelectRow("Sync Offset (ms)", s.subtitleSyncOffset.toString(),
                    listOf("-5000", "-2000", "-500", "0", "500", "2000", "5000"), palette) { viewModel.setSubtitleSyncOffset(it.toIntOrNull() ?: 0) }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 9. Extensions ─────────────────────────────────────────────────
            SettingsGroup("Extensions", palette) {
                SettingsToggleRow("Auto-update Extensions", s.extensionAutoUpdate, palette) { viewModel.setExtensionAutoUpdate(!s.extensionAutoUpdate) }
                SettingsToggleRow("Strict Sandbox",         s.sandboxStrictMode, palette) { viewModel.setSandboxStrictMode(!s.sandboxStrictMode) }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 10. Network & Privacy ─────────────────────────────────────────
            SettingsGroup("Network", palette) {
                SettingsSelectRow("DNS-over-HTTPS", s.dohProvider,
                    listOf("Off", "Cloudflare", "Quad9", "AdGuard"), palette) { viewModel.setDohProvider(it) }
                SettingsToggleRow("Certificate Pinning", s.certificatePinning, palette) { viewModel.setCertificatePinning(!s.certificatePinning) }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 11. Parental Controls ─────────────────────────────────────────
            SettingsGroup("Parental Controls", palette) {
                SettingsToggleRow("Kids Mode", s.kidsMode, palette) { viewModel.setKidsMode(!s.kidsMode) }
                SettingsSelectRow("Max Content Rating", s.parentalRatingLimit,
                    listOf("G", "PG", "PG-13", "R", "NC-17"), palette) { viewModel.setParentalRatingLimit(it) }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 12. Accounts & Sync ───────────────────────────────────────────
            SettingsGroup("Accounts", palette) {
                SettingsToggleRow("Trakt.tv Sync",   s.traktSync,   palette) { viewModel.setTraktSync(!s.traktSync) }
                SettingsToggleRow("AniList Sync",    s.anilistSync, palette) { viewModel.setAnilistSync(!s.anilistSync) }
                SettingsToggleRow("MyAnimeList Sync",s.malSync,     palette) { viewModel.setMalSync(!s.malSync) }
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── 13. System ─────────────────────────────────────────────────────
            SettingsGroup("System", palette) {
                SettingsActionRow("Clear Cache",       palette = palette, isDestructive = false, onClick = {})
                SettingsActionRow("Reset to Defaults", palette = palette, isDestructive = true,  onClick = { viewModel.resetDefaults() })
            }
            Spacer(Modifier.height(AFSpacing.px4))

            // ── System info ───────────────────────────────────────────────────
            SettingsGroup("System Information", palette) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AFSpacing.px8),
                ) {
                    SystemInfoItem("Version", systemInfo.version, palette)
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
            text         = title.uppercase(),
            color        = palette.textDim,
            fontSize     = AFTypo.micro,
            fontWeight   = FontWeight.Bold,
            letterSpacing = AFTypo.sectionTitleSpacing,
            modifier     = Modifier.padding(bottom = 4.dp),
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
