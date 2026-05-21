package com.kurostream.tv.ui.mylist

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
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.kurostream.tv.domain.model.AnimeListEntry
import com.kurostream.tv.domain.model.WatchStatus
import com.kurostream.tv.ui.components.AnimeCard
import com.kurostream.tv.ui.components.LoadingIndicator
import com.kurostream.tv.ui.theme.KuroStreamColors
import com.kurostream.tv.ui.theme.KuroStreamTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync

/**
 * My List Screen - User's anime library.
 * 
 * Features:
 * - Watch status tabs (Watching, Completed, Plan to Watch, etc.)
 * - Grid layout of anime in user's list
 * - AniList sync integration
 * - Quick actions (change status, remove)
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MyListScreen(
    modifier: Modifier = Modifier,
    viewModel: MyListViewModel = hiltViewModel(),
    onAnimeClick: (String) -> Unit,
    onBackPressed: () -> Unit,
    onNavigateToAniList: () -> Unit
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
        MyListTopBar(
            onBackPressed = onBackPressed,
            onSyncClick = onNavigateToAniList,
            isSyncing = uiState.isSyncing
        )
        
        // Status tabs
        StatusTabs(
            selectedStatus = uiState.selectedStatus,
            statusCounts = uiState.statusCounts,
            onStatusSelected = { viewModel.selectStatus(it) }
        )
        
        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = KuroStreamTheme.spacing.screenPadding.dp)
        ) {
            when {
                uiState.isLoading -> {
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
                uiState.filteredList.isEmpty() -> {
                    EmptyState(
                        status = uiState.selectedStatus,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    AnimeListGrid(
                        entries = uiState.filteredList,
                        onAnimeClick = onAnimeClick,
                        focusRequester = focusRequester
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MyListTopBar(
    onBackPressed: () -> Unit,
    onSyncClick: () -> Unit,
    isSyncing: Boolean
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
                text = "My List",
                style = KuroStreamTheme.typography.headlineMedium,
                color = Color.White
            )
        }
        
        // Sync button
        Button(
            onClick = onSyncClick,
            enabled = !isSyncing
        ) {
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSyncing) "Syncing..." else "Sync with AniList"
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StatusTabs(
    selectedStatus: WatchStatus?,
    statusCounts: Map<WatchStatus, Int>,
    onStatusSelected: (WatchStatus?) -> Unit
) {
    val tabs = listOf<WatchStatus?>(null) + WatchStatus.entries
    val selectedIndex = tabs.indexOf(selectedStatus)
    
    TabRow(
        selectedTabIndex = selectedIndex.coerceAtLeast(0),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KuroStreamTheme.spacing.screenPadding.dp)
    ) {
        tabs.forEachIndexed { index, status ->
            val count = if (status == null) {
                statusCounts.values.sum()
            } else {
                statusCounts[status] ?: 0
            }
            
            Tab(
                selected = index == selectedIndex,
                onFocus = { onStatusSelected(status) },
                onClick = { onStatusSelected(status) }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = status?.displayName ?: "All",
                        style = KuroStreamTheme.typography.labelLarge
                    )
                    Text(
                        text = count.toString(),
                        style = KuroStreamTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AnimeListGrid(
    entries: List<AnimeListEntry>,
    onAnimeClick: (String) -> Unit,
    focusRequester: FocusRequester
) {
    TvLazyVerticalGrid(
        columns = TvGridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(vertical = KuroStreamTheme.spacing.medium.dp),
        verticalArrangement = Arrangement.spacedBy(KuroStreamTheme.spacing.cardSpacing.dp),
        horizontalArrangement = Arrangement.spacedBy(KuroStreamTheme.spacing.cardSpacing.dp)
    ) {
        items(
            items = entries,
            key = { it.anime.id }
        ) { entry ->
            AnimeCard(
                anime = entry.anime,
                onClick = { onAnimeClick(entry.anime.id) },
                modifier = if (entries.indexOf(entry) == 0) {
                    Modifier.focusRequester(focusRequester)
                } else Modifier
            )
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
            text = "Error loading list",
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
    status: WatchStatus?,
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
            text = when (status) {
                WatchStatus.WATCHING -> "You're not currently watching any anime"
                WatchStatus.COMPLETED -> "You haven't completed any anime yet"
                WatchStatus.ON_HOLD -> "No anime on hold"
                WatchStatus.DROPPED -> "No dropped anime"
                WatchStatus.PLAN_TO_WATCH -> "Add anime to your plan to watch list"
                null -> "Your list is empty. Start adding anime!"
            },
            style = KuroStreamTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

/**
 * Extension property for display name.
 */
val WatchStatus.displayName: String
    get() = when (this) {
        WatchStatus.WATCHING -> "Watching"
        WatchStatus.COMPLETED -> "Completed"
        WatchStatus.ON_HOLD -> "On Hold"
        WatchStatus.DROPPED -> "Dropped"
        WatchStatus.PLAN_TO_WATCH -> "Plan to Watch"
    }
