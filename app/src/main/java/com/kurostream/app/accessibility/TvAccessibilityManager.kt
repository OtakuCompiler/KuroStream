package com.kurostream.app.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

class TvAccessibilityManager(context: Context) {
    private val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    val isTalkBackEnabled: Boolean
        get() = am.isEnabled && am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_SPOKEN
        ).isNotEmpty()

    val isHighContrastEnabled: Boolean
        get() = am.isEnabled && am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_TOUCH_EXPLORATION
        ).isNotEmpty()
}

@Composable
fun Modifier.tvAccessible(label: String, hint: String = ""): Modifier {
    return this.semantics {
        contentDescription = if (hint.isNotEmpty()) "$label. $hint" else label
    }.focusable()
}
