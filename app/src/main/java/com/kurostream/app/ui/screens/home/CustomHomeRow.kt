// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.app.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kurostream.app.model.MediaItem
import com.kurostream.app.ui.components.MediaCard
import com.kurostream.app.ui.theme.TvFocusBorder
import com.kurostream.app.ui.theme.TvOnSurfaceVariant
import com.kurostream.app.ui.theme.TvSurfaceHighlight
import kotlin.math.roundToInt

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun CustomHomeRow(
    title: String,
    items: List<MediaItem>,
    filterGenres: List<String> = emptyList(),
    filterStudios: List<String> = emptyList(),
    filterYear: Int? = null,
    onReorder: (List<MediaItem>) -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredItems = remember(items, filterGenres, filterStudios, filterYear) {
        items.filter { item ->
            val genreMatch = filterGenres.isEmpty() || item.genres.any { it in filterGenres }
            val studioMatch = filterStudios.isEmpty() || item.format in filterStudios
            val yearMatch = filterYear == null || item.year == filterYear
            genreMatch && studioMatch && yearMatch
        }
    }
    
    val rowItems = remember { mutableStateListOf<MediaItem>().apply { addAll(filteredItems) } }
    
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (filterGenres.isNotEmpty() || filterStudios.isNotEmpty() || filterYear != null) {
                FilterBadges(
                    genres = filterGenres,
                    studios = filterStudios,
                    year = filterYear
                )
            }
        }
        
        ReorderableMediaRow(
            items = rowItems,
            onReorder = { newOrder ->
                rowItems.clear()
                rowItems.addAll(newOrder)
                onReorder(newOrder)
            },
            onItemClick = onItemClick
        )
    }
}

@Composable
private fun FilterBadges(
    genres: List<String>,
    studios: List<String>,
    year: Int?
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        genres.take(2).forEach { genre ->
            Text(
                text = genre,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        
        studios.take(1).forEach { studio ->
            Text(
                text = studio,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        
        if (year != null) {
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ReorderableMediaRow(
    items: SnapshotStateList<MediaItem>,
    onReorder: (List<MediaItem>) -> Unit,
    onItemClick: (String) -> Unit
) {
    var draggedIndex by remember { mutableStateOf(-1) }
    var offsetX by remember { mutableStateOf(0f) }
    
    LazyRow(
        state = rememberLazyListState(),
        contentPadding = PaddingValues(horizontal = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(items) { index, item ->
            val isDragged = index == draggedIndex
            val scale by animateFloatAsState(
                targetValue = if (isDragged) 1.1f else 1f,
                animationSpec = tween(200),
                label = "dragScale"
            )
            
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = if (isDragged) offsetX else 0f
                    }
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.DirectionLeft -> {
                                    if (index > 0 && isDragged) {
                                        val temp = items[index]
                                        items[index] = items[index - 1]
                                        items[index - 1] = temp
                                        draggedIndex = index - 1
                                        true
                                    } else false
                                }
                                Key.DirectionRight -> {
                                    if (index < items.size - 1 && isDragged) {
                                        val temp = items[index]
                                        items[index] = items[index + 1]
                                        items[index + 1] = temp
                                        draggedIndex = index + 1
                                        true
                                    } else false
                                }
                                Key.Enter, Key.DirectionCenter -> {
                                    draggedIndex = if (draggedIndex == -1) index else -1
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
                    .pointerInput(index) {
                        detectDragGestures(
                            onDragStart = { draggedIndex = index },
                            onDragEnd = { 
                                draggedIndex = -1
                                offsetX = 0f
                                onReorder(items.toList())
                            },
                            onDragCancel = { 
                                draggedIndex = -1
                                offsetX = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                            }
                        )
                    }
            ) {
                MediaCard(
                    item = item,
                    onClick = { if (draggedIndex == -1) onItemClick(item.id) }
                )
            }
        }
    }
}

@Composable
fun SeasonalRow(
    title: String,
    items: List<MediaItem>,
    season: String,
    year: Int,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$title - $season $year",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Action", "Romance", "Fantasy").forEach { genre ->
                    FilterChip(
                        label = genre,
                        onClick = { },
                        selected = false
                    )
                }
            }
        }
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { item ->
                MediaCard(
                    item = item,
                    onClick = { onItemClick(item.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FilterChip(
    label: String,
    onClick: () -> Unit,
    selected: Boolean
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        colors = CardDefaults.colors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                TvSurfaceHighlight
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                TvOnSurfaceVariant
            }
        ),
        border = CardDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF7C4DFF), Color(0xFF00E5FF))
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
        )
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .onFocusChanged { isFocused = it.isFocused }
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun BecauseYouWatchedRow(
    sourceTitle: String,
    recommendations: List<MediaItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Because You Watched",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = sourceTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(recommendations) { item ->
                MediaCard(
                    item = item,
                    onClick = { onItemClick(item.id) }
                )
            }
        }
    }
}