package com.kurostream.tv.ui.anilist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.kurostream.tv.data.remote.anilist.AniListMedia
import com.kurostream.tv.data.remote.anilist.AniListMediaEntry
import com.kurostream.tv.data.remote.anilist.AniListMediaStatus
import com.kurostream.tv.data.remote.anilist.AniListUser
import com.kurostream.tv.ui.components.LoadingIndicator
import com.kurostream.tv.ui.components.ErrorMessage

/**
 * AniList integration screen for viewing and managing anime list.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AniListTabScreen(
    onAnimeClick: (Int) -> Unit,
    viewModel: AniListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading && !uiState.isLoggedIn -> {
                LoadingIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            !uiState.isLoggedIn -> {
                AniListLoginPrompt(
                    onLoginClick = { viewModel.startLogin() },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                AniListContent(
                    user = uiState.user,
                    currentList = uiState.currentList,
                    planningList = uiState.planningList,
                    completedList = uiState.completedList,
                    droppedList = uiState.droppedList,
                    selectedTab = uiState.selectedTab,
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    focusRequester = focusRequester,
                    onTabSelected = { viewModel.selectTab(it) },
                    onAnimeClick = onAnimeClick,
                    onLogoutClick = { viewModel.logout() },
                    onRefreshClick = { viewModel.refreshList() }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AniListLoginPrompt(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(48.dp)
    ) {
        // AniList Logo placeholder
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AL",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Connect to AniList",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Sync your anime list and track your progress",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onLoginClick,
            colors = ButtonDefaults.colors(
                containerColor = Color(0xFF02A9FF), // AniList blue
                contentColor = Color.White
            )
        ) {
            Text("Login with AniList")
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AniListContent(
    user: AniListUser?,
    currentList: List<AniListMediaEntry>,
    planningList: List<AniListMediaEntry>,
    completedList: List<AniListMediaEntry>,
    droppedList: List<AniListMediaEntry>,
    selectedTab: Int,
    isLoading: Boolean,
    error: String?,
    focusRequester: FocusRequester,
    onTabSelected: (Int) -> Unit,
    onAnimeClick: (Int) -> Unit,
    onLogoutClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    val tabs = listOf("Watching", "Planning", "Completed", "Dropped")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header with user info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User avatar
                if (user?.avatarUrl != null) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user?.name?.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = user?.name ?: "User",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "AniList",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row {
                Button(
                    onClick = onRefreshClick,
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Refresh")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = onLogoutClick,
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Logout")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Tab row
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onFocus = { onTabSelected(index) },
                    modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Error message
        if (error != null) {
            ErrorMessage(
                message = error,
                onRetry = onRefreshClick,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Content based on selected tab
        val entries = when (selectedTab) {
            0 -> currentList
            1 -> planningList
            2 -> completedList
            3 -> droppedList
            else -> emptyList()
        }
        
        if (isLoading) {
            LoadingIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No anime in this list",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            TvLazyVerticalGrid(
                columns = TvGridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(entries) { entry ->
                    AniListAnimeCard(
                        entry = entry,
                        onClick = { entry.mediaId.let(onAnimeClick) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AniListAnimeCard(
    entry: AniListMediaEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val media = entry.media ?: return
    
    Card(
        onClick = onClick,
        modifier = modifier.width(160.dp),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Poster with progress overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
            ) {
                AsyncImage(
                    model = media.coverImageLarge ?: media.coverImageMedium,
                    contentDescription = media.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Progress indicator
                if (entry.progress > 0 && media.episodes != null && media.episodes > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.8f)
                                    )
                                )
                            )
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "${entry.progress}/${media.episodes}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(entry.progress.toFloat() / media.episodes)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
                
                // Score badge
                if (entry.score > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "★ ${entry.score.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            // Title
            Text(
                text = media.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
