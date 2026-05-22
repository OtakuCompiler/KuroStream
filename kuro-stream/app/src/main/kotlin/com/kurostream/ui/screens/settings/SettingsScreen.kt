package com.kurostream.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kurostream.data.model.Plugin
import com.kurostream.ui.components.FocusableCard
import com.kurostream.ui.theme.*

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddPluginDialog by remember { mutableStateOf(false) }
    var newPluginUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KuroBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FocusableCard(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = KuroOnSurface, modifier = Modifier.padding(10.dp))
            }
            Text("Settings", style = MaterialTheme.typography.headlineMedium, color = KuroOnSurface)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SectionHeader(title = "Plugins")
            }

            item {
                FocusableCard(
                    onClick = { showAddPluginDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.Add, "Add plugin", tint = KuroPrimary)
                        Column {
                            Text("Add Plugin", style = MaterialTheme.typography.bodyLarge, color = KuroOnSurface)
                            Text(
                                "Stremio, CloudStream, AIOMetadata, Nuvio",
                                style = MaterialTheme.typography.bodySmall,
                                color = KuroOnSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (uiState.plugins.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No plugins installed yet.", color = KuroOnSurfaceVariant)
                    }
                }
            }

            items(uiState.plugins) { plugin ->
                PluginCard(
                    plugin = plugin,
                    onToggle = { viewModel.togglePlugin(plugin.id, !plugin.isEnabled) },
                    onDelete = { viewModel.removePlugin(plugin.id) }
                )
            }

            item { SectionHeader(title = "Playback") }

            item {
                SettingRow(
                    title = "Default Resolution",
                    subtitle = uiState.defaultResolution,
                    icon = Icons.Default.HdVideoCall,
                    onClick = { viewModel.cycleResolution() }
                )
            }

            item {
                SettingRow(
                    title = "Memory Pressure",
                    subtitle = uiState.memoryStatus,
                    icon = Icons.Default.Memory,
                    onClick = {}
                )
            }
        }
    }

    if (showAddPluginDialog) {
        AlertDialog(
            onDismissRequest = { showAddPluginDialog = false },
            title = { Text("Add Plugin") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste a Stremio manifest URL, CloudStream plugin URL, or AIOMetadata endpoint:")
                    OutlinedTextField(
                        value = newPluginUrl,
                        onValueChange = { newPluginUrl = it },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    uiState.addError?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPluginUrl.isNotBlank()) {
                            viewModel.addPlugin(newPluginUrl) { success ->
                                if (success) {
                                    showAddPluginDialog = false
                                    newPluginUrl = ""
                                }
                            }
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPluginDialog = false; newPluginUrl = "" }) {
                    Text("Cancel")
                }
            },
            containerColor = KuroSurface
        )
    }
}

@Composable
private fun PluginCard(
    plugin: Plugin,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    FocusableCard(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(plugin.name, style = MaterialTheme.typography.bodyLarge, color = KuroOnSurface)
                Text(
                    "${plugin.type.name} • v${plugin.version}",
                    style = MaterialTheme.typography.bodySmall,
                    color = KuroOnSurfaceVariant
                )
                if (plugin.description.isNotBlank()) {
                    Text(
                        plugin.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = KuroOnSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = plugin.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedThumbColor = KuroPrimary, checkedTrackColor = KuroPrimary.copy(alpha = 0.5f))
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Remove plugin", tint = KuroError)
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    FocusableCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, null, tint = KuroPrimary)
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = KuroOnSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = KuroOnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = KuroPrimary
    )
    HorizontalDivider(color = KuroSurfaceVariant)
}
