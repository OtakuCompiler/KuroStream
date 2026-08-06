// This file is part of KuroStream.
//
// ArcticFuseInfoPanel — slide-in panel pinned to the bottom-left of the
// viewport.  When a media card reports focus, this panel mirrors the card's
// metadata (title, rating, year, runtime, genres, plot, cast).  Mirrors the
// Arctic Fuse InfoPanel.jsx layout: 420dp wide, overlay-bg background,
// rounded top-right corner.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kurostream.app.model.MediaItem

@Composable
fun ArcticFuseInfoPanel(
    item: MediaItem?,
    visible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    cast: List<String> = emptyList(),
    runtimeMinutes: Int? = null,
) {
    AnimatedVisibility(
        visible = visible && item != null,
        enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(AFMotion.panelEnter)) + fadeIn(),
        exit  = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(AFMotion.panelEnter)) + fadeOut(),
        modifier = modifier,
    ) {
        if (item != null) {
            PanelContent(
                item = item,
                onClose = onClose,
                cast = cast,
                runtimeMinutes = runtimeMinutes,
            )
        }
    }
}

@Composable
private fun PanelContent(
    item: MediaItem,
    onClose: () -> Unit,
    cast: List<String>,
    runtimeMinutes: Int?,
) {
    Column(
        modifier = Modifier
            .width(420.dp)
            .background(AFOverlay, RoundedCornerShape(topEnd = AFRadius.xl))
            .border(width = 1.dp, color = AFGlass.cardBorder, shape = RoundedCornerShape(topEnd = AFRadius.xl))
            .padding(AFSpacing.px6),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = item.title,
                color = AFText,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            CloseIconButton(onClick = onClose)
        }

        Spacer(Modifier.size(AFSpacing.px3))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item.rating > 0f) {
                IconStar(tint = AFStarGold, iconSize = 14.dp)
                Spacer(Modifier.width(AFSpacing.px1))
                Text(
                    text = "%.1f".format(item.rating),
                    color = AFStarGold,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.width(AFSpacing.px3))
            }
            if (item.year > 0) {
                Text(
                    text = item.year.toString(),
                    color = AFTextDim,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.width(AFSpacing.px3))
            }
            runtimeMinutes?.let {
                Text(
                    text = "${it}min",
                    color = AFTextDim,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                )
            }
        }

        if (item.genre.isNotEmpty()) {
            Spacer(Modifier.size(AFSpacing.px3))
            Row(horizontalArrangement = Arrangement.spacedBy(AFSpacing.px2)) {
                item.genre.forEach { g ->
                    Box(
                        modifier = Modifier
                            .background(AFTeal.copy(alpha = 0.2f), RoundedCornerShape(AFRadius.sm))
                            .padding(horizontal = AFSpacing.px2, vertical = 2.dp),
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
            Spacer(Modifier.size(AFSpacing.px3))
            Text(
                text = item.description,
                color = AFTextSec,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (cast.isNotEmpty()) {
            Spacer(Modifier.size(AFSpacing.px3))
            Row {
                Text(
                    text = "Cast: ",
                    color = AFTextSec,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = cast.joinToString(", "),
                    color = AFTextDim,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun CloseIconButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .size(28.dp)
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
            .border(
                width = if (isFocused) 1.dp else 0.dp,
                color = AFCyan,
                shape = RoundedCornerShape(AFRadius.sm),
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconClose(tint = AFTextDim)
    }
}
