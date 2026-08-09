// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurostream.app.ui.components.Af3PillButton
import com.kurostream.app.ui.components.Af3ScreenScaffold
import com.kurostream.app.ui.theme.Af3Theme

private data class SettingItem(
    val icon: String,
    val label: String,
    val description: String? = null,
    val value: String? = null,
    val section: String,
    val onClick: () -> Unit = {},
)

private data class SettingSection(val title: String, val items: List<SettingItem>)

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onMarketplaceClick: () -> Unit = {},
    onExtensionsClick: () -> Unit = {},
    onSourceLockClick: () -> Unit = {},
    onPlaybackClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onDisplayClick: () -> Unit = {},
    onNetworkClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onStorageClick: () -> Unit = {},
    onDebridClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onDiagnosticsClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
    onTorrentsClick: () -> Unit = {},
    onTraktClick: () -> Unit = {},
    onRealDebridClick: () -> Unit = {},
    onAllDebridClick: () -> Unit = {},
    onOpenSubtitlesClick: () -> Unit = {},
    onTmdbClick: () -> Unit = {},
    onTvdbClick: () -> Unit = {},
    onKitsuClick: () -> Unit = {},
    onSimklClick: () -> Unit = {},
    onAniListClick: () -> Unit = {},
    onMalClick: () -> Unit = {},
    traktConfigured: Boolean = false,
    realDebridConfigured: Boolean = false,
    allDebridConfigured: Boolean = false,
    openSubtitlesConfigured: Boolean = false,
    tmdbConfigured: Boolean = false,
    tvdbConfigured: Boolean = false,
    kitsuConfigured: Boolean = false,
    simklConfigured: Boolean = false,
    anilistConfigured: Boolean = false,
    malConfigured: Boolean = false,
) {
    val sections = remember(
        traktConfigured, realDebridConfigured, allDebridConfigured, openSubtitlesConfigured,
        tmdbConfigured, tvdbConfigured, kitsuConfigured, simklConfigured,
        anilistConfigured, malConfigured,
    ) {
        buildSections(
            traktConfigured = traktConfigured,
            realDebridConfigured = realDebridConfigured,
            allDebridConfigured = allDebridConfigured,
            openSubtitlesConfigured = openSubtitlesConfigured,
            tmdbConfigured = tmdbConfigured,
            tvdbConfigured = tvdbConfigured,
            kitsuConfigured = kitsuConfigured,
            simklConfigured = simklConfigured,
            anilistConfigured = anilistConfigured,
            malConfigured = malConfigured,
            onMarketplaceClick = onMarketplaceClick,
            onExtensionsClick = onExtensionsClick,
            onSourceLockClick = onSourceLockClick,
            onPlaybackClick = onPlaybackClick,
            onPrivacyClick = onPrivacyClick,
            onDisplayClick = onDisplayClick,
            onNetworkClick = onNetworkClick,
            onAccountClick = onAccountClick,
            onStorageClick = onStorageClick,
            onDebridClick = onDebridClick,
            onAboutClick = onAboutClick,
            onDiagnosticsClick = onDiagnosticsClick,
            onBackupClick = onBackupClick,
            onTorrentsClick = onTorrentsClick,
            onTraktClick = onTraktClick,
            onRealDebridClick = onRealDebridClick,
            onAllDebridClick = onAllDebridClick,
            onOpenSubtitlesClick = onOpenSubtitlesClick,
            onTmdbClick = onTmdbClick,
            onTvdbClick = onTvdbClick,
            onKitsuClick = onKitsuClick,
            onSimklClick = onSimklClick,
            onAniListClick = onAniListClick,
            onMalClick = onMalClick,
        )
    }

    var focusedKey by remember { mutableStateOf<String?>("${sections.first().items.first().label}") }
    var focusedIndex by remember { mutableStateOf(0) }

    val flatItems = remember(sections) { sections.flatMap { it.items } }
    val totalCount = flatItems.size

    Af3ScreenScaffold(title = "Settings", onBack = onBackClick) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> {
                            focusedIndex = (focusedIndex + 1).coerceAtMost(totalCount - 1)
                            focusedKey = flatItems[focusedIndex].label
                            true
                        }
                        Key.DirectionUp -> {
                            focusedIndex = (focusedIndex - 1).coerceAtLeast(0)
                            focusedKey = flatItems[focusedIndex].label
                            true
                        }
                        Key.DirectionRight -> {
                            focusedIndex = (focusedIndex + 5).coerceAtMost(totalCount - 1)
                            focusedKey = flatItems[focusedIndex].label
                            true
                        }
                        Key.DirectionLeft -> {
                            focusedIndex = (focusedIndex - 5).coerceAtLeast(0)
                            focusedKey = flatItems[focusedIndex].label
                            true
                        }
                        Key.Enter, Key.DirectionCenter -> {
                            flatItems[focusedIndex].onClick()
                            true
                        }
                        else -> false
                    }
                },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                sections.forEach { section ->
                    item(key = "section_${section.title}") {
                        SectionHeader(section.title)
                    }
                    items(
                        items = section.items,
                        key = { item -> "${section.title}_${item.label}" },
                    ) { item ->
                        SettingRow(
                            item = item,
                            focused = focusedKey == item.label,
                            onFocus = {
                                focusedKey = item.label
                                focusedIndex = flatItems.indexOfFirst { it.label == item.label }.coerceAtLeast(0)
                            },
                            onClick = item.onClick,
                        )
                    }
                }
                item("footer") {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "KuroStream 1.0.0-debug · privacy-first, no telemetry",
                        color = Af3Theme.palette.textDim,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val palette = Af3Theme.palette
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            text = title.uppercase(),
            color = palette.accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.12f)
                .height(2.dp)
                .background(palette.accent.copy(alpha = 0.6f), RoundedCornerShape(1.dp)),
        )
    }
}

