// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import com.kurostream.app.model.MediaItem
import com.kurostream.app.ui.theme.Af3Theme
import kotlinx.coroutines.delay

// =============================================================================
// Af3Card — single composable for poster, landscape and icon cards.
// Performance: graphicsLayer scale + elevation, single remember'd interaction,
// animation runs on the animation thread.
// =============================================================================

enum class Af3CardLayout { Poster, Landscape, Icon }

@Composable
fun Af3Card(
    item: MediaItem,
    layout: Af3CardLayout = Af3CardLayout.Poster,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = Af3Theme.palette
    val sizes = Af3Theme.size
    val aspect = Af3Theme.aspect

    val (width, height, focusScale) = when (layout) {
        Af3CardLayout.Poster -> Triple(
            sizes.posterW * aspect.posterScale,
            sizes.posterH * aspect.posterScale,
            sizes.posterFocusScale,
        )
        Af3CardLayout.Landscape -> Triple(
            sizes.landscapeW * aspect.landscapeScale,
            sizes.landscapeH * aspect.landscapeScale,
            sizes.landscapeFocusScale,
        )
        Af3CardLayout.Icon -> Triple(sizes.iconW, sizes.iconH, 1.10f)
    }

    val interaction = remember(item.id) { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shadow(if (focused) 6.dp else 1.dp, RoundedCornerShape(sizes.cardRadius))
            .clip(RoundedCornerShape(sizes.cardRadius))
            .background(palette.surface)
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) palette.borderFocus else Color.Transparent,
                shape = RoundedCornerShape(sizes.cardRadius),
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        when (layout) {
            Af3CardLayout.Poster, Af3CardLayout.Landscape -> {
                val url = if (layout == Af3CardLayout.Poster)
                    item.posterUrl.ifBlank { item.backdropUrl }
                else item.backdropUrl.ifBlank { item.posterUrl }
                Box(modifier = Modifier.fillMaxSize().background(palette.surfaceVariant))
                AsyncImage(
                    model = url,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (layout == Af3CardLayout.Landscape) {
                    // Bottom gradient for title readability
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.85f),
                                ),
                            ),
                    )
                }
            }
            Af3CardLayout.Icon -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(palette.accent.copy(alpha = 0.4f), palette.accentSec.copy(alpha = 0.4f)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.title,
                        color = palette.text,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }
        // Title overlay (poster shows nothing overlaid; landscape shows title at bottom)
        if (layout == Af3CardLayout.Landscape && item.title.isNotBlank()) {
            Text(
                text = item.title,
                color = palette.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp),
            )
        }
        // Progress bar (continue watching)
        if (item.watchProgress > 0L && layout == Af3CardLayout.Landscape) {
            val progress = (item.watchProgress.coerceAtLeast(0L)).toFloat()
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.Black.copy(alpha = 0.4f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceAtMost(1f))
                        .height(3.dp)
                        .background(palette.accent),
                )
            }
        }
        if (focused && onLongPress != null) {
            // Optional: future long-press hint
        }
    }
}

// =============================================================================
// Af3WidgetRow — horizontal scrolling row with title + accent bar
// =============================================================================

@Composable
fun Af3WidgetRow(
    title: String,
    items: List<MediaItem>,
    layout: Af3CardLayout = Af3CardLayout.Poster,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val palette = Af3Theme.palette
    val space = Af3Theme.space
    val aspect = Af3Theme.aspect
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = aspect.safeH, top = space.s12, bottom = space.s8),
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .background(palette.accent, RoundedCornerShape(50)),
            )
            Spacer(Modifier.width(space.s8))
            Text(
                text = title,
                color = palette.text,
                fontWeight = FontWeight.SemiBold,
                fontSize = aspect.widgetTitleSize.sp,
            )
        }
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = aspect.safeH),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(space.s12),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(items, key = { it.id }) { item ->
                Af3Card(
                    item = item,
                    layout = layout,
                    onClick = { onItemClick(item) },
                )
            }
        }
    }
}

// =============================================================================
// Af3HeroSpotlight — auto-advancing hero with parallax backdrop
// =============================================================================

@Composable
fun Af3HeroSpotlight(
    items: List<MediaItem>,
    onPlay: (MediaItem) -> Unit,
    onInfo: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    autoAdvanceMs: Long? = null,
) {
    if (items.isEmpty()) return
    val palette = Af3Theme.palette
    val sizes = Af3Theme.size
    val motion = Af3Theme.motion
    val aspect = Af3Theme.aspect

    var currentIndex by remember { mutableIntStateOf(0) }
    val tickMs = autoAdvanceMs ?: 0L
    LaunchedEffect(items.firstOrNull()?.id) {
        if (items.size <= 1 || tickMs <= 0) return@LaunchedEffect
        while (true) {
            delay(tickMs)
            currentIndex = (currentIndex + 1) % items.size
        }
    }
    val current = items.getOrNull(currentIndex) ?: items.firstOrNull() ?: return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(aspect.heroHeight.coerceAtLeast(sizes.heroH))
            .clip(RoundedCornerShape(sizes.heroRadius))
            .background(palette.surfaceVariant),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(palette.surfaceVariant))
        // Backdrop
        AsyncImage(
            model = current.backdropUrl.ifBlank { current.posterUrl },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Vignette
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color.Black.copy(alpha = 0.45f),
                        1f to Color.Black.copy(alpha = 0.85f),
                    ),
                ),
        )
        // Left scrim
        Box(
            Modifier
                .width(180.dp)
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.65f),
                        1f to Color.Transparent,
                    ),
                ),
        )
        // Metadata
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Text(
                text = current.title,
                color = palette.text,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (current.genre.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = current.genre.joinToString(" • "),
                    color = palette.textSec,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                Af3PillButton(label = "▶  Play", primary = true, onClick = { onPlay(current) })
                Af3PillButton(label = "More info", primary = false, onClick = { onInfo(current) })
            }
        }
        // Indicator dots
        if (items.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
            ) {
                items.forEachIndexed { idx, _ ->
                    val isActive = idx == currentIndex
                    Box(
                        Modifier
                            .height(3.dp)
                            .width(if (isActive) 18.dp else 8.dp)
                            .background(
                                if (isActive) palette.accent else Color.White.copy(alpha = 0.35f),
                                RoundedCornerShape(50),
                            ),
                    )
                }
            }
        }
    }
}

