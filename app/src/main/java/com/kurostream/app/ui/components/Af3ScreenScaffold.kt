// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.kurostream.app.ui.theme.Af3Theme

/**
 * Af3ScreenScaffold — common header + content wrapper used by all secondary
 * screens. Provides:
 *  - Standardized top bar (back affordance + title)
 *  - D-pad back handling via [onBack]
 *  - AF3 backdrop overlay
 */
@Composable
fun Af3ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val palette = Af3Theme.palette
    val space = Af3Theme.space
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (event.key == Key.Back && onBack != null) {
                    onBack()
                    true
                } else false
            },
    ) {
        Af3Backdrop(backdropUrl = null)
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = space.safeH, vertical = space.s16)
                    .background(palette.bg.copy(alpha = 0.5f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    Af3PillButton(label = "← Back", primary = false, onClick = onBack)
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = 8.dp))
                }
                Text(
                    text = title,
                    color = palette.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f),
                )
                actions()
            }
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = space.safeH)) {
                content()
            }
        }
    }
}
