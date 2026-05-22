package com.kurostream.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil.compose.AsyncImage
import com.kurostream.data.model.ContentItem
import com.kurostream.data.model.WatchHistoryEntry
import com.kurostream.ui.components.FocusableCard
import com.kurostream.ui.components.LoadingScreen
import com.kurostream.ui.components.NoPluginsScreen
import com.kurostream.ui.theme.*

@Composable
fun HomeScreen(
    onContentClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchFocusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KuroBackground)
    ) {
        when {
            uiState.isLoading -> LoadingScreen()
            !uiState.hasPlugins -> NoPluginsScreen(onSettingsClick = onSettingsClick)
            else -> HomeContent(
                uiState = uiState,
                onContentClick = onContentClick,
                onSearchClick = onSearchClick,
                onSettingsClick = onSettingsClick
            )
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onContentClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            TopBar(onSearchClick = onSearchClick, onSettingsClick = onSettingsClick)
        }

        if (uiState.featuredContent.isNotEmpty()) {
            item {
                FeaturedBanner(
                    content = uiState.featuredContent.first(),
                    onContentClick = onContentClick
                )
            }
        }

        if (uiState.continueWatching.isNotEmpty()) {
            item {
                ContentRow(
                    title = "Continue Watching",
                    items = uiState.continueWatching.map { history ->
                        ContentItem(
                            id = history.contentId,
                            title = history.contentTitle,
                            type = "movie",
                            poster = history.poster
                        )
                    },
                    onItemClick = onContentClick
                )
            }
        }

        if (uiState.trending.isNotEmpty()) {
            item {
                ContentRow(
                    title = "Trending Now",
                    items = uiState.trending,
                    onItemClick = onContentClick
                )
            }
        }

        item { Spacer(modifier = Modifier.height(48.dp)) }
    }
}

@Composable
private fun TopBar(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "KURO",
            style = MaterialTheme.typography.headlineLarge,
            color = KuroPrimary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FocusableCard(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = KuroOnSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
            FocusableCard(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = KuroOnSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun FeaturedBanner(
    content: ContentItem,
    onContentClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        AsyncImage(
            model = content.backdrop ?: content.poster,
            contentDescription = content.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(KuroBackground, KuroBackground.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 48.dp)
                .widthIn(max = 400.dp)
        ) {
            Text(
                text = content.title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            content.description?.let { desc ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KuroOnSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(20.dp))
            FocusableCard(
                onClick = { onContentClick(content.id) },
                containerColor = KuroPrimary
            ) {
                Text(
                    text = "Watch Now",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ContentRow(
    title: String,
    items: List<ContentItem>,
    onItemClick: (String) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = KuroOnSurface,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { content ->
                ContentCard(content = content, onClick = { onItemClick(content.id) })
            }
        }
    }
}

@Composable
private fun ContentCard(
    content: ContentItem,
    onClick: () -> Unit
) {
    FocusableCard(
        onClick = onClick,
        modifier = Modifier.width(140.dp)
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
