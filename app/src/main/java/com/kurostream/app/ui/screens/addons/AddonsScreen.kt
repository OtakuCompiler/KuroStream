// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.addons

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
import com.kurostream.app.ui.components.Af3EmptyState
import com.kurostream.app.ui.components.Af3PillButton
import com.kurostream.app.ui.components.Af3ScreenScaffold
import com.kurostream.app.ui.theme.Af3Theme

@Composable
fun AddonsScreen(
    onBackClick: () -> Unit,
) {
    val palette = Af3Theme.palette
    Af3ScreenScaffold(title = "Add-ons", onBack = onBackClick) {
        Column {
            Text(
                text = "Manage community add-ons for new content sources.",
                color = palette.textSec,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Af3PillButton(label = "Browse Repository", primary = true, onClick = {})
                Af3PillButton(label = "Install from URL", primary = false, onClick = {})
            }
            Af3EmptyState(
                icon = "🔌",
                title = "No add-ons installed",
                subtitle = "Add-ons extend KuroStream with new catalogs and resolvers.",
            )
        }
    }
}
