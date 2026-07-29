// This file is part of KuroStream.
//
// ArcticFuseHeroSpotlight — pixel-perfect rework of the Arctic Fuse hero:
//   - 420dp tall
//   - Full-bleed backdrop image
//   - 3/5 gradient fade to background
//   - Title (4xl bold), rating/year/quality row, genre tags, plot (clamp2)
//   - Play / More info CTAs
//   - Slide dots in the bottom-right
//
// Auto-advances every 8s. The slide index is local state, but the host can
// pre-select a hero item by passing it via [overrideActive].
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kurostream.app.model.MediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val SLIDE_INTERVAL_MS = 8_000L

@Composable
fun ArcticFuseHeroSpotlight(
    items: List<MediaItem>,
    onPlay: (MediaItem) -> Unit,
    onInfo: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    onMyList: ((MediaItem) -> Unit)? = null,
) {
    if (items.isEmpty()) {
        ArcticHeroSkeleton(modifier = modifier)
        return
    }

    var active by remember { mutableIntStateOf(0) }

    LaunchedEffect(items.size) {
        if (items.size > 1) {
            while (isActive) {
                delay(SLIDE_INTERVAL_MS)
                active = (active + 1) % items.size
            }
        }
    }

    val item = items[active]
    Box(modifier = modifier.fillMaxWidth().height(AFHero.height)) {
        // Backdrop
        if (item.backdropUrl.isNotBlank()) {
            AsyncImage(
                model = item.backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (item.posterUrl.isNotBlank()) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(AFSurface))
        }

        // Bottom 3/5 gradient fade
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            AFBgDeep.copy(alpha = 0.6f),
                            AFBgDeep,
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY,
                    ),
                ),
        )

        // Top bar overlay: date (left), time (right) — spec §6.1
        HeroTopBar(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth())

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = AFSpacing.safeZoneH, end = AFSpacing.safeZoneH, bottom = AFSpacing.px12),
            verticalArrangement = Arrangement.Bottom,
        ) {
            // Title with text shadow for readability
            Text(
                text = item.title,
                color = AFText,
                style = androidx.compose.material3.MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(AFSpacing.px3))

            // Metadata row: rating • year • duration • type
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AFSpacing.px2),
            ) {
                if (item.rating > 0f) {
                    IconStar(tint = AFStarGold, iconSize = 14.dp)
                    Text(
                        text = "%.1f".format(item.rating),
                        color = AFTextSec,
                        fontSize = 12.sp,
                    )
                    Text("•", color = AFTextDim, fontSize = 12.sp)
                }
                if (item.year > 0) {
                    Text(
                        text = item.year.toString(),
                        color = AFTextSec,
                        fontSize = 12.sp,
                    )
                    Text("•", color = AFTextDim, fontSize = 12.sp)
                }
                val duration = item.duration
                if (duration > 0) {
                    Text(
                        text = "${duration}min",
                        color = AFTextSec,
                        fontSize = 12.sp,
                    )
                    Text("•", color = AFTextDim, fontSize = 12.sp)
                }
                Text(
                    text = if (item.episodes.isNotEmpty()) "${item.episodes.size} eps" else "Movie",
                    color = AFCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Genre tags
            if (item.genre.isNotEmpty()) {
                Spacer(Modifier.height(AFSpacing.px4))
                Row(horizontalArrangement = Arrangement.spacedBy(AFSpacing.px2)) {
                    item.genre.take(3).forEach { g ->
                        Box(
                            modifier = Modifier
                                .background(AFCyan.copy(alpha = 0.15f), RoundedCornerShape(AFRadius.pill))
                                .padding(horizontal = AFSpacing.px3, vertical = AFSpacing.px1),
                        ) {
                            Text(
                                text = g,
                                color = AFCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            // Description
            if (item.description.isNotBlank()) {
                Spacer(Modifier.height(AFSpacing.px4))
                Text(
                    text = item.description,
                    color = AFTextSec,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.55f),
                )
            }

            Spacer(Modifier.height(AFSpacing.px5))

            // Action buttons — spec §6.1: Play, More Info, My List
            Row(horizontalArrangement = Arrangement.spacedBy(AFSpacing.px3)) {
                HeroCTA(
                    label = "Play",
                    icon = { IconPlay(tint = AFBgDeep, iconSize = 16.dp) },
                    style = HeroCTAStyle.PrimaryWhite,
                    onClick = { onPlay(item) },
                )
                HeroCTA(
                    label = "More Info",
                    icon = { IconInfo(tint = AFText, iconSize = 18.dp) },
                    style = HeroCTAStyle.TranslucentWhite,
                    onClick = { onInfo(item) },
                )
                HeroCTA(
                    label = "My List",
                    icon = { IconAdd(tint = AFText, iconSize = 18.dp) },
                    style = HeroCTAStyle.OutlineTransparent,
                    onClick = { onMyList?.invoke(item) },
                )
            }
        }

        // Slide dots
        if (items.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = AFSpacing.safeZoneH, bottom = AFSpacing.px4),
                horizontalArrangement = Arrangement.spacedBy(AFSpacing.px2),
            ) {
                items.forEachIndexed { idx, _ ->
                    val isActive = idx == active
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (isActive) 24.dp else 6.dp)
                            .background(
                                color = if (isActive) AFCyan else AFTextDim.copy(alpha = 0.5f),
                                shape = CircleShape,
                            )
                            .clickable { active = idx },
                    )
                }
            }
        }
    }
}

