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

package com.kurostream.players.advanced.audio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSettingsScreen(
    dspManager: AudioDSPManager,
    onNavigateBack: () -> Unit
) {
    val eqGains by dspManager.eqSettingsFlow.collectAsStateWithLifecycle()
    val loudness by dspManager.loudnessFlow.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
            LoudnessMeterCard(loudness = loudness)
            Spacer(modifier = Modifier.height(16.dp))
            NightModeSection(dspManager = dspManager)
            Spacer(modifier = Modifier.height(16.dp))
            LoudnessNormalizationSection(dspManager = dspManager)
            Spacer(modifier = Modifier.height(16.dp))
            EqualizerSection(
                gains = eqGains,
                onGainChanged = { band, gain -> dspManager.setEQBand(band, gain) },
                onPresetSelected = { dspManager.applyEQPreset(it) }
            )
        }
    }
}

@Composable
private fun LoudnessMeterCard(loudness: Float) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Loudness", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (loudness + 70f) / 70f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Text(
                String.format("%.1f LUFS", loudness),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun NightModeSection(dspManager: AudioDSPManager) {
    var enabled by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Night Mode", style = MaterialTheme.typography.titleMedium)
                    Text("Compress dynamic range for quiet listening", 
                        style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { 
                        enabled = it
                        dspManager.setNightMode(it)
                    }
                )
            }
        }
    }
}

@Composable
private fun LoudnessNormalizationSection(dspManager: AudioDSPManager) {
    var enabled by remember { mutableStateOf(true) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Loudness Normalization", style = MaterialTheme.typography.titleMedium)
                    Text("Normalize to -14 LUFS (EBU R128)", 
                        style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { 
                        enabled = it
                        dspManager.setLoudnessNormalization(it)
                    }
                )
            }
        }
    }
}

@Composable
private fun EqualizerSection(
    gains: FloatArray,
    onGainChanged: (Int, Float) -> Unit,
    onPresetSelected: (AudioDSPManager.EQPreset) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("10-Band Equalizer", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AudioDSPManager.EQPreset.values().forEach { preset ->
                    FilterChip(
                        selected = false,
                        onClick = { onPresetSelected(preset) },
                        label = { Text(preset.name.replace("_", " ")) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            val frequencies = arrayOf("31Hz", "62Hz", "125Hz", "250Hz", "500Hz", 
                "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")
            frequencies.forEachIndexed { index, freq ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(freq, modifier = Modifier.width(60.dp), 
                        style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = gains[index],
                        onValueChange = { onGainChanged(index, it) },
                        valueRange = -12f..12f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(String.format("%+.1fdB", gains[index]), 
                        modifier = Modifier.width(60.dp),
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
