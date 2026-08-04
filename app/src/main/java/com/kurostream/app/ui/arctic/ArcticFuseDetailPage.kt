// This file is part of KuroStream.
//
// ArcticFuseDetailPage — full-screen detail overlay matching Arctic Fuse
// DetailPage.jsx: backdrop header, poster + metadata column, action buttons,
// and tabbed content (Overview / Cast / Related / Episodes).
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kurostream.app.model.MediaItem

private val detailTabs = listOf("Overview", "Cast", "Related", "Episodes")

@Composable
fun ArcticFuseDetailPage(
    item: MediaItem?,
    visible: Boolean,
    onClose: () -> Unit,
    onPlay: (MediaItem) -> Unit,
    onAddWatchlist: ((MediaItem) -> Unit)? = null,
    onShare: ((MediaItem) -> Unit)? = null,
    onMediaClick: ((String) -> Unit)? = null,
    relatedItems: List<MediaItem> = emptyList(),
    cast: List<String> = emptyList(),
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible && item != null,
        enter = fadeIn(animationSpec = tween(AFMotion.pageEnter)),
        exit = fadeOut(animationSpec = tween(AFMotion.pageEnter)),
        modifier = modifier.fillMaxSize(),
    ) {
        if (item != null) {
            DetailContent(
                item = item,
                onClose = onClose,
                onPlay = { onPlay(item) },
                onAddWatchlist = { onAddWatchlist?.invoke(item) },
                onShare = { onShare?.invoke(item) },
                onMediaClick = onMediaClick,
                relatedItems = relatedItems,
                cast = cast,
            )
        }
    }
}

@Composable
private fun DetailContent(
    item: MediaItem,
    onClose: () -> Unit,
    onPlay: () -> Unit,
    onAddWatchlist: () -> Unit,
    onShare: () -> Unit,
    onMediaClick: ((String) -> Unit)?,
    relatedItems: List<MediaItem>,
    cast: List<String>,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AFBgDeep),
    ) {
        // Backdrop header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
        ) {
            if (item.backdropUrl.isNotBlank()) {
                AsyncImage(
                    model = item.backdropUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (item.posterUrl.isNotBlank()) {
                AsyncImage(
                    model = item.posterUrl,
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
                            colors = listOf(
                                Color.Transparent,
                                AFBgDeep.copy(alpha = 0.3f),
                                AFBgDeep,
                            ),
                        ),
                    ),
            )
            DetailCloseButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(AFSpacing.px6),
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AFSpacing.safeZoneH)
                .padding(top = (-80).dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AFSpacing.px8),
            ) {
                // Poster
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .clip(RoundedCornerShape(AFRadius.xl))
                        .background(AFSurface),
                ) {
                    if (item.posterUrl.isNotBlank()) {
                        AsyncImage(
                            model = item.posterUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                        )
                    }
                }

                // Info column
                Column(modifier = Modifier.weight(1f)) {
                    Spacer(Modifier.height(AFSpacing.px4))
                    Text(
                        text = item.title,
                        color = AFText,
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(AFSpacing.px2))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.rating > 0f) {
                            IconStar(tint = AFStarGold, iconSize = 14.dp)
                            Spacer(Modifier.width(AFSpacing.px1))
                            Text(
                                text = "%.1f".format(item.rating),
                                color = AFStarGold,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.width(AFSpacing.px3))
                        }
                        if (item.year > 0) {
                            Text(
                                text = item.year.toString(),
                                color = AFTextDim,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.width(AFSpacing.px3))
                        }
                        if (item.duration > 0) {
                            Text(
                                text = "${item.duration}min",
                                color = AFTextDim,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.width(AFSpacing.px3))
                        }
                        Text(
                            text = if (item.episodes.isNotEmpty()) "${item.episodes.size} eps" else "Movie",
                            color = AFCyan,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        )
                    }

                    if (item.genre.isNotEmpty()) {
                        Spacer(Modifier.height(AFSpacing.px3))
                        Row(horizontalArrangement = Arrangement.spacedBy(AFSpacing.px2)) {
                            item.genre.forEach { g ->
                                Box(
                                    modifier = Modifier
                                        .border(width = 1.dp, color = AFTeal.copy(alpha = 0.3f), shape = RoundedCornerShape(AFRadius.pill))
                                        .padding(horizontal = AFSpacing.px3, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = g,
                                        color = AFTeal,
                                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }

                    if (item.description.isNotBlank()) {
                        Spacer(Modifier.height(AFSpacing.px4))
                        Text(
                            text = item.description,
                            color = AFTextSec,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(Modifier.height(AFSpacing.px5))

                    Row(horizontalArrangement = Arrangement.spacedBy(AFSpacing.px3)) {
                        DetailCTA(
                            label = "Play",
                            icon = { IconPlay(tint = AFBgDeep, iconSize = 18.dp) },
                            primary = true,
                            onClick = onPlay,
                        )
                        DetailCTA(
                            label = "Watchlist",
                            icon = { IconAdd(tint = AFText, iconSize = 18.dp) },
                            primary = false,
                            onClick = onAddWatchlist,
                        )
                        DetailCTA(
                            label = "Share",
                            icon = { IconInfo(tint = AFText, iconSize = 18.dp) },
                            primary = false,
                            onClick = onShare,
                        )
                    }

                    if (cast.isNotEmpty()) {
                        Spacer(Modifier.height(AFSpacing.px4))
                        Text(
                            text = "Cast: ${cast.joinToString(", ")}",
                            color = AFTextDim,
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Tabs
            Spacer(Modifier.height(AFSpacing.px8))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = AFBorder),
                horizontalArrangement = Arrangement.spacedBy(AFSpacing.px6),
            ) {
                detailTabs.forEachIndexed { idx, tab ->
                    DetailTab(
                        label = tab,
                        isSelected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                    )
                }
            }

            Spacer(Modifier.height(AFSpacing.px6))

            // Tab content
            when (selectedTab) {
                0 -> OverviewTab(item)
                1 -> CastTab(cast)
                2 -> RelatedTab(relatedItems, onMediaClick)
                3 -> EpisodesTab(item)
            }

            Spacer(Modifier.height(AFSpacing.px16))
        }
    }
}

@Composable
private fun DetailCloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    Box(
        modifier = modifier
            .size(36.dp)
            .background(AFBgDeep.copy(alpha = 0.8f), RoundedCornerShape(AFRadius.md))
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) { onClick(); true } else false
            }
            .clickable(onClick = onClick)
            .border(width = if (isFocused) 1.dp else 0.dp, color = AFCyan, shape = RoundedCornerShape(AFRadius.md))
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconClose(tint = AFText)
    }
}

