package com.kurostream.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kurostream.app.ui.components.HeroBanner
import com.kurostream.app.ui.components.LiveWallpaperView
import com.kurostream.app.ui.components.SkeletonRow
import com.kurostream.app.ui.components.TvTopAppBar
import com.kurostream.app.ui.theme.TvOnSurfaceVariant

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    onMediaClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddonsClick: () -> Unit,
    onTorrentsClick: () -> Unit,
    onBackupClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val showTopBarBg by remember { derivedStateOf {
        listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 100
    } }
    LaunchedEffect(Unit) { focusRequester.requestFocus(); viewModel.onScreenVisible() }
    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.liveWallpaperEnabled) LiveWallpaperView(
            wallpaperType = uiState.liveWallpaperType, modifier = Modifier.fillMaxSize()
        )
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
            item { HeroBanner(items = uiState.heroItems, onItemClick = onMediaClick, modifier = Modifier.fillMaxWidth().height(420.dp)) }
            item { ContinueWatchingRow(state = uiState.continueWatching, onItemClick = onMediaClick, onResumeClick = { _, _, _ -> }, modifier = Modifier.padding(top = 24.dp)) }
            item { TrendingAnimeRow(state = uiState.trending, onItemClick = onMediaClick, modifier = Modifier.padding(top = 24.dp)) }
            item { NewReleasesRow(state = uiState.newReleases, onItemClick = onMediaClick, modifier = Modifier.padding(top = 24.dp)) }
            item { SeasonalAnimeRow(state = uiState.seasonal, onItemClick = onMediaClick, modifier = Modifier.padding(top = 24.dp)) }
            item {
                val s = uiState.becauseYouWatched
                if (s is RowState.Success && s.items.isNotEmpty()) BecauseYouWatchedRow(
                    sourceTitle = uiState.becauseYouWatchedSource, recommendations = s.items,
                    onItemClick = onMediaClick, modifier = Modifier.padding(top = 24.dp),
                )
            }
            items(uiState.placeholderSections) { section ->
                PlaceholderRow(title = section.title, modifier = Modifier.padding(top = 24.dp))
            }
        }
        TvTopAppBar(showBackground = showTopBarBg, onSearchClick = onSearchClick,
            onDownloadsClick = onDownloadsClick, onSettingsClick = onSettingsClick,
            onAddonsClick = onAddonsClick, onTorrentsClick = onTorrentsClick,
            onBackupClick = onBackupClick, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlaceholderRow(title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        SkeletonRow(itemCount = 6)
    }
}
