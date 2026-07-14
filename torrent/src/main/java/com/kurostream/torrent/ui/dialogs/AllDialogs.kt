package com.kurostream.torrent.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kurostream.torrent.domain.*

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
        Card(modifier = Modifier.fillMaxWidth(0.9f).height(500.dp).padding(16.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Add Torrent", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogTab("Magnet Link", tabIndex == 0) { tabIndex = 0 }
                    DialogTab("Torrent File", tabIndex == 1) { tabIndex = 1 }
                }
                if (tabIndex == 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = magnetText, onValueChange = { magnetText = it }, label = { Text("Magnet URI") }, modifier = Modifier.fillMaxWidth(), singleLine = false, minLines = 3)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            OutlinedTextField(value = downloadDir, onValueChange = { downloadDir = it }, label = { Text("Download Directory") }, modifier = Modifier.weight(1f))
                            Switch(checked = sequentialDownload, onCheckedChange = { sequentialDownload = it })
                            Text("Sequential Download")
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = filePath, onValueChange = { filePath = it }, label = { Text("Torrent File Path") }, modifier = Modifier.fillMaxWidth())
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            OutlinedTextField(value = downloadDir, onValueChange = { downloadDir = it }, label = { Text("Download Directory") }, modifier = Modifier.weight(1f))
                            Switch(checked = sequentialDownload, onCheckedChange = { sequentialDownload = it })
                            Text("Sequential Download")
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        if (tabIndex == 0 && magnetText.isNotBlank()) onAddMagnet(magnetText, downloadDir, sequentialDownload)
                        else if (tabIndex == 1 && filePath.isNotBlank()) onAddFile(filePath, downloadDir, sequentialDownload)
                    }, enabled = (tabIndex == 0 && magnetText.isNotBlank()) || (tabIndex == 1 && filePath.isNotBlank())) { Text("Add") }
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
        Card(modifier = Modifier.fillMaxWidth(0.9f).height(600.dp).padding(16.dp)) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Torrent Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                SettingsGroup("Network") {
                    SettingsItem("Listen Port") { OutlinedTextField(value = listenPort.toString(), onValueChange = { listenPort = it.toIntOrNull() ?: 6881 }, modifier = Modifier.width(150.dp), singleLine = true) }
                    SettingsToggle("Enable DHT", enableDht) { enableDht = it }
                    SettingsToggle("Enable LSD", enableLsd) { enableLsd = it }
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
                    SettingsItem("Max Connections") { OutlinedTextField(value = maxConnections.toString(), onValueChange = { maxConnections = it.toIntOrNull() ?: 200 }, modifier = Modifier.width(150.dp), singleLine = true) }
                    SettingsItem("Max Upload Slots") { OutlinedTextField(value = maxUploadSlots.toString(), onValueChange = { maxUploadSlots = it.toIntOrNull() ?: 50 }, modifier = Modifier.width(150.dp), singleLine = true) }
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        onSave(settings.copy(listenPort = listenPort, enableDht = enableDht, enableLsd = enableLsd, enableUpnp = enableUpnp, enableNatpmp = enableNatpmp, encryptionMode = encryptionMode, maxConnections = maxConnections, maxUploadSlots = maxUploadSlots))
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
        Card(modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.8f).padding(16.dp)) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Files: ${torrent.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row { Switch(checked = torrent.isSequentialDownload, onCheckedChange = onSequentialDownloadChange); Text("Sequential Download") }
                }
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    torrent.files.forEach { file ->
                        FileRowWithPriority(file = file, onPriorityChange = { priority -> onFilePriorityChange(file.path, priority) })
                    }
                }
            }
        }
    }
}

@Composable
fun FileRowWithPriority(file: TorrentFile, onPriorityChange: (FilePriority) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = if (file.isMediaFile) Icons.Default.PlayCircle else Icons.Default.InsertDriveFile, contentDescription = null, modifier = Modifier.size(20.dp))
            Column {
                Text(file.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Priority: ${file.priority.name} \u2022 ${formatSize(file.size)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DialogDropdownButton(file.priority.name) {
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
fun TorrentSpeedLimitsDialog(torrent: TorrentInfo, onDismiss: () -> Unit, onLimitsChange: (Long, Long) -> Unit) {
    var downloadLimit by remember { mutableStateOf(torrent.downloadLimit) }
    var uploadLimit by remember { mutableStateOf(torrent.uploadLimit) }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(0.7f).padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Speed Limits: ${torrent.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Download (KB/s)")
                    OutlinedTextField(value = if (downloadLimit > 0) (downloadLimit / 1024).toString() else "", onValueChange = { downloadLimit = if (it.toLongOrNull() != null && it.toLong() > 0) it.toLong() * 1024 else -1L }, modifier = Modifier.width(150.dp), singleLine = true)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Upload (KB/s)")
                    OutlinedTextField(value = if (uploadLimit > 0) (uploadLimit / 1024).toString() else "", onValueChange = { uploadLimit = if (it.toLongOrNull() != null && it.toLong() > 0) it.toLong() * 1024 else -1L }, modifier = Modifier.width(150.dp), singleLine = true)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onLimitsChange(downloadLimit, uploadLimit); onDismiss() }) { Text("Apply") }
                }
            }
        }
    }
}

@Composable
fun TorrentStreamPlayerDialog(torrent: TorrentInfo, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f).padding(16.dp)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text("Stream Player", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Integration with KuroEngine player", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Button(onClick = onDismiss) { Text("Close") } }
            }
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
fun SettingsItem(title: String, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@Composable
fun SettingsToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsDropdown(title: String, selectedValue: String, items: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        DialogDropdownButton(text = selectedValue, onClick = { expanded = true })
    }
    if (expanded) {
        DropdownMenu(expanded = true, onDismissRequest = { expanded = false }) { items() }
    }
}

@Composable
fun DialogDropdownButton(text: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(MaterialTheme.colorScheme.surfaceVariant), horizontalArrangement = Arrangement.End) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DialogTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable { onClick() }.wrapContentSize())
}

fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "${bytes} B"
    bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
    bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
    else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
}
