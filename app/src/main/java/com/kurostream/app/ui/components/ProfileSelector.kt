// This file is part of KuroStream.
//
// ProfileSelector — Arctic Fuse TV-friendly profile picker.
// Shows profile avatars in a horizontal row with D-pad focus.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurostream.app.ui.arctic.AFCyan
import com.kurostream.app.ui.arctic.AFSpacing
import com.kurostream.app.ui.arctic.AFRadius
import com.kurostream.app.ui.arctic.AFText
import com.kurostream.domain.model.Profile
import androidx.compose.material3.Text

@Composable
fun ProfileSelector(
    profiles: List<Profile>,
    activeProfileId: String?,
    onSelectProfile: (Profile) -> Unit,
    onAddProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focusedIndex by remember { mutableStateOf(-1) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AFSpacing.px3),
    ) {
        profiles.forEachIndexed { index, profile ->
            val isFocused = focusedIndex == index
            val isActive = profile.id == activeProfileId
            val fr = remember { FocusRequester() }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .focusRequester(fr)
                    .onFocusChanged { focusedIndex = if (it.isFocused) index else focusedIndex }
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp &&
                            (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                        ) {
                            onSelectProfile(profile)
                            true
                        } else false
                    }
                    .clickable { onSelectProfile(profile) },
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(if (isFocused) 1.15f else 1f)
                        .clip(CircleShape)
                        .background(
                            if (isActive) AFCyan else Color(0xFF1E1E2D),
                            CircleShape,
                        )
                        .border(
                            width = if (isFocused) 3.dp else if (isActive) 2.dp else 1.dp,
                            color = if (isFocused || isActive) AFCyan else Color.White.copy(alpha = 0.2f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = profile.displayName.firstOrNull()?.toString() ?: "?",
                        color = if (isActive) Color.Black else AFText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = profile.displayName,
                    color = if (isFocused || isActive) AFCyan else AFText.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                .clickable { onAddProfile() },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "+", color = AFText.copy(alpha = 0.6f), fontSize = 32.sp)
        }
    }
}
