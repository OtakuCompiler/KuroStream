package com.kurostream.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.kurostream.app.model.MediaItem
import com.kurostream.app.ui.components.ModernHeroSection
import com.kurostream.app.ui.components.ModernContentRow
import com.kurostream.app.ui.components.ModernCardType
import com.kurostream.app.ui.theme.TvBackground

/**
 * ModernHomeScreen — NuvioTV-style home screen with:
 * - Full-screen hero carousel with backdrop, metadata, and CTAs
 * - Horizontal content rows: Continue Watching, Trending, Popular, Recently Added, Recommended, Genres, My List
 * - Smooth focus animations (scale + border) on cards
 * - Skeleton loading states
 * - Real data from repositories via HomeViewModel
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ModernHomeScreen(
    onMediaClick: (String) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onSearchClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddonsClick: () -> Unit,
    onTorrentsClick: () -> Unit,
    onBackupClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(TvBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // ===== HERO SECTION =====
            item(key = "hero") {
                ModernHeroSection(
                    items = uiState.heroItems,
                    onPlayClick = onPlayClick,
                    onInfoClick = onMediaClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ===== CONTINUE WATCHING =====
            item(key = "continue_watching") {
                ModernContentRow(
                    title = "Continue Watching",
                    state = uiState.continueWatching,
                    onItemClick = onMediaClick,
                    onPlayClick = onPlayClick,
                    onRetry = { viewModel.retry() },
                    cardType = ModernCardType.Landscape,
                    showProgress = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ===== TRENDING NOW =====
            item(key = "trending") {
                ModernContentRow(
                    title = "Trending Now",
                    state = uiState.trending,
                    onItemClick = onMediaClick,
                    onRetry = { viewModel.retry() },
                    cardType = ModernCardType.Poster,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ===== POPULAR =====
            item(key = "popular") {
                ModernContentRow(
                    title = "Popular",
                    state = uiState.popular,
                    onItemClick = onMediaClick,
                    onRetry = { viewModel.retry() },
                    cardType = ModernCardType.Poster,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ===== RECENTLY ADDED =====
            item(key = "recently_added") {
                ModernContentRow(
                    title = "Recently Added",
                    state = uiState.recentlyAdded,
                    onItemClick = onMediaClick,
                    onRetry = { viewModel.retry() },
                    cardType = ModernCardType.Poster,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ===== RECOMMENDED =====
            item(key = "recommended") {
                ModernContentRow(
                    title = "Recommended For You",
                    state = uiState.recommended,
                    onItemClick = onMediaClick,
                    onRetry = { viewModel.retry() },
                    cardType = ModernCardType.Poster,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ===== GENRES =====
            item(key = "genres") {
                ModernContentRow(
                    title = "Genres",
                    state = uiState.genres,
                    onItemClick = onMediaClick,
                    onRetry = { viewModel.retry() },
                    cardType = ModernCardType.Genre,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ===== MY LIST =====
            item(key = "my_list") {
                ModernContentRow(
                    title = "My List",
                    state = uiState.myList,
                    onItemClick = onMediaClick,
                    onRetry = { viewModel.retry() },
                    cardType = ModernCardType.Poster,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}