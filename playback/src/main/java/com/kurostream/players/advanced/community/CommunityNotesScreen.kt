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

package com.kurostream.players.advanced.community

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityNotesScreen(
    videoId: String,
    manager: CommunityNotesManager,
    currentUserId: String,
    currentUserName: String,
    isModerator: Boolean = false,
    onNavigateBack: () -> Unit
) {
    val notes by manager.observeNotesForVideo(videoId).collectAsStateWithLifecycle(initialValue = emptyList())
    val pendingNotes by manager.observePendingNotes().collectAsStateWithLifecycle(initialValue = emptyList())

    var showCreateDialog by remember { mutableStateOf(false) }
    var showModerationPanel by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community Notes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isModerator) {
                        IconButton(onClick = { showModerationPanel = !showModerationPanel }) {
                            BadgeBox(
                                badge = { if (pendingNotes.isNotEmpty()) Badge { Text(pendingNotes.size.toString()) } }
                            ) {
                                Icon(Icons.Default.Shield, "Moderation")
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, "Add Note")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (showModerationPanel && isModerator) {
            ModerationPanel(
                pendingNotes = pendingNotes,
                onModerate = { noteId, action, reason ->
                    scope.launch {
                        manager.moderateNote(noteId, currentUserId, action, reason)
                            .onSuccess {
                                snackbarHostState.showSnackbar("Note moderated successfully")
                            }
                            .onFailure {
                                snackbarHostState.showSnackbar("Moderation failed: ${it.message}")
                            }
                    }
                },
                modifier = Modifier.padding(padding)
            )
        } else {
            NotesList(
                notes = notes,
                currentUserId = currentUserId,
                onRateNote = { noteId, isHelpful ->
                    scope.launch {
                        manager.rateNote(noteId, currentUserId, isHelpful)
                            .onFailure {
                                snackbarHostState.showSnackbar("Failed to rate: ${it.message}")
                            }
                    }
                },
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showCreateDialog) {
        CreateNoteDialog(
            onCreate = { content, tags, sourceUrl, sourceTitle ->
                scope.launch {
                    manager.createNote(
                        videoId = videoId,
                        content = content,
                        authorId = currentUserId,
                        authorName = currentUserName,
                        tags = tags,
                        sourceUrl = sourceUrl,
                        sourceTitle = sourceTitle
                    ).onSuccess {
                        snackbarHostState.showSnackbar("Note submitted for review")
                        showCreateDialog = false
                    }.onFailure {
                        snackbarHostState.showSnackbar("Failed: ${it.message}")
                    }
                }
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun NotesList(
    notes: List<CommunityNotesManager.CommunityNote>,
    currentUserId: String,
    onRateNote: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (notes.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Notes,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No community notes yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    "Be the first to add context",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    } else {
        LazyColumn(modifier = modifier.padding(16.dp)) {
            items(notes, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    currentUserId = currentUserId,
                    onRateNote = onRateNote
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: CommunityNotesManager.CommunityNote,
    currentUserId: String,
    onRateNote: (String, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (note.helpfulnessScore >= 0.7f) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (note.content.length > 150) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Show less" else "Read more")
                }
            }

            // Source attribution
            note.sourceUrl?.let { url ->
                TextButton(
                    onClick = { /* Open source URL */ },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.Link, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(note.sourceTitle ?: "Source", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Tags
            if (note.tags.isNotEmpty()) {
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    note.tags.forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = { Text("#$tag") },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }

            // Rating bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${(note.helpfulnessScore * 100).toInt()}% helpful",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(
                        onClick = { onRateNote(note.id, true) }
                    ) {
                        Icon(Icons.Default.ThumbUp, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(note.helpfulCount.toString())
                    }
                    TextButton(
                        onClick = { onRateNote(note.id, false) }
                    ) {
                        Icon(Icons.Default.ThumbDown, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(note.notHelpfulCount.toString())
                    }
                }

                Text(
                    "by ${note.authorName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun ModerationPanel(
    pendingNotes: List<CommunityNotesManager.CommunityNote>,
    onModerate: (String, CommunityNotesManager.ModerationAction.Action, String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.padding(16.dp)) {
        items(pendingNotes) { note ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(note.content, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("By: ${note.authorName}", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(
                            onClick = { onModerate(note.id, CommunityNotesManager.ModerationAction.Action.APPROVE, "Approved") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Approve")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onModerate(note.id, CommunityNotesManager.ModerationAction.Action.REJECT, "Rejected") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Reject")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { onModerate(note.id, CommunityNotesManager.ModerationAction.Action.FLAG, "Flagged for review") }
                        ) {
                            Text("Flag")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateNoteDialog(
    onCreate: (String, List<String>, String?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var sourceUrl by remember { mutableStateOf("") }
    var sourceTitle by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Community Note") },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note Content *") },
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${content.length}/500",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (content.length > 500) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = sourceUrl,
                    onValueChange = { sourceUrl = it },
                    label = { Text("Source URL (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (sourceUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sourceTitle,
                        onValueChange = { sourceTitle = it },
                        label = { Text("Source Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    val tagList = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onCreate(
                        content,
                        tagList,
                        sourceUrl.takeIf { it.isNotBlank() },
                        sourceTitle.takeIf { it.isNotBlank() }
                    )
                },
                enabled = content.isNotBlank() && content.length <= 500 && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Submit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
