// This file is part of KuroStream.
//
// ArcticFuseSearchHub — full-screen search overlay matching Arctic Fuse
// SearchHub.jsx: dark scrim, centered search input, category chips,
// recent searches, and live results grid.
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kurostream.app.model.MediaItem

private val searchCategories = listOf("All", "Movies", "TV Shows", "Actors", "Genres")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArcticFuseSearchHub(
    visible: Boolean,
    allItems: List<MediaItem>,
    onClose: () -> Unit,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var recentSearches by remember { mutableStateOf(listOf<String>()) }
    val inputFr = remember { FocusRequester() }

    val filtered = remember(query, allItems) {
        if (query.isBlank()) emptyList()
        else {
            val q = query.lowercase()
            allItems.filter { it.title.lowercase().contains(q) }.take(20)
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            inputFr.requestFocus()
        } else {
            query = ""
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(AFMotion.pageEnter)),
        exit = fadeOut(animationSpec = tween(AFMotion.pageEnter)),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AFBgDeep.copy(alpha = 0.95f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AFSpacing.safeZoneH, vertical = AFSpacing.px16),
            ) {
                // Search input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(AFSurface, RoundedCornerShape(AFRadius.lg))
                        .border(width = if (query.isNotEmpty()) 2.dp else 0.dp, color = AFCyan, shape = RoundedCornerShape(AFRadius.lg))
                        .padding(horizontal = AFSpacing.px4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconSearch(tint = AFTextDim, iconSize = 20.dp)
                    Spacer(Modifier.width(AFSpacing.px3))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search movies, shows, actors...",
                                color = AFTextDim,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(color = AFText),
                            cursorBrush = SolidColor(AFCyan),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (query.isNotBlank() && !recentSearches.contains(query)) {
                                    recentSearches = (listOf(query) + recentSearches).take(AFMaxRecentSearches)
                                }
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(inputFr)
                                .onFocusChanged { /* keep focus */ },
                        )
                    }
                    if (query.isNotEmpty()) {
                        SearchClearButton(onClick = { query = "" })
                    }
                }

                Spacer(Modifier.height(AFSpacing.px6))

                // Category chips
                Row(horizontalArrangement = Arrangement.spacedBy(AFSpacing.px3)) {
                    searchCategories.forEach { cat ->
                        CategoryChip(
                            label = cat,
                            isSelected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                        )
                    }
                }

                Spacer(Modifier.height(AFSpacing.px6))

                // Recent searches (when query empty)
                if (query.isBlank() && recentSearches.isNotEmpty()) {
                    Column {
                        Text(
                            text = "Recent Searches",
                            color = AFTextDim,
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        )
                        Spacer(Modifier.height(AFSpacing.px2))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(AFSpacing.px2),
                            verticalArrangement = Arrangement.spacedBy(AFSpacing.px2),
                        ) {
                            recentSearches.forEach { r ->
                                RecentChip(label = r, onClick = { query = r })
                            }
                        }
                    }
                    Spacer(Modifier.height(AFSpacing.px6))
                }

                // Results
                if (filtered.isNotEmpty()) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(AFSpacing.px3),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AFSurface, RoundedCornerShape(AFRadius.md))
                                    .clickable { onItemClick(item) }
                                    .padding(AFSpacing.px3),
                                horizontalArrangement = Arrangement.spacedBy(AFSpacing.px3),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 80.dp, height = 120.dp)
                                        .background(AFSurfaceVariant, RoundedCornerShape(AFRadius.sm)),
                                ) {
                                    if (item.posterUrl.isNotBlank()) {
                                        coil.compose.AsyncImage(
                                            model = item.posterUrl,
                                            contentDescription = item.title,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = AFText,
                                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (item.year > 0) {
                                        Text(
                                            text = item.year.toString(),
                                            color = AFTextDim,
                                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    if (item.description.isNotBlank()) {
                                        Text(
                                            text = item.description,
                                            color = AFTextSec,
                                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (query.isNotBlank()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No results for \"$query\"",
                            color = AFTextDim,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            // Close button top-right
            CloseButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(AFSpacing.px6),
            )
        }
    }
}

@Composable
private fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
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
            .background(
                color = when {
                    isSelected -> AFCyan
                    isFocused -> AFSurface
                    else -> AFSurface
                },
                shape = RoundedCornerShape(AFRadius.pill),
            )
            .padding(horizontal = AFSpacing.px4, vertical = AFSpacing.px2),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isSelected) AFBgDeep else if (isFocused) AFText else AFTextDim,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            ),
        )
    }
}

@Composable
private fun RecentChip(label: String, onClick: () -> Unit) {
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
            .background(if (isFocused) AFSurface else AFBgAlt, RoundedCornerShape(AFRadius.pill))
            .border(width = if (isFocused) 1.dp else 0.dp, color = AFCyan, shape = RoundedCornerShape(AFRadius.pill))
            .padding(horizontal = AFSpacing.px3, vertical = AFSpacing.px2),
    ) {
        Text(
            text = label,
            color = if (isFocused) AFText else AFTextSec,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun SearchClearButton(onClick: () -> Unit) {
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
                ) { onClick(); true } else false
            }
            .clickable(onClick = onClick)
            .border(width = if (isFocused) 1.dp else 0.dp, color = AFCyan, shape = RoundedCornerShape(AFRadius.sm))
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconClose(tint = AFTextDim, iconSize = 16.dp)
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    Box(
        modifier = modifier
            .size(36.dp)
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
        IconClose(tint = AFTextDim)
    }
}