// This file is part of KuroStream.
//
// ArcticFuseWidgets — horizontal carousel of cards (WidgetRow) and grid wall
// of poster cards (WidgetWall). Both mirror the Arctic Fuse Kodi skin
// heuristics: px-12 horizontal padding, title with chevron "More" affordance.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.kurostream.app.model.MediaItem

@Composable
fun ArcticFuseWidgetRow(
    title: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    onItemFocus: ((MediaItem) -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    view: CardView = CardView.Poster,
    loading: Boolean = false,
    error: String? = null,
    maxItems: Int = 15,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AFSpacing.safeZoneH, vertical = AFSpacing.px3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                color = AFText,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (onMoreClick != null) {
                WidgetMoreButton(onClick = onMoreClick)
            }
        }

        if (loading) {
            ArcticCardSkeleton(count = 6, view = view)
            return@Column
        }

        if (error != null) {
            WidgetErrorRow(message = error, onRetry = onRetry)
            return@Column
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AFSpacing.safeZoneH, vertical = AFSpacing.px3)
            ) {
                Text(
                    text = "No items yet",
                    color = AFTextDim,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
            }
            return@Column
        }

        val visibleItems = items.take(maxItems)
        LazyRow(
            contentPadding = PaddingValues(horizontal = AFSpacing.safeZoneH),
            horizontalArrangement = Arrangement.spacedBy(AFSpacing.px4),
        ) {
            items(
                items = visibleItems,
                key = { it.id },
                contentType = { "media_card" },
            ) { item ->
                ArcticFuseMediaCard(
                    item = item,
                    view = view,
                    progress = item.watchProgress.toFloat(),
                    onClick = { onItemClick(item) },
                    onFocus = { onItemFocus?.invoke(item) },
                    textColor = Color.White,
                )
            }
        }
    }
}

@Composable
fun ArcticFuseWidgetWall(
    title: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    onMoreClick: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    loading: Boolean = false,
    error: String? = null,
    maxItems: Int = 30,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AFSpacing.safeZoneH, vertical = AFSpacing.px3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                color = AFText,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (onMoreClick != null) {
                WidgetMoreButton(onClick = onMoreClick)
            }
        }

        if (loading) {
            ArcticCardSkeleton(count = 12)
            return@Column
        }

        if (error != null) {
            WidgetErrorRow(message = error, onRetry = onRetry)
            return@Column
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AFSpacing.safeZoneH, vertical = AFSpacing.px3),
            ) {
                Text(
                    text = "No items yet",
                    color = AFTextDim,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
            }
            return@Column
        }

        val visibleItems = items.take(maxItems)
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AFSpacing.safeZoneH),
            horizontalArrangement = Arrangement.spacedBy(AFSpacing.px3),
            verticalArrangement = Arrangement.spacedBy(AFSpacing.px3),
        ) {
            items(
                items = visibleItems,
                key = { it.id },
                contentType = { "media_card" },
            ) { item ->
                ArcticFuseMediaCard(
                    item = item,
                    view = CardView.Poster,
                    onClick = { onItemClick(item) },
                    textColor = Color.White,
                )
            }
        }
    }
}

/**
 * Error display row used by both [ArcticFuseWidgetRow] and [ArcticFuseWidgetWall].
 * Shows the error message with an optional retry button.
 */
@Composable
private fun WidgetErrorRow(
    message: String,
    onRetry: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AFSpacing.safeZoneH, vertical = AFSpacing.px3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            color = AFDanger,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (onRetry != null) {
            WidgetRetryButton(onClick = onRetry)
        }
    }
}

/**
 * Retry button used inside [WidgetErrorRow].
 * TV-safe with D-pad focus handling (focusable, clickable, key events).
 */
@Composable
private fun WidgetRetryButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .padding(start = AFSpacing.px3)
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) { onClick(); true } else { false }
            }
            .clickable(onClick = onClick)
            .background(
                if (isFocused) AFSurfaceHighlight else AFSurface,
                RoundedCornerShape(AFRadius.sm),
            )
            .padding(horizontal = AFSpacing.px4, vertical = AFSpacing.px2),
    ) {
        Text(
            text = "Retry",
            color = if (isFocused) AFText else AFCyan,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
private fun WidgetMoreButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    Text(
        text = "More",
        color = if (isFocused) AFCyan else AFTeal,
        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            .copy(fontWeight = FontWeight.Medium),
        modifier = Modifier
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) {
                    onClick(); true
                } else {
                    false
                }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}
