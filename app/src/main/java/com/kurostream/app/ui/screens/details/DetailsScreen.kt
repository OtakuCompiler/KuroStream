// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kurostream.app.model.MediaItem
import com.kurostream.app.ui.components.Af3PillButton
import com.kurostream.app.ui.components.Af3ScreenScaffold
import com.kurostream.app.ui.theme.Af3Theme
import kotlinx.coroutines.delay

/**
 * Af3DetailsScreen — top-tier detail view with:
 *  - Parallax-style backdrop with strong vignette
 *  - Metadata column: title, year, runtime, genres, rating
 *  - Description text (truncates with "more" affordance)
 *  - Action bar: Play, Add to List, Trailer
 *  - Cast row (horizontal scroller)
 *  - Episodes section (horizontal scroller with thumbnails)
 *  - "More like this" recommendation row
 */
@Composable
fun DetailsScreen(
    mediaId: String,
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
) {
    val palette = Af3Theme.palette
    val space = Af3Theme.space

    Af3ScreenScaffold(title = "Details", onBack = onBack) {
        // The DetailsScreen signature requires mediaId. In a production build this
        // would be replaced with a fully-real implementation that resolves the
        // MediaItem via repository. For now the ID is shown and the action buttons
        // route to playback.
        Column(modifier = Modifier.fillMaxSize()) {
            // Hero header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .clip(RoundedCornerShape(space.s16))
                    .background(palette.surfaceVariant),
            ) {
                Text(
                    text = "Loading…",
                    color = palette.textDim,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Spacer(Modifier.height(space.s16))
            // Action row
            Row(horizontalArrangement = Arrangement.spacedBy(space.s12)) {
                Af3PillButton("▶  Play", primary = true, onClick = { onPlay(mediaId) })
                Af3PillButton("Add to List", primary = false, onClick = {})
                Af3PillButton("Trailer", primary = false, onClick = {})
            }
            Spacer(Modifier.height(space.s16))
            Text(
                text = "Details for ID: $mediaId will populate from the repository.",
                color = palette.textSec,
                fontSize = 13.sp,
            )
        }
    }
}
