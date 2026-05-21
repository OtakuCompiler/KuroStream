package com.kurostream.tv.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CircularProgressIndicator
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedTextField
import androidx.tv.material3.Text
import com.kurostream.tv.domain.model.Anime
import com.kurostream.tv.ui.components.AnimeCard
import com.kurostream.tv.ui.theme.KuroStreamColors

/**
 * Search screen — full-text anime search with TV-optimised D-pad navigation.
 *
 * Layout:
 *  ┌─────────────────────────────────────────────────────────┐
 *  │  ← Back      🔍 [search input field]           [Clear] │
 *  ├─────────────────────────────────────────────────────────┤
 *  │  Results grid (adaptive, 4 cols on 1080p TV)            │
 *  └─────────────────────────────────────────────────────────┘
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    onAnimeClick: (animeId: String) -> Unit,
    onBackPressed: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchFocusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 48.dp, vertical = 24.dp)
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(Modifier.width(16.dp))

            // Search field — D-pad users can press OK/Select to commit search
            SearchTextField(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                onClear = viewModel::clearSearch,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(searchFocusRequester)
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Content area ─────────────────────────────────────────────────────
        when {
            uiState.isLoading -> {
                LoadingState(modifier = Modifier.fillMaxSize())
            }

            uiState.error != null -> {
                ErrorState(
                    message = uiState.error!!,
                    modifier = Modifier.fillMaxSize()
                )
            }

            uiState.hasSearched && uiState.results.isEmpty() -> {
                EmptyState(
                    query = uiState.query,
                    modifier = Modifier.fillMaxSize()
                )
            }

            uiState.results.isNotEmpty() -> {
                SearchResultsGrid(
                    results = uiState.results,
                    onAnimeClick = onAnimeClick,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                SearchHint(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

// ── Sub-composables ──────────────────────────────────────────────────────────

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = {
            Text(
                text = "Search anime…",
                style = MaterialTheme.typography.bodyLarge,
                color = KuroStreamColors.OnSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = KuroStreamColors.Primary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = KuroStreamColors.OnSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { /* debounce handles it */ }),
        shape = RoundedCornerShape(8.dp)
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchResultsGrid(
    results: List<Anime>,
    onAnimeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "${results.size} result${if (results.size != 1) "s" else ""}",
            style = MaterialTheme.typography.labelMedium,
            color = KuroStreamColors.OnSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(results, key = { it.id }) { anime ->
                AnimeCard(
                    anime = anime,
                    onClick = { onAnimeClick(anime.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = KuroStreamColors.Primary)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Searching…",
                style = MaterialTheme.typography.bodyLarge,
                color = KuroStreamColors.OnSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EmptyState(query: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = KuroStreamColors.OnSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No results for \"$query\"",
                style = MaterialTheme.typography.headlineSmall,
                color = KuroStreamColors.OnBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Try a different title, romaji, or English name",
                style = MaterialTheme.typography.bodyMedium,
                color = KuroStreamColors.OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "⚠",
                style = MaterialTheme.typography.displayMedium,
                color = KuroStreamColors.Error
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = KuroStreamColors.OnBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchHint(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = KuroStreamColors.Primary.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Search for anime",
                style = MaterialTheme.typography.headlineMedium,
                color = KuroStreamColors.OnBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Type at least 2 characters to start searching",
                style = MaterialTheme.typography.bodyLarge,
                color = KuroStreamColors.OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
