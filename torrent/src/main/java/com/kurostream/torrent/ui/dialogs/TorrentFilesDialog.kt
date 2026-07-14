package com.kurostream.torrent.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kurostream.torrent.domain.FilePriority
import com.kurostream.torrent.domain.TorrentInfo

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
    file: com.kurostream.torrent.domain.TorrentFile,
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
                    "Priority: ${file.priority.name} \u2022 ${formatSize(file.size)}",
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

fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}
