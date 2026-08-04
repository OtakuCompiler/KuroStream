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

package com.kurostream.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kurostream.app.ui.arctic.AFCyan
import com.kurostream.app.ui.components.SettingsSkinPicker
import com.kurostream.app.ui.arctic.ArcticFuseTheme
import com.kurostream.app.ui.theme.Skin

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    onMarketplaceClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ArcticFuseTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(48.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Settings", style = MaterialTheme.typography.displaySmall, color = AFText)
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AFText,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Appearance Section
            SettingsSection(title = "Appearance") {
                SettingsSkinPicker(
                    selected = Skin.entries.find {
                        it.name.equals(uiState.skinName, ignoreCase = true)
                    } ?: Skin.ARCTIC_FUSE,
                    onSkinSelected = { skin ->
                        viewModel.setSkinName(skin.name)
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Accessibility Section
            SettingsSection(title = "Accessibility") {
                ToggleRow("Reduce Motion", uiState.reduceMotionEnabled) {
                    viewModel.setReduceMotionEnabled(it)
                }
                ToggleRow("Focus Highlight", uiState.focusHighlightEnabled) {
                    viewModel.setFocusHighlightEnabled(it)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Source Lock Section
            SettingsSection(title = "Source Lock") {
                ToggleRow("Source Lock Enabled", uiState.sourceLockEnabled) {
                    viewModel.setSourceLockEnabled(it)
                }
                ToggleRow("Persist Across Sessions", uiState.sourceLockPersist) {
                    viewModel.setSourceLockPersist(it)
                }
                ToggleRow("Notify on Fallback", uiState.sourceLockNotifyFallback) {
                    viewModel.setSourceLockNotifyFallback(it)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Torrent Section
            SettingsSection(title = "Streaming") {
                ToggleRow("Auto-play Next Episode", uiState.autoPlayNextEnabled) {
                    viewModel.setAutoPlayNextEnabled(it)
                }
                ToggleRow("Skip Intro", uiState.skipIntroEnabled) {
                    viewModel.setSkipIntroEnabled(it)
                }
                ToggleRow("Hardware Acceleration", uiState.hardwareAccelerationEnabled) {
                    viewModel.setHardwareAccelerationEnabled(it)
                }
                ToggleRow("Background Playback", uiState.backgroundPlaybackEnabled) {
                    viewModel.setBackgroundPlaybackEnabled(it)
                }
                ToggleRow("Cinematic Mode", uiState.cinematicModeEnabled) {
                    viewModel.setCinematicModeEnabled(it)
                }
                ToggleRow("Ambient Mode", uiState.ambientModeEnabled) {
                    viewModel.setAmbientModeEnabled(it)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Kuro Store (premium skins marketplace)
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onMarketplaceClick,
            ) {
                Text("Kuro Store — Skins & Premium")
            }

            // Advanced link
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* Navigate to advanced settings */ },
            ) {
                Text("Advanced Settings")
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = AFCyan
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = AFText
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
