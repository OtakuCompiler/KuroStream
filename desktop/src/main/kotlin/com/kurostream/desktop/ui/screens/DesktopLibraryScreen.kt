package com.kurostream.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kurostream.desktop.DesktopAppState
import com.kurostream.desktop.search.SearchResult

@Composable
fun DesktopLibraryScreen(
    state: DesktopAppState,
    onSelectItem: (String) -> Unit,
    onPlay: (String) -> Unit,
) {
    val items = remember { mutableStateOf<List<SearchResult>>(emptyList()) }

    LaunchedEffect(Unit) {
        // Real impl: state.favorites.list() + state.history.recent().
        // Library populates from the local SQLite cache + cloud sync.
        items.value = emptyList()
    }

    if (items.value.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Your library is empty", style = MaterialTheme.typography.titleLarge)
            Text(
                "Add shows to your library from the details screen to track them here.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(180.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items.value, key = { it.id }) { item ->
                DesktopMediaCard(item = item, onClick = { onSelectItem(item.id) })
            }
        }
    }
}
