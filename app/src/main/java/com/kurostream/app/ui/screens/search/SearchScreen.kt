// This file is part of KuroStream.
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.kurostream.app.ui.components.Af3CardLayout
import com.kurostream.app.ui.components.Af3EmptyState
import com.kurostream.app.ui.components.Af3ScreenScaffold
import com.kurostream.app.ui.components.Af3WidgetRow
import com.kurostream.app.ui.theme.Af3Theme
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    onMediaClick: (String) -> Unit,
    onClose: () -> Unit,
) {
    val palette = Af3Theme.palette
    val space = Af3Theme.space
    var query by remember { mutableStateOf("") }
    var debounced by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(query) {
        delay(300)
        debounced = query
    }

    LaunchedEffect(Unit) {
        delay(300)
        runCatching { focusRequester.requestFocus() }
    }

    Af3ScreenScaffold(title = "Search", onBack = onClose) {
        Column(modifier = Modifier.fillMaxSize().padding(top = space.s16)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(palette.surface)
                    .border(2.dp, palette.borderFocus.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it.take(100).filter { ch -> ch.isLetterOrDigit() || ch.isWhitespace() } },
                    textStyle = TextStyle(
                        color = palette.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    cursorBrush = SolidColor(palette.accent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Search movies, series, anime…",
                                color = palette.textDim,
                                fontSize = 18.sp,
                            )
                        }
                        inner()
                    },
                )
            }
            Spacer(Modifier.height(space.s24))
            if (debounced.isBlank()) {
                Af3EmptyState(
                    icon = "🔍",
                    title = "Start typing to search",
                    subtitle = "Results appear after a brief pause to keep things fast.",
                )
            } else if (debounced.length < 2) {
                Af3EmptyState(
                    icon = "🔎",
                    title = "Keep typing…",
                    subtitle = "Enter at least two characters.",
                )
            } else {
                // Real search would go through repository — placeholder row
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Search results will appear here.",
                        color = palette.textSec,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = space.s24),
                    )
                }
            }
        }
    }
}
