package com.kurostream.app.controller

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay

@Composable
fun DpadFocusRestoration(state: LazyListState, focusRequester: FocusRequester) {
    LaunchedEffect(state.firstVisibleItemIndex) {
        delay(50)
        focusRequester.requestFocus()
    }
}
