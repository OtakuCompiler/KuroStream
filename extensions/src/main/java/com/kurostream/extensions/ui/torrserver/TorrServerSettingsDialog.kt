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

package com.kurostream.extensions.ui.torrserver

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TorrServerSettingsDialog(
    onDismiss: () -> Unit,
    viewModel: TorrServerSettingsViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("TorrServer Settings") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = config.serverUrl,
                    onValueChange = { viewModel.updateServerUrl(it) },
                    label = { Text("Server URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = config.cacheSize.toString(),
                    onValueChange = { viewModel.updateCacheSize(it.toIntOrNull() ?: 200) },
                    label = { Text("Cache Size (MB)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = config.connectionsLimit.toString(),
                    onValueChange = { viewModel.updateConnectionsLimit(it.toIntOrNull() ?: 50) },
                    label = { Text("Connections Limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Checkbox(
                        checked = config.useDiskCache,
                        onCheckedChange = { viewModel.updateUseDiskCache(it) }
                    )
                    Text("Use Disk Cache", modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.saveSettings()
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
