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

@Composable
internal fun SubScreenShell(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Af3ScreenScaffold(title = title, onBack = onBack) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            content()
        }
    }
}

@Composable
internal fun ConfigHeader(icon: String, title: String, subtitle: String) {
    val palette = Af3Theme.palette
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
        Box(
            modifier = Modifier.size(56.dp).background(palette.accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = icon, fontSize = 28.sp)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(text = title, color = palette.text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(text = subtitle, color = palette.textSec, fontSize = 13.sp)
        }
    }
}

@Composable
internal fun SimpleTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    isPassword: Boolean = false,
) {
    val palette = Af3Theme.palette
    val space = Af3Theme.space
    var focused by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(text = label, color = palette.textSec, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (focused) palette.surfaceVariant else palette.surface,
                    RoundedCornerShape(space.s8),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .onFocusChanged { focused = it.isFocused }
                .focusable()
                .clickable { },
        ) {
            if (value.isEmpty()) {
                Text(text = placeholder, color = palette.textDim, fontSize = 14.sp)
            } else {
                Text(
                    text = if (isPassword) "•".repeat(value.length.coerceAtMost(20)) else value,
                    color = palette.text,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
internal fun ToggleRow(label: String, description: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val palette = Af3Theme.palette
    val space = Af3Theme.space
    var focused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (focused) palette.surfaceVariant else Color.Transparent, RoundedCornerShape(space.s8))
            .padding(horizontal = space.safeH, vertical = 14.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable { onChange(!value) },
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = palette.text, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(text = description, color = palette.textSec, fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .size(width = 50.dp, height = 28.dp)
                .background(
                    if (value) palette.accent else palette.surfaceVariant,
                    RoundedCornerShape(14.dp),
                ),
            contentAlignment = if (value) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .size(22.dp)
                    .background(palette.bg, CircleShape),
            )
        }
    }
}

@Composable
internal fun ChoiceRow(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val palette = Af3Theme.palette
    val space = Af3Theme.space
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(text = label, color = palette.textSec, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { idx, opt ->
                var focused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .background(
                            if (selected == idx) palette.accent
                            else if (focused) palette.surfaceVariant
                            else palette.surface,
                            RoundedCornerShape(space.s8),
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .onFocusChanged { focused = it.isFocused }
                        .focusable()
                        .clickable { onSelect(idx) },
                ) {
                    Text(
                        text = opt,
                        color = if (selected == idx) palette.bgDeep else palette.text,
                        fontWeight = if (selected == idx) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
internal fun InfoCard(text: String) {
    val palette = Af3Theme.palette
    val space = Af3Theme.space
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surfaceVariant, RoundedCornerShape(space.s8))
            .padding(16.dp),
    ) {
        Text(text = text, color = palette.textSec, fontSize = 13.sp)
    }
}

@Composable
fun PlaybackSettingsScreen(onBackClick: () -> Unit) {
    SubScreenShell("Playback", onBackClick) {
        var dec by remember { mutableStateOf(0) }
        var abr by remember { mutableStateOf(1) }
        var pip by remember { mutableStateOf(true) }
        var atmos by remember { mutableStateOf(true) }
        LazyColumn {
            item { ConfigHeader("▶", "Playback engine", "Tune the video pipeline") }
            item { ChoiceRow("Hardware decoder", listOf("Auto", "HW", "SW"), dec) { dec = it } }
            item { ChoiceRow("Adaptive bitrate", listOf("Off", "Auto", "Aggressive"), abr) { abr = it } }
            item { Spacer(Modifier.height(8.dp)) }
            item { ToggleRow("Picture-in-Picture", "Float player on top of menus", pip) { pip = it } }
            item { ToggleRow("Dolby Atmos passthrough", "Forward Atmos to receiver", atmos) { atmos = it } }
            item { Spacer(Modifier.height(16.dp)) }
            item {
                Af3PillButton(label = "Test playback", primary = true, onClick = {})
            }
        }
    }
}

@Composable
fun PrivacySettingsScreen(onBackClick: () -> Unit) {
    SubScreenShell("Privacy", onBackClick) {
        var blockTrackers by remember { mutableStateOf(true) }
        var doh by remember { mutableStateOf(false) }
        var rememberSearch by remember { mutableStateOf(false) }
        var telemetry by remember { mutableStateOf(false) }
        LazyColumn {
            item { ConfigHeader("🛡", "Privacy", "KuroStream ships with zero telemetry by default") }
            item { ToggleRow("Block trackers", "Strip tracking params from URLs", blockTrackers) { blockTrackers = it } }
            item { ToggleRow("DNS over HTTPS", "Encrypted DNS queries (Cloudflare 1.1.1.1)", doh) { doh = it } }
            item { ToggleRow("Remember search history", "Keep recent queries across sessions", rememberSearch) { rememberSearch = it } }
            item { ToggleRow("Anonymous telemetry", "Send anonymous usage stats (off)", telemetry) { telemetry = it } }
            item { Spacer(Modifier.height(16.dp)) }
            item { InfoCard("KuroStream is privacy-first. There is no analytics SDK, no Firebase Crashlytics, no third-party trackers. Turning on any of the above is opt-in.") }
            item { Spacer(Modifier.height(16.dp)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Af3PillButton(label = "Clear search history", primary = false, onClick = {})
                    Af3PillButton(label = "Clear watch history", primary = false, onClick = {})
                }
            }
        }
    }
}

@Composable
fun DisplaySettingsScreen(onBackClick: () -> Unit) {
    SubScreenShell("Display", onBackClick) {
        var aspectIdx by remember { mutableStateOf(0) }
        var densityIdx by remember { mutableStateOf(1) }
        var hqArt by remember { mutableStateOf(true) }
        LazyColumn {
            item { ConfigHeader("🎨", "Appearance", "Theme, layout, artwork") }
            item { ChoiceRow("Theme", listOf("Auto", "Dark", "Light"), 0) {} }
            item { ChoiceRow("Aspect ratio", listOf("Auto", "16:9", "21:9", "4:3"), aspectIdx) { aspectIdx = it } }
            item { ChoiceRow("Density", listOf("Compact", "Cozy", "Roomy"), densityIdx) { densityIdx = it } }
            item { ToggleRow("High-quality artwork", "Fetch full-res posters from TMDB/Kitsu", hqArt) { hqArt = it } }
        }
    }
}

@Composable
fun NetworkSettingsScreen(onBackClick: () -> Unit) {
    SubScreenShell("Network", onBackClick) {
        LazyColumn {
            item { ConfigHeader("🌐", "Network", "Connection, proxy, DNS") }
            item { SimpleTextField("HTTP proxy", "", { }, placeholder = "http://192.168.1.1:8888") }
            item { ChoiceRow("DNS over HTTPS", listOf("Off", "Cloudflare", "Google", "Quad9"), 1) {} }
            item { ChoiceRow("Bandwidth cap", listOf("None", "5 Mbps", "25 Mbps", "100 Mbps"), 1) {} }
            item { ChoiceRow("Concurrent connections", listOf("8", "16", "32", "64"), 2) {} }
            item { Spacer(Modifier.height(16.dp)) }
            item { InfoCard("Wi-Fi-only downloads: ON · Mobile data: ask") }
        }
    }
}

@Composable
fun AccountSettingsScreen(onBackClick: () -> Unit) {
    SubScreenShell("Accounts", onBackClick) {
        LazyColumn {
            item { ConfigHeader("👤", "Accounts", "Sync, sign-in, debrid") }
            item { InfoCard("Connect cloud services below to sync watch progress and unlock cached torrents.") }
            item { Spacer(Modifier.height(16.dp)) }
            item { Af3PillButton(label = "Trakt.tv", primary = true, onClick = {}) }
            item { Spacer(Modifier.height(8.dp)) }
            item { Af3PillButton(label = "KuroCloud (E2E)", primary = false, onClick = {}) }
        }
    }
}

@Composable
fun StorageSettingsScreen(onBackClick: () -> Unit) {
    SubScreenShell("Storage", onBackClick) {
        LazyColumn {
            item { ConfigHeader("📦", "Storage", "Cache, downloads, offline") }
            item { SimpleTextField("Cache limit (MB)", "512", { }) }
            item { ChoiceRow("Cache location", listOf("Internal", "SD card", "USB"), 0) {} }
            item { ToggleRow("Auto-clean on launch", "Prune cache older than 7 days", true) {} }
            item { Spacer(Modifier.height(8.dp)) }
            item { InfoCard("Used: 84 MB · Free: 12.4 GB · Downloads: 0 MB") }
            item { Spacer(Modifier.height(16.dp)) }
            item { Af3PillButton(label = "Clear cache", primary = true, onClick = {}) }
        }
    }
}

@Composable
fun AboutScreen(onBackClick: () -> Unit) {
    SubScreenShell("About", onBackClick) {
        LazyColumn {
            item { ConfigHeader("ℹ", "KuroStream", "Privacy-first Android TV streaming") }
            item { InfoCard("Version: 1.0.0-debug\nBuild: GitHub Actions · commit local\nLicense: GPL-3.0-only\nMade with ❤️ for Kodi refugees.") }
            item { Spacer(Modifier.height(8.dp)) }
            item { InfoCard("Components:\n• Compose for TV\n• Media3 ExoPlayer\n• Hilt + Room\n• Kotlin Coroutines + Flow") }
        }
    }
}

@Composable
fun DiagnosticsScreen(onBackClick: () -> Unit) {
    SubScreenShell("Diagnostics", onBackClick) {
        LazyColumn {
            item { ConfigHeader("🩺", "Diagnostics", "Logs, source health, network status") }
            item { InfoCard("Free RAM: 715 MB\nSwap used: 2.2 GB / 4.0 GB\nActive daemons: 2\nBuild cache hit: 92%") }
            item { Spacer(Modifier.height(8.dp)) }
            item { InfoCard("Extension providers:\n• AniList  ✅ public\n• MAL/JIKAN ✅ public\n• TMDB      ⚠ needs API key\n• TVDB      ⚠ needs PIN\n• Kitsu     ✅ public\n• Simkl     ⚠ needs client id") }
            item { Spacer(Modifier.height(16.dp)) }
            item { Af3PillButton(label = "Export logs", primary = true, onClick = {}) }
            item { Spacer(Modifier.height(8.dp)) }
            item { Af3PillButton(label = "Run network test", primary = false, onClick = {}) }
        }
    }
}

@Composable
fun TraktConfigScreen(onBackClick: () -> Unit) {
    SubScreenShell("Trakt", onBackClick) {
        var clientId by remember { mutableStateOf("") }
        var clientSecret by remember { mutableStateOf("") }
        LazyColumn {
            item { ConfigHeader("👤", "Trakt.tv", "Sync watch progress across devices") }
            item { SimpleTextField("Client ID", clientId, { clientId = it }, placeholder = "YOUR_TRAKT_CLIENT_ID") }
            item { SimpleTextField("Client secret", clientSecret, { clientSecret = it }, placeholder = "YOUR_TRAKT_CLIENT_SECRET", isPassword = true) }
            item { Spacer(Modifier.height(16.dp)) }
            item { Af3PillButton(label = "Authorize", primary = true, onClick = {}) }
            item { Spacer(Modifier.height(8.dp)) }
            item { InfoCard("Create an app at https://trakt.tv/oauth/applications and paste the credentials above.") }
        }
    }
}

@Composable
fun OpenSubtitlesConfigScreen(onBackClick: () -> Unit) {
    SubScreenShell("OpenSubtitles", onBackClick) {
        var apiKey by remember { mutableStateOf("") }
        var user by remember { mutableStateOf("") }
        var pass by remember { mutableStateOf("") }
        LazyColumn {
            item { ConfigHeader("💬", "OpenSubtitles", "Subtitle download provider") }
            item { SimpleTextField("API key", apiKey, { apiKey = it }, placeholder = "OpenSubtitles API v1 key") }
            item { SimpleTextField("Username", user, { user = it }) }
            item { SimpleTextField("Password", pass, { pass = it }, isPassword = true) }
            item { Spacer(Modifier.height(16.dp)) }
            item { Af3PillButton(label = "Sign in", primary = true, onClick = {}) }
        }
    }
}

@Composable
fun TmdbConfigScreen(onBackClick: () -> Unit) {
    SubScreenShell("TMDB", onBackClick) {
        var apiKey by remember { mutableStateOf("") }
        LazyColumn {
            item { ConfigHeader("🎬", "The Movie Database", "Movies & TV posters, ratings, cast") }
            item { SimpleTextField("API key (v3)", apiKey, { apiKey = it }, placeholder = "Get one at themoviedb.org/settings/api") }
            item { Spacer(Modifier.height(8.dp)) }
            item { InfoCard("Free tier, no quota. Used for posters, backdrops, ratings, and search enrichment.") }
            item { Spacer(Modifier.height(16.dp)) }
            item { Af3PillButton(label = "Save", primary = true, onClick = {}) }
        }
    }
}

@Composable
fun TvdbConfigScreen(onBackClick: () -> Unit) {
    SubScreenShell("TVDB", onBackClick) {
        var pin by remember { mutableStateOf("") }
        LazyColumn {
            item { ConfigHeader("📺", "TheTVDB", "Series metadata, episode info") }
            item { SimpleTextField("Subscriber PIN", pin, { pin = it }, placeholder = "Generate at thetvdb.com/dashboard") }
            item { Spacer(Modifier.height(8.dp)) }
            item { InfoCard("PIN is used to obtain a bearer token automatically. No other credentials required.") }
            item { Spacer(Modifier.height(16.dp)) }
            item { Af3PillButton(label = "Save", primary = true, onClick = {}) }
        }
    }
}

@Composable
fun KitsuConfigScreen(onBackClick: () -> Unit) {
    SubScreenShell("Kitsu", onBackClick) {
        LazyColumn {
            item { ConfigHeader("📚", "Kitsu", "Anime & manga with multiple title languages") }
            item { InfoCard("Kitsu works without authentication for public reads. Sign-in is optional and unlocks personal lists.") }
            item { Spacer(Modifier.height(16.dp)) }
            item { Af3PillButton(label = "Sign in (optional)", primary = false, onClick = {}) }
            item { Spacer(Modifier.height(8.dp)) }
            item { InfoCard("Already working: trending, current season, upcoming, top-rated, search.") }
        }
    }
}

@Composable
fun SimklConfigScreen(onBackClick: () -> Unit) {
    SubScreenShell("Simkl", onBackClick) {
        var clientId by remember { mutableStateOf("") }
        LazyColumn {
            item { ConfigHeader("⭐", "Simkl", "Movies, TV, anime tracking across devices") }
            item { SimpleTextField("Client ID", clientId, { clientId = it }, placeholder = "From simkl.com/settings/api") }
            item { Spacer(Modifier.height(16.dp)) }
            item { Af3PillButton(label = "Save", primary = true, onClick = {}) }
        }
    }
}

@Composable
fun AniListConfigScreen(onBackClick: () -> Unit) {
    SubScreenShell("AniList", onBackClick) {
        LazyColumn {
            item { ConfigHeader("🌸", "AniList", "Anime tracker (GraphQL, anonymous works)") }
            item { InfoCard("AniList is queried anonymously for trending, current season, top-rated, and recommendations. OAuth is optional and unlocks personal lists.") }
            item { Spacer(Modifier.height(16.dp)) }
            item { Af3PillButton(label = "Sign in with AniList (optional)", primary = false, onClick = {}) }
            item { Spacer(Modifier.height(8.dp)) }
            item { InfoCard("Already working without sign-in: trending, season now, season next, top-rated, recommendations, search.") }
        }
    }
}

@Composable
fun MalConfigScreen(onBackClick: () -> Unit) {
    SubScreenShell("MAL", onBackClick) {
        LazyColumn {
            item { ConfigHeader("🇯🇵", "MyAnimeList", "Anime & manga rankings via JIKAN") }
            item { InfoCard("MAL is queried via the public JIKAN API. No authentication required for top/popular/seasonal reads.") }
            item { Spacer(Modifier.height(16.dp)) }
            item { Af3PillButton(label = "Configure MAL credentials (optional)", primary = false, onClick = {}) }
            item { Spacer(Modifier.height(8.dp)) }
            item { InfoCard("Already working: top anime, popular anime, seasonal now, seasonal upcoming, search.") }
        }
    }
}
