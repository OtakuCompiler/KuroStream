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

package com.kurostream.torrent.ui

import android.content.Context
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kurostream.torrent.domain.*
import com.kurostream.torrent.ui.viewmodel.TorrentsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TorrentsScreen(
    onBackClick: () -> Unit,
    viewModel: TorrentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(48.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Torrents", style = MaterialTheme.typography.displaySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(onClick = { viewModel.showSettingsDialog() }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
                IconButton(onClick = { viewModel.showAddTorrentDialog() }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Torrent"
                    )
                }
                androidx.tv.material3.IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Global Stats
        GlobalStatsCard(stats = uiState.globalStats)

        Spacer(modifier = Modifier.height(16.dp))

        // Speed Limits
        if (uiState.globalStats != null) {
            SpeedLimitsCard(
                downloadLimit = uiState.sessionSettings?.downloadRateLimit ?: -1L,
                uploadLimit = uiState.sessionSettings?.uploadRateLimit ?: -1L,
                onDownloadLimitChange = { limit ->
                    viewModel.updateSessionSettings(
                        (uiState.sessionSettings ?: TorrentSessionSettings()).copy(
                            downloadRateLimit = limit
                        )
                    )
                },
                onUploadLimitChange = { limit ->
                    viewModel.updateSessionSettings(
                        (uiState.sessionSettings ?: TorrentSessionSettings()).copy(
                            uploadRateLimit = limit
                        )
                    )
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Torrent List
        if (uiState.torrents.isEmpty()) {
            EmptyState(onAddClick = { viewModel.showAddTorrentDialog() })
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.torrents.forEach { torrent ->
                    TorrentCard(
                        torrent = torrent,
                        onPause = { viewModel.pauseTorrent(it.infoHash) },
                        onResume = { viewModel.resumeTorrent(it.infoHash) },
                        onRemove = { viewModel.removeTorrent(it.infoHash, false) },
                        onRemoveWithFiles = { viewModel.removeTorrent(it.infoHash, true) },
                        onFilesClick = { viewModel.showFilesDialog(it) },
                        onSpeedLimitsClick = { viewModel.showSpeedLimitsDialog(it) },
                        onOpenPlayer = { viewModel.openStreamPlayer(it) },
                    )
                }
            }
        }

        // Error message
        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }

    // Dialogs
    if (uiState.showAddTorrentDialog) {
        AddTorrentDialog(
            onDismiss = { viewModel.dismissAddTorrentDialog() },
            onAddMagnet = { magnet, dir, sequential ->
                viewModel.addMagnet(magnet, dir, sequential)
            },
            onAddFile = { file, dir, sequential ->
                viewModel.addTorrentFile(file, dir, sequential)
            }
        )
    }

    if (uiState.showSettingsDialog) {
        TorrentSettingsDialog(
            settings = uiState.sessionSettings ?: TorrentSessionSettings(),
            onDismiss = { viewModel.dismissSettingsDialog() },
            onSave = { viewModel.updateSessionSettings(it) }
        )
    }

    uiState.filesDialogTorrent?.let { torrent ->
        TorrentFilesDialog(
            torrent = torrent,
            onDismiss = { viewModel.dismissFilesDialog() },
            onFilePriorityChange = { filePath, priority ->
                viewModel.setFilePriority(torrent.infoHash, filePath, priority)
            },
            onSequentialDownloadChange = { enabled ->
                viewModel.setSequentialDownload(torrent.infoHash, enabled)
            }
        )
    }

    uiState.speedLimitsTorrent?.let { torrent ->
        TorrentSpeedLimitsDialog(
            torrent = torrent,
            onDismiss = { viewModel.dismissSpeedLimitsDialog() },
            onLimitsChange = { download, upload ->
                viewModel.setTorrentSpeedLimits(torrent.infoHash, download, upload)
            }
        )
    }

    uiState.streamTorrent?.let { torrent ->
        TorrentStreamPlayerDialog(
            torrent = torrent,
            onDismiss = { viewModel.dismissStreamPlayer() }
        )
    }
}

@Composable
fun GlobalStatsCard(stats: GlobalStats?) {
    stats?.let {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Global Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem("Down", formatSpeed(it.totalDownloadSpeed), Icons.Default.ArrowDownward)
                    StatItem("Up", formatSpeed(it.totalUploadSpeed), Icons.Default.ArrowUpward)
                    StatItem("Active", it.activeTorrents.toString(), Icons.Default.PlayArrow)
                    StatItem("Seeding", it.pausedTorrents.toString(), Icons.Default.Upload)
                    StatItem("DHT", it.dhtNodes.toString(), Icons.Default.Router)
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SpeedLimitsCard(
    downloadLimit: Long,
    uploadLimit: Long,
    onDownloadLimitChange: (Long) -> Unit,
    onUploadLimitChange: (Long) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Speed Limits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SpeedLimitInput(
                    label = "Download (KB/s)",
                    value = if (downloadLimit > 0) downloadLimit else 0L,
                    onValueChange = { onDownloadLimitChange(if (it == 0L) -1L else it) },
                    unlimited = downloadLimit <= 0
                )
                SpeedLimitInput(
                    label = "Upload (KB/s)",
                    value = if (uploadLimit > 0) uploadLimit else 0L,
                    onValueChange = { onUploadLimitChange(if (it == 0L) -1L else it) },
                    unlimited = uploadLimit <= 0
                )
            }
        }
    }
}

