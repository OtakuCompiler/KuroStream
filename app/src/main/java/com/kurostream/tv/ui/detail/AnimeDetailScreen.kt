package com.kurostream.tv.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.kurostream.tv.domain.model.Anime
import com.kurostream.tv.domain.model.Episode
import com.kurostream.tv.ui.components.AnimeCard
import com.kurostream.tv.ui.components.EpisodeCard
import com.kurostream.tv.ui.components.GenreChip
import com.kurostream.tv.ui.components.LoadingIndicator
import com.kurostream.tv.ui.components.SectionHeader
import com.kurostream.tv.ui.theme.KuroStreamColors
import com.kurostream.tv.ui.theme.KuroStreamTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star

/**
 * Anime Detail Screen.
 * 
 * Shows comprehensive anime information:
 * - Hero banner with background image
 * - Anime metadata (rating, year, episodes, etc.)
 * - Synopsis
 * - Episode list with watch progress
 * - Related anime recommendations
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AnimeDetailScreen(
    animeId: String,
    modifier: Modifier = Modifier,
    viewModel: AnimeDetailViewModel = hiltViewModel(),
    onPlayEpisode: (String, Int) -> Unit,
    onBackPressed: () -> Unit,
    onRelatedAnimeClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(animeId) {
        viewModel.loadAnime(animeId)
    }
    
    LaunchedEffect(uiState.anime) {
        if (uiState.anime != null) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    onRetry = { viewModel.loadAnime(animeId) },
                    onBackPressed = onBackPressed,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.anime != null -> {
                AnimeDetailContent(
                    anime = uiState.anime!!,
                    episodes = uiState.episodes,
                    relatedAnime = uiState.relatedAnime,
                    isInMyList = uiState.isInMyList,
                    isFavorite = uiState.isFavorite,
                    onPlayEpisode = onPlayEpisode,
                    onBackPressed = onBackPressed,
                    onRelatedAnimeClick = onRelatedAnimeClick,
                    onToggleMyList = { viewModel.toggleMyList() },
                    onToggleFavorite = { viewModel.toggleFavorite() },
                    focusRequester = focusRequester
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AnimeDetailContent(
    anime: Anime,
    episodes: List<Episode>,
    relatedAnime: List<Anime>,
    isInMyList: Boolean,
    isFavorite: Boolean,
    onPlayEpisode: (String, Int) -> Unit,
    onBackPressed: () -> Unit,
    onRelatedAnimeClick: (String) -> Unit,
    onToggleMyList: () -> Unit,
    onToggleFavorite: () -> Unit,
    focusRequester: FocusRequester
) {
    val spacing = KuroStreamTheme.spacing
    
    TvLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Hero Section
        item {
            HeroSection(
                anime = anime,
                isInMyList = isInMyList,
                isFavorite = isFavorite,
                onBackPressed = onBackPressed,
                onPlayClick = {
                    val nextEp = anime.watchProgress?.currentEpisode?.plus(1) ?: 1
                    onPlayEpisode(anime.id, nextEp)
                },
                onToggleMyList = onToggleMyList,
                onToggleFavorite = onToggleFavorite,
                focusRequester = focusRequester
            )
        }
        
        // Episodes Section
        if (episodes.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Episodes (${episodes.size})",
                    modifier = Modifier.padding(
                        horizontal = spacing.screenPadding.dp,
                        vertical = spacing.medium.dp
                    )
                )
            }
            
            item {
                EpisodeRow(
                    episodes = episodes,
                    animeId = anime.id,
                    onEpisodeClick = { episodeNumber ->
                        onPlayEpisode(anime.id, episodeNumber)
                    }
                )
            }
        }
        
        // Related Anime Section
        if (relatedAnime.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "You Might Also Like",
                    modifier = Modifier.padding(
                        horizontal = spacing.screenPadding.dp,
                        vertical = spacing.medium.dp
                    )
                )
            }
            
            item {
                RelatedAnimeRow(
                    animeList = relatedAnime,
                    onAnimeClick = onRelatedAnimeClick
                )
            }
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeroSection(
    anime: Anime,
    isInMyList: Boolean,
    isFavorite: Boolean,
    onBackPressed: () -> Unit,
    onPlayClick: () -> Unit,
    onToggleMyList: () -> Unit,
    onToggleFavorite: () -> Unit,
    focusRequester: FocusRequester
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
    ) {
        // Background image
        AsyncImage(
            model = anime.bannerImage ?: anime.coverImage,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Gradient overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            KuroStreamColors.Background,
                            KuroStreamColors.Background.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            KuroStreamColors.Background
                        ),
                        startY = 300f
                    )
                )
        )
        
        // Back button
        IconButton(
            onClick = onBackPressed,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(KuroStreamTheme.spacing.medium.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        
        // Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(KuroStreamTheme.spacing.screenPadding.dp)
                .padding(top = 60.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            // Cover image
            AsyncImage(
                model = anime.coverImage,
                contentDescription = anime.getDisplayTitle(),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.medium)
            )
            
            Spacer(modifier = Modifier.width(32.dp))
            
            // Info column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Title
                    Text(
                        text = anime.getDisplayTitle(),
                        style = KuroStreamTheme.typography.displaySmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Japanese title
                    anime.titleJapanese?.let { japTitle ->
                        Text(
                            text = japTitle,
                            style = KuroStreamTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Metadata row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rating
                        anime.rating?.let { rating ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = KuroStreamColors.Warning,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%.1f", rating / 10f),
                                    style = KuroStreamTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                        }
                        
                        // Year
                        anime.year?.let { year ->
                            Text(
                                text = year.toString(),
                                style = KuroStreamTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        
                        // Type
                        Text(
                            text = anime.type.name,
                            style = KuroStreamTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        
                        // Episodes
                        anime.totalEpisodes?.let { eps ->
                            Text(
                                text = "$eps Episodes",
                                style = KuroStreamTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        
                        // Status
                        Text(
                            text = when {
                                anime.isAiring -> "Airing"
                                anime.isCompleted -> "Completed"
                                else -> anime.status.name
                            },
                            style = KuroStreamTheme.typography.bodyLarge,
                            color = if (anime.isAiring) KuroStreamColors.Success else Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Genres
                    if (anime.genres.isNotEmpty()) {
                        TvLazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(anime.genres.take(6)) { genre ->
                                GenreChip(text = genre)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Synopsis
                    Text(
                        text = anime.synopsis ?: "No synopsis available.",
                        style = KuroStreamTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onPlayClick,
                        modifier = Modifier.focusRequester(focusRequester)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (anime.watchProgress != null && anime.watchProgress!!.currentEpisode > 0) {
                                "Continue Ep. ${anime.watchProgress!!.currentEpisode + 1}"
                            } else {
                                "Play"
                            }
                        )
                    }
                    
                    Button(onClick = onToggleMyList) {
                        Icon(
                            imageVector = if (isInMyList) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isInMyList) "In My List" else "Add to List")
                    }
                    
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) KuroStreamColors.Error else Color.White
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodeRow(
    episodes: List<Episode>,
    animeId: String,
    onEpisodeClick: (Int) -> Unit
) {
    TvLazyRow(
        contentPadding = PaddingValues(horizontal = KuroStreamTheme.spacing.screenPadding.dp),
        horizontalArrangement = Arrangement.spacedBy(KuroStreamTheme.spacing.cardSpacing.dp)
    ) {
        items(
            items = episodes,
            key = { "${animeId}_${it.number}" }
        ) { episode ->
            EpisodeCard(
                episodeNumber = episode.number,
                title = episode.title,
                thumbnail = episode.thumbnail,
                duration = episode.duration,
                isWatched = episode.isWatched,
                watchProgress = episode.watchProgressPercentage / 100f,
                onClick = { onEpisodeClick(episode.number) }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RelatedAnimeRow(
    animeList: List<Anime>,
    onAnimeClick: (String) -> Unit
) {
    TvLazyRow(
        contentPadding = PaddingValues(horizontal = KuroStreamTheme.spacing.screenPadding.dp),
        horizontalArrangement = Arrangement.spacedBy(KuroStreamTheme.spacing.cardSpacing.dp)
    ) {
        items(
            items = animeList,
            key = { it.id }
        ) { anime ->
            AnimeCard(
                anime = anime,
                onClick = { onAnimeClick(anime.id) }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(KuroStreamTheme.spacing.screenPadding.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error loading anime",
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onBackPressed) {
                Text("Go Back")
            }
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
