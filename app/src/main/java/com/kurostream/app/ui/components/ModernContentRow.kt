package com.kurostream.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kurostream.app.model.MediaItem
import com.kurostream.app.ui.screens.home.RowState
import com.kurostream.app.ui.theme.TvOnSurfaceVariant
import com.kurostream.app.ui.theme.TvSurfaceHighlight
import com.kurostream.app.ui.theme.focusedCardBorder

/**
 * ModernMediaCard — Card with smooth focus animations (scale + border)
 * Supports Poster (2:3), Landscape (16:9), and Genre card types
 */
@Composable
fun ModernMediaCard(
    item: MediaItem,
    cardType: ModernCardType = ModernCardType.Poster,
    showProgress: Boolean = false,
    progress: Float = 0f,
    onClick: () -> Unit = {},
    onFocus: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val (cardWidth, cardHeight) = when (cardType) {
        ModernCardType.Poster -> 180.dp to 270.dp
        ModernCardType.Landscape -> 280.dp to 158.dp
        ModernCardType.Genre -> 160.dp to 80.dp
    }

    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
        ),
        label = "cardBorderWidth",
    )
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
        ),
        label = "cardScale",
    )
    val elevation by animateDpAsState(
        targetValue = if (isFocused) 16.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
        ),
        label = "cardElevation",
    )

    Box(
        modifier = modifier
            .size(width = cardWidth, height = cardHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation.toPx()
                ambientShadowColor = Color(0xFF7C4DFF).copy(alpha = 0.3f)
                spotShadowColor = Color(0xFF7C4DFF).copy(alpha = 0.3f)
            }
            .clip(RoundedCornerShape(12.dp))
            .background(TvSurfaceHighlight)
            .border(width = borderWidth, color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(12.dp))
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocus?.invoke()
            }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .clickable(onClick = onClick),
    ) {
        when (cardType) {
            ModernCardType.Poster -> PosterCardContent(item = item)
            ModernCardType.Landscape -> LandscapeCardContent(item = item, showProgress = showProgress, progress = progress)
            ModernCardType.Genre -> GenreCardContent(item = item)
        }
    }
}

@Composable
private fun PosterCardContent(item: MediaItem) {
    Column {
        AsyncImage(
            model = item.posterUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
        )
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${item.year}",
                style = MaterialTheme.typography.bodyMedium,
                color = TvOnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LandscapeCardContent(
    item: MediaItem,
    showProgress: Boolean,
    progress: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(158.dp)
    ) {
        AsyncImage(
            model = item.posterUrl.ifEmpty { item.backdropUrl },
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(158.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
        )

        // Progress bar overlay
        if (showProgress && progress > 0f) {
            val clampedProgress = progress.coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { clampedProgress },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Play button overlay
        IconButton(
            onClick = { /* handled by parent click */ },
            modifier = Modifier.align(Alignment.Center),
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Resume",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }

    Column(modifier = Modifier.padding(12.dp)) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ep ${item.episodes.firstOrNull()?.number ?: "?"} · ${formatDuration(item.watchProgress)}",
            style = MaterialTheme.typography.bodyMedium,
            color = TvOnSurfaceVariant,
        )
    }
}

@Composable
private fun GenreCardContent(item: MediaItem) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.primaryContainer,
                RoundedCornerShape(12.dp),
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/**
 * ModernContentRow — Horizontal scrolling content row with:
 * - Section title
 * - LazyRow for horizontal scrolling
 * - Smooth focus animations on cards
 * - Skeleton loading state
 * - Error state with retry
 * - Empty state
 */
@Composable
fun ModernContentRow(
    title: String,
    state: RowState<MediaItem>,
    onItemClick: (String) -> Unit,
    onPlayClick: ((MediaItem) -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    cardType: ModernCardType = ModernCardType.Poster,
    showProgress: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 48.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Content based on state
        when (state) {
            is RowState.Loading -> {
                ModernSkeletonRow(
                    itemCount = if (cardType == ModernCardType.Landscape) 4 else 6,
                    cardType = cardType,
                    modifier = Modifier.padding(horizontal = 48.dp),
                )
            }
            is RowState.Error -> {
                ModernErrorRow(
                    message = state.message,
                    onRetry = onRetry ?: {},
                    modifier = Modifier.padding(horizontal = 48.dp),
                )
            }
            is RowState.Success -> {
                if (state.items.isEmpty()) {
                    ModernEmptyRow(
                        message = "No items available",
                        modifier = Modifier.padding(horizontal = 48.dp),
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(
                            items = state.items,
                            key = { it.id },
                        ) { item ->
                            ModernMediaCard(
                                item = item,
                                cardType = cardType,
                                showProgress = showProgress,
                                progress = if (item.duration > 0 && item.watchProgress > 0) {
                                    (item.watchProgress.toFloat() / (item.duration * 60 * 1000).toFloat()).coerceIn(0f, 1f)
                                } else 0f,
                                onClick = { onItemClick(item.id) },
                                onFocus = { onPlayClick?.invoke(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Skeleton loading row for content rows
 */
@Composable
fun ModernSkeletonRow(
    itemCount: Int = 6,
    cardType: ModernCardType = ModernCardType.Poster,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        items(itemCount, key = { it }) {
            ModernSkeletonCard(cardType = cardType)
        }
    }
}

@Composable
private fun ModernSkeletonCard(cardType: ModernCardType) {
    val (cardWidth, cardHeight) = when (cardType) {
        ModernCardType.Poster -> 180.dp to 270.dp
        ModernCardType.Landscape -> 280.dp to 158.dp
        ModernCardType.Genre -> 160.dp to 80.dp
    }

    Column(modifier = Modifier.width(cardWidth)) {
        ModernShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .clip(RoundedCornerShape(12.dp)),
        )
        if (cardType != ModernCardType.Genre) {
            Spacer(modifier = Modifier.height(10.dp))
            ModernShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
            Spacer(modifier = Modifier.height(4.dp))
            ModernShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
    }
}

/**
 * Error row with retry button
 */
@Composable
fun ModernErrorRow(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Error: $message",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

/**
 * Empty state row
 */
@Composable
fun ModernEmptyRow(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = TvOnSurfaceVariant,
        )
    }
}

/**
 * Shimmer box for skeleton loading
 */
@Composable
fun ModernShimmerBox(modifier: Modifier = Modifier) {
    val shimmerColors = listOf(
        Color(0xFF1F2833),
        Color(0xFF2A3441),
        Color(0xFF1F2833),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 1200,
                easing = androidx.compose.animation.core.FastOutSlowInEasing,
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(x = translateAnim.value, y = translateAnim.value),
    )

    Box(
        modifier = modifier.background(brush),
    )
}