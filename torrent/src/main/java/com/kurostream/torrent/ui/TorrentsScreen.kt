package com.kurostream.torrent.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kurostream.torrent.domain.*
import com.kurostream.torrent.ui.dialogs.*
import com.kurostream.torrent.ui.viewmodel.TorrentsViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TorrentsScreen(
    onBackClick: () -> Unit,
    viewModel: TorrentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Torrents", style = MaterialTheme.typography.displaySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(onClick = { viewModel.showSettingsDialog() }) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                }
                IconButton(onClick = { viewModel.showAddTorrentDialog() }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Torrent")
                }
                androidx.tv.material3.IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        GlobalStatsCard(stats = uiState.globalStats)
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.globalStats != null) {
            SpeedLimitsCard(
                downloadLimit = uiState.sessionSettings?.downloadRateLimit ?: -1L,
                uploadLimit = uiState.sessionSettings?.uploadRateLimit ?: -1L,
                onDownloadLimitChange = { viewModel.updateSessionSettings((uiState.sessionSettings ?: TorrentSessionSettings()).copy(downloadRateLimit = it)) },
                onUploadLimitChange = { viewModel.updateSessionSettings((uiState.sessionSettings ?: TorrentSessionSettings()).copy(uploadRateLimit = it)) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.torrents.isEmpty()) {
            EmptyState(onAddClick = { viewModel.showAddTorrentDialog() })
        } else {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = { viewModel.clearError() }) { Text("Dismiss") }
                }
            }
        }
    }

    if (uiState.showAddTorrentDialog) {
        AddTorrentDialog(
            onDismiss = { viewModel.dismissAddTorrentDialog() },
            onAddMagnet = { magnet, dir, sequential -> viewModel.addMagnet(magnet, dir, sequential) },
            onAddFile = { file, dir, sequential -> viewModel.addTorrentFile(file, dir, sequential) }
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
            onFilePriorityChange = { filePath, priority -> viewModel.setFilePriority(torrent.infoHash, filePath, priority) },
            onSequentialDownloadChange = { viewModel.setSequentialDownload(torrent.infoHash, it) }
        )
    }

    uiState.speedLimitsTorrent?.let { torrent ->
        TorrentSpeedLimitsDialog(
            torrent = torrent,
            onDismiss = { viewModel.dismissSpeedLimitsDialog() },
            onLimitsChange = { download, upload -> viewModel.setTorrentSpeedLimits(torrent.infoHash, download, upload) }
        )
    }

    uiState.streamTorrent?.let { torrent ->
        TorrentStreamPlayerDialog(torrent = torrent, onDismiss = { viewModel.dismissStreamPlayer() })
    }
}

// --- Main Screen Composables ---

@Composable
fun GlobalStatsCard(stats: GlobalStats?) {
    stats?.let {
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Global Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
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
fun SpeedLimitsCard(downloadLimit: Long, uploadLimit: Long, onDownloadLimitChange: (Long) -> Unit, onUploadLimitChange: (Long) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Speed Limits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                SpeedLimitInput(label = "Download (KB/s)", value = if (downloadLimit > 0) downloadLimit else 0L, onValueChange = { onDownloadLimitChange(if (it == 0L) -1L else it) }, unlimited = downloadLimit <= 0)
                SpeedLimitInput(label = "Upload (KB/s)", value = if (uploadLimit > 0) uploadLimit else 0L, onValueChange = { onUploadLimitChange(if (it == 0L) -1L else it) }, unlimited = uploadLimit <= 0)
            }
        }
    }
}

@Composable
fun SpeedLimitInput(label: String, value: Long, onValueChange: (Long) -> Unit, unlimited: Boolean) {
    var text by remember { mutableStateOf(if (value > 0) value.toString() else "") }
    Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            TextField(value = text, onValueChange = { text = it; onValueChange(it.toLongOrNull() ?: 0L) }, modifier = Modifier.fillMaxWidth().weight(1f).padding(end = 8.dp), singleLine = true)
            Switch(checked = unlimited, onCheckedChange = { if (it) { text = ""; onValueChange(-1L) } else onValueChange(1024) })
        }
    }
}

@Composable
fun EmptyState(onAddClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Text("No Torrents", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Add a magnet link or torrent file to start downloading", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAddClick) { Text("Add Torrent") }
        }
    }
}

