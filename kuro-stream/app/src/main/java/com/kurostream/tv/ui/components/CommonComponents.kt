package com.kurostream.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.LinearProgressIndicator
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.kurostream.tv.domain.model.Anime
import com.kurostream.tv.domain.model.AnimeListEntry
import com.kurostream.tv.ui.theme.KuroStreamColors
import com.kurostream.tv.ui.theme.KuroStreamTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star

private val CardCornerRadius = RoundedCornerShape(8.dp)

/**
 * Standard anime card for grids and rows.
 *
 * Focus ring uses NuvioTV's CardDefaults.border() + BorderStroke pattern
 * so the border is part of the card's draw pass (no layout shift).
 * Scale 1.1x on focus matches NuvioTV's ContentCard.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AnimeCard(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showRating: Boolean = true
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(160.dp)
            .aspectRatio(0.7f),
        shape = CardDefaults.shape(shape = CardCornerRadius),
        colors = CardDefaults.colors(
            containerColor = KuroStreamColors.Surface,
            focusedContainerColor = KuroStreamColors.SurfaceVariant
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, KuroStreamColors.FocusBorder),
                shape = CardCornerRadius
            )
        ),
        scale = CardDefaults.scale(focusedScale = 1.1f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Poster image
            AsyncImage(
                model = anime.coverImage,
                contentDescription = anime.getDisplayTitle(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                KuroStreamColors.Background.copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            // Content overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                // Rating badge
                if (showRating && anime.rating != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = KuroStreamColors.Warning,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", anime.rating!! / 10f),
                            style = KuroStreamTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = anime.getDisplayTitle(),
                    style = KuroStreamTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Airing badge
            if (anime.isAiring) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            KuroStreamColors.Success,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "AIRING",
                        style = KuroStreamTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Continue watching card with progress indicator.
 * Focus ring via CardDefaults.border() — no layout shift on focus.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ContinueWatchingCard(
    entry: AnimeListEntry,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val anime = entry.anime
    val progress = entry.progress

    Card(
        onClick = onClick,
        modifier = modifier
            .width(320.dp)
            .height(180.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = CardDefaults.shape(shape = CardCornerRadius),
        colors = CardDefaults.colors(
            containerColor = KuroStreamColors.Surface,
            focusedContainerColor = KuroStreamColors.SurfaceVariant
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, KuroStreamColors.FocusBorder),
                shape = CardCornerRadius
            )
        ),
        scale = CardDefaults.scale(focusedScale = 1.05f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background image
            AsyncImage(
                model = anime.bannerImage ?: anime.coverImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Left-to-right gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                KuroStreamColors.Background.copy(alpha = 0.95f),
                                KuroStreamColors.Background.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Content
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side — Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = anime.getDisplayTitle(),
                            style = KuroStreamTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Episode ${progress.currentEpisode}${
                                progress.totalEpisodes.let { if (it > 0) " of $it" else "" }
                            }",
                            style = KuroStreamTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Column {
                        val watchProgress = if (progress.totalEpisodes > 0) {
                            progress.currentEpisode.toFloat() / progress.totalEpisodes
                        } else 0f

                        LinearProgressIndicator(
                            progress = { watchProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(MaterialTheme.shapes.small),
                            color = KuroStreamColors.Primary,
                            trackColor = KuroStreamColors.ProgressTrack
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${(watchProgress * 100).toInt()}% watched",
                            style = KuroStreamTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // Right side — Play button
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterVertically)
                        .background(KuroStreamColors.Primary, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * Section header with optional "See All" action.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onSeeAllClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = KuroStreamTheme.typography.headlineSmall,
            color = Color.White
        )

        if (onSeeAllClick != null) {
            Card(
                onClick = onSeeAllClick,
                colors = CardDefaults.colors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = KuroStreamColors.Primary.copy(alpha = 0.2f)
                )
            ) {
                Text(
                    text = "See All",
                    style = KuroStreamTheme.typography.labelLarge,
                    color = KuroStreamColors.Primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Full-screen loading indicator.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            color = KuroStreamColors.Primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading...",
            style = KuroStreamTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

/**
 * Episode card for the detail screen.
 * Focus ring via CardDefaults.border() — matches NuvioTV ContentCard pattern.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpisodeCard(
    episodeNumber: Int,
    title: String?,
    thumbnail: String?,
    duration: Int?,
    isWatched: Boolean,
    watchProgress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier
            .width(280.dp)
            .height(160.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = CardDefaults.shape(shape = CardCornerRadius),
        colors = CardDefaults.colors(
            containerColor = KuroStreamColors.Surface,
            focusedContainerColor = KuroStreamColors.SurfaceVariant
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, KuroStreamColors.FocusBorder),
                shape = CardCornerRadius
            )
        ),
        scale = CardDefaults.scale(focusedScale = 1.05f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Thumbnail
            AsyncImage(
                model = thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, KuroStreamColors.Background.copy(alpha = 0.9f)),
                            startY = 50f
                        )
                    )
            )

            // Watched badge
            if (isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            KuroStreamColors.Success.copy(alpha = 0.9f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "WATCHED",
                        style = KuroStreamTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // Episode info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "Episode $episodeNumber",
                    style = KuroStreamTheme.typography.titleSmall,
                    color = Color.White
                )
                title?.let {
                    Text(
                        text = it,
                        style = KuroStreamTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                duration?.let {
                    Text(
                        text = "${it}m",
                        style = KuroStreamTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (watchProgress > 0f && watchProgress < 1f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { watchProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(MaterialTheme.shapes.small),
                        color = KuroStreamColors.Primary,
                        trackColor = KuroStreamColors.ProgressTrack
                    )
                }
            }

            // Focused play overlay
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .background(KuroStreamColors.Primary.copy(alpha = 0.9f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * Genre/Tag chip.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GenreChip(
    text: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            colors = CardDefaults.colors(
                containerColor = KuroStreamColors.SurfaceVariant,
                focusedContainerColor = KuroStreamColors.Primary.copy(alpha = 0.3f)
            )
        ) {
            Text(
                text = text,
                style = KuroStreamTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    } else {
        Box(
            modifier = modifier
                .background(KuroStreamColors.SurfaceVariant, shape = MaterialTheme.shapes.small)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = text,
                style = KuroStreamTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}
