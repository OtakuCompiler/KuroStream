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

package com.kurostream.extensions.asr

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AutoCaptionOverlay(isVisible: Boolean, onDismiss: () -> Unit, viewModel: AutoCaptionViewModel = hiltViewModel()) {
    val captionText by viewModel.captionText.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val partialText by viewModel.partialText.collectAsState()

    AnimatedVisibility(visible = isVisible, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
            Surface(color = Color.Black.copy(alpha = 0.75f), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto-Caption", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        IconButton(onClick = { if (isListening) viewModel.stopListening() else viewModel.startListening() }) {
                            Icon(imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff, contentDescription = if (isListening) "Stop" else "Start", tint = if (isListening) MaterialTheme.colorScheme.primary else Color.White)
                        }
                    }
                    if (partialText.isNotBlank() && isListening) {
                        Text(text = partialText, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    if (captionText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = captionText, style = MaterialTheme.typography.bodyLarge, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(onClick = onDismiss) { Text("Close", color = Color.White.copy(alpha = 0.7f)) }
                }
            }
        }
    }
}
