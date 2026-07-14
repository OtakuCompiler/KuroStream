package com.kurostream.torrent.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

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
                            minLines = 3
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
                            modifier = Modifier.fillMaxWidth()
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