@Composable
private fun DetailCTA(
    label: String,
    icon: @Composable () -> Unit,
    primary: Boolean,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    val bg = when {
        primary && isFocused -> AFCyan.copy(alpha = 0.9f)
        primary -> AFCyan
        isFocused -> AFSurface
        else -> Color.Transparent
    }
    val textColor = if (primary) AFBgDeep else AFText
    val borderColor = if (isFocused || primary) AFCyan else AFBorder

    Row(
        modifier = Modifier
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) { onClick(); true } else false
            }
            .clickable(onClick = onClick)
            .background(bg, RoundedCornerShape(AFRadius.lg))
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(AFRadius.lg))
            .padding(horizontal = AFSpacing.px6, vertical = AFSpacing.px3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AFSpacing.px2),
    ) {
        icon()
        Text(
            text = label,
            color = textColor,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun DetailTab(label: String, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) { onClick(); true } else false
            }
            .clickable(onClick = onClick)
            .padding(vertical = AFSpacing.px3),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = when {
                    isSelected -> AFCyan
                    isFocused -> AFText
                    else -> AFTextDim
                },
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
            if (isSelected) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(AFCyan),
                )
            }
        }
    }
}

@Composable
private fun OverviewTab(item: MediaItem) {
    Column {
        Text(
            text = item.description.ifBlank { "No description available." },
            color = AFTextSec,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(AFSpacing.px6))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AFSpacing.px8),
        ) {
            if (item.year > 0) {
                DetailMetaItem("Year", item.year.toString())
            }
            if (item.duration > 0) {
                DetailMetaItem("Runtime", "${item.duration} min")
            }
            if (item.episodes.isNotEmpty()) {
                DetailMetaItem("Episodes", "${item.episodes.size}")
            }
            if (item.rating > 0f) {
                DetailMetaItem("Rating", "%.1f/10".format(item.rating))
            }
        }
    }
}

@Composable
private fun DetailMetaItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = AFTextDim,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        )
        Text(
            text = value,
            color = AFText,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CastTab(cast: List<String>) {
    if (cast.isEmpty()) {
        Text(
            text = "Cast information will appear here.",
            color = AFTextDim,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )
    } else {
        Text(
            text = cast.joinToString(", "),
            color = AFTextSec,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun RelatedTab(relatedItems: List<MediaItem>, onMediaClick: ((String) -> Unit)?) {
    if (relatedItems.isEmpty()) {
        Text(
            text = "Related content will appear here.",
            color = AFTextDim,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )
    } else {
        ArcticFuseWidgetRow(
            title = "Related",
            items = relatedItems,
            onItemClick = { item -> onMediaClick?.invoke(item.id) },
            view = CardView.Poster,
        )
    }
}

@Composable
private fun EpisodesTab(item: MediaItem) {
    if (item.episodes.isEmpty()) {
        Text(
            text = "Episode list will appear here.",
            color = AFTextDim,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(AFSpacing.px2)) {
            item.episodes.forEach { ep ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AFSurface, RoundedCornerShape(AFRadius.md))
                        .padding(AFSpacing.px3),
                    horizontalArrangement = Arrangement.spacedBy(AFSpacing.px3),
                ) {
                    Text(
                        text = "E${ep.number}",
                        color = AFCyan,
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = ep.title,
                        color = AFText,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}