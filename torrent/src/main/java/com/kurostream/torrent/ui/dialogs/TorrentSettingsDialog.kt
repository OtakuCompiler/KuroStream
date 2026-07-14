package com.kurostream.torrent.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kurostream.torrent.domain.EncryptionMode
import com.kurostream.torrent.domain.TorrentSessionSettings

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
                            singleLine = true
                        )
                    }
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
                    SettingsItem("Max Connections") {
                        OutlinedTextField(
                            value = maxConnections.toString(),
                            onValueChange = { maxConnections = it.toIntOrNull() ?: 200 },
                            modifier = Modifier.width(150.dp),
                            singleLine = true
                        )
                    }
                    SettingsItem("Max Upload Slots") {
                        OutlinedTextField(
                            value = maxUploadSlots.toString(),
                            onValueChange = { maxUploadSlots = it.toIntOrNull() ?: 50 },
                            modifier = Modifier.width(150.dp),
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
        androidx.compose.material3.DropdownMenuButton(text = selectedValue, onClick = { expanded = !expanded })
    }
    if (expanded) {
        DropdownMenu(expanded = true, onDismissRequest = { expanded = false }) {
            items()
        }
    }
}
