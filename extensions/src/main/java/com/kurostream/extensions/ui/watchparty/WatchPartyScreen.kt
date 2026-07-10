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

package com.kurostream.extensions.ui.watchparty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kurostream.extensions.watchparty.Participant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchPartyScreen(viewModel: WatchPartyViewModel = hiltViewModel()) {
    val sessionState by viewModel.sessionState.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val isHost by viewModel.isHost.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text("Watch Party") }, actions = {
            ConnectionIndicator(isConnected = connectionState)
            IconButton(onClick = { viewModel.showInviteDialog() }) { Icon(Icons.Default.Share, "Invite") }
        })
    }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            sessionState?.let { session ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Session: ${session.id}", style = MaterialTheme.typography.titleMedium)
                        Text("Media: ${session.mediaId}", style = MaterialTheme.typography.bodyMedium)
                        Text("Status: ${if (session.isPlaying) "Playing" else "Paused"}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Participants (${participants.size})", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(participants) { participant ->
                    ParticipantItem(participant = participant, isHost = isHost && participant.isHost)
                }
            }
            if (isHost) {
                HostControls(onPlay = { viewModel.broadcastPlay() }, onPause = { viewModel.broadcastPause() }, onSeek = { viewModel.broadcastSeek(it) })
            }
        }
    }
}

@Composable
private fun ConnectionIndicator(isConnected: Boolean) {
    Box(modifier = Modifier.size(12.dp).padding(end = 8.dp), contentAlignment = Alignment.Center) {
        Surface(shape = MaterialTheme.shapes.small, color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) { Box(modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
private fun ParticipantItem(participant: Participant, isHost: Boolean) {
    ListItem(
        headlineContent = { Text(participant.name) },
        supportingContent = { Text(if (participant.isHost) "Host" else "Guest", color = if (participant.isHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = { Icon(if (participant.isConnected) Icons.Default.Person else Icons.Default.PersonOff, null, tint = if (participant.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = if (isHost) { { IconButton(onClick = {}) { Icon(Icons.Default.Close, "Remove") } } } else null
    )
}

@Composable
private fun HostControls(onPlay: () -> Unit, onPause: () -> Unit, onSeek: (Long) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton(onClick = onPause) { Icon(Icons.Default.Pause, "Pause all") }
        IconButton(onClick = onPlay) { Icon(Icons.Default.PlayArrow, "Play all") }
        IconButton(onClick = { onSeek(0) }) { Icon(Icons.Default.Replay, "Restart all") }
    }
}
