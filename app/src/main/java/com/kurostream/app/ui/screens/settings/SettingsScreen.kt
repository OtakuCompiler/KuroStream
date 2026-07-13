// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kurostream.app.navigation.BackupRoute
import com.kurostream.app.navigation.TorrentsRoute
import com.kurostream.app.ui.components.SettingsSkinPicker
import com.kurostream.app.ui.theme.AnimeStreamTVTheme
import com.kurostream.app.ui.theme.Skin
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Observe high contrast setting
    val settings by viewModel.settingsRepository.observeSettings().collectAsStateWithLifecycle()

    // Use appropriate color scheme
    val colorScheme = if (settings.highContrastEnabled) {
        androidx.tv.material3.darkColorScheme(
            primary = androidx.compose.ui.graphics.Color.White,
            onPrimary = androidx.compose.ui.graphics.Color.Black,
            background = androidx.compose.ui.graphics.Color.Black,
            onBackground = androidx.compose.ui.graphics.Color.White,
            surface = androidx.compose.ui.graphics.Color.Black,
            onSurface = androidx.compose.ui.graphics.Color.White,
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFF333333),
            onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFCCCCCC),
            error = androidx.compose.ui.graphics.Color(0xFFFF3333),
            onError = androidx.compose.ui.graphics.Color.Black,
            outline = androidx.compose.ui.graphics.Color.White,
            outlineVariant = androidx.compose.ui.graphics.Color(0xFF999999)
        )
    } else {
        MaterialTheme.colorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(48.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Settings", style = MaterialTheme.typography.displaySmall)
                androidx.tv.material3.IconButton(onClick = onBackClick) {
                    androidx.tv.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Accessibility Section
            SettingsSection(title = "Accessibility", icon = androidx.compose.material.icons.Icons.Default.Accessibility) {
                SettingsToggle(
                    title = "High Contrast Mode",
                    subtitle = "Uses pure black/white colors for maximum contrast (WCAG AAA)",
                    checked = settings.highContrastEnabled,
                    onCheckedChange = { viewModel.setHighContrastEnabled(it) },
                    contentDescription = if (settings.highContrastEnabled) "High contrast enabled" else "High contrast disabled"
                )

                SettingsToggle(
                    title = "Reduce Motion",
                    subtitle = "Minimizes animations, transitions, and parallax effects",
                    checked = uiState.reduceMotionEnabled,
                    onCheckedChange = { viewModel.setReduceMotionEnabled(it) },
                    contentDescription = if (uiState.reduceMotionEnabled) "Reduced motion enabled" else "Reduced motion disabled"
                )

                SettingsToggle(
                    title = "Enhanced Focus Indicator",
                    subtitle = "Increases focus border thickness and adds glow effect",
                    checked = uiState.focusHighlightEnabled,
                    onCheckedChange = { viewModel.setFocusHighlightEnabled(it) },
                    contentDescription = if (uiState.focusHighlightEnabled) "Enhanced focus enabled" else "Enhanced focus disabled"
                )

                ListItem(
                    headlineContent = { Text("TalkBack Settings", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text("Configure system TalkBack preferences", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        TextButton(onClick = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }) {
                            Text("Open")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )

                ListItem(
                    headlineContent = { Text("Text Size", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text("Adjust system font size", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        TextButton(onClick = {
                            context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                        }) {
                            Text("Open")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Source Lock Section
            SettingsSection(title = "Source Lock", icon = androidx.compose.material.icons.Icons.Default.Lock) {
                SettingsToggle(
                    title = "Enable Source Lock",
                    subtitle = "Remember source provider for series and reuse for subsequent episodes",
                    checked = uiState.sourceLockEnabled,
                    onCheckedChange = { viewModel.setSourceLockEnabled(it) },
                    contentDescription = if (uiState.sourceLockEnabled) "Source lock enabled" else "Source lock disabled"
                )

                SettingsToggle(
                    title = "Persist Across Sessions",
                    subtitle = "Keep source locks after app restart",
                    checked = uiState.sourceLockPersistEnabled,
                    onCheckedChange = { viewModel.setSourceLockPersistEnabled(it) },
                    enabled = uiState.sourceLockEnabled,
                    contentDescription = if (uiState.sourceLockPersistEnabled) "Persist locks enabled" else "Persist locks disabled"
                )

                ListItem(
                    headlineContent = { Text("Fallback Behavior", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text("What to do when locked source becomes unavailable", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        DropdownMenuButton(
                            text = uiState.sourceLockFallbackMode.name,
                            onClick = { viewModel.showFallbackModePicker() }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )

                ListItem(
                    headlineContent = { Text("Clear All Source Locks", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text("Remove all remembered source preferences", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        TextButton(onClick = { viewModel.clearAllSourceLocks() }) {
                            Text("Clear")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Playback Section
            SettingsSection(title = "Playback", icon = androidx.compose.material.icons.Icons.Default.PlayCircle) {
                SettingsToggle(
                    title = "Auto-play Next Episode",
                    subtitle = "Automatically continue to the next episode",
                    checked = uiState.autoPlayNextEnabled,
                    onCheckedChange = { viewModel.setAutoPlayNextEnabled(it) }
                )

                SettingsToggle(
                    title = "Skip Intro/Outro",
                    subtitle = "Automatically skip opening and ending sequences when detected",
                    checked = settings.skipIntroEnabled,
                    onCheckedChange = { viewModel.settingsRepository.setSkipIntroEnabled(it) }
                )

                SettingsToggle(
                    title = "Hardware Acceleration",
                    subtitle = "Use hardware decoder for better performance (may cause issues on some devices)",
                    checked = settings.hardwareAccelerationEnabled,
                    onCheckedChange = { viewModel.settingsRepository.setHardwareAccelerationEnabled(it) }
                )

                SettingsToggle(
                    title = "Background Playback",
                    subtitle = "Continue audio playback when app is in background",
                    checked = settings.backgroundPlaybackEnabled,
                    onCheckedChange = { viewModel.settingsRepository.setBackgroundPlaybackEnabled(it) }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Appearance Section
            SettingsSection(title = "Appearance", icon = androidx.compose.material.icons.Icons.Default.Palette) {
                ListItem(
                    headlineContent = { Text("Theme", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text("Dark theme (system default)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        Text("Dark", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )

                ListItem(
                    headlineContent = { Text("Accent Color", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text("Purple (default)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        Text("Purple", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )

                val selectedSkin = try { Skin.valueOf(uiState.skinName) } catch (_: Exception) { Skin.AMOLED_BLACK }
                SettingsSkinPicker(
                    selected = selectedSkin,
                    onSkinSelected = { viewModel.setSkinName(it.name) },
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Storage Section
            SettingsSection(title = "Storage", icon = androidx.compose.material.icons.Icons.Default.Storage) {
                ListItem(
                    headlineContent = { Text("Cache Size", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text(settings.cacheSizeFormatted, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        TextButton(onClick = { viewModel.settingsRepository.clearCache() }) {
                            Text("Clear")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )

                ListItem(
                    headlineContent = { Text("Download Quality", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text("1080p (default)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        Text("1080p", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Language Section
            SettingsSection(title = "Language", icon = androidx.compose.material.icons.Icons.Default.Language) {
                ListItem(
                    headlineContent = { Text("Audio Language", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text(settings.preferredAudioLanguages.joinToString(", ") { it.uppercase() }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        TextButton(onClick = { /* Show language picker */ }) {
                            Text("Change")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )

                ListItem(
                    headlineContent = { Text("Subtitle Language", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text(settings.preferredSubtitleLanguages.joinToString(", ") { it.uppercase() }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        TextButton(onClick = { /* Show language picker */ }) {
                            Text("Change")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Torrents Section
            SettingsSection(title = "Torrents", icon = androidx.compose.material.icons.Icons.Default.CloudDownload) {
                ListItem(
                    headlineContent = { Text("Manage Torrents", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text("View and manage active torrent downloads", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        TextButton(onClick = { navController.navigate(TorrentsRoute) }) {
                            Text("Open")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )

                ListItem(
                    headlineContent = { Text("Bandwidth Limits", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text("Configure global download/upload speed limits", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        TextButton(onClick = { /* Show bandwidth limits dialog */ }) {
                            Text("Configure")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )

                SettingsToggle(
                    title = "Seed While Idle",
                    subtitle = "Continue seeding when app is in background",
                    checked = settings.seedWhileIdleEnabled,
                    onCheckedChange = { viewModel.setSeedWhileIdleEnabled(it) },
                    contentDescription = if (settings.seedWhileIdleEnabled) "Seed while idle enabled" else "Seed while idle disabled"
                )

                SettingsToggle(
                    title = "Sequential Download",
                    subtitle = "Prioritize file beginning and pieces near playback position for streaming",
                    checked = settings.sequentialDownloadEnabled,
                    onCheckedChange = { viewModel.setSequentialDownloadEnabled(it) },
                    contentDescription = if (settings.sequentialDownloadEnabled) "Sequential download enabled" else "Sequential download disabled"
                )

                ListItem(
                    headlineContent = { Text("Seed Ratio Limit", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text("Stop seeding after reaching ratio limit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        DropdownMenuButton(
                            text = "${settings.seedRatioLimit}",
                            onClick = { /* Show ratio picker */ }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // GitHub Backup & Sync Section
            SettingsSection(title = "Backup & Sync", icon = androidx.compose.material.icons.Icons.Default.CloudUpload) {
                ListItem(
                    headlineContent = { Text("GitHub Backup", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text("Backup and restore your data to GitHub", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        TextButton(onClick = { navController.navigate(BackupRoute) }) {
                            Text("Open")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Developer Options Section
            SettingsSection(title = "Developer Options", icon = androidx.compose.material.icons.Icons.Default.DeveloperMode) {
                ListItem(
                    headlineContent = { Text("Run Benchmarks", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text("Measure startup time, playback performance, and AI upscaling latency", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        if (uiState.isRunningBenchmarks) {
                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            TextButton(onClick = { viewModel.runBenchmarks() }) {
                                Text("Run")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )

                ListItem(
                    headlineContent = { Text("Debug Overlay", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text("Show playback diagnostics (bitrate, dropped frames, buffer health)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        Switch(
                            checked = settings.debugOverlayEnabled,
                            onCheckedChange = { viewModel.settingsRepository.setDebugOverlayEnabled(it) }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )

                ListItem(
                    headlineContent = { Text("Log Level", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text("Info", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        Text("Info", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )

                ListItem(
                    headlineContent = { Text("Export Logs", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text("Save debug logs to storage for bug reports", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = {
                        TextButton(onClick = { /* Export logs */ }) {
                            Text("Export")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // About Section
            SettingsSection(title = "About", icon = androidx.compose.material.icons.Icons.Default.Info) {
                ListItem(
                    headlineContent = { Text("Version", style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { Text("1.0.0 (Build 100)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )

                ListItem(
                    headlineContent = { Text("Open Source Licenses", style = MaterialTheme.typography.bodyLarge) },
                    trailingContent = {
                        TextButton(onClick = { /* Show licenses */ }) {
                            Text("View")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )

                ListItem(
                    headlineContent = { Text("Privacy Policy", style = MaterialTheme.typography.bodyLarge) },
                    trailingContent = {
                        TextButton(onClick = { /* Open privacy policy */ }) {
                            Text("View")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )

                ListItem(
                    headlineContent = { Text("Report a Bug", style = MaterialTheme.typography.bodyLarge) },
                    trailingContent = {
                        TextButton(onClick = { /* Open bug report */ }) {
                            Text("Report")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                androidx.tv.material3.Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        content()
    }
}

@Composable
fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String? = null,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize()
                    .semantics { contentDescription = contentDescription ?: (if (checked) "$title enabled" else "$title disabled") }
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    )
}

@Composable
fun DropdownMenuButton(
    text: String,
    onClick: () -> Unit
) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    androidx.tv.material3.Icon(
        imageVector = androidx.compose.material.icons.Icons.Default.ExpandMore,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(24.dp)
    )
}