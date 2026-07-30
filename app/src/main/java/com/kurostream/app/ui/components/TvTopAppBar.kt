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

package com.kurostream.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.kurostream.app.ui.theme.TvBackground

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvTopAppBar(
    showBackground: Boolean,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddonsClick: () -> Unit,
    onTorrentsClick: () -> Unit,
    onBackupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(
                brush = if (showBackground) {
                    Brush.verticalGradient(
                        colors = listOf(
                            TvBackground.copy(alpha = 0.95f),
                            TvBackground.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                } else androidx.compose.ui.graphics.SolidColor(Color.Transparent)
            )
            .padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "KuroStream",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = onTorrentsClick) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Torrents")
            }
            IconButton(onClick = onBackupClick) {
                Icon(Icons.Default.Share, contentDescription = "Backup & Sync")
            }
            IconButton(onClick = onAddonsClick) {
                Icon(Icons.Default.Add, contentDescription = "Addons")
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    }
}