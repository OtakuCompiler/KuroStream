package com.kurostream.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kurostream.desktop.DesktopAppState

@Composable
fun DesktopSettingsScreen(
    state: DesktopAppState,
    onExit: () -> Unit,
) {
    val current = state.settings.snapshot()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        SettingRow(
            label = "Playback backend",
            value = current.playbackBackend,
            options = listOf("auto", "vlc", "mpv"),
            onChange = { state.settings.update { it.copy(playbackBackend = v) } },
        )
        SettingRow(
            label = "Preferred quality",
            value = current.preferredQuality,
            options = listOf("720p", "1080p", "4K"),
            onChange = { state.settings.update { it.copy(preferredQuality = v) } },
        )
        ToggleRow(
            label = "Dolby Atmos passthrough",
            value = current.dolbyAtmosPassthrough,
            onChange = { state.settings.update { it.copy(dolbyAtmosPassthrough = it) } },
        )
        ToggleRow(
            label = "Frame interpolation",
            value = current.frameInterpolation,
            onChange = { state.settings.update { it.copy(frameInterpolation = it) } },
        )
        ToggleRow(
            label = "AI upscaling",
            value = current.aiUpscaling,
            onChange = { state.settings.update { it.copy(aiUpscaling = it) } },
        )
        ToggleRow(
            label = "Hardware decoder",
            value = current.hardwareDecoder,
            onChange = { state.settings.update { it.copy(hardwareDecoder = it) } },
        )
        ToggleRow(
            label = "Cross-device sync",
            value = current.crossDeviceSync,
            onChange = { state.settings.update { it.copy(crossDeviceSync = it) } },
        )

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text("Exit KuroStream")
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    options: List<String>,
    onChange: (String) -> Unit,
) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        options.forEach { opt ->
            FilterChip(
                selected = opt == value,
                onClick = { onChange(opt) },
                label = { Text(opt) },
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}