@Composable
fun SpeedLimitInput(
    label: String,
    value: Long,
    onValueChange: (Long) -> Unit,
    unlimited: Boolean
) {
    var text by remember { mutableStateOf(if (value > 0) value.toString() else "") }
    Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = text,
                onValueChange = { newText ->
                    text = newText
                    onValueChange(newText.toLongOrNull() ?: 0L)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(end = 8.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Switch(
                checked = unlimited,
                onCheckedChange = { checked ->
                    if (checked) {
                        text = ""
                        onValueChange(-1L)
                    } else {
                        onValueChange(1024)
                    }
                }
            )
        }
    }
}

@Composable
fun EmptyState(onAddClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No Torrents",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Add a magnet link or torrent file to start downloading",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAddClick) {
                Text("Add Torrent")
            }
        }
    }
}

@Composable
fun TorrentCard(
    torrent: TorrentInfo,
    onPause: (TorrentInfo) -> Unit,
    onResume: (TorrentInfo) -> Unit,
    onRemove: (TorrentInfo) -> Unit,
    onRemoveWithFiles: (TorrentInfo) -> Unit,
    onFilesClick: (TorrentInfo) -> Unit,
    onSpeedLimitsClick: (TorrentInfo) -> Unit,
    onOpenPlayer: (TorrentInfo) -> Unit,
) {
    val statusColor = when (torrent.status) {
        TorrentStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
        TorrentStatus.SEEDING -> Color.Green
        TorrentStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
        TorrentStatus.ERROR -> MaterialTheme.colorScheme.error
        TorrentStatus.FINISHED -> Color.Green
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        torrent.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (torrent.status) {
                                TorrentStatus.DOWNLOADING -> Icons.Default.ArrowDownward
                                TorrentStatus.SEEDING -> Icons.Default.ArrowUpward
                                TorrentStatus.PAUSED -> Icons.Default.Pause
                                TorrentStatus.ERROR -> Icons.Default.Error
                                TorrentStatus.FINISHED -> Icons.Default.CheckCircle
                                else -> Icons.Default.HourglassEmpty
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            torrent.status.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { onFilesClick(torrent) }) {
                        Icon(Icons.Default.Folder, "Files")
                    }
                    IconButton(onClick = { onSpeedLimitsClick(torrent) }) {
                        Icon(Icons.Default.Speed, "Speed Limits")
                    }
                    IconButton(onClick = { onOpenPlayer(torrent) }, enabled = torrent.progress >= 10f) {
                        Icon(Icons.Default.PlayCircle, "Play")
                    }
                    MenuButton(torrent, onPause, onResume, onRemove, onRemoveWithFiles)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${String.format("%.1f", torrent.progress)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "↓ ${formatSpeed(torrent.downloadSpeed)}  ↑ ${formatSpeed(torrent.uploadSpeed)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = torrent.progress / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Peers: ${torrent.peers}  Seeds: ${torrent.seeds}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (torrent.eta > 0) "ETA: ${formatDuration(torrent.eta)}" else "∞",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // File list preview (first 3 media files)
            if (torrent.files.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    torrent.files
                        .filter { it.isMediaFile }
                        .take(3)
                        .forEach { file ->
                            FileRow(file = file)
                        }
                }
            }
        }
    }
}

