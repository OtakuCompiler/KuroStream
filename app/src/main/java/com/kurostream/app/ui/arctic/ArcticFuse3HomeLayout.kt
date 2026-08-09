// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil.compose.AsyncImage
import com.kurostream.app.model.MediaItem
import com.kurostream.app.ui.components.MediaCard
import kotlinx.coroutines.delay

/**
 * AF3 Home Layout — restructured Arctic Fuse 3 home screen with:
 * - Hero spotlight (auto-advancing, parallax backdrop, animated vignette)
 * - Hub switcher at the top (Home / Movies / Series / Search / My List)
 * - Combined widget rows (Continue Watching, Trending, New Releases, etc.)
 * - Smooth focus transitions with elevation + scale + glow
 *
 * Performance notes:
 * - Widget rows use `key()` for stable composition
 * - LazyRow with stable item identity (MediaItem.id)
 * - Backdrop uses image-cache-friendly Coil without recomposition churn
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ArcticFuse3HomeLayout(
    heroItems: List<MediaItem>,
    widgets: List<Af3WidgetRow>,
    hubs: List<Af3Hub> = Af3HubDefaults.Default,
    initialHubIndex: Int = 0,
    reduceMotion: Boolean = false,
    onMediaClick: (MediaItem) -> Unit,
    onHeroPlay: (MediaItem) -> Unit,
    onHubChange: (Int, Af3Hub) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val tokens = LocalArcticFuse3Tokens.current
    val palette = LocalArcticFusePalette.current

    var currentHeroIndex by remember(heroItems) { mutableIntStateOf(0) }
    var currentHubIndex by remember { mutableIntStateOf(initialHubIndex.coerceIn(0, hubs.lastIndex.coerceAtLeast(0))) }

    // Auto-advance hero (only when there are multiple items and reduceMotion is off)
    LaunchedEffect(heroItems, reduceMotion) {
        if (reduceMotion || heroItems.size <= 1) return@LaunchedEffect
        while (true) {
            delay(7_000)
            currentHeroIndex = (currentHeroIndex + 1) % heroItems.size
        }
    }

    val currentHero = heroItems.getOrNull(currentHeroIndex)

    Box(modifier = modifier.fillMaxSize().background(palette.bg)) {
        // ===== AMBIENT BACKDROP =====
        Af3Backdrop(
            backdropUrl = currentHero?.backdropUrl ?: currentHero?.posterUrl,
            accent = palette.cyan,
            reduceMotion = reduceMotion,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ===== HUB SWITCHER =====
            Af3HubSwitcher(
                hubs = hubs,
                activeIndex = currentHubIndex,
                onHubSelected = { index ->
                    currentHubIndex = index
                    onHubChange(index, hubs[index])
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = tokens.hubInset, top = tokens.space16, bottom = tokens.space8),
            )

            // ===== HERO SPOTLIGHT =====
            if (currentHero != null) {
                Af3HeroSpotlight(
                    item = currentHero,
                    onPlay = { onHeroPlay(currentHero) },
                    onInfo = { onMediaClick(currentHero) },
                    reduceMotion = reduceMotion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = tokens.hubInset)
                        .padding(top = tokens.space8),
                )
            }

            // ===== COMBINED WIDGET ROWS =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = tokens.space16)
                    .weight(1f),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(tokens.space16),
            ) {
                widgets.forEach { widget ->
                    key(widget.id) {
                        Af3WidgetRowView(widget = widget, onItemClick = onMediaClick)
                    }
                }
                Spacer(Modifier.height(tokens.space24))
            }
        }
    }
}

/**
 * Compact data class for an AF3 widget row.
 */
data class Af3WidgetRow(
    val id: String,
    val title: String,
    val items: List<MediaItem>,
    val layout: Af3WidgetLayout = Af3WidgetLayout.Poster,
    val onItemClick: (MediaItem) -> Unit = {},
)

enum class Af3WidgetLayout { Poster, Landscape, Icon, Square }

