package com.kurostream.app.ui.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import timber.log.Timber

object TvFocusManager {
    private val focusHistory = mutableMapOf<String, String>()

    @Composable
    fun rememberFocus(screen: String, defaultId: String): FocusRequester {
        val fr = remember { FocusRequester() }
        val lastId = rememberSaveable { mutableStateOf(focusHistory[screen] ?: defaultId) }
        
        LaunchedEffect(Unit) {
            try { fr.requestFocus() } catch (e: Exception) { Timber.w("Focus request failed") }
        }
        
        return fr
    }

    fun saveFocus(screen: String, itemId: String) {
        focusHistory[screen] = itemId
    }

    @Composable
    fun Modifier.tvFocusable(
        screen: String,
        itemId: String,
        onClick: () -> Unit,
    ): Modifier = this
        .onFocusChanged { state ->
            if (state.isFocused) saveFocus(screen, itemId)
        }
        .onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                onClick()
                true
            } else false
        }
}
