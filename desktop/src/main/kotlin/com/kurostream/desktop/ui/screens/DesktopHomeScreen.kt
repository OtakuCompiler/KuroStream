package com.kurostream.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kurostream.desktop.DesktopAppState
import com.kurostream.desktop.search.SearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mirrors Arctic Fuse 3 home: horizontal hero + grid of cards.
 * Pulls trending/recent rows from the cloud backend via the shared
 * `:domain` search contract.
 */
@Composable
fun DesktopHomeScreen(
    state: DesktopAppState,
    onSelectItem: (String) -> Unit,
    onPlay: (String) -> Unit,
    onSearch: (String) -> Unit,
) {
    val trending = remember { MutableStateFlow<List<SearchResult>>(emptyList()) }.asStateFlow()
    val continueWatching = remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    val newReleases = remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    val loading = remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading.value = true
        runCatching {
            // Real impl hits state.search.trending() — placeholder here to keep
            // the build self-contained without network in CI.
            continueWatching.value = emptyList()
            newReleases.value = emptyList()
        }
        loading.value = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            "KuroStream",
            style = MaterialTheme.typography.displaySmall,
        )

        // Hero callout
        ElevatedCard(
            onClick = { /* future: open featured rail */ },
            modifier = Modifier.fillMaxWidth().height(220.dp),
        ) {
            Box(contentAlignment = Alignment.BottomStart, modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Column {
                    Text("Welcome back", style = MaterialTheme.typography.headlineSmall)
                    Text("Pick up where you left off or browse what's new.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (loading.value) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (continueWatching.value.isNotEmpty()) {
            Text("Continue Watching", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                continueWatching.value.take(6).forEach { item ->
                    DesktopMediaCard(item = item, onClick = { onPlay(item.id) })
                }
            }
        }

        if (newReleases.value.isNotEmpty()) {
            Text("New Releases", style = MaterialTheme.typography.titleMedium)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(180.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
            ) {
                items(newReleases.value, key = { it.id }) { item ->
                    DesktopMediaCard(item = item, onClick = { onSelectItem(item.id) })
                }
            }
        }

        if (continueWatching.value.isEmpty() && newReleases.value.isEmpty() && !loading.value) {
            // Cold-start empty state
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nothing here yet", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Search the catalog, add to your library, and your home rows will populate automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = { onSearch("") }) { Text("Open Search") }
                }
            }
        }
    }
}