@Composable
fun MenuButton(
    torrent: TorrentInfo,
    onPause: (TorrentInfo) -> Unit,
    onResume: (TorrentInfo) -> Unit,
    onRemove: (TorrentInfo) -> Unit,
    onRemoveWithFiles: (TorrentInfo) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        if (torrent.status == TorrentStatus.DOWNLOADING || torrent.status == TorrentStatus.SEEDING) {
            DropdownMenuItem(
                text = { Text("Pause") },
                onClick = { onPause(torrent); expanded = false }
            )
        } else if (torrent.status == TorrentStatus.PAUSED) {
            DropdownMenuItem(
                text = { Text("Resume") },
                onClick = { onResume(torrent); expanded = false }
            )
        }
        DropdownMenuItem(
            text = { Text("Remove (Keep Files)") },
            onClick = { onRemove(torrent); expanded = false }
        )
        DropdownMenuItem(
            text = { Text("Remove (Delete Files)") },
            onClick = { onRemoveWithFiles(torrent); expanded = false },
            contentColor = MaterialTheme.colorScheme.error
        )
        DropdownMenuItem(
            text = { Text("Reannounce") },
            onClick = { /* TODO: reannounce */ expanded = false }
        )
        DropdownMenuItem(
            text = { Text("Force Recheck") },
            onClick = { /* TODO: force recheck */ expanded = false }
        )
    }
    IconButton(onClick = { expanded = !expanded }) {
        Icon(Icons.Default.MoreVert, "More options")
    }
}

