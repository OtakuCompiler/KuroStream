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

package com.kurostream.players.advanced.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceSettingsScreen(
    viewModel: PerformanceSettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val superResolutionEnabled by viewModel.superResolutionEnabled.collectAsStateWithLifecycle()
    val frameInterpolationEnabled by viewModel.frameInterpolationEnabled.collectAsStateWithLifecycle()
    val nnapiEnabled by viewModel.nnapiEnabled.collectAsStateWithLifecycle()
    val targetQuality by viewModel.targetQuality.collectAsStateWithLifecycle()
    val deviceCapabilities by viewModel.deviceCapabilities.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance & AI") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Device capability banner
            if (deviceCapabilities.hasNnapi) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "NNAPI Acceleration Available",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Your device supports hardware-accelerated AI inference.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Super Resolution
            SettingsSection(title = "Super Resolution") {
                SettingsToggle(
                    title = "Enable ESRGAN Upscaling",
                    subtitle = "Upscale video to 2x using AI (increases battery usage)",
                    checked = superResolutionEnabled,
                    onCheckedChange = viewModel::setSuperResolutionEnabled,
                    enabled = deviceCapabilities.supportsSuperResolution
                )

                if (superResolutionEnabled) {
                    Text(
                        "Processing at 2x scale. May cause frame drops on 4K content.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Frame Interpolation
            SettingsSection(title = "Frame Interpolation") {
                SettingsToggle(
                    title = "Enable RIFE Interpolation",
                    subtitle = "Generate intermediate frames for smoother playback",
                    checked = frameInterpolationEnabled,
                    onCheckedChange = viewModel::setFrameInterpolationEnabled,
                    enabled = deviceCapabilities.supportsFrameInterpolation
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // NNAPI Settings
            SettingsSection(title = "Acceleration") {
                SettingsToggle(
                    title = "Use NNAPI",
                    subtitle = "Hardware neural network acceleration",
                    checked = nnapiEnabled,
                    onCheckedChange = viewModel::setNnapiEnabled,
                    enabled = deviceCapabilities.hasNnapi
                )

                if (!deviceCapabilities.hasNnapi) {
                    Text(
                        "NNAPI not available on this device. Using CPU fallback.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Quality Target
            SettingsSection(title = "Quality Target") {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = targetQuality == "battery",
                        onClick = { viewModel.setTargetQuality("battery") },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("Battery")
                    }
                    SegmentedButton(
                        selected = targetQuality == "balanced",
                        onClick = { viewModel.setTargetQuality("balanced") },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("Balanced")
                    }
                    SegmentedButton(
                        selected = targetQuality == "quality",
                        onClick = { viewModel.setTargetQuality("quality") },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("Quality")
                    }
                }
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
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}
