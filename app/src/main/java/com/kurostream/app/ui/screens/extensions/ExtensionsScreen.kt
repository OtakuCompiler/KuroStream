// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.extensions

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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurostream.app.ui.components.Af3PillButton
import com.kurostream.app.ui.components.Af3ScreenScaffold
import com.kurostream.app.ui.theme.Af3Theme

private data class ProviderEntry(
    val id: String,
    val name: String,
    val description: String,
    val enabled: Boolean,
    val icon: String,
)

private val DefaultProviders = listOf(
    ProviderEntry("anilist",     "AniList",       "Anime metadata (GraphQL)",        true,  "🌸"),
    ProviderEntry("mal",         "MAL/JIKAN",     "Anime/manga rankings",            true,  "🇯🇵"),
    ProviderEntry("kitsu",       "Kitsu",         "Anime/manga with multi-language", true,  "📚"),
    ProviderEntry("tmdb",        "TMDB",          "Movies & TV posters, ratings",    false, "🎬"),
    ProviderEntry("tvdb",        "TVDB",          "Series metadata",                 false, "📺"),
    ProviderEntry("simkl",       "Simkl",         "Cross-device tracking",           false, "⭐"),
    ProviderEntry("opensubs",    "OpenSubtitles", "Subtitle downloads",              false, "💬"),
    ProviderEntry("trakt",       "Trakt",         "Watch progress sync",             false, "👤"),
    ProviderEntry("rd",          "Real-Debrid",   "Premium cached torrents",         false, "🔓"),
    ProviderEntry("alldebrid",   "All-Debrid",    "Multi-host premium",              false, "🔓"),
    ProviderEntry("premiumize",  "Premiumize",    "Multi-host + VPN",                false, "🔓"),
)

@Composable
fun ExtensionsScreen(
    onBackClick: () -> Unit,
    onConfigure: (String) -> Unit = {},
) {
    val palette = Af3Theme.palette
    val space = Af3Theme.space
    var enabledSet by remember { mutableStateOf(DefaultProviders.filter { it.enabled }.map { it.id }.toSet()) }

    Af3ScreenScaffold(title = "Extensions", onBack = onBackClick) {
        Column(modifier = Modifier.fillMaxSize().padding(space.safeH)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Af3PillButton(label = "Install from URL", primary = true, onClick = {})
                Af3PillButton(label = "Reload Marketplace", primary = false, onClick = {})
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${enabledSet.size} of ${DefaultProviders.size} providers active — toggle to enable, tap to configure.",
                color = palette.textSec,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(DefaultProviders, key = { it.id }) { p ->
                    ExtensionRow(
                        entry = p,
                        enabled = p.id in enabledSet,
                        onToggle = {
                            enabledSet = enabledSet.toMutableSet().apply {
                                if (p.id in this) remove(p.id) else add(p.id)
                            }
                        },
                        onClick = { onConfigure(p.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtensionRow(
    entry: ProviderEntry,
    enabled: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    val palette = Af3Theme.palette
    val space = Af3Theme.space
    var focused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (focused) palette.surfaceVariant else Color.Transparent,
                RoundedCornerShape(space.s8),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(palette.surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = entry.icon, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                color = palette.text,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
            Text(
                text = entry.description,
                color = palette.textSec,
                fontSize = 11.sp,
            )
        }
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 24.dp)
                .background(
                    if (enabled) palette.accent else palette.surfaceVariant,
                    RoundedCornerShape(12.dp),
                )
                .clickable { onToggle() }
                .focusable(),
            contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .size(20.dp)
                    .background(palette.bg, CircleShape),
            )
        }
    }
}

@Composable
fun ExtensionConfigScreen(
    extensionId: String,
    onBackClick: () -> Unit,
) {
    val palette = Af3Theme.palette
    val space = Af3Theme.space
    val entry = DefaultProviders.firstOrNull { it.id == extensionId }
    Af3ScreenScaffold(title = entry?.let { "${it.icon} ${it.name}" } ?: "Extension: $extensionId", onBack = onBackClick) {
        Column(modifier = Modifier.fillMaxSize().padding(space.safeH)) {
            if (entry == null) {
                Text(
                    text = "No metadata for extension \"$extensionId\".",
                    color = palette.textSec,
                )
                return@Column
            }
            Text(
                text = entry.description,
                color = palette.textSec,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(16.dp))
            Af3PillButton(label = "Configure", primary = true, onClick = {})
            Spacer(Modifier.height(8.dp))
            Af3PillButton(label = "Test connection", primary = false, onClick = {})
        }
    }
}
