// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kurostream.app.model.MediaItem

/**
 * Nuvio-style poster card. Clean, no spring animation — uses a static
 * border on focus instead. Aspect ratio 2:3 (standard movie poster).
 *
 * Width is dynamic: (n + 0.25) * base — shows a 25% peek of the next
 * poster, matching the Nuvio home-screen convention.
 */
@Composable
fun NuvioPosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    width: Dp = 120.dp,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(8.dp))
            .background(cs.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        val imgUrl = item.posterUrl.ifBlank { item.backdropUrl }
        if (imgUrl.isNotBlank()) {
            AsyncImage(
                model = imgUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Top-right rating badge
        if (item.rating > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "★ ${"%.1f".format(item.rating)}",
                    color = Color.White,
                    fontSize = 10.sp(),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        // Bottom title overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.85f),
                    ),
                )
                .padding(8.dp),
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 12.sp(),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Nuvio-style landscape card for Continue Watching. 2.33:1 aspect.
 * Shows progress bar overlay at the bottom.
 */
@Composable
fun NuvioLandscapeCard(
    item: MediaItem,
    onClick: () -> Unit,
    width: Dp = 280.dp,
    progress: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .width(width)
            .aspectRatio(2.33f)
            .clip(RoundedCornerShape(10.dp))
            .background(cs.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        val imgUrl = item.backdropUrl.ifBlank { item.posterUrl }
        if (imgUrl.isNotBlank()) {
            AsyncImage(
                model = imgUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Bottom gradient
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.9f),
                    ),
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Column {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 13.sp(),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.year > 0) {
                    Text(
                        text = "${item.year}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp(),
                    )
                }
                if (progress > 0f) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = cs.primary,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        strokeCap = ProgressIndicatorDefaults.linearStrokeCap,
                    )
                }
            }
        }
    }
}

/**
 * Nuvio-style genre icon card. Square, colored background with icon.
 */
@Composable
fun NuvioGenreCard(
    label: String,
    icon: String,
    onClick: () -> Unit,
    width: Dp = 100.dp,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .width(width)
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(10.dp))
            .background(cs.primaryContainer)
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column {
            Text(text = icon, fontSize = 22.sp())
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                color = cs.onPrimaryContainer,
                fontSize = 13.sp(),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Nuvio-style catalog section: title row + horizontal scroller.
 */
@Composable
fun NuvioCatalogSection(
    title: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    cardType: CardType = CardType.Poster,
    modifier: Modifier = Modifier,
    emptyMessage: String? = null,
) {
    val cs = MaterialTheme.colorScheme
    if (items.isEmpty() && emptyMessage == null) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
        ) {
            Text(
                text = title,
                color = cs.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp(),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "See all",
                color = cs.primary,
                fontSize = 12.sp(),
                fontWeight = FontWeight.Medium,
            )
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(60.dp)
                    .background(cs.surfaceVariant, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = emptyMessage ?: "Nothing here yet",
                    color = cs.onSurfaceVariant,
                    fontSize = 12.sp(),
                )
            }
        } else {
            val posterWidth: Dp = when (cardType) {
                CardType.Poster -> 120.dp
                CardType.Landscape -> 280.dp
                CardType.Genre -> 100.dp
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(
                    count = items.size,
                    key = { idx -> items[idx].id },
                ) { idx ->
                    val item = items[idx]
                    when (cardType) {
                        CardType.Poster -> NuvioPosterCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            width = posterWidth,
                        )
                        CardType.Landscape -> NuvioLandscapeCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            width = posterWidth,
                            progress = (item.watchProgress.toFloat() / 100f).coerceIn(0f, 1f),
                        )
                        CardType.Genre -> NuvioGenreCard(
                            label = item.title,
                            icon = "🎬",
                            onClick = { onItemClick(item) },
                            width = posterWidth,
                        )
                    }
                }
            }
        }
    }
}

enum class CardType { Poster, Landscape, Genre }

// Local helper since kotlin stdlib has no sp() extension
private fun Int.sp(): androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)

@Composable
fun NuvioHeroCard(
    item: MediaItem,
    onPlay: () -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(cs.surfaceVariant),
    ) {
        val imgUrl = item.backdropUrl.ifBlank { item.posterUrl }
        if (imgUrl.isNotBlank()) {
            AsyncImage(
                model = imgUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to Color.Black.copy(alpha = 0.2f),
                        1f to Color.Black.copy(alpha = 0.85f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 22.sp(),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NuvioPillButton(label = "▶ Play", primary = true, onClick = onPlay)
                NuvioPillButton(label = "More info", primary = false, onClick = onInfo)
            }
        }
    }
}

@Composable
fun NuvioPillButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .background(
                if (primary) cs.primary else Color.White.copy(alpha = 0.18f),
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (primary) cs.onPrimary else Color.White,
            fontSize = 13.sp(),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun NuvioTopAppBar(
    title: String,
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(cs.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(cs.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "K", color = cs.onPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp())
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            color = cs.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp(),
            modifier = Modifier.weight(1f),
        )
        NuvioIconButton(icon = "🔍", onClick = onSearchClick)
        Spacer(Modifier.width(4.dp))
        NuvioIconButton(icon = "⚙", onClick = onSettingsClick)
    }
}

@Composable
fun NuvioIconButton(
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(40.dp)
            .background(cs.surfaceVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = icon, fontSize = 18.sp())
    }
}
