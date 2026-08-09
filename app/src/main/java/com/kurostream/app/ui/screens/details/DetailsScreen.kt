// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.details

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kurostream.app.model.MediaItem
import com.kurostream.app.ui.components.Af3EmptyState
import com.kurostream.app.ui.components.Af3PillButton
import com.kurostream.app.ui.components.Af3ScreenScaffold
import com.kurostream.app.ui.theme.Af3Theme

@Composable
fun DetailsScreen(
    mediaId: String,
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(mediaId) { viewModel.load(mediaId) }

    Af3ScreenScaffold(title = "Details", onBack = onBack) {
        val item = state.item
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Af3EmptyState(
                        icon = "⏳",
                        title = "Loading…",
                        subtitle = "Fetching details from ${state.provider.ifBlank { "the local repository" }}.",
                    )
                }
            }
            state.error != null -> {
                Af3EmptyState(
                    icon = "⚠",
                    title = "Couldn't load details",
                    subtitle = state.error,
                )
            }
            item == null -> {
                Af3EmptyState(
                    icon = "🔎",
                    title = "No details available",
                    subtitle = "Media ID: $mediaId",
                )
            }
            else -> DetailsContent(item, onPlay)
        }
    }
}

@Composable
private fun DetailsContent(item: MediaItem, onPlay: (String) -> Unit) {
    val palette = Af3Theme.palette
    val space = Af3Theme.space
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp),
    ) {
        item("hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(space.s16))
                    .background(palette.surfaceVariant),
            ) {
                val imgUrl = item.backdropUrl.ifBlank { item.posterUrl }
                if (imgUrl.isNotBlank()) {
                    AsyncImage(
                        model = imgUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.6f to Color.Black.copy(alpha = 0.4f),
                                1f to Color.Black.copy(alpha = 0.85f),
                            ),
                        ),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(space.s16),
                ) {
                    if (item.year > 0) {
                        Text(text = "${item.year}", color = palette.textSec, fontSize = 13.sp)
                    }
                    Text(
                        text = item.title,
                        color = palette.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.rating > 0f) {
                        Spacer(Modifier.height(4.dp))
                        Text(text = "★ ${"%.1f".format(item.rating)}", color = palette.accent, fontSize = 14.sp)
                    }
                }
            }
        }
        item("actions") {
            Spacer(Modifier.height(space.s16))
            Row(horizontalArrangement = Arrangement.spacedBy(space.s12)) {
                Af3PillButton("▶  Play", primary = true, onClick = { onPlay(item.id) })
                Af3PillButton("+ My List", primary = false, onClick = {})
                Af3PillButton("Trailer", primary = false, onClick = {})
            }
            Spacer(Modifier.height(space.s16))
        }
        if (item.genre.isNotEmpty()) {
            item("genres") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(space.s8),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    item.genre.take(5).forEach { g ->
                        Box(
                            modifier = Modifier
                                .background(palette.surfaceVariant, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(text = g, color = palette.textSec, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(space.s12))
            }
        }
        if (item.description.isNotBlank()) {
            item("description") {
                var expanded by remember { mutableStateOf(item.description.length <= 240) }
                Text(
                    text = item.description,
                    color = palette.textSec,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                )
                if (item.description.length > 240) {
                    Text(
                        text = if (expanded) "Show less" else "Show more",
                        color = palette.accent,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    expanded = !expanded
                }
                Spacer(Modifier.height(space.s16))
            }
        }
        item("source") {
            Text(
                text = "Source: ${item.source.ifBlank { "local" }}",
                color = palette.textDim,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(space.s24))
        }
    }
}
