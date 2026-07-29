package com.kurostream.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.tv.material3.ExperimentalTvMaterial3Api

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DynamicThemeProvider(
    palette: DynamicPalette,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalDynamicPalette provides palette) {
        content()
    }
}

val LocalDynamicPalette = staticCompositionLocalOf { TvDarkColorScheme.toDynamicPalette() }
