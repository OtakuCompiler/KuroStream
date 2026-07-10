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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kurostream.app.ui.components.MediaCard
import com.kurostream.app.ui.components.SkeletonRow
import com.kurostream.app.model.MediaItem

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TrendingAnimeRow(
    state: RowState<MediaItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ContentRow(
        title = "Trending Now",
        state = state,
        onItemClick = onItemClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NewReleasesRow(
    state: RowState<MediaItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ContentRow(
        title = "New Releases",
        state = state,
        onItemClick = onItemClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeasonalAnimeRow(
    state: RowState<MediaItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ContentRow(
        title = "This Season",
        state = state,
        onItemClick = onItemClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ContentRow(
    title: String,
    state: RowState<MediaItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        when (state) {
            is RowState.Loading -> {
                SkeletonRow(itemCount = 6)
            }
            is RowState.Error -> {
                ErrorRowState(
                    message = state.message,
                    onRetry = { }
                )
            }
            is RowState.Success -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = state.items,
                        key = { it.id }
                    ) { item ->
                        MediaCard(
                            item = item,
                            onClick = { onItemClick(item.id) }
                        )
                    }
                }
            }
        }
    }
}
