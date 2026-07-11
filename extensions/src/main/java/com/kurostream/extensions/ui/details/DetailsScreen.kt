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

package com.kurostream.extensions.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kurostream.extensions.domain.model.CharacterInfo
import com.kurostream.extensions.domain.model.MediaDetail
import com.kurostream.extensions.domain.model.StaffInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(onPlayClick: (MediaDetail) -> Unit, onBackClick: () -> Unit, viewModel: DetailsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(topBar = {
        TopAppBar(title = { Text("Details") }, navigationIcon = {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") }
        }, actions = {
            IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Default.Refresh, "Refresh") }
        })
    }) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is DetailsUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is DetailsUiState.Success -> DetailsContent(media = state.mediaDetail, onPlayClick = onPlayClick)
                is DetailsUiState.Error -> ErrorState(message = state.message, onRetry = { viewModel.refresh() }, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun DetailsContent(media: MediaDetail, onPlayClick: (MediaDetail) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(model = media.backdropUrl ?: media.posterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(220.dp))
                Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Bottom) {
                        Card(modifier = Modifier.width(100.dp).aspectRatio(0.7f)) {
                            AsyncImage(model = media.posterUrl, contentDescription = media.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = media.title, style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                media.year?.let { Text(it, style = MaterialTheme.typography.bodyMedium); Spacer(modifier = Modifier.width(8.dp)) }
                                media.rating?.let { Text("%.1f".format(it), style = MaterialTheme.typography.bodyMedium); Spacer(modifier = Modifier.width(8.dp)) }
                                media.ageRating?.let { SuggestionChip(onClick = {}, label = { Text(it) }) }
                            }
                        }
                    }
                }
            }
        }
        item {
            Button(onClick = { onPlayClick(media) }, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Icon(Icons.Default.PlayArrow, null); Spacer(modifier = Modifier.width(8.dp)); Text("Play")
            }
        }
        item {
            Text("Synopsis", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Text(text = media.description ?: "No synopsis available.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp))
        }
        if (media.genres.isNotEmpty()) {
            item {
                Text("Genres", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                FlowRow(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    media.genres.forEach { genre -> AssistChip(onClick = {}, label = { Text(genre) }) }
                }
            }
        }
        item { InfoGrid(media = media) }
        if (media.characters?.isNotEmpty() == true) {
            item {
                Text("Characters", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(media.characters) { CharacterCard(character = it) }
                }
            }
        }
        if (media.staff?.isNotEmpty() == true) {
            item {
                Text("Staff", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(media.staff) { StaffCard(staff = it) }
                }
            }
        }
        if (media.episodes.isNotEmpty()) {
            item { Text("Episodes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            items(media.episodes) { episode ->
                ListItem(
                    headlineContent = { Text("Ep ${episode.episodeNumber}: ${episode.title}") },
                    supportingContent = episode.overview?.let { { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) } },
                    leadingContent = { episode.thumbnail?.let { AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(80.dp, 45.dp)) } }
                )
            }
        }
    }
}

@Composable
private fun InfoGrid(media: MediaDetail) {
    val items = buildList {
        media.status?.let { add("Status" to it) }
        media.episodeCount?.let { add("Episodes" to it.toString()) }
        media.runtime?.let { add("Duration" to "${it} min") }
        media.year?.let { add("Year" to it) }
    }
    if (items.isNotEmpty()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Info", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            items.chunked(2).forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowItems.forEach { (label, value) ->
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CharacterCard(character: CharacterInfo) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
        Card(modifier = Modifier.size(80.dp)) {
            AsyncImage(model = character.imageUrl, contentDescription = character.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = character.name, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
        character.role?.let { Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun StaffCard(staff: StaffInfo) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
        Card(modifier = Modifier.size(80.dp)) {
            AsyncImage(model = staff.imageUrl, contentDescription = staff.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = staff.name, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
        staff.role?.let { Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Error, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Error", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}
