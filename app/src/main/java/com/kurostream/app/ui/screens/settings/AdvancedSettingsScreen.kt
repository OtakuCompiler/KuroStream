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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import com.kurostream.app.ui.components.FocusableCard
import com.kurostream.app.ui.theme.TvOnSurfaceVariant
import com.kurostream.app.ui.theme.TvSurfaceVariant
import com.kurostream.app.repository.SettingsRepository
import com.kurostream.app.network.NetworkMonitorRepository
import com.kurostream.app.home.CustomHomeRowRepository
import com.kurostream.app.metadata.UnifiedMetadataRepository
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@Composable
fun AdvancedSettingsScreen(
    onBackClick: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    networkViewModel: NetworkDashboardViewModel = hiltViewModel(),
    homeRowViewModel: CustomHomeRowViewModel = hiltViewModel(),
    metadataViewModel: MetadataProvidersViewModel = hiltViewModel(),
) {
    val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val networkStats by networkViewModel.networkStats.collectAsStateWithLifecycle()
    val connectionQuality by networkViewModel.connectionQuality.collectAsStateWithLifecycle()
    val customRows by homeRowViewModel.customRows.collectAsStateWithLifecycle()
    val metadataProviders by metadataViewModel.enabledProviders.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(48.dp)
    ) {
        // Header
        SettingsHeader(onBackClick = onBackClick)

        Spacer(modifier = Modifier.height(24.dp))

        // Network Dashboard Section
        SettingsSection(title = "Network Dashboard", icon = Icons.Default.Wifi) {
            NetworkDashboardCard(
                stats = networkStats,
                quality = connectionQuality,
                onSpeedTestClick = { networkViewModel.runSpeedTest() },
                onPingTestClick = { networkViewModel.runPingTest() },
                onDetailsClick = { /* Navigate to detailed network screen */ },
            )
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // Source Lock Section
        SettingsSection(title = "Source Lock", icon = Icons.Default.Lock) {
            SourceLockSettingsCard(
                enabled = settings.sourceLockEnabled,
                fallbackMode = settings.sourceLockFallbackMode,
                maxRetries = settings.sourceLockMaxRetries,
                retryDelayMs = settings.sourceLockRetryDelayMs,
                persistAcrossSessions = settings.sourceLockPersist,
                notifyOnFallback = settings.sourceLockNotifyFallback,
                onEnabledChange = { settingsViewModel.setSourceLockEnabled(it) },
                onFallbackModeChange = { settingsViewModel.setSourceLockFallbackMode(it) },
                onMaxRetriesChange = { settingsViewModel.setSourceLockMaxRetries(it) },
                onRetryDelayChange = { settingsViewModel.setSourceLockRetryDelayMs(it) },
                onPersistChange = { settingsViewModel.setSourceLockPersist(it) },
                onNotifyChange = { settingsViewModel.setSourceLockNotifyFallback(it) },
                onClearAll = { settingsViewModel.clearAllSourceLocks() },
            )
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // Disk Buffer Settings (NEW)
        SettingsSection(title = "Disk Buffer", icon = Icons.Default.Storage) {
            DiskBufferSettingsCard(
                bufferSizeMb = settings.diskBufferSizeMb,
                readAheadMb = settings.diskBufferReadAheadMb,
                bufferLocation = settings.diskBufferLocation,
                deleteOnShutdown = settings.diskBufferDeleteOnShutdown,
                onBufferSizeChange = { settingsViewModel.setDiskBufferSizeMb(it) },
                onReadAheadChange = { settingsViewModel.setDiskBufferReadAheadMb(it) },
                onLocationChange = { settingsViewModel.setDiskBufferLocation(it) },
                onDeleteOnShutdownChange = { settingsViewModel.setDiskBufferDeleteOnShutdown(it) },
            )
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // Metadata Providers Section
        SettingsSection(title = "Metadata Providers", icon = Icons.Default.Database) {
            MetadataProvidersCard(
                providers = metadataProviders,
                onProviderToggle = { id, enabled -> metadataViewModel.setProviderEnabled(id, enabled) },
                onPriorityChange = { id, priority -> metadataViewModel.setProviderPriority(id, priority) },
                onRefreshAll = { metadataViewModel.refreshAllProviders() },
            )
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // Custom Home Rows Section
        SettingsSection(title = "Custom Home Rows", icon = Icons.Default.DashboardCustomize) {
            CustomHomeRowsCard(
                rows = customRows,
                onAddRow = { homeRowViewModel.showCreateRowDialog() },
                onEditRow = { row -> homeRowViewModel.showEditRowDialog(row) },
                onDeleteRow = { rowId -> homeRowViewModel.deleteRow(rowId) },
                onReorderRows = { rowIds -> homeRowViewModel.reorderRows(rowIds) },
                onToggleVisibility = { rowId, visible -> homeRowViewModel.toggleRowVisibility(rowId, visible) },
                onPreviewRow = { row -> homeRowViewModel.previewRow(row) },
            )
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // Playback Enhancements
        SettingsSection(title = "Playback Enhancements", icon = Icons.Default.PlayCircle) {
            PlaybackEnhancementsCard(
                autoPlayNext = settings.autoPlayNextEnabled,
                skipIntro = settings.skipIntroEnabled,
                hardwareAcceleration = settings.hardwareAccelerationEnabled,
                backgroundPlayback = settings.backgroundPlaybackEnabled,
                cinematicMode = settings.cinematicModeEnabled,
                ambientMode = settings.ambientModeEnabled,
                onAutoPlayNextChange = { settingsViewModel.setAutoPlayNextEnabled(it) },
                onSkipIntroChange = { settingsViewModel.setSkipIntroEnabled(it) },
                onHardwareAccelerationChange = { settingsViewModel.setHardwareAccelerationEnabled(it) },
                onBackgroundPlaybackChange = { settingsViewModel.setBackgroundPlaybackEnabled(it) },
                onCinematicModeChange = { settingsViewModel.setCinematicModeEnabled(it) },
                onAmbientModeChange = { settingsViewModel.setAmbientModeEnabled(it) },
            )
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // AI Features
        SettingsSection(title = "AI Features", icon = Icons.Default.Psychology) {
            AiFeaturesCard(
                offlineTranslation = settings.offlineTranslationEnabled,
                predictivePrecache = settings.predictivePrecacheEnabled,
                aiUpscaling = settings.aiUpscalingEnabled,
                frameInterpolation = settings.frameInterpolationEnabled,
                onOfflineTranslationChange = { settingsViewModel.setOfflineTranslationEnabled(it) },
                onPredictivePrecacheChange = { settingsViewModel.setPredictivePrecacheEnabled(it) },
                onAiUpscalingChange = { settingsViewModel.setAiUpscalingEnabled(it) },
                onFrameInterpolationChange = { settingsViewModel.setFrameInterpolationEnabled(it) },
            )
        }
    }
}

@Composable
fun SettingsHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Advanced Settings", style = MaterialTheme.typography.displaySmall)
        androidx.tv.material3.IconButton(onClick = onBackClick) {
            androidx.tv.material3.Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
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
fun NetworkDashboardCard(
    stats: NetworkStats,
    quality: ConnectionQuality,
    onSpeedTestClick: () -> Unit,
    onPingTestClick: () -> Unit,
    onDetailsClick: () -> Unit,
) {
    FocusableCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onDetailsClick,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quality indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Connection Quality", style = MaterialTheme.typography.labelLarge, color = TvOnSurfaceVariant)
                    Text(
                        text = quality.rating.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = when (quality.rating) {
                            QualityRating.EXCELLENT -> Color.Green
                            QualityRating.GOOD -> Color(0xFF4CAF50)
                            QualityRating.FAIR -> Color(0xFFFFC107)
                            QualityRating.POOR -> Color(0xFFFF9800)
                            else -> Color.Red
                        }
                    )
                }
                
                CircularProgressIndicator(
                    progress = quality.overallScore / 100f,
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 8.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = TvSurfaceVariant,
                )
            }

            // Stats grid
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(listOf(
                    StatItem("Download", "${stats.downloadSpeedMbps:.1f} Mbps", Icons.Default.Download),
                    StatItem("Upload", "${stats.uploadSpeedMbps:.1f} Mbps", Icons.Default.Upload),
                    StatItem("Latency", "${stats.latencyMs:.0f} ms", Icons.Default.Speed),
                    StatItem("Jitter", "${stats.jitterMs:.1f} ms", Icons.Default.WifiTethering),
                    StatItem("Packet Loss", "${stats.packetLossPercent:.2f}%", Icons.Default.SignalWifiStatusbar4),
                    StatItem("Signal", stats.wifiSignalStrengthDbm?.let { "$it dBm" } ?: "N/A", Icons.Default.SignalWifi4Bar),
                )) { item ->
                    StatCard(item)
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = onSpeedTestClick) {
                    Text("Speed Test")
                }
                Button(onClick = onPingTestClick) {
                    Text("Ping Test")
                }
                OutlinedButton(onClick = onDetailsClick) {
                    Text("Details")
                }
            }
        }
    }
}

@Composable
fun StatCard(item: StatItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(TvSurfaceVariant, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.tv.material3.Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(item.label, style = MaterialTheme.typography.bodyLarge, color = TvOnSurfaceVariant)
            }
            Text(item.value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

data class StatItem(val label: String, val value: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun SourceLockSettingsCard(
    enabled: Boolean,
    fallbackMode: Int,
    maxRetries: Int,
    retryDelayMs: Long,
    persistAcrossSessions: Boolean,
    notifyOnFallback: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onFallbackModeChange: (Int) -> Unit,
    onMaxRetriesChange: (Int) -> Unit,
    onRetryDelayChange: (Long) -> Unit,
    onPersistChange: (Boolean) -> Unit,
    onNotifyChange: (Boolean) -> Unit,
    onClearAll: () -> Unit,
) {
    FocusableCard(modifier = Modifier.fillMaxWidth(), onClick = { }) {
        Column(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsToggle(
                title = "Enable Source Lock",
                subtitle = "Remember source provider for series and reuse for subsequent episodes",
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )

            SettingsToggle(
                title = "Persist Across Sessions",
                subtitle = "Keep source locks after app restart",
                checked = persistAcrossSessions,
                onCheckedChange = onPersistChange,
                enabled = enabled,
            )

            SettingsToggle(
                title = "Notify on Fallback",
                subtitle = "Show toast when falling back to another source",
                checked = notifyOnFallback,
                onCheckedChange = onNotifyChange,
                enabled = enabled,
            )

            ListItem(
                headlineContent = { Text("Fallback Behavior", style = MaterialTheme.typography.bodyLarge) },
                supportingContent = { Text("What to do when locked source becomes unavailable", style = MaterialTheme.typography.bodyMedium, color = TvOnSurfaceVariant) },
                trailingContent = {
                    DropdownMenuButton(
                        text = when (fallbackMode) { 0 -> "Automatic" else -> "Manual" },
                        onClick = { /* Show picker */ }
                    )
                },
                modifier = Modifier.fillMaxWidth().background(TvSurfaceVariant).padding(16.dp),
                enabled = enabled,
            )

            ListItem(
                headlineContent = { Text("Max Retries", style = MaterialTheme.typography.bodyLarge) },
                supportingContent = { Text("Number of retry attempts before fallback", style = MaterialTheme.typography.bodyMedium, color = TvOnSurfaceVariant) },
                trailingContent = {
                    Text("$maxRetries", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.fillMaxWidth().background(TvSurfaceVariant).padding(16.dp),
                enabled = enabled,
            )

            ListItem(
                headlineContent = { Text("Retry Delay", style = MaterialTheme.typography.bodyLarge) },
                supportingContent = { Text("Delay between retries (ms)", style = MaterialTheme.typography.bodyMedium, color = TvOnSurfaceVariant) },
                trailingContent = {
                    Text("${retryDelayMs}ms", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.fillMaxWidth().background(TvSurfaceVariant).padding(16.dp),
                enabled = enabled,
            )

            ListItem(
                headlineContent = { Text("Clear All Source Locks", style = MaterialTheme.typography.bodyLarge) },
                supportingContent = { Text("Remove all remembered source preferences", style = MaterialTheme.typography.bodyMedium, color = TvOnSurfaceVariant) },
                trailingContent = {
                    TextButton(onClick = onClearAll) { Text("Clear") }
                },
                modifier = Modifier.fillMaxWidth().background(TvSurfaceVariant).padding(16.dp),
                enabled = enabled,
            )
        }
    }
}

@Composable
fun MetadataProvidersCard(
    providers: List<MetadataProvider>,
    onProviderToggle: (String, Boolean) -> Unit,
    onPriorityChange: (String, Int) -> Unit,
    onRefreshAll: () -> Unit,
) {
    FocusableCard(modifier = Modifier.fillMaxWidth(), onClick = { }) {
        Column(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enabled Providers", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onRefreshAll) { Text("Refresh All") }
            }

            providers.forEach { provider ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TvSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Provider icon/color indicator
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(provider.color)
                        )
                        Column {
                            Text(provider.name, style = MaterialTheme.typography.bodyLarge)
                            Text("Priority: ${provider.priority}", style = MaterialTheme.typography.bodySmall, color = TvOnSurfaceVariant)
                        }
                    }

                    Switch(
                        checked = provider.isEnabled,
                        onCheckedChange = { onProviderToggle(provider.id, it) },
                    )
                }
            }
        }
    }
}

@Composable
fun CustomHomeRowsCard(
    rows: List<CustomHomeRow>,
    onAddRow: () -> Unit,
    onEditRow: (CustomHomeRow) -> Unit,
    onDeleteRow: (String) -> Unit,
    onReorderRows: (List<String>) -> Unit,
    onToggleVisibility: (String, Boolean) -> Unit,
    onPreviewRow: (CustomHomeRow) -> Unit,
) {
    FocusableCard(modifier = Modifier.fillMaxWidth(), onClick = { }) {
        Column(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Custom Rows", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onAddRow) {
                    androidx.tv.material3.Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add Row")
                }
            }

            if (rows.isEmpty()) {
                Text("No custom rows created yet", style = MaterialTheme.typography.bodyMedium, color = TvOnSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp))
            } else {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TvSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.tv.material3.Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(row.title, style = MaterialTheme.typography.bodyLarge)
                                Text(row.filter.summary, style = MaterialTheme.typography.bodySmall, color = TvOnSurfaceVariant)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Switch(
                                checked = row.isVisible,
                                onCheckedChange = { onToggleVisibility(row.id, it) },
                            )
                            IconButton(onClick = { onPreviewRow(row) }) {
                                androidx.tv.material3.Icon(Icons.Default.Visibility, contentDescription = "Preview")
                            }
                            IconButton(onClick = { onEditRow(row) }) {
                                androidx.tv.material3.Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { onDeleteRow(row.id) }) {
                                androidx.tv.material3.Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaybackEnhancementsCard(
    autoPlayNext: Boolean,
    skipIntro: Boolean,
    hardwareAcceleration: Boolean,
    backgroundPlayback: Boolean,
    cinematicMode: Boolean,
    ambientMode: Boolean,
    onAutoPlayNextChange: (Boolean) -> Unit,
    onSkipIntroChange: (Boolean) -> Unit,
    onHardwareAccelerationChange: (Boolean) -> Unit,
    onBackgroundPlaybackChange: (Boolean) -> Unit,
    onCinematicModeChange: (Boolean) -> Unit,
    onAmbientModeChange: (Boolean) -> Unit,
) {
    FocusableCard(modifier = Modifier.fillMaxWidth(), onClick = { }) {
        Column(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsToggle("Auto-play Next Episode", "Automatically continue to the next episode", autoPlayNext, onAutoPlayNextChange)
            SettingsToggle("Skip Intro/Outro", "Automatically skip opening and ending sequences when detected", skipIntro, onSkipIntroChange)
            SettingsToggle("Hardware Acceleration", "Use hardware decoder for better performance (may cause issues on some devices)", hardwareAcceleration, onHardwareAccelerationChange)
            SettingsToggle("Background Playback", "Continue audio playback when app is in background", backgroundPlayback, onBackgroundPlaybackChange)
            SettingsToggle("Cinematic Mode", "Auto-hide UI after 5 seconds of inactivity during playback", cinematicMode, onCinematicModeChange)
            SettingsToggle("Ambient Mode", "Show subtle color glow based on content artwork", ambientMode, onAmbientModeChange)
        }
    }
}

@Composable
fun AiFeaturesCard(
    offlineTranslation: Boolean,
    predictivePrecache: Boolean,
    aiUpscaling: Boolean,
    frameInterpolation: Boolean,
    onOfflineTranslationChange: (Boolean) -> Unit,
    onPredictivePrecacheChange: (Boolean) -> Unit,
    onAiUpscalingChange: (Boolean) -> Unit,
    onFrameInterpolationChange: (Boolean) -> Unit,
) {
    FocusableCard(modifier = Modifier.fillMaxWidth(), onClick = { }) {
        Column(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsToggle("Offline Subtitle Translation", "Translate subtitles locally using on-device AI model (<30MB)", offlineTranslation, onOfflineTranslationChange)
            SettingsToggle("Predictive Pre-caching", "Pre-load next episode based on viewing patterns", predictivePrecache, onPredictivePrecacheChange)
            SettingsToggle("AI Upscaling", "Enhance video quality using on-device upscaling (requires supported hardware)", aiUpscaling, onAiUpscalingChange)
            SettingsToggle("Frame Interpolation", "Generate intermediate frames for smoother motion (requires supported hardware)", frameInterpolation, onFrameInterpolationChange)
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TvOnSurfaceVariant) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .background(TvSurfaceVariant)
            .padding(16.dp),
        enabled = enabled,
    )
}

@Composable
fun DropdownMenuButton(
    text: String,
    onClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        androidx.tv.material3.Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun DiskBufferSettingsCard(
    bufferSizeMb: Int,
    readAheadMb: Int,
    bufferLocation: String,
    deleteOnShutdown: Boolean,
    onBufferSizeChange: (Int) -> Unit,
    onReadAheadChange: (Int) -> Unit,
    onLocationChange: (String) -> Unit,
    onDeleteOnShutdownChange: (Boolean) -> Unit,
) {
    FocusableCard(modifier = Modifier.fillMaxWidth(), onClick = { }) {
        Column(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsToggle(
                title = "Enable Disk Buffer",
                subtitle = "Store playback buffer on disk instead of RAM (reduces memory usage)",
                checked = true,
                onCheckedChange = { /* Always enabled for now */ },
                enabled = false,
            )

            ListItem(
                headlineContent = { Text("Buffer Size", style = MaterialTheme.typography.bodyLarge) },
                supportingContent = { Text("Total disk space allocated for playback buffer", style = MaterialTheme.typography.bodyMedium, color = TvOnSurfaceVariant) },
                trailingContent = {
                    Text("${bufferSizeMb} MB", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvSurfaceVariant)
                    .padding(16.dp),
                enabled = true,
            )

            ListItem(
                headlineContent = { Text("Read-Ahead Size", style = MaterialTheme.typography.bodyLarge) },
                supportingContent = { Text("In-memory read-ahead window for smooth seeking", style = MaterialTheme.typography.bodyMedium, color = TvOnSurfaceVariant) },
                trailingContent = {
                    Text("${readAheadMb} MB", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvSurfaceVariant)
                    .padding(16.dp),
                enabled = true,
            )

            ListItem(
                headlineContent = { Text("Buffer Location", style = MaterialTheme.typography.bodyLarge) },
                supportingContent = { Text("Where to store the disk buffer file", style = MaterialTheme.typography.bodyMedium, color = TvOnSurfaceVariant) },
                trailingContent = {
                    DropdownMenuButton(
                        text = when (bufferLocation) {
                            "internal" -> "Internal Storage"
                            "external" -> "External Storage (SD Card)"
                            else -> "Internal Storage"
                        },
                        onClick = { /* Show picker */ }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvSurfaceVariant)
                    .padding(16.dp),
                enabled = true,
            )

            SettingsToggle(
                title = "Delete Buffer on Shutdown",
                subtitle = "Automatically remove buffer file when app closes",
                checked = deleteOnShutdown,
                onCheckedChange = onDeleteOnShutdownChange,
            )
        }
    }
}