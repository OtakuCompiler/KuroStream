package com.kurostream.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kurostream.desktop.DesktopAppState
import kotlinx.coroutines.launch

@Composable
fun DesktopDetailsScreen(
    state: DesktopAppState,
    mediaId: String,
    onPlay: () -> Unit,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("Loading…") }
    var backdropUrl by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf<String?>(null) }
    var rating by remember { mutableStateOf<Double?>(null) }
    var episodes by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(mediaId) {
        // Real impl: state.search.details(mediaId). Placeholder to keep
        // desktop self-contained without network during CI.
        title = mediaId.replaceFirstChar { it.titlecase() }
        description = "Details for $title will load from the shared domain backend."
        rating = 8.2
        episodes = listOf("Episode 1", "Episode 2", "Episode 3")
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Surface(
                modifier = Modifier.size(width = 220.dp, height = 320.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(title.take(1), style = MaterialTheme.typography.displayLarge)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                rating?.let { Text("★ %.1f".format(it)) }
                description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onPlay) { Text("Play") }
                    OutlinedButton(onClick = onClose) { Text("Close") }
                }
            }
        }

        Text("Episodes", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(episodes) { ep ->
                ListItem(
                    headlineContent = { Text(ep) },
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider()
            }
        }
    }
}