data class Af3Hub(val id: String, val label: String, val icon: String = "")

object Af3HubDefaults {
    val Default: List<Af3Hub> = listOf(
        Af3Hub("home", "Home", "⌂"),
        Af3Hub("movies", "Movies", "🎬"),
        Af3Hub("series", "Series", "📺"),
        Af3Hub("search", "Search", "🔍"),
        Af3Hub("list", "My List", "★"),
    )
}

/**
 * AF3-style hub switcher: pill tabs across the top.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Af3HubSwitcher(
    hubs: List<Af3Hub>,
    activeIndex: Int,
    onHubSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalArcticFuse3Tokens.current
    val palette = LocalArcticFusePalette.current

    LazyRow(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(tokens.space8),
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Af3HubChip(
    label: String,
    icon: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalArcticFuse3Tokens.current
    val palette = LocalArcticFusePalette.current

    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val bg = when {
        isActive -> palette.surfaceActive
        focused -> palette.surfaceHighlight
        else -> palette.surface
    }
    val border = if (isActive) palette.cyan else if (focused) palette.borderFocus else palette.border
    val textColor = when {
        isActive -> palette.text
        focused -> palette.text
        else -> palette.textSec
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .graphicsLayer { scaleX = if (focused) 1.06f else 1f; scaleY = if (focused) 1.06f else 1f }
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(
                width = if (focused || isActive) tokens.hubFocusBorder else 1.dp,
                color = border,
                shape = RoundedCornerShape(50),
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = tokens.hubPadding, vertical = tokens.space8),
    ) {
        if (icon.isNotBlank()) {
            Text(icon, color = textColor, fontSize = 14.sp)
            Spacer(Modifier.width(tokens.space6))
        }
        Text(
            text = label,
            color = textColor,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 14.sp,
        )
    }
}

/**
 * AF3 widget row — title + horizontal scroller of poster/landscape cards.
 */
@Composable
fun Af3WidgetRowView(
    widget: Af3WidgetRow,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalArcticFuse3Tokens.current
    val palette = LocalArcticFusePalette.current

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.hubInset),
        ) {
            Box(
                Modifier
                    .size(width = 3.dp, height = 16.dp)
                    .background(palette.cyan, RoundedCornerShape(50)),
            )
            Spacer(Modifier.width(tokens.space8))
            Text(
                text = widget.title,
                color = palette.text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
        Spacer(Modifier.height(tokens.space8))
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = tokens.hubInset),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(tokens.space12),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(widget.items.size, key = { idx -> "${widget.id}_${widget.items[idx].id}" }) { idx ->
                val item = widget.items[idx]
                key(item.id) {
                    Af3TypedCard(
                        item = item,
                        layout = widget.layout,
                        onClick = { onItemClick(item) },
                    )
                }
            }
        }
    }
}

