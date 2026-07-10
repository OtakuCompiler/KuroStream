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

package com.kurostream.extensions.community

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@Composable
fun CommunityNotesOverlay(mediaId: String, currentTimeMs: Long, isVisible: Boolean, onDismiss: () -> Unit, viewModel: CommunityNotesViewModel = hiltViewModel()) {
    val notes by viewModel.visibleNotes.collectAsState()
    val showSpoilers by viewModel.showSpoilers.collectAsState()

    LaunchedEffect(mediaId) { viewModel.loadNotes(mediaId) }
    LaunchedEffect(currentTimeMs) { viewModel.updateCurrentTime(currentTimeMs) }

    AnimatedVisibility(visible = isVisible, enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }) {
        Surface(modifier = Modifier.fillMaxHeight().width(320.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Community Notes", style = MaterialTheme.typography.titleMedium)
                    Row {
                        IconButton(onClick = { viewModel.toggleSpoilers() }) {
                            Icon(imageVector = if (showSpoilers) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = "Toggle spoilers")
                        }
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                    }
                }
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(notes, key = { it.note.id }) { displayState ->
                        NoteCard(displayState = displayState, showSpoilers = showSpoilers, onLike = { viewModel.likeNote(it) }, onExpand = { viewModel.toggleExpand(it) })
                    }
                }
                FloatingActionButton(onClick = { /* Open add note dialog */ }, modifier = Modifier.align(Alignment.End).padding(16.dp)) {
                    Icon(Icons.Default.Add, "Add note")
                }
            }
        }
    }
}

@Composable
private fun NoteCard(displayState: NoteDisplayState, showSpoilers: Boolean, onLike: (String) -> Unit, onExpand: (String) -> Unit) {
    val note = displayState.note
    val isSpoiler = note.type == NoteType.SPOILER && !showSpoilers

    Card(onClick = { if (isSpoiler) onExpand(note.id) }, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = note.authorAvatar, contentDescription = null, modifier = Modifier.size(28.dp).clip(CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(note.author, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.width(8.dp))
                NoteTypeChip(type = note.type)
                Spacer(modifier = Modifier.weight(1f))
                Text(formatTimestamp(note.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (isSpoiler && !displayState.isExpanded) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                    Text("Spoiler - Tap to reveal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(8.dp))
                }
            } else {
                Text(note.content, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onLike(note.id) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.ThumbUp, "Like", modifier = Modifier.size(16.dp)) }
                Text("${note.likes}", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.width(16.dp))
                if (note.replies.isNotEmpty()) { Icon(Icons.Default.ChatBubble, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("${note.replies.size}", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

@Composable
private fun NoteTypeChip(type: NoteType) {
    val (label, color) = when (type) {
        NoteType.COMMENT -> "Comment" to MaterialTheme.colorScheme.primary
        NoteType.SPOILER -> "Spoiler" to MaterialTheme.colorScheme.error
        NoteType.MEME -> "Meme" to MaterialTheme.colorScheme.tertiary
        NoteType.TRANSLATION -> "TL" to MaterialTheme.colorScheme.secondary
        NoteType.CORRECTION -> "Fix" to MaterialTheme.colorScheme.inversePrimary
        NoteType.FUN_FACT -> "Fact" to MaterialTheme.colorScheme.surfaceTint
    }
    Surface(color = color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

private fun formatTimestamp(ms: Long): String {
    val seconds = ms / 1000
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
