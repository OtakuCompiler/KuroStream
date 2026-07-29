// This file is part of KuroStream.
//
// ArcticFuseToast — top-right notification stack with success/error/info
// variants.  Mirrors Arctic Fuse Toast.jsx: 4px left border colored by type,
// 3-second auto-dismiss, dismiss button.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.app.ui.arctic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

enum class ArcticToastType { Success, Error, Info }

data class ArcticToast(
    val id: String,
    val type: ArcticToastType,
    val message: String,
)

@Composable
fun ArcticFuseToastContainer(
    toasts: List<ArcticToast>,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(AFSpacing.px4),
        verticalArrangement = Arrangement.spacedBy(AFSpacing.px2),
    ) {
        toasts.takeLast(AFMaxToasts).forEach { toast ->
            ArcticToastItem(toast = toast, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun ArcticToastItem(toast: ArcticToast, onDismiss: (String) -> Unit) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(toast.id) {
        delay(AFMotion.toast.toLong())
        visible = false
        delay(AFMotion.normal.toLong())
        onDismiss(toast.id)
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(AFMotion.normal)) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(AFMotion.normal)) + fadeOut(),
    ) {
        val accentColor = when (toast.type) {
            ArcticToastType.Success -> AFTeal
            ArcticToastType.Error -> AFDanger
            ArcticToastType.Info -> AFCyan
        }
        Row(
            modifier = Modifier
                .background(AFSurface, RoundedCornerShape(topStart = 0.dp, topEnd = AFRadius.md, bottomEnd = AFRadius.md, bottomStart = 0.dp))
                .border(width = 4.dp, color = accentColor, shape = RoundedCornerShape(topStart = 0.dp, topEnd = AFRadius.md, bottomEnd = AFRadius.md, bottomStart = 0.dp))
                .padding(horizontal = AFSpacing.px4, vertical = AFSpacing.px3)
                .width(280.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AFSpacing.px3),
        ) {
            Text(
                text = toast.message,
                color = AFText,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            ToastDismissButton(onClick = { visible = false; onDismiss(toast.id) })
        }
    }
}

@Composable
private fun ToastDismissButton(onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .size(24.dp)
            .focusRequester(fr)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter)
                ) {
                    onClick(); true
                } else {
                    false
                }
            }
            .clickable(onClick = onClick)
            .border(
                width = if (isFocused) 1.dp else 0.dp,
                color = AFCyan,
                shape = RoundedCornerShape(AFRadius.sm),
            )
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconClose(tint = AFTextDim, iconSize = 16.dp)
    }
}
