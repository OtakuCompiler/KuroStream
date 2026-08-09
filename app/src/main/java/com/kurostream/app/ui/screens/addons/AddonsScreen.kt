// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.addons

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurostream.app.ui.components.Af3PillButton
import com.kurostream.app.ui.components.Af3ScreenScaffold
import com.kurostream.app.ui.theme.Af3Theme

private data class BundledAddon(
    val id: String,
    val name: String,
    val description: String,
    val status: String,
    val icon: String,
)

private val DefaultBundledAddons = listOf(
    BundledAddon("anilist",      "AniList",          "Anime tracker, GraphQL, no auth needed",       "Built-in", "🌸"),
    BundledAddon("mal_jikan",    "MAL (JIKAN)",      "MyAnimeList rankings & seasonal anime",        "Built-in", "🇯🇵"),
    BundledAddon("tmdb",         "TMDB",             "Movies & TV posters, ratings, search",          "Needs key","🎬"),
    BundledAddon("tvdb",         "TVDB",             "Series metadata, episode info",                 "Needs PIN","📺"),
    BundledAddon("kitsu",        "Kitsu",            "Anime & manga with multi-language titles",      "Built-in", "📚"),
    BundledAddon("simkl",        "Simkl",            "Movies / TV / anime tracking across devices",   "Needs key","⭐"),
    BundledAddon("mdblist",      "MDBList",          "User-curated lists (Trakt sync)",               "Built-in", "📋"),
    BundledAddon("rpdb",         "RPDB",             "Rating posters overlay (poster corrections)",   "Built-in", "🖼"),
    BundledAddon("stremio",      "Stremio add-ons",  "Install any Stremio community addon",           "Ready",    "🎯"),
    BundledAddon("kodi",         "Kodi repos",       "Browse and install Kodi repository addons",     "Ready",    "🎞"),
    BundledAddon("cloudstream",  "CloudStream",      "Import CSv3 plugin repositories",               "Ready",    "☁"),
    BundledAddon("consumet",     "Consumet",         "Multi-source anime/manga/movies aggregator",     "Built-in", "🔌"),
)

@Composable
fun AddonsScreen(
    onBackClick: () -> Unit,
    onConfigure: (String) -> Unit = {},
    onMarketplaceClick: () -> Unit = {},
) {
    val palette = Af3Theme.palette
    val space = Af3Theme.space

    Af3ScreenScaffold(title = "Add-ons", onBack = onBackClick) {
        Column(modifier = Modifier.fillMaxSize().padding(space.safeH)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Af3PillButton(label = "Browse Marketplace", primary = true, onClick = onMarketplaceClick)
                Af3PillButton(label = "Install from URL", primary = false, onClick = {})
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${DefaultBundledAddons.size} bundled providers — open any row to configure, enable, or sign in.",
                color = palette.textSec,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(DefaultBundledAddons, key = { it.id }) { addon ->
                    AddonRow(addon, onClick = { onConfigure(addon.id) })
                }
            }
        }
    }
}

@Composable
private fun AddonRow(addon: BundledAddon, onClick: () -> Unit) {
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
            Text(text = addon.icon, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = addon.name,
                color = palette.text,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
            Text(
                text = addon.description,
                color = palette.textSec,
                fontSize = 11.sp,
            )
        }
        Box(
            modifier = Modifier
                .background(palette.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = addon.status,
                color = palette.textSec,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