// =============================================================================
// Af3PillButton — primary/secondary action pill
// =============================================================================

@Composable
fun Af3PillButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Af3Theme.palette
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val bg = when {
        primary && focused -> palette.accent.copy(alpha = 0.92f)
        primary -> palette.accent
        focused -> palette.surfaceActive
        else -> palette.surfaceHighlight
    }
    val fg = if (primary) palette.bgDeep else palette.text
    val scale = if (focused) 1.05f else 1f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(2.dp, palette.borderFocus, RoundedCornerShape(50))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

// =============================================================================
// Af3HubSwitcher — pill tabs at top
// =============================================================================

data class Af3Hub(val id: String, val label: String, val icon: String)

@Composable
fun Af3HubSwitcher(
    hubs: List<Af3Hub>,
    activeIndex: Int,
    onHubSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Af3Theme.palette
    val space = Af3Theme.space
    val aspect = Af3Theme.aspect
    LazyRow(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = aspect.safeH),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(space.s8),
    ) {
        items(hubs.size) { index ->
            val hub = hubs[index]
            val isActive = index == activeIndex
            Af3HubChip(
                label = hub.label,
                icon = hub.icon,
                isActive = isActive,
                onClick = { onHubSelected(index) },
            )
        }
    }
}

@Composable
private fun Af3HubChip(
    label: String,
    icon: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val palette = Af3Theme.palette
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val bg = when {
        isActive -> palette.surfaceActive
        focused -> palette.surfaceHighlight
        else -> palette.surface
    }
    val border = when {
        isActive -> palette.accent
        focused -> palette.borderFocus
        else -> palette.border
    }
    val textColor = if (isActive || focused) palette.text else palette.textSec
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(if (focused || isActive) 2.dp else 1.dp, border, RoundedCornerShape(50))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        if (icon.isNotBlank()) {
            Text(icon, color = textColor, fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = textColor,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 14.sp,
        )
    }
}

// =============================================================================
// Af3Backdrop — ambient color wash + radial vignette for the home page.
// =============================================================================

@Composable
fun Af3Backdrop(
    backdropUrl: String?,
    modifier: Modifier = Modifier,
) {
    val palette = Af3Theme.palette
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to palette.bgDeep, 1f to palette.bg)),
    ) {
        if (!backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.55f },
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to palette.accent.copy(alpha = 0.06f),
                            0.4f to Color.Black.copy(alpha = 0.35f),
                            1f to Color.Black.copy(alpha = 0.75f),
                        ),
                    ),
            )
        }
    }
}

// =============================================================================
// Af3LoadingShimmer — single composable shimmer skeleton
// =============================================================================

@Composable
fun Af3LoadingShimmer(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 10.dp,
) {
    val palette = Af3Theme.palette
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(palette.surfaceVariant),
    )
}

// =============================================================================
// Af3EmptyState — reusable empty/error state for screens
// =============================================================================

@Composable
fun Af3EmptyState(
    icon: String = "📺",
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = Af3Theme.palette
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(icon, fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            color = palette.text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = palette.textSec,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Af3PillButton(label = actionLabel, primary = true, onClick = onAction)
        }
    }
}

// =============================================================================
// D-pad navigation helper. Wrap any focusable row so left/right arrows
// jump between focus requesters in a list.
// =============================================================================

class Af3FocusGroup {
    val requesters: MutableList<FocusRequester> = mutableListOf()
    fun get(index: Int): FocusRequester = requesters[index]
    fun add(): FocusRequester = FocusRequester().also { requesters.add(it) }
    fun size(): Int = requesters.size
}

@Composable
fun rememberAf3FocusGroup(size: Int): Af3FocusGroup {
    val group = remember(size) { Af3FocusGroup() }
    LaunchedEffect(size) {
        // Ensure capacity
        while (group.requesters.size < size) group.requesters.add(FocusRequester())
    }
    return group
}

fun Modifier.af3DpadNavigation(
    group: Af3FocusGroup,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    when (event.key) {
        Key.DirectionRight, Key.MediaNext -> {
            val next = (currentIndex + 1).coerceAtMost(group.size() - 1)
            if (next != currentIndex) {
                onIndexChange(next)
                group.get(next).requestFocus()
                true
            } else false
        }
        Key.DirectionLeft, Key.MediaPrevious -> {
            val prev = (currentIndex - 1).coerceAtLeast(0)
            if (prev != currentIndex) {
                onIndexChange(prev)
                group.get(prev).requestFocus()
                true
            } else false
        }
        else -> false
    }
}
