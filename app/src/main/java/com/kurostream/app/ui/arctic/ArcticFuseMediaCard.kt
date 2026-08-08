// This file is part of KuroStream.
//
// ArcticFuseMediaCard — pixel-perfect card matching Arctic Fuse D-pad focus
// styling:
//   - 2px transparent border at rest
//   - cyan border on focus
//   - quality badge (top-left) and star rating (top-right)
//   - title below image
//   - bottom progress bar when watch progress > 0 (continue-watching/episode)
//
// Performance: loadState-aware, small footprint for low-RAM devices.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import com.kurostream.app.model.MediaItem

@Composable
fun ArcticFuseMediaCard(
    item: MediaItem,
    modifier: Modifier = Modifier,
    view: CardView = CardView.Poster,
    quality: String? = null,
    progress: Float = item.watchProgress.toFloat().coerceAtLeast(0f),
    onClick: () -> Unit = {},
    onFocus: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.35f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "glowAlpha",
    )
    val focusRequester = remember { FocusRequester() }

    val cardWidth: Dp = when (view) {
        CardView.Poster -> AFCardSize.posterWidth
        CardView.Landscape -> AFCardSize.landscapeWidth
        CardView.Episode -> AFCardSize.episodeWidth
    }
    val cardHeight: Dp = when (view) {
        CardView.Poster -> AFCardSize.posterHeight
        CardView.Landscape -> AFCardSize.landscapeHeight
        CardView.Episode -> AFCardSize.episodeHeight
    }

    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 150),
        label = "afCardBorder",
    )
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "afCardScale",
    )
    val elevation by animateDpAsState(
        targetValue = if (isFocused) 16.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "afCardElevation",
    )

    Box(
        modifier = modifier
            .size(width = cardWidth, height = cardHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation.toPx()
            }
            .drawBehind {
                // Outer glow ring — spec §6.2/11.2: 0 0 0 4px rgba(accent, 0.3)
                if (glowAlpha > 0f) {
                    drawRoundRect(
                        color = AFCyan.copy(alpha = glowAlpha),
                        topLeft = androidx.compose.ui.geometry.Offset(-4.dp.toPx(), -4.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(this.size.width + 8.dp.toPx(), this.size.height + 8.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(AFRadius.md.toPx() + 4.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx()),
                    )
                }
            }
            .clip(RoundedCornerShape(AFRadius.md))
            .background(AFSurface)
            .border(width = borderWidth, color = Color.White, shape = RoundedCornerShape(AFRadius.md))
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocus?.invoke()
            }
            .focusable()
            .semantics {
                contentDescription = "${item.title}, ${item.genre.joinToString()}, rated ${item.rating}"
            }
            .onPreviewKeyEvent { event ->
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
        CardImage(
            url = item.posterUrl,
            title = item.title,
            modifier = Modifier.fillMaxSize(),
        )

        // Bottom gradient fade over image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, AFBgDeep.copy(alpha = 0.85f)),
                        startY = cardHeight.value * 0.5f,
                    ),
                ),
        )

        // Quality badge top-left (AF3: resource quality or source tag)
        val shownQuality = quality ?: item.genre.firstOrNull()
        if (!shownQuality.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(AFSpacing.px1)
                    .background(AFBgDeep.copy(alpha = 0.8f), RoundedCornerShape(AFRadius.sm))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = shownQuality,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = AFCyan,
                    maxLines = 1,
                )
            }
        }

        // AF3 badge cluster — top-right: rating + resolution badges
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(AFSpacing.px1),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            // Rating
            if (item.rating > 0f) {
                Badge(
                    text = "★ %.1f".format(item.rating),
                    color = AFBadges.rating,
                    textColor = Color.Black,
                )
            }
            // Resolution stack
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                if (item.has4k) Badge("4K", AFBadges.uhd)
                if (item.hasDolbyVision) Badge("DV", AFBadges.dolbyVision)
                if (item.hasHdr) Badge("HDR", AFBadges.hdr10)
            }
        }

        // AF3 audio badge — bottom-right
        if (item.audioCodec.contains("Atmos", ignoreCase = true) ||
            item.audioCodec.contains("TrueHD", ignoreCase = true) ||
            item.audioCodec.contains("DTS-HD", ignoreCase = true)
        ) {
            Badge(
                text = "Atmos",
                color = AFBadges.atmos,
                textColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(AFSpacing.px1),
            )
        }

        // Title + optional sub-line
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(AFSpacing.px2)
                .fillMaxWidth(),
        ) {
            Text(
                text = item.title,
                color = if (isFocused) AFCyan else AFText,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (view == CardView.Episode) {
                Text(
                    text = item.episodes.firstOrNull()?.title.orEmpty(),
                    color = AFTextDim,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Progress bar bottom (continue-watching / episode)
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(AFSurfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(AFCyan),
                )
            }
        }

        // (focus glow is rendered via Compose's elevation / soft tint by the border itself)
    }
}

@Composable
private fun CardImage(url: String, title: String, modifier: Modifier = Modifier) {
    if (url.isBlank()) {
        Box(modifier = modifier.background(AFSurfaceVariant))
    } else {
        AsyncImage(
            model = url,
            contentDescription = "$title poster",
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

@Composable
private fun Badge(
    text: String,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AFRadius.xs))
            .background(color)
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = text,
            color      = textColor,
            fontSize   = AFTypo.micro,
            fontWeight = FontWeight.Bold,
            maxLines   = 1,
        )
    }
}

