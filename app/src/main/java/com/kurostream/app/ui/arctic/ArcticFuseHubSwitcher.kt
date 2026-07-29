// This file is part of KuroStream.
//
// ArcticFuseHubSwitcher — horizontal hub tabs with active underline indicator.
// Style matches Arctic Fuse: bg=#0B0C10, 56dp tall, uppercase bold labels with
// 2px letter-spacing, cyan underline on the active hub.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text

enum class ArcticHubTab(val label: String) {
    Home("HOME"),
    Movies("MOVIES"),
    TVShows("TV SHOWS"),
    Anime("ANIME"),
    RecentlyAdded("RECENTLY ADDED"),
    MyList("MY LIST"),
    Genres("GENRES"),
}

@Composable
fun ArcticFuseHubSwitcher(
    activeHub: ArcticHubTab,
    onSelect: (ArcticHubTab) -> Unit,
    modifier: Modifier = Modifier,
    hubs: List<ArcticHubTab> = ArcticHubTab.values().toList(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AFHub.height)
            .background(AFBgDeep)
            .border(width = 1.dp, color = AFBorder)
            .padding(horizontal = AFSpacing.safeZoneH)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AFSpacing.px8),
    ) {
        hubs.forEach { hub ->
            HubTabChip(
                hub = hub,
                isActive = activeHub == hub,
                onSelect = { onSelect(hub) },
            )
        }
    }
}

@Composable
private fun HubTabChip(hub: ArcticHubTab, isActive: Boolean, onSelect: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }

    val textColor = when {
        isActive -> AFCyan
        isFocused -> AFText
        else -> AFTextDim
    }

    Column(
        modifier = Modifier
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) {
                    onSelect(); true
                } else {
                    false
                }
            }
            .clickable(onClick = onSelect)
            .padding(vertical = AFSpacing.px4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = hub.label,
            color = textColor,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = AFHub.tabLetterSpacing,
            ),
        )

        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .height(AFHub.indicatorHeight)
                .fillMaxWidthShort()
                .background(if (isActive) AFCyan else Color.Transparent),
        )
    }
}

/** Width-of-text helper to size underline naturally without custom measurement. */
@Composable
private fun Modifier.fillMaxWidthShort(): Modifier = this.width(48.dp)