/**
 * AF3 hero spotlight with animated vignette + ambient color overlay.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Af3HeroSpotlight(
    item: MediaItem,
    onPlay: () -> Unit,
    onInfo: () -> Unit,
    reduceMotion: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalArcticFuse3Tokens.current
    val palette = LocalArcticFusePalette.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(item.id) {
        // No automatic focus; user navigates to it from the row above.
    }

    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(tokens.spotlightHeight)
            .graphicsLayer {
                scaleX = if (focused) 1.02f else 1f
                scaleY = if (focused) 1.02f else 1f
            }
            .shadow(if (focused) tokens.elevationSpotlight else tokens.elevationHigh, RoundedCornerShape(tokens.spotlightCorner))
            .clip(RoundedCornerShape(tokens.spotlightCorner))
            .background(palette.surfaceVariant),
    ) {
        // Backdrop image area
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.backdropUrl.ifBlank { item.posterUrl },
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Vignette gradient
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
            // Left scrim (toward sidebar)
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(180.dp)
                    .background(
                        Brush.horizontalGradient(
                            0f to Color.Black.copy(alpha = 0.65f),
                            1f to Color.Transparent,
                        ),
                    ),
            )
        }

        // Metadata
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(tokens.space24),
        ) {
            Text(
                text = item.title,
                color = palette.text,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
            )
            Spacer(Modifier.height(tokens.space4))
            Text(
                text = item.genre.joinToString(" • "),
                color = palette.textSec,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(tokens.space12))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(tokens.space12)) {
                Af3SpotlightCta(label = "▶  Play", primary = true, onClick = onPlay)
                Af3SpotlightCta(label = "More info", primary = false, onClick = onInfo)
            }
        }
    }
}

@Composable
private fun Af3SpotlightCta(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalArcticFuse3Tokens.current
    val palette = LocalArcticFusePalette.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val bg = when {
        primary && focused -> palette.cyan.copy(alpha = 0.92f)
        primary -> palette.cyan
        focused -> palette.surfaceActive
        else -> palette.surfaceHighlight
    }
    val fg = if (primary) Color.Black else palette.text
    val scale = if (focused) 1.05f else 1f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(tokens.focusBorderWidth, palette.borderFocus, RoundedCornerShape(50))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = tokens.space20, vertical = tokens.space8),
    ) {
        Text(
            text = label,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
    }
}

/**
 * AF3 ambient backdrop with parallax + soft blur.
 * When `reduceMotion` is true, the parallax is disabled.
 */
@Composable
fun Af3Backdrop(
    backdropUrl: String?,
    accent: Color,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalArcticFusePalette.current
    val tokens = LocalArcticFuse3Tokens.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to palette.bgDeep,
                    1f to palette.bg,
                ),
            ),
    ) {
        if (!backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (!reduceMotion) {
                            val f = tokens.parallaxFactor
                            scaleX = 1f + f
                            scaleY = 1f + f
                        }
                    },
            )
            // Heavy vignette + ambient color wash
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to accent.copy(alpha = tokens.ambientTintAlpha),
                            0.4f to Color.Black.copy(alpha = 0.35f),
                            1f to Color.Black.copy(alpha = 0.75f),
                        ),
                    ),
            )
        }
    }
}

/**
 * AF3 typed card — switches between Poster / Landscape / Icon layouts.
 * Stable identity via `key(item.id)` is expected at the call site.
 */
@Composable
private fun Af3TypedCard(
    item: MediaItem,
    layout: Af3WidgetLayout,
    onClick: () -> Unit,
) {
    val tokens = LocalArcticFuse3Tokens.current
    when (layout) {
        Af3WidgetLayout.Landscape -> Af3FocusCard(
            onClick = onClick,
            focusedScale = tokens.landscapeFocusScale,
            baseElevationDp = 2,
            focusedElevationDp = 8,
            cornerRadiusDp = tokens.radiusLandscape.value.toInt().coerceAtLeast(1),
            modifier = Modifier
                .width(tokens.landscapeWidth)
                .height(tokens.landscapeHeight),
        ) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.backdropUrl.ifBlank { item.posterUrl },
                    contentDescription = item.title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Af3WidgetLayout.Icon -> Af3FocusCard(
            onClick = onClick,
            focusedScale = 1.10f,
            baseElevationDp = 2,
            focusedElevationDp = 8,
            cornerRadiusDp = tokens.radiusMedium.value.toInt().coerceAtLeast(1),
            modifier = Modifier
                .width(tokens.iconCardSize * 2)
                .height(tokens.iconCardSize),
        ) {
            Box(Modifier.fillMaxSize()) {
                Text(
                    text = item.title,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        Af3WidgetLayout.Poster, Af3WidgetLayout.Square -> Af3FocusCard(
            onClick = onClick,
            focusedScale = tokens.posterFocusScale,
            baseElevationDp = 2,
            focusedElevationDp = 12,
            cornerRadiusDp = tokens.radiusPoster.value.toInt().coerceAtLeast(1),
            modifier = Modifier
                .width(tokens.posterWidth)
                .height(tokens.posterHeight),
        ) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.posterUrl.ifBlank { item.backdropUrl },
                    contentDescription = item.title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
