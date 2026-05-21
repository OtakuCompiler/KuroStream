package com.kurostream.tv.ui.discover

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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.kurostream.tv.domain.model.Anime
import com.kurostream.tv.domain.model.AnimeSeason
import com.kurostream.tv.domain.model.AnimeType
import com.kurostream.tv.ui.components.AnimeCard
import com.kurostream.tv.ui.components.GenreChip
import com.kurostream.tv.ui.components.LoadingIndicator
import com.kurostream.tv.ui.theme.KuroStreamColors
import com.kurostream.tv.ui.theme.KuroStreamTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList

/**
 * Discover Screen - Browse and filter anime.
 * 
 * Features:
 * - Category tabs (All, Trending, Popular, Seasonal)
 * - Genre filters
 * - Type filters (TV, Movie, OVA, etc.)
 * - Grid layout for browsing
 * - Infinite scroll pagination
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = hiltViewModel(),
    onAnimeClick: (String) -> Unit,
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        DiscoverTopBar(
            onBackPressed = onBackPressed,
            onFilterClick = { viewModel.toggleFilterPanel() }
        )
        
        // Category tabs
        CategoryTabs(
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = { viewModel.selectCategory(it) }
        )
        
        // Filter panel (if visible)
        if (uiState.isFilterPanelVisible) {
            FilterPanel(
                selectedGenres = uiState.selectedGenres,
                selectedTypes = uiState.selectedTypes,
                selectedSeason = uiState.selectedSeason,
                selectedYear = uiState.selectedYear,
                onGenreToggle = { viewModel.toggleGenre(it) },
                onTypeToggle = { viewModel.toggleType(it) },
                onSeasonSelected = { viewModel.selectSeason(it) },
                onYearSelected = { viewModel.selectYear(it) },
                onClearFilters = { viewModel.clearFilters() }
            )
        }
        
        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = KuroStreamTheme.spacing.screenPadding.dp)
        ) {
            when {
                uiState.isLoading && uiState.animeList.isEmpty() -> {
                    LoadingIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.animeList.isEmpty() -> {
                    EmptyState(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    AnimeGrid(
                        animeList = uiState.animeList,
                        onAnimeClick = onAnimeClick,
                        onLoadMore = { viewModel.loadMore() },
                        isLoadingMore = uiState.isLoadingMore,
                        focusRequester = focusRequester
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DiscoverTopBar(
    onBackPressed: () -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = KuroStreamTheme.spacing.screenPadding.dp,
                vertical = KuroStreamTheme.spacing.medium.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Discover",
                style = KuroStreamTheme.typography.headlineMedium,
                color = Color.White
            )
        }
        
        IconButton(onClick = onFilterClick) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filters",
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryTabs(
    selectedCategory: DiscoverCategory,
    onCategorySelected: (DiscoverCategory) -> Unit
) {
    val categories = DiscoverCategory.entries
    val selectedIndex = categories.indexOf(selectedCategory)
    
    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KuroStreamTheme.spacing.screenPadding.dp)
    ) {
        categories.forEachIndexed { index, category ->
            Tab(
                selected = index == selectedIndex,
                onFocus = { onCategorySelected(category) },
                onClick = { onCategorySelected(category) }
            ) {
                Text(
                    text = category.displayName,
                    style = KuroStreamTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FilterPanel(
    selectedGenres: Set<String>,
    selectedTypes: Set<AnimeType>,
    selectedSeason: AnimeSeason?,
    selectedYear: Int?,
    onGenreToggle: (String) -> Unit,
    onTypeToggle: (AnimeType) -> Unit,
    onSeasonSelected: (AnimeSeason?) -> Unit,
    onYearSelected: (Int?) -> Unit,
    onClearFilters: () -> Unit
) {
    val genres = listOf(
        "Action", "Adventure", "Comedy", "Drama", "Fantasy",
        "Horror", "Mecha", "Mystery", "Romance", "Sci-Fi",
        "Slice of Life", "Sports", "Supernatural", "Thriller"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KuroStreamColors.BackgroundElevated)
            .padding(KuroStreamTheme.spacing.screenPadding.dp)
    ) {
        // Genres
        Text(
            text = "Genres",
            style = KuroStreamTheme.typography.titleSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres) { genre ->
                FilterChip(
                    text = genre,
                    isSelected = selectedGenres.contains(genre),
                    onClick = { onGenreToggle(genre) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Types
        Text(
            text = "Type",
            style = KuroStreamTheme.typography.titleSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AnimeType.entries.filter { it != AnimeType.UNKNOWN }) { type ->
                FilterChip(
                    text = type.name.replace("_", " "),
                    isSelected = selectedTypes.contains(type),
                    onClick = { onTypeToggle(type) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Seasons
        Text(
            text = "Season",
            style = KuroStreamTheme.typography.titleSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    text = "All",
                    isSelected = selectedSeason == null,
                    onClick = { onSeasonSelected(null) }
                )
            }
            items(AnimeSeason.entries) { season ->
                FilterChip(
                    text = season.name.lowercase().replaceFirstChar { it.uppercase() },
                    isSelected = selectedSeason == season,
                    onClick = { onSeasonSelected(season) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Clear button
        if (selectedGenres.isNotEmpty() || selectedTypes.isNotEmpty() || 
            selectedSeason != null || selectedYear != null) {
            Button(onClick = onClearFilters) {
                Text("Clear Filters")
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = if (isSelected) KuroStreamColors.Primary else KuroStreamColors.SurfaceVariant,
            focusedContainerColor = if (isSelected) KuroStreamColors.PrimaryVariant else KuroStreamColors.Primary.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = text,
            style = KuroStreamTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AnimeGrid(
    animeList: List<Anime>,
    onAnimeClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    focusRequester: FocusRequester
) {
    TvLazyVerticalGrid(
        columns = TvGridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(vertical = KuroStreamTheme.spacing.medium.dp),
        verticalArrangement = Arrangement.spacedBy(KuroStreamTheme.spacing.cardSpacing.dp),
        horizontalArrangement = Arrangement.spacedBy(KuroStreamTheme.spacing.cardSpacing.dp)
    ) {
        items(
            items = animeList,
            key = { it.id }
        ) { anime ->
            AnimeCard(
                anime = anime,
                onClick = { onAnimeClick(anime.id) },
                modifier = if (animeList.indexOf(anime) == 0) {
                    Modifier.focusRequester(focusRequester)
                } else Modifier
            )
            
            // Load more when approaching end
            if (animeList.indexOf(anime) == animeList.size - 5) {
                LaunchedEffect(Unit) {
                    onLoadMore()
                }
            }
        }
        
        // Loading more indicator
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = KuroStreamColors.Primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Error loading content",
            style = KuroStreamTheme.typography.headlineSmall,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = KuroStreamTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No anime found",
            style = KuroStreamTheme.typography.headlineSmall,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try adjusting your filters",
            style = KuroStreamTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

/**
 * Discover categories.
 */
enum class DiscoverCategory(val displayName: String) {
    ALL("All"),
    TRENDING("Trending"),
    POPULAR("Popular"),
    SEASONAL("This Season"),
    TOP_RATED("Top Rated"),
    UPCOMING("Upcoming")
}
