package com.kurostream.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kurostream.ui.components.FocusableCard
import com.kurostream.ui.components.LoadingScreen
import com.kurostream.ui.theme.*

@Composable
fun SearchScreen(
    onContentClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KuroBackground)
            .padding(horizontal = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FocusableCard(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack, "Back",
                    tint = KuroOnSurface,
                    modifier = Modifier.padding(10.dp)
                )
            }
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = {
                    Text("Search movies, series...", color = KuroOnSurfaceVariant)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = KuroOnSurfaceVariant)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KuroPrimary,
                    unfocusedBorderColor = KuroSurfaceVariant,
                    focusedTextColor = KuroOnSurface,
                    unfocusedTextColor = KuroOnSurface,
                    cursorColor = KuroPrimary
                ),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        when {
            uiState.isLoading -> LoadingScreen("Searching...")
            uiState.results.isEmpty() && uiState.query.isNotBlank() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results for \"${uiState.query}\"", color = KuroOnSurfaceVariant)
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 48.dp)
                ) {
                    items(uiState.results) { content ->
                        FocusableCard(
                            onClick = { onContentClick(content.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                AsyncImage(
                                    model = content.poster,
                                    contentDescription = content.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                                Text(
                                    text = content.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KuroOnSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
