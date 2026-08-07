package com.kurostream.desktop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kurostream.desktop.DesktopAppState
import com.kurostream.desktop.search.SearchResult
import kotlinx.coroutines.flow.collectLatest

/**
 * Keyboard-friendly search: type, see live suggestions, Enter to play.
 * On a TV remote this maps to the on-screen keyboard + D-pad navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopSearchScreen(
    state: DesktopAppState,
    initialQuery: String?,
    onSelectItem: (String) -> Unit,
    onPlay: (String) -> Unit,
) {
    var query by remember { mutableStateOf(initialQuery.orEmpty()) }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            return@LaunchedEffect
        }
        loading = true
        error = null
        runCatching {
            state.search.search(query).collectLatest { results = it }
        }.onFailure { error = it.message }
        loading = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search anime, movies, shows…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            },
        )

        error?.let {
            Text("Error: $it", color = MaterialTheme.colorScheme.error)
        }

        if (results.isEmpty() && !loading && query.isNotBlank()) {
            Text("No results for \"$query\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results, key = { it.id }) { item ->
                ListItem(
                    headlineContent = { Text(item.title) },
                    supportingContent = {
                        Text(
                            listOfNotNull(item.year?.toString(), item.rating?.let { "%.1f ★".format(it) })
                                .joinToString(" • ")
                        )
                    },
                    modifier = Modifier.clickable { onSelectItem(item.id) },
                )
                HorizontalDivider()
            }
        }
    }
}
