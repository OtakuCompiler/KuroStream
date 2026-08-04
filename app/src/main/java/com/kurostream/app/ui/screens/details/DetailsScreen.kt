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

package com.kurostream.app.ui.screens.details

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kurostream.app.ui.arctic.ArcticFuseDetailPage
import com.kurostream.app.ui.arctic.ArcticFuseTheme

/**
 * DetailsScreen — shows media details using the Arctic Fuse 3 UI.
 *
 * Delegates to [ArcticFuseDetailPage] for the full pixel-perfect Arctic Fuse 3 detail experience.
 */
@Composable
fun DetailsScreen(
    mediaId: String,
    onBack: () -> Unit,
    onPlay: (String) -> Unit = { onBack() },
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(mediaId) {
        viewModel.loadDetails(mediaId)
    }

    ArcticFuseTheme {
        when (val state = uiState) {
            is DetailsUiState.Loading -> {
                com.kurostream.app.ui.arctic.ArcticFuseSkeletonPage(
                    modifier = Modifier.fillMaxSize()
                )
            }
            is DetailsUiState.Success -> {
                ArcticFuseDetailPage(
                    item = state.media,
                    visible = true,
                    onClose = onBack,
                    onPlay = { item ->
                        onPlay(item.id)
                    },
                    onAddWatchlist = { item ->
                        viewModel.toggleFavorite(item.id)
                    },
                    relatedItems = emptyList(), // Could load related from repository
                    cast = emptyList(), // Could load cast from repository
                    modifier = Modifier,
                )
            }
            is DetailsUiState.Error -> {
                com.kurostream.app.ui.arctic.ArcticFuseErrorPage(
                    message = state.message,
                    onRetry = { viewModel.loadDetails(mediaId) },
                    onBack = onBack,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}