@Composable
fun FileRow(file: TorrentFile) {
    val priorityColor = when (file.priority) {
        FilePriority.HIGH -> Color.Green
        FilePriority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
        FilePriority.DONT_DOWNLOAD -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (file.isMediaFile) Icons.Default.PlayCircle else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = priorityColor,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(file.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "Priority: ${file.priority.name} • ${formatSize(file.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${String.format("%.1f", file.progress * 100)}%", style = MaterialTheme.typography.bodySmall, color = priorityColor)
            LinearProgressIndicator(
                progress = file.progress,
                modifier = Modifier.width(100.dp),
                color = priorityColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun AddTorrentDialog(
    onDismiss: () -> Unit,
    onAddMagnet: (String, String, Boolean) -> Unit,
    onAddFile: (String, String, Boolean) -> Unit,
) {
    var magnetText by remember { mutableStateOf("") }
    var filePath by remember { mutableStateOf("") }
    var downloadDir by remember { mutableStateOf("/storage/emulated/0/Download") }
    var sequentialDownload by remember { mutableStateOf(false) }
    var tabIndex by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(500.dp)
            .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Add Torrent", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                // Tab Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Tab(text = "Magnet Link", selected = tabIndex == 0, onClick = { tabIndex = 0 })
                    Tab(text = "Torrent File", selected = tabIndex == 1, onClick = { tabIndex = 1 })
                }

                if (tabIndex == 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = magnetText,
                            onValueChange = { magnetText = it },
                            label = { Text("Magnet URI") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            minLines = 3,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedTextField(
                                value = downloadDir,
                                onValueChange = { downloadDir = it },
                                label = { Text("Download Directory") },
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = sequentialDownload,
                                onCheckedChange = { sequentialDownload = it }
                            )
                            Text("Sequential Download")
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = filePath,
                            onValueChange = { filePath = it },
                            label = { Text("Torrent File Path") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { /* Pick file */ }) {
                                    Icon(Icons.Default.FolderOpen, "Pick file")
                                }
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedTextField(
                                value = downloadDir,
                                onValueChange = { downloadDir = it },
                                label = { Text("Download Directory") },
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = sequentialDownload,
                                onCheckedChange = { sequentialDownload = it }
                            )
                            Text("Sequential Download")
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (tabIndex == 0 && magnetText.isNotBlank()) {
                                onAddMagnet(magnetText, downloadDir, sequentialDownload)
                            } else if (tabIndex == 1 && filePath.isNotBlank()) {
                                onAddFile(filePath, downloadDir, sequentialDownload)
                            }
                        },
                        enabled = (tabIndex == 0 && magnetText.isNotBlank()) || (tabIndex == 1 && filePath.isNotBlank())
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
fun TorrentSettingsDialog(
    settings: TorrentSessionSettings,
    onDismiss: () -> Unit,
    onSave: (TorrentSessionSettings) -> Unit
) {
    var listenPort by remember { mutableStateOf(settings.listenPort) }
    var enableDht by remember { mutableStateOf(settings.enableDht) }
    var enableLsd by remember { mutableStateOf(settings.enableLsd) }
    var enableUpnp by remember { mutableStateOf(settings.enableUpnp) }
    var enableNatpmp by remember { mutableStateOf(settings.enableNatpmp) }
    var encryptionMode by remember { mutableStateOf(settings.encryptionMode) }
    var maxConnections by remember { mutableStateOf(settings.maxConnections) }
    var maxUploadSlots by remember { mutableStateOf(settings.maxUploadSlots) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(600.dp)
            .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Torrent Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                SettingsGroup("Network") {
                    SettingsItem("Listen Port") {
                        OutlinedTextField(
                            value = listenPort.toString(),
                            onValueChange = { listenPort = it.toIntOrNull() ?: 6881 },
                            modifier = Modifier.width(150.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                    SettingsToggle("Enable DHT", enableDht) { enableDht = it }
                    SettingsToggle("Enable LSD (Local Service Discovery)", enableLsd) { enableLsd = it }
                    SettingsToggle("Enable UPnP", enableUpnp) { enableUpnp = it }
                    SettingsToggle("Enable NAT-PMP", enableNatpmp) { enableNatpmp = it }
                }

                SettingsGroup("Encryption") {
                    SettingsDropdown("Encryption Mode", encryptionMode.name) {
                        DropdownMenuItem(text = { Text(EncryptionMode.NONE.name) }) { encryptionMode = EncryptionMode.NONE }
                        DropdownMenuItem(text = { Text(EncryptionMode.PREFER_ENCRYPTION.name) }) { encryptionMode = EncryptionMode.PREFER_ENCRYPTION }
                        DropdownMenuItem(text = { Text(EncryptionMode.REQUIRE_ENCRYPTION.name) }) { encryptionMode = EncryptionMode.REQUIRE_ENCRYPTION }
                        DropdownMenuItem(text = { Text(EncryptionMode.REQUIRE_RC4.name) }) { encryptionMode = EncryptionMode.REQUIRE_RC4 }
                    }
                }

                SettingsGroup("Connection Limits") {
                    SettingsItem("Max Connections") {
                        OutlinedTextField(
                            value = maxConnections.toString(),
                            onValueChange = { maxConnections = it.toIntOrNull() ?: 200 },
                            modifier = Modifier.width(150.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                    SettingsItem("Max Upload Slots") {
                        OutlinedTextField(
                            value = maxUploadSlots.toString(),
                            onValueChange = { maxUploadSlots = it.toIntOrNull() ?: 50 },
                            modifier = Modifier.width(150.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        onSave(settings.copy(
                            listenPort = listenPort,
                            enableDht = enableDht,
                            enableLsd = enableLsd,
                            enableUpnp = enableUpnp,
                            enableNatpmp = enableNatpmp,
                            encryptionMode = encryptionMode,
                            maxConnections = maxConnections,
                            maxUploadSlots = maxUploadSlots,
                        ))
                        onDismiss()
                    }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun TorrentFilesDialog(
    torrent: TorrentInfo,
    onDismiss: () -> Unit,
    onFilePriorityChange: (String, FilePriority) -> Unit,
    onSequentialDownloadChange: (Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.8f)
            .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Files: ${torrent.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row {
                        Switch(
                            checked = torrent.isSequentialDownload,
                            onCheckedChange = onSequentialDownloadChange
                        )
                        Text("Sequential Download")
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    torrent.files.forEach { file ->
                        FileRowWithPriority(
                            file = file,
                            onPriorityChange = { priority ->
                                onFilePriorityChange(file.path, priority)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileRowWithPriority(
    file: TorrentFile,
    onPriorityChange: (FilePriority) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (file.isMediaFile) Icons.Default.PlayCircle else Icons.Default.InsertDriveFile,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(file.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "Priority: ${file.priority.name} • ${formatSize(file.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DropdownMenuButton(file.priority.name) {
                DropdownMenuItem(text = { Text(FilePriority.HIGH.name) }) { onPriorityChange(FilePriority.HIGH) }
                DropdownMenuItem(text = { Text(FilePriority.NORMAL.name) }) { onPriorityChange(FilePriority.NORMAL) }
                DropdownMenuItem(text = { Text(FilePriority.LOW.name) }) { onPriorityChange(FilePriority.LOW) }
                DropdownMenuItem(text = { Text(FilePriority.DONT_DOWNLOAD.name) }) { onPriorityChange(FilePriority.DONT_DOWNLOAD) }
            }
            Text("${String.format("%.1f", file.progress * 100)}%", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TorrentSpeedLimitsDialog(
    torrent: TorrentInfo,
    onDismiss: () -> Unit,
    onLimitsChange: (Long, Long) -> Unit
) {
    var downloadLimit by remember { mutableStateOf(torrent.downloadLimit) }
    var uploadLimit by remember { mutableStateOf(torrent.uploadLimit) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier
            .fillMaxWidth(0.7f)
            .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Speed Limits: ${torrent.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                SpeedLimitInput(
                    label = "Download Limit (KB/s, 0 = unlimited)",
                    value = if (downloadLimit > 0) downloadLimit / 1024 else 0L,
                    onValueChange = { downloadLimit = if (it == 0L) -1L else it * 1024 },
                    unlimited = downloadLimit <= 0
                )
                SpeedLimitInput(
                    label = "Upload Limit (KB/s, 0 = unlimited)",
                    value = if (uploadLimit > 0) uploadLimit / 1024 else 0L,
                    onValueChange = { uploadLimit = if (it == 0L) -1L else it * 1024 },
                    unlimited = uploadLimit <= 0
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onLimitsChange(downloadLimit, uploadLimit); onDismiss() }) { Text("Apply") }
                }
            }
        }
    }
}

@Composable
fun TorrentStreamPlayerDialog(
    torrent: TorrentInfo,
    onDismiss: () -> Unit
) {
    // This would integrate with the KuroEngine player
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.9f)
            .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text("Stream Player", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Integration with KuroEngine player", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

// Helper composables
@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
fun SettingsItem(title: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@Composable
fun SettingsToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsDropdown(title: String, selectedValue: String, items: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        DropdownMenuButton(text = selectedValue, onClick = { expanded = !expanded })
    }
    if (expanded) {
        DropdownMenu(expanded = true, onDismissRequest = { expanded = false }) {
            items()
        }
    }
}

@Composable
fun DropdownMenuButton(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.End
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Tab(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable { onClick() }
            .wrapContentSize()
    )
}

// Formatters
fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec < 1024 -> "${bytesPerSec} B/s"
        bytesPerSec < 1024 * 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
        bytesPerSec < 1024 * 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024))
        else -> String.format("%.1f GB/s", bytesPerSec / (1024.0 * 1024 * 1024))
    }
}

fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

fun formatDuration(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        seconds < 86400 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        else -> "${seconds / 86400}d ${(seconds % 86400) / 3600}h"
    }
}