@Composable
fun TorrentCard(torrent: TorrentInfo, onPause: (TorrentInfo) -> Unit, onResume: (TorrentInfo) -> Unit, onRemove: (TorrentInfo) -> Unit, onRemoveWithFiles: (TorrentInfo) -> Unit, onFilesClick: (TorrentInfo) -> Unit, onSpeedLimitsClick: (TorrentInfo) -> Unit, onOpenPlayer: (TorrentInfo) -> Unit) {
    val statusColor = when (torrent.status) {
        TorrentStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
        TorrentStatus.SEEDING -> Color.Green
        TorrentStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
        TorrentStatus.ERROR -> MaterialTheme.colorScheme.error
        TorrentStatus.FINISHED -> Color.Green
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(torrent.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = when (torrent.status) { TorrentStatus.DOWNLOADING -> Icons.Default.ArrowDownward; TorrentStatus.SEEDING -> Icons.Default.ArrowUpward; TorrentStatus.PAUSED -> Icons.Default.Pause; TorrentStatus.ERROR -> Icons.Default.Error; TorrentStatus.FINISHED -> Icons.Default.CheckCircle; else -> Icons.Default.HourglassEmpty }, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(torrent.status.name, style = MaterialTheme.typography.bodySmall, color = statusColor, fontWeight = FontWeight.Bold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { onFilesClick(torrent) }) { Icon(Icons.Default.Folder, "Files") }
                    IconButton(onClick = { onSpeedLimitsClick(torrent) }) { Icon(Icons.Default.Speed, "Speed Limits") }
                    IconButton(onClick = { onOpenPlayer(torrent) }, enabled = torrent.progress >= 10f) { Icon(Icons.Default.PlayCircle, "Play") }
                    MenuButton(torrent, onPause, onResume, onRemove, onRemoveWithFiles)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${String.format("%.1f", torrent.progress)}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("\u2193 ${formatSpeed(torrent.downloadSpeed)}  \u2191 ${formatSpeed(torrent.uploadSpeed)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(progress = torrent.progress / 100f, modifier = Modifier.fillMaxWidth(), color = statusColor, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Peers: ${torrent.peers}  Seeds: ${torrent.seeds}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (torrent.eta > 0) "ETA: ${formatDuration(torrent.eta)}" else "\u221E", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (torrent.files.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    torrent.files.filter { it.isMediaFile }.take(3).forEach { file -> FileRow(file = file) }
                }
            }
        }
    }
}

@Composable
fun MenuButton(torrent: TorrentInfo, onPause: (TorrentInfo) -> Unit, onResume: (TorrentInfo) -> Unit, onRemove: (TorrentInfo) -> Unit, onRemoveWithFiles: (TorrentInfo) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        if (torrent.status == TorrentStatus.DOWNLOADING || torrent.status == TorrentStatus.SEEDING) {
            DropdownMenuItem(text = { Text("Pause") }, onClick = { onPause(torrent); expanded = false })
        } else if (torrent.status == TorrentStatus.PAUSED) {
            DropdownMenuItem(text = { Text("Resume") }, onClick = { onResume(torrent); expanded = false })
        }
        DropdownMenuItem(text = { Text("Remove (Keep Files)") }, onClick = { onRemove(torrent); expanded = false })
        DropdownMenuItem(text = { Text("Remove (Delete Files)") }, onClick = { onRemoveWithFiles(torrent); expanded = false }, contentColor = MaterialTheme.colorScheme.error)
        DropdownMenuItem(text = { Text("Reannounce") }, onClick = { expanded = false })
        DropdownMenuItem(text = { Text("Force Recheck") }, onClick = { expanded = false })
    }
    IconButton(onClick = { expanded = !expanded }) { Icon(Icons.Default.MoreVert, "More options") }
}

@Composable
fun FileRow(file: TorrentFile) {
    val priorityColor = when (file.priority) { FilePriority.HIGH -> Color.Green; FilePriority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant; FilePriority.DONT_DOWNLOAD -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.onSurface }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = if (file.isMediaFile) Icons.Default.PlayCircle else Icons.Default.InsertDriveFile, contentDescription = null, tint = priorityColor, modifier = Modifier.size(20.dp))
            Column {
                Text(file.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Priority: ${file.priority.name} \u2022 ${formatSize(file.size)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${String.format("%.1f", file.progress * 100)}%", style = MaterialTheme.typography.bodySmall, color = priorityColor)
            LinearProgressIndicator(progress = file.progress, modifier = Modifier.width(100.dp), color = priorityColor, trackColor = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

fun formatSpeed(bytesPerSec: Long): String = when {
    bytesPerSec < 1024 -> "${bytesPerSec} B/s"
    bytesPerSec < 1024 * 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
    bytesPerSec < 1024 * 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024))
    else -> String.format("%.1f GB/s", bytesPerSec / (1024.0 * 1024 * 1024))
}

fun formatDuration(seconds: Long): String = when {
    seconds < 60 -> "${seconds}s"
    seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
    seconds < 86400 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    else -> "${seconds / 86400}d ${(seconds % 86400) / 3600}h"
}
