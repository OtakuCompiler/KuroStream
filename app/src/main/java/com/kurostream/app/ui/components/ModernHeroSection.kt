package com.kurostream.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kurostream.app.model.MediaItem
import com.kurostream.app.ui.theme.TvBackground
import com.kurostream.app.ui.theme.TvOnSurfaceVariant
import com.kurostream.app.ui.theme.TvSurfaceHighlight
import com.kurostream.app.ui.theme.focusedCardBorder
import com.kurostream.common.memory.LowRamDevice
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.absoluteValue

private const val AUTO_SCROLL_DELAY_MS = 8000L
private val HERO_HEIGHT = 480.dp

/**
 * ModernHeroSection — NuvioTV-style hero carousel with:
 * - Full-screen backdrop image with gradient overlay
 * - Title, metadata (rating, year, runtime, genres)
 * - Description (2 lines)
 * - Play / Details buttons
 * - Auto-advancing carousel with page indicators
 * - Smooth crossfade transitions
 */
@Composable
fun ModernHeroSection(
    items: List<MediaItem>,
    onPlayClick: (MediaItem) -> Unit,
    onInfoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        ModernHeroSkeleton(modifier = modifier)
        return
    }

    val pagerState = rememberPagerState(pageCount = { items.size })
    var isAutoScrolling by remember { mutableStateOf(true) }

    LaunchedEffect(pagerState.currentPage, isAutoScrolling) {
        if (isAutoScrolling && items.size > 1) {
            delay(AUTO_SCROLL_DELAY_MS)
            val nextPage = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Box(modifier = modifier.fillMaxWidth().height(HERO_HEIGHT)) {
        HorizontalPager(
            state = pagerState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 0.dp,
            beyondViewportPageCount = LowRamDevice.heroBannerOffscreenPages,
        ) { page ->
            val item = items[page]
            val pageOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            ).absoluteValue

            androidx.compose.runtime.key(item.id) {
                // Parallax backdrop
                ModernParallaxBackdrop(
                    imageUrl = item.backdropUrl.ifEmpty { item.posterUrl },
                    scrollOffset = pageOffset * 200f,
                    modifier = Modifier.fillMaxSize(),
                )

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.1f),
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.8f),
                                    TvBackground,
                                ),
                                startY = 0f,
                                endY = 1f,
                            )
                        ),
                )

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 48.dp, vertical = 64.dp),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    // Genre tags
                    if (item.genre.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item.genre.take(3).forEach { genre ->
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            RoundedCornerShape(4.dp),
                                        )
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Title
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Metadata row: Rating, Year, Runtime, Type
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (item.rating > 0f) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "%.1f".format(item.rating),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                )
                            }
                        }
                        if (item.year > 0) {
                            Text(
                                text = item.year.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                        if (item.duration > 0) {
                            Text(
                                text = "${item.duration} min",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                        if (item.episodes.isNotEmpty()) {
                            Text(
                                text = "${item.episodes.size} episodes",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description
                    if (item.description.isNotBlank()) {
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(0.6f),
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    } else {
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // CTA Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onPlayClick(item) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play")
                        }

                        Button(
                            onClick = { onInfoClick(item.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Details")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Details")
                        }
                    }
                }
            }
        }

        // Page indicators
        if (items.size > 1) {
            ModernPageIndicator(
                pageCount = items.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
            )
        }
    }
}

/**
 * Parallax backdrop with subtle scale/translation on page scroll
 */
@Composable
fun ModernParallaxBackdrop(
    imageUrl: String?,
    scrollOffset: Float = 0f,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (imageUrl.isNullOrBlank()) {
            Box(modifier = Modifier.fillMaxSize().background(TvBackground))
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = scrollOffset * 0.3f
                        scaleX = 1f + (abs(scrollOffset) / 2000f)
                        scaleY = 1f + (abs(scrollOffset) / 2000f)
                    },
            )
        }
    }
}

/**
 * Animated page indicator dots
 */
@Composable
fun ModernPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        (0 until pageCount).forEach { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isSelected) 32.dp else 12.dp,
                animationSpec = tween(300),
                label = "indicatorWidth",
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    ),
            )
        }
    }
}

/**
 * Hero skeleton loading state
 */
@Composable
fun ModernHeroSkeleton(modifier: Modifier = Modifier) {
    ModernShimmerEffect(
        modifier = modifier
            .fillMaxWidth()
            .height(HERO_HEIGHT),
    )
}

/**
 * Shimmer loading effect
 */
@Composable
fun ModernShimmerEffect(
    modifier: Modifier = Modifier,
    baseColor: Color = Color(0xFF1F2833),
    highlightColor: Color = Color(0xFF2A3441),
    cornerRadius: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(baseColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(baseColor, highlightColor, baseColor),
                        startX = -1f + translateAnim.value * 1f,
                        endX = translateAnim.value * 1f,
                    ),
                ),
        )
    }
}