@Composable
private fun SettingRow(
    item: SettingItem,
    focused: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    val palette = Af3Theme.palette
    val space = Af3Theme.space
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (focused) palette.surfaceVariant else Color.Transparent,
                RoundedCornerShape(space.s8),
            )
            .padding(horizontal = space.safeH, vertical = 14.dp)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .focusable()
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (focused) palette.accent else palette.surface,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = item.icon, fontSize = 18.sp, color = if (focused) palette.bgDeep else palette.text)
        }
        Spacer(Modifier.width(space.s16))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                color = palette.text,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
            if (item.description != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.description,
                    color = palette.textSec,
                    fontSize = 12.sp,
                )
            }
        }
        if (item.value != null) {
            Text(
                text = item.value,
                color = if (focused) palette.accent else palette.textDim,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        } else {
            Text(
                text = "›",
                color = if (focused) palette.accent else palette.textDim,
                fontSize = 22.sp,
            )
        }
    }
}

@Composable
fun SourceLockSettingsScreen(
    onBackClick: () -> Unit,
) {
    Af3ScreenScaffold(title = "Source Lock", onBack = onBackClick) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Pin specific sources per title to always prefer them.",
                color = Af3Theme.palette.textSec,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(16.dp))
            Af3PillButton(label = "Manage per-title locks", primary = true, onClick = {})
            Spacer(Modifier.height(8.dp))
            Af3PillButton(label = "Default source order", primary = false, onClick = {})
        }
    }
}

private fun buildSections(
    traktConfigured: Boolean,
    realDebridConfigured: Boolean,
    allDebridConfigured: Boolean,
    openSubtitlesConfigured: Boolean,
    tmdbConfigured: Boolean,
    tvdbConfigured: Boolean,
    kitsuConfigured: Boolean,
    simklConfigured: Boolean,
    anilistConfigured: Boolean,
    malConfigured: Boolean,
    onMarketplaceClick: () -> Unit,
    onExtensionsClick: () -> Unit,
    onSourceLockClick: () -> Unit,
    onPlaybackClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onDisplayClick: () -> Unit,
    onNetworkClick: () -> Unit,
    onAccountClick: () -> Unit,
    onStorageClick: () -> Unit,
    onDebridClick: () -> Unit,
    onAboutClick: () -> Unit,
    onDiagnosticsClick: () -> Unit,
    onBackupClick: () -> Unit,
    onTorrentsClick: () -> Unit,
    onTraktClick: () -> Unit,
    onRealDebridClick: () -> Unit,
    onAllDebridClick: () -> Unit,
    onOpenSubtitlesClick: () -> Unit,
    onTmdbClick: () -> Unit,
    onTvdbClick: () -> Unit,
    onKitsuClick: () -> Unit,
    onSimklClick: () -> Unit,
    onAniListClick: () -> Unit,
    onMalClick: () -> Unit,
): List<SettingSection> = listOf(
    SettingSection(
        title = "Playback",
        items = listOf(
            SettingItem("▶", "Playback", "Player engine, decoders, ABR, PiP", section = "Playback", onClick = onPlaybackClick),
            SettingItem("⏱", "Sleep timer", "Set a stop time during playback", section = "Playback"),
            SettingItem("🎞", "Source lock", "Pin preferred sources per title", section = "Playback", onClick = onSourceLockClick),
            SettingItem("🔊", "Audio passthrough", "Dolby Atmos / DTS:X to receiver", section = "Playback", value = "Auto"),
        ),
    ),
    SettingSection(
        title = "Display",
        items = listOf(
            SettingItem("🎨", "Appearance", "Theme, accent color, layout density", section = "Display", onClick = onDisplayClick),
            SettingItem("📺", "Aspect ratio", "Auto-detected, override for legacy TVs", section = "Display", value = "Auto"),
            SettingItem("🌐", "Network", "Wi-Fi data usage, proxy, DNS over HTTPS", section = "Display", onClick = onNetworkClick),
            SettingItem("📦", "Storage", "Cache size, offline downloads", section = "Display", onClick = onStorageClick),
        ),
    ),
    SettingSection(
        title = "Privacy",
        items = listOf(
            SettingItem("🛡", "Privacy", "Telemetry, search history, watch history", section = "Privacy", onClick = onPrivacyClick),
            SettingItem("🚫", "Block trackers", "Strip tracking params from URLs", section = "Privacy", value = "On"),
            SettingItem("🔒", "DNS over HTTPS", "Encrypted DNS queries", section = "Privacy", value = "Off"),
        ),
    ),
    SettingSection(
        title = "Extensions & Sources",
        items = listOf(
            SettingItem("🛒", "Skins marketplace", "Browse and install community skins", section = "Extensions", onClick = onMarketplaceClick),
            SettingItem("🧩", "Extensions", "Stremio, Kodi, Consumet, CloudStream", section = "Extensions", onClick = onExtensionsClick),
            SettingItem("📡", "Torrents", "Torrent server, streaming, WebDAV", section = "Extensions", onClick = onTorrentsClick),
            SettingItem("🔑", "Debrid", "Real-Debrid, All-Debrid, Premiumize", section = "Extensions", onClick = onDebridClick),
        ),
    ),
    SettingSection(
        title = "Accounts & Sync",
        items = listOf(
            SettingItem("👤", "Trakt", "Sync watch progress across devices", section = "Accounts", value = if (traktConfigured) "Connected" else "Not connected", onClick = onTraktClick),
            SettingItem("🔓", "Real-Debrid", "Premium cached torrents", section = "Accounts", value = if (realDebridConfigured) "Connected" else "Not connected", onClick = onRealDebridClick),
            SettingItem("🔓", "All-Debrid", "Multi-host premium", section = "Accounts", value = if (allDebridConfigured) "Connected" else "Not connected", onClick = onAllDebridClick),
            SettingItem("💬", "OpenSubtitles", "Subtitle download provider", section = "Accounts", value = if (openSubtitlesConfigured) "Connected" else "Not connected", onClick = onOpenSubtitlesClick),
            SettingItem("☁", "KuroCloud", "Encrypted sync between your devices", section = "Accounts", value = "Disabled", onClick = onAccountClick),
            SettingItem("💾", "Backup & restore", "Export / import all settings and add-ons", section = "Accounts", onClick = onBackupClick),
        ),
    ),
    SettingSection(
        title = "Metadata Providers",
        items = listOf(
            SettingItem("🎬", "TMDB", "The Movie Database — movies & TV posters, ratings", section = "Metadata", value = if (tmdbConfigured) "API key set" else "Not configured", onClick = onTmdbClick),
            SettingItem("📺", "TVDB", "TheTVDB — series metadata, episode info", section = "Metadata", value = if (tvdbConfigured) "PIN set" else "Not configured", onClick = onTvdbClick),
            SettingItem("🌸", "AniList", "Anime tracker with rich metadata (GraphQL, no auth)", section = "Metadata", value = if (anilistConfigured) "OAuth set" else "Anonymous (limited)", onClick = onAniListClick),
            SettingItem("🇯🇵", "MAL", "MyAnimeList via JIKAN — anime/manga rankings", section = "Metadata", value = if (malConfigured) "Authenticated" else "Public reads", onClick = onMalClick),
            SettingItem("📚", "Kitsu", "Anime/manga catalog with multiple title languages", section = "Metadata", value = if (kitsuConfigured) "Authenticated" else "Public reads", onClick = onKitsuClick),
            SettingItem("⭐", "Simkl", "Movies, TV, anime tracking across devices", section = "Metadata", value = if (simklConfigured) "Client ID set" else "Not configured", onClick = onSimklClick),
        ),
    ),
    SettingSection(
        title = "System",
        items = listOf(
            SettingItem("ℹ", "About", "Version, licenses, build info", section = "System", onClick = onAboutClick),
            SettingItem("🩺", "Diagnostics", "Logs, network status, source health", section = "System", onClick = onDiagnosticsClick),
        ),
    ),
)
