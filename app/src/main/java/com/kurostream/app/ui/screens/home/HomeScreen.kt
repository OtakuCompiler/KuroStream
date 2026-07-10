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

package com.kurostream.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kurostream.app.ui.components.HeroBanner
import com.kurostream.app.ui.components.SkeletonRow
import com.kurostream.app.ui.components.TvTopAppBar
import com.kurostream.app.ui.theme.TvOnSurfaceVariant
import com.kurostream.app.ui.theme.TvSurfaceVariant
import com.kurostream.app.model.MediaItem

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
    val lazyListState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    val showTopBarBg by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 100 }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                HeroBanner(
                    items = uiState.heroItems,
                    onItemClick = onMediaClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                )
            }

            item {
                ContinueWatchingRow(
                    state = uiState.continueWatching,
                    onItemClick = onMediaClick,
                    onResumeClick = { mediaId, episodeId, position ->
                    },
                    modifier = Modifier.padding(top = 24.dp)
                )
            }

            item {
                TrendingAnimeRow(
                    state = uiState.trending,
                    onItemClick = onMediaClick,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }

            item {
                NewReleasesRow(
                    state = uiState.newReleases,
                    onItemClick = onMediaClick,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }

            item {
                SeasonalAnimeRow(
                    state = uiState.seasonal,
                    onItemClick = onMediaClick,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }

            items(uiState.placeholderSections) { section ->
                PlaceholderRow(
                    title = section.title,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }

        TvTopAppBar(
            showBackground = showTopBarBg,
            onSearchClick = onSearchClick,
            onDownloadsClick = onDownloadsClick,
            onSettingsClick = onSettingsClick,
            onAddonsClick = onAddonsClick,
            onTorrentsClick = onTorrentsClick,
            onBackupClick = onBackupClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlaceholderRow(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        SkeletonRow(itemCount = 6)
    }
}