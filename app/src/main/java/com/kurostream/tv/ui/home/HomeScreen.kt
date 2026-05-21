package com.kurostream.tv.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.kurostream.tv.R
import com.kurostream.tv.domain.model.Anime
import com.kurostream.tv.domain.model.AnimeListEntry
import com.kurostream.tv.ui.components.AnimeCard
import com.kurostream.tv.ui.components.ContinueWatchingCard
import com.kurostream.tv.ui.components.LoadingIndicator
import com.kurostream.tv.ui.components.SectionHeader
import com.kurostream.tv.ui.theme.KuroStreamColors
import com.kurostream.tv.ui.theme.KuroStreamTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Explore
import kotlinx.coroutines.delay

private const val HERO_AUTO_ADVANCE_MS = 10_000L
private const val HERO_FIRST_ADVANCE_DELAY_MS = 20_000L

/**
 * Home Screen - Main entry point of the app.
 *
 * Displays:
 * - Featured anime hero banner with NuvioTV-style Crossfade auto-rotation
 * - Continue watching row
 * - Trending anime
 * - Popular anime
 * - Seasonal anime
 *
 * Optimized for TV navigation with proper focus management.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onAnimeClick: (String) -> Unit,
    onNavigateToDiscover: () -> Unit,
    onNavigateToMyList: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onPlayEpisode: (String, Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading && uiState.featuredAnime.isEmpty() -> {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.error != null -> {
                ErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                HomeContent(
                    uiState = uiState,
                    onAnimeClick = onAnimeClick,
                    onNavigateToDiscover = onNavigateToDiscover,
                    onNavigateToMyList = onNavigateToMyList,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToSearch = onNavigateToSearch,
                    onPlayEpisode = onPlayEpisode,
                    focusRequester = focusRequester
                )
            }
        }

        // Top navigation bar floats above content
        TopNavBar(
            onSearchClick = onNavigateToSearch,
            onDiscoverClick = onNavigateToDiscover,
            onMyListClick = onNavigateToMyList,
            onSettingsClick = onNavigateToSettings,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onAnimeClick: (String) -> Unit,
    onNavigateToDiscover: () -> Unit,
    onNavigateToMyList: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onPlayEpisode: (String, Int) -> Unit,
    focusRequester: FocusRequester
) {
    val spacing = KuroStreamTheme.spacing

    TvLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = spacing.large.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.rowSpacing.dp)
    ) {
        // Featured Hero Banner
        item {
            if (uiState.featuredAnime.isNotEmpty()) {
                FeaturedHeroBanner(
                    animeList = uiState.featuredAnime,
                    onAnimeClick = onAnimeClick,
                    onPlayClick = { anime ->
                        val nextEp = anime.watchProgress?.currentEpisode?.plus(1) ?: 1
                        onPlayEpisode(anime.id, nextEp)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Continue Watching
        if (uiState.continueWatching.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Continue Watching",
                    modifier = Modifier.padding(horizontal = spacing.screenPadding.dp)
                )
            }
            item {
                ContinueWatchingRow(
                    entries = uiState.continueWatching,
                    onAnimeClick = onAnimeClick,
                    onPlayClick = { entry ->
                        onPlayEpisode(entry.anime.id, entry.progress.currentEpisode)
                    },
                    focusRequester = focusRequester
                )
            }
        }

        // Trending Anime
        if (uiState.trendingAnime.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Trending Now",
                    onSeeAllClick = onNavigateToDiscover,
                    modifier = Modifier.padding(horizontal = spacing.screenPadding.dp)
                )
            }
            item {
                AnimeRow(
                    animeList = uiState.trendingAnime,
                    onAnimeClick = onAnimeClick,
                    focusRequester = if (uiState.continueWatching.isEmpty()) focusRequester else null
                )
            }
        }

        // Popular Anime
        if (uiState.popularAnime.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Popular Anime",
                    onSeeAllClick = onNavigateToDiscover,
                    modifier = Modifier.padding(horizontal = spacing.screenPadding.dp)
                )
            }
            item {
                AnimeRow(animeList = uiState.popularAnime, onAnimeClick = onAnimeClick)
            }
        }

        // Seasonal Anime
        if (uiState.seasonalAnime.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "This Season",
                    onSeeAllClick = onNavigateToDiscover,
                    modifier = Modifier.padding(horizontal = spacing.screenPadding.dp)
                )
            }
            item {
                AnimeRow(animeList = uiState.seasonalAnime, onAnimeClick = onAnimeClick)
            }
        }

        // Recently Updated
        if (uiState.recentlyUpdated.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recently Updated",
                    onSeeAllClick = onNavigateToDiscover,
                    modifier = Modifier.padding(horizontal = spacing.screenPadding.dp)
                )
            }
            item {
                AnimeRow(animeList = uiState.recentlyUpdated, onAnimeClick = onAnimeClick)
            }
        }
    }
}

/**
 * NuvioTV-style hero carousel: 400dp, Crossfade, dual gradients via drawBehind,
 * dynamic pill-shaped dot indicators, 10s auto-advance (20s delay before first).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FeaturedHeroBanner(
    animeList: List<Anime>,
    onAnimeClick: (String) -> Unit,
    onPlayClick: (Anime) -> Unit,
    modifier: Modifier = Modifier
) {
    if (animeList.isEmpty()) return

    var activeIndex by remember { mutableIntStateOf(0) }
    var isFocused by remember { mutableStateOf(false) }
    val currentOnPlayClick by rememberUpdatedState(onPlayClick)
    val currentOnAnimeClick by rememberUpdatedState(onAnimeClick)
    val dotShape = remember { RoundedCornerShape(3.dp) }

    // Auto-advance — mirrors NuvioTV: 20s delay before first, then every 10s
    LaunchedEffect(isFocused, animeList.size) {
        if (animeList.size <= 1) return@LaunchedEffect
        delay(HERO_FIRST_ADVANCE_DELAY_MS)
        while (true) {
            delay(HERO_AUTO_ADVANCE_MS)
            if (!isFocused) {
                activeIndex = (activeIndex + 1) % animeList.size
            }
        }
    }

    Box(
        modifier = modifier
            .height(400.dp)
            .focusable()
            .onFocusChanged { isFocused = it.hasFocus || it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (activeIndex > 0) { activeIndex--; true } else false
                        }
                        Key.DirectionRight -> {
                            if (activeIndex < animeList.size - 1) { activeIndex++; true } else false
                        }
                        else -> false
                    }
                } else if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    currentOnAnimeClick(animeList[activeIndex].id)
                    true
                } else {
                    false
                }
            }
    ) {
        // Crossfade between slides — matches NuvioTV's tween(300)
        Crossfade(
            targetState = activeIndex,
            animationSpec = tween(300),
            label = "heroSlide"
        ) { index ->
            val anime = animeList.getOrNull(index) ?: return@Crossfade
            HeroSlide(
                anime = anime,
                onPlayClick = { currentOnPlayClick(anime) },
                onMoreInfoClick = { currentOnAnimeClick(anime.id) }
            )
        }

        // Pill-shaped indicator dots — NuvioTV style with dynamic width
        val focusRing = KuroStreamColors.FocusBorder
        val dotColorFocusedInactive = remember(focusRing) { focusRing.copy(alpha = 0.4f) }
        val dotColorUnfocusedInactive = remember { Color.White.copy(alpha = 0.3f) }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(animeList.size) { index ->
                val isActive = index == activeIndex
                val dotBackground = when {
                    isFocused && isActive -> focusRing
                    isFocused           -> dotColorFocusedInactive
                    isActive            -> focusRing
                    else                -> dotColorUnfocusedInactive
                }
                val dotWidth = when {
                    isFocused && isActive -> 32.dp
                    isActive              -> 24.dp
                    else                  -> 12.dp
                }
                val dotHeight = if (isFocused && isActive) 6.dp else 4.dp

                Box(
                    modifier = Modifier
                        .size(width = dotWidth, height = dotHeight)
                        .clip(dotShape)
                        .background(dotBackground)
                )
            }
        }
    }
}

/**
 * Single hero slide with NuvioTV's combined bottom+left gradient applied via drawBehind,
 * content anchored to bottom-left, 48dp safe-zone padding.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeroSlide(
    anime: Anime,
    onPlayClick: () -> Unit,
    onMoreInfoClick: () -> Unit
) {
    val bgColor = KuroStreamColors.Background

    val bottomGradient = remember(bgColor) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.Transparent,
                0.3f to Color.Transparent,
                0.6f to bgColor.copy(alpha = 0.5f),
                0.8f to bgColor.copy(alpha = 0.85f),
                1.0f to bgColor
            )
        )
    }
    val leftGradient = remember(bgColor) {
        Brush.horizontalGradient(
            colorStops = arrayOf(
                0.0f to bgColor.copy(alpha = 0.98f),
                0.16f to bgColor.copy(alpha = 0.88f),
                0.34f to bgColor.copy(alpha = 0.56f),
                0.56f to bgColor.copy(alpha = 0.20f),
                0.72f to Color.Transparent,
                1.0f to Color.Transparent
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        AsyncImage(
            model = anime.bannerImage ?: anime.coverImage,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )

        // Combined gradients drawn in a single pass — NuvioTV technique
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(brush = bottomGradient)
                    drawRect(brush = leftGradient)
                }
        )

        // Content — bottom-left, 48dp safe-zone, max 50% width
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 48.dp, end = 48.dp)
                .fillMaxWidth(0.5f)
        ) {
            Text(
                text = anime.getDisplayTitle(),
                style = KuroStreamTheme.typography.displayMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                anime.rating?.let { rating ->
                    Text(
                        text = "★ ${String.format("%.1f", rating / 10f)}",
                        style = KuroStreamTheme.typography.labelLarge,
                        color = KuroStreamColors.Warning
                    )
                }
                anime.year?.let { year ->
                    Text(
                        text = year.toString(),
                        style = KuroStreamTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                anime.totalEpisodes?.let { eps ->
                    Text(
                        text = "$eps Episodes",
                        style = KuroStreamTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            anime.synopsis?.takeIf { it.isNotBlank() }?.let { synopsis ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = synopsis,
                    style = KuroStreamTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onPlayClick) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play")
                }
                Button(onClick = onMoreInfoClick) {
                    Text("More Info")
                }
            }
        }
    }
}

/**
 * Top navigation bar with logo image (80dp height) and icon nav items.
 * 80dp total height matches NuvioTV's NuvioTopBar.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopNavBar(
    onSearchClick: () -> Unit,
    onDiscoverClick: () -> Unit,
    onMyListClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(80.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        KuroStreamColors.Background.copy(alpha = 0.95f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = KuroStreamTheme.spacing.screenPadding.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo image — uses banner.png as a wide logo mark
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Kuro Stream",
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(40.dp)
        )

        // Navigation icons
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White
                )
            }
            IconButton(onClick = onDiscoverClick) {
                Icon(
                    imageVector = Icons.Outlined.Explore,
                    contentDescription = "Discover",
                    tint = Color.White
                )
            }
            IconButton(onClick = onMyListClick) {
                Icon(
                    imageVector = Icons.Outlined.Bookmark,
                    contentDescription = "My List",
                    tint = Color.White
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ContinueWatchingRow(
    entries: List<AnimeListEntry>,
    onAnimeClick: (String) -> Unit,
    onPlayClick: (AnimeListEntry) -> Unit,
    focusRequester: FocusRequester
) {
    TvLazyRow(
        contentPadding = PaddingValues(horizontal = KuroStreamTheme.spacing.screenPadding.dp),
        horizontalArrangement = Arrangement.spacedBy(KuroStreamTheme.spacing.cardSpacing.dp)
    ) {
        items(items = entries, key = { it.anime.id }) { entry ->
            ContinueWatchingCard(
                entry = entry,
                onClick = { onAnimeClick(entry.anime.id) },
                onPlayClick = { onPlayClick(entry) },
                modifier = Modifier.then(
                    if (entries.indexOf(entry) == 0) Modifier.focusRequester(focusRequester)
                    else Modifier
                )
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AnimeRow(
    animeList: List<Anime>,
    onAnimeClick: (String) -> Unit,
    focusRequester: FocusRequester? = null
) {
    TvLazyRow(
        contentPadding = PaddingValues(horizontal = KuroStreamTheme.spacing.screenPadding.dp),
        horizontalArrangement = Arrangement.spacedBy(KuroStreamTheme.spacing.cardSpacing.dp)
    ) {
        items(items = animeList, key = { it.id }) { anime ->
            AnimeCard(
                anime = anime,
                onClick = { onAnimeClick(anime.id) },
                modifier = Modifier.then(
                    if (focusRequester != null && animeList.indexOf(anime) == 0)
                        Modifier.focusRequester(focusRequester)
                    else Modifier
                )
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
        modifier = modifier.padding(KuroStreamTheme.spacing.screenPadding.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Something went wrong",
            style = KuroStreamTheme.typography.headlineMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = KuroStreamTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}
