// This file is part of KuroStream.
//
// ArcticFuseContextMenu — long-press / D-pad-center-hold menu that pops up
// over a media card.  Mirrors Arctic Fuse ContextMenu.jsx: floating panel
// with Play / Watchlist / Favourites / Mark Watched / Info actions.
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kurostream.app.model.MediaItem

enum class ArcticContextAction { Play, Watchlist, Favourite, MarkWatched, Info }

@Composable
fun ArcticFuseContextMenu(
    item: MediaItem?,
    visible: Boolean,
    onClose: () -> Unit,
    onAction: (ArcticContextAction, MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    anchorOffset: IntOffset = IntOffset(0, 0),
) {
    AnimatedVisibility(
        visible = visible && item != null,
        enter = fadeIn(animationSpec = tween(AFMotion.fast)),
        exit = fadeOut(animationSpec = tween(AFMotion.fast)),
        modifier = modifier,
    ) {
        if (item != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Click-away scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onClose)
            .semantics {
                contentDescription = "Close context menu"
                onClick(label = "Close context menu", action = null)
            },
                )
                Column(
                    modifier = Modifier
                        .offset(x = anchorOffset.x.dp, y = anchorOffset.y.dp)
                        .widthIn(min = 200.dp)
                        .background(AFOverlay, RoundedCornerShape(AFRadius.lg))
                        .border(width = 1.dp, color = AFBorderStrong, shape = RoundedCornerShape(AFRadius.lg))
                        .padding(vertical = AFSpacing.px2),
                ) {
                    ContextRow(action = ArcticContextAction.Play, label = "Play", icon = { IconPlay() }, onClick = { onAction(it, item); onClose() })
                    ContextRow(action = ArcticContextAction.Watchlist, label = "Add to Watchlist", icon = { IconAdd() }, onClick = { onAction(it, item); onClose() })
                    ContextRow(action = ArcticContextAction.Favourite, label = "Add to Favourites", icon = { IconFav() }, onClick = { onAction(it, item); onClose() })
                    ContextRow(action = ArcticContextAction.MarkWatched, label = "Mark as Watched", icon = { IconCheck() }, onClick = { onAction(it, item); onClose() })
                    ContextRow(action = ArcticContextAction.Info, label = "Info", icon = { IconInfo() }, onClick = { onAction(it, item); onClose() })
                }
            }
        }
    }
}

@Composable
private fun ContextRow(
    action: ArcticContextAction,
    label: String,
    icon: @Composable () -> Unit,
    onClick: (ArcticContextAction) -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) {
                    onClick(action); true
                } else {
                    false
                }
            }
            .clickable(onClick = { onClick(action) })
            .semantics {
                contentDescription = label
                onClick(label = label, action = null)
            }
            .background(if (isFocused) AFSurface else Color.Transparent)
            .padding(horizontal = AFSpacing.px4, vertical = AFSpacing.px3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AFSpacing.px3),
    ) {
        icon()
        Text(
            text = label,
            color = if (isFocused) AFText else AFTextSec,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        )
    }
}