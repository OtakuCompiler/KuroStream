package com.kurostream.app.ui.components

import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.kurostream.app.ui.theme.TvBackground
import com.kurostream.common.memory.LowRamDevice
import com.kurostream.app.model.MediaItem
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

private const val AUTO_SCROLL_DELAY_MS = 8000L

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroBanner(
    items: List<MediaItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        HeroSkeleton(modifier = modifier)
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

    Box(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 0.dp,
            beyondViewportPageCount = LowRamDevice.heroBannerOffscreenPages,
        ) { page ->
            val item = items[page]
            val pageOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            ).absoluteValue

            key(item.id) {
                ParallaxHeroBackground(
                    imageUrl = item.bannerImage ?: item.coverImage,
                    scrollOffset = pageOffset * 200f,
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 48.dp, top = 48.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Anime Spotlight",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 48.dp, vertical = 64.dp),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item.genres.take(3).forEach { genre ->
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        RoundedCornerShape(4.dp),
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${item.rating}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "${item.year} · ${item.format} · ${item.episodeCount} eps",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = item.synopsis,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.6f),
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onItemClick(item.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play")
                        }

                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White,
                            ),
                        ) {
                            Text("+ My List")
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            TvBackground.copy(alpha = 0.7f),
                            TvBackground,
                        ),
                    ),
                ),
        )

        if (items.size > 1) {
            AnimatedPageIndicator(
                pageCount = items.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun HeroSkeleton(modifier: Modifier = Modifier) {
    ShimmerEffect(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp),
    )
}
