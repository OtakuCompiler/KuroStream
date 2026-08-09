// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.kurostream.app.ui.components.Af3PillButton
import com.kurostream.app.ui.components.Af3ScreenScaffold
import com.kurostream.app.ui.theme.Af3Theme

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onMarketplaceClick: () -> Unit = {},
) {
    val palette = Af3Theme.palette
    Af3ScreenScaffold(title = "Settings", onBack = onBackClick) {
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                text = "Personalize your KuroStream experience.",
                color = palette.textSec,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Af3PillButton("Skins marketplace", primary = true, onClick = onMarketplaceClick)
                Af3PillButton("Source lock", primary = false, onClick = {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Af3PillButton("Playback", primary = false, onClick = {})
                Af3PillButton("Privacy", primary = false, onClick = {})
            }
        }
    }
}

@Composable
fun SourceLockSettingsScreen(
    onBackClick: () -> Unit,
) {
    Af3ScreenScaffold(title = "Source Lock", onBack = onBackClick) {
        Text(
            text = "Pin specific sources per title to always prefer them.",
            color = Af3Theme.palette.textSec,
            fontSize = 14.sp,
        )
    }
}
