package com.kurostream.tv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.kurostream.tv.ui.theme.KuroStreamColors
import com.kurostream.tv.ui.theme.KuroStreamTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Settings Screen - App configuration.
 * 
 * Features:
 * - Playback settings (quality, buffer size, auto-play)
 * - Subtitle settings (language, size, style)
 * - Appearance settings (theme)
 * - Profile & Security (PIN lock)
 * - AniList account linking
 * - Cache management
 * - About & version info
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onNavigateToPinLock: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        SettingsTopBar(onBackPressed = onBackPressed)
        
        // Settings list
        TvLazyColumn(
            contentPadding = PaddingValues(
                horizontal = KuroStreamTheme.spacing.screenPadding.dp,
                vertical = KuroStreamTheme.spacing.medium.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Playback Section
            item {
                SettingsSectionHeader(
                    title = "Playback",
                    icon = Icons.Default.PlayCircle
                )
            }
            
            item {
                SettingsItem(
                    title = "Preferred Quality",
                    subtitle = uiState.preferredQuality.displayName,
                    onClick = { viewModel.showQualityPicker() }
                )
            }
            
            item {
                SettingsSwitch(
                    title = "Auto-play Next Episode",
                    subtitle = "Automatically play the next episode when current one ends",
                    checked = uiState.autoPlayNext,
                    onCheckedChange = { viewModel.setAutoPlayNext(it) }
                )
            }
            
            item {
                SettingsSwitch(
                    title = "Skip Intro Automatically",
                    subtitle = "Auto-skip opening when skip timestamps are available",
                    checked = uiState.autoSkipIntro,
                    onCheckedChange = { viewModel.setAutoSkipIntro(it) }
                )
            }
            
            item {
                SettingsSwitch(
                    title = "Skip Outro Automatically",
                    subtitle = "Auto-skip ending when skip timestamps are available",
                    checked = uiState.autoSkipOutro,
                    onCheckedChange = { viewModel.setAutoSkipOutro(it) }
                )
            }
            
            // Subtitle Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(
                    title = "Subtitles",
                    icon = Icons.Default.Subtitles
                )
            }
            
            item {
                SettingsItem(
                    title = "Preferred Language",
                    subtitle = uiState.preferredSubtitleLanguage,
                    onClick = { viewModel.showSubtitleLanguagePicker() }
                )
            }
            
            item {
                SettingsItem(
                    title = "Subtitle Size",
                    subtitle = uiState.subtitleSize.displayName,
                    onClick = { viewModel.showSubtitleSizePicker() }
                )
            }
            
            item {
                SettingsSwitch(
                    title = "Subtitle Background",
                    subtitle = "Show semi-transparent background behind subtitles",
                    checked = uiState.subtitleBackground,
                    onCheckedChange = { viewModel.setSubtitleBackground(it) }
                )
            }
            
            // Appearance Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(
                    title = "Appearance",
                    icon = Icons.Default.Palette
                )
            }
            
            item {
                SettingsSwitch(
                    title = "Prefer English Titles",
                    subtitle = "Show English titles instead of Romaji when available",
                    checked = uiState.preferEnglishTitles,
                    onCheckedChange = { viewModel.setPreferEnglishTitles(it) }
                )
            }
            
            // Security Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(
                    title = "Security",
                    icon = Icons.Default.Lock
                )
            }
            
            item {
                SettingsItem(
                    title = "PIN Lock",
                    subtitle = if (uiState.pinEnabled) "Enabled" else "Disabled",
                    onClick = onNavigateToPinLock
                )
            }
            
            // Sync Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(
                    title = "Sync & Accounts",
                    icon = Icons.Default.Sync
                )
            }
            
            item {
                SettingsItem(
                    title = "AniList Account",
                    subtitle = uiState.anilistUsername ?: "Not connected",
                    onClick = { viewModel.showAniListLogin() }
                )
            }
            
            item {
                SettingsSwitch(
                    title = "Auto-sync Watch Progress",
                    subtitle = "Automatically sync watch progress with AniList",
                    checked = uiState.autoSyncEnabled,
                    onCheckedChange = { viewModel.setAutoSync(it) },
                    enabled = uiState.anilistUsername != null
                )
            }
            
            // Storage Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(
                    title = "Storage",
                    icon = Icons.Default.Storage
                )
            }
            
            item {
                SettingsItem(
                    title = "Image Cache",
                    subtitle = uiState.imageCacheSize,
                    onClick = { }
                )
            }
            
            item {
                SettingsItem(
                    title = "Video Cache",
                    subtitle = uiState.videoCacheSize,
                    onClick = { }
                )
            }
            
            item {
                SettingsButton(
                    title = "Clear All Cache",
                    icon = Icons.Default.Delete,
                    onClick = { viewModel.clearCache() }
                )
            }
            
            // About Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(
                    title = "About",
                    icon = Icons.Default.Info
                )
            }
            
            item {
                SettingsItem(
                    title = "Version",
                    subtitle = uiState.appVersion,
                    onClick = { }
                )
            }
            
            item {
                SettingsItem(
                    title = "Build",
                    subtitle = uiState.buildNumber,
                    onClick = { }
                )
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsTopBar(
    onBackPressed: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = KuroStreamTheme.spacing.screenPadding.dp,
                vertical = KuroStreamTheme.spacing.medium.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackPressed) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "Settings",
            style = KuroStreamTheme.typography.headlineMedium,
            color = Color.White
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = KuroStreamColors.Primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = KuroStreamTheme.typography.titleLarge,
            color = KuroStreamColors.Primary
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = KuroStreamColors.Surface,
            focusedContainerColor = KuroStreamColors.SurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = KuroStreamTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = KuroStreamTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Card(
        onClick = { if (enabled) onCheckedChange(!checked) },
        colors = CardDefaults.colors(
            containerColor = KuroStreamColors.Surface,
            focusedContainerColor = KuroStreamColors.SurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = KuroStreamTheme.typography.titleMedium,
                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = subtitle,
                    style = KuroStreamTheme.typography.bodySmall,
                    color = if (enabled) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = { if (enabled) onCheckedChange(it) },
                enabled = enabled
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(title)
    }
}