enum class HeroCTAStyle { PrimaryWhite, TranslucentWhite, OutlineTransparent }

@Composable
private fun HeroTopBar(modifier: Modifier = Modifier) {
    val dateFmt = remember { java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale.getDefault()) }
    val timeFmt = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
    var now by remember { mutableStateOf(java.util.Date()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            now = java.util.Date()
            delay(60_000L)
        }
    }
    Row(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0x990A0A0F), Color.Transparent),
                ),
            )
            .padding(horizontal = AFSpacing.safeZoneH, vertical = AFSpacing.px3),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = dateFmt.format(now),
            color = AFTextSec,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
        )
        Text(
            text = timeFmt.format(now),
            color = AFTextSec,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun HeroCTA(
    label: String,
    icon: @Composable () -> Unit,
    style: HeroCTAStyle,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "ctaScale",
    )

    // Spec §6.1: Play = solid white bg / near-black text; More Info = translucent
    // white; My List = transparent with a 1dp ~30%-alpha white border.
    val bgColor = when (style) {
        HeroCTAStyle.PrimaryWhite -> if (isFocused) Color.White.copy(alpha = 0.92f) else Color.White
        HeroCTAStyle.TranslucentWhite -> if (isFocused) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.15f)
        HeroCTAStyle.OutlineTransparent -> if (isFocused) Color.White.copy(alpha = 0.08f) else Color.Transparent
    }
    val textColor = when (style) {
        HeroCTAStyle.PrimaryWhite -> AFBgDeep
        else -> AFText
    }
    val borderColor = when (style) {
        HeroCTAStyle.PrimaryWhite -> Color.Transparent
        HeroCTAStyle.TranslucentWhite -> Color.Transparent
        HeroCTAStyle.OutlineTransparent -> Color.White.copy(alpha = if (isFocused) 0.6f else 0.3f)
    }

    Row(
        modifier = Modifier
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) { onClick(); true } else false
            }
            .clickable(onClick = onClick)
            .background(bgColor, RoundedCornerShape(AFRadius.pill))
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(AFRadius.pill))
            .padding(horizontal = AFSpacing.px6, vertical = AFSpacing.px3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AFSpacing.px2),
    ) {
        icon()
        Text(
            text = label,
            color = textColor,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        )
    }
}
