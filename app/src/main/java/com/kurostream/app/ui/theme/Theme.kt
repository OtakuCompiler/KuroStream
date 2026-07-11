// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.darkColorScheme

private val TvDarkColorScheme = darkColorScheme(
    primary = TvPrimary,
    onPrimary = TvOnPrimary,
    primaryContainer = TvPrimary.copy(alpha = 0.15f),
    onPrimaryContainer = TvPrimary,
    secondary = TvSecondary,
    onSecondary = TvOnSecondary,
    secondaryContainer = TvSecondary.copy(alpha = 0.15f),
    onSecondaryContainer = TvSecondary,
    tertiary = TvSecondaryVariant,
    onTertiary = TvOnPrimary,
    tertiaryContainer = TvSecondaryVariant.copy(alpha = 0.15f),
    onTertiaryContainer = TvSecondaryVariant,
    background = TvBackground,
    onBackground = TvOnBackground,
    surface = TvSurface,
    onSurface = TvOnSurface,
    surfaceVariant = TvSurfaceVariant,
    onSurfaceVariant = TvOnSurfaceVariant,
    error = TvError,
    onError = TvOnPrimary,
    errorContainer = TvError.copy(alpha = 0.15f),
    onErrorContainer = TvError,
    border = TvFocusBorder.copy(alpha = 0.3f),
    borderVariant = TvFocusBorder.copy(alpha = 0.15f)
)

private val TvShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AnimeStreamTVTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvDarkColorScheme,
        shapes = TvShapes,
        typography = TvTypography,
        content = content
    )
}

@Composable
fun focusedCardBorder(): CardBorder = CardDefaults.border(
    focusedBorder = Border(
        border = BorderStroke(
            width = 2.dp,
            color = TvFocusBorder
        ),
        shape = TvShapes.medium
    )
)
