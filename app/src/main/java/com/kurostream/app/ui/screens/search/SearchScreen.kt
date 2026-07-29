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

package com.kurostream.app.ui.screens.search

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kurostream.app.model.MediaItem
import com.kurostream.app.ui.arctic.ArcticFuseErrorPage
import com.kurostream.app.ui.arctic.ArcticFuseSearchHub
import com.kurostream.app.ui.arctic.ArcticFuseSkeletonPage
import com.kurostream.app.ui.arctic.ArcticFuseTheme

/**
 * SearchScreen — search interface using the Arctic Fuse 3 UI.
 *
 * Delegates to [ArcticFuseSearchHub] for the full pixel-perfect Arctic Fuse 3 search experience.
 */
@Composable
fun SearchScreen(
    onMediaClick: (String) -> Unit,
    onClose: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ArcticFuseTheme {
        when (val state = uiState) {
            is SearchUiState.Loading -> {
                ArcticFuseSkeletonPage(modifier = Modifier.fillMaxSize())
            }
            is SearchUiState.Success -> {
                val allItems = state.items.map { result ->
                    MediaItem(
                        id = result.id,
                        title = result.title,
                        year = result.year,
                        posterUrl = result.posterUrl,
                        rating = result.score.toFloat(),
                    )
                }
                ArcticFuseSearchHub(
                    visible = true,
                    allItems = allItems,
                    onClose = onClose,
                    onItemClick = { item -> onMediaClick(item.id) },
                    modifier = Modifier,
                )
            }
            is SearchUiState.Error -> {
                ArcticFuseErrorPage(
                    message = state.message,
                    onRetry = { viewModel.search() },
                    onBack = onClose,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            is SearchUiState.Idle -> {
                ArcticFuseSearchHub(
                    visible = true,
                    allItems = emptyList(),
                    onClose = onClose,
                    onItemClick = { item -> onMediaClick(item.id) },
                    modifier = Modifier,
                )
            }
        }
    }
}