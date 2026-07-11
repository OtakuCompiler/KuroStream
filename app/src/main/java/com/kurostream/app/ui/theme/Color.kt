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

import androidx.compose.ui.graphics.Color

// Base color constants
val TvPrimary = Color(0xFF7C4DFF)
val TvOnPrimary = Color(0xFFFFFFFF)
val TvSecondary = Color(0xFF00E5FF)
val TvOnSecondary = Color(0xFF000000)
val TvSecondaryVariant = Color(0xFF00E5FF)
val TvBackground = Color(0xFF0A0A0F)
val TvOnBackground = Color(0xFFE0E0E0)
val TvSurface = Color(0xFF14141F)
val TvOnSurface = Color(0xFFE0E0E0)
val TvSurfaceVariant = Color(0xFF1E1E2E)
val TvOnSurfaceVariant = Color(0xFFB0B0C0)
val TvSurfaceHighlight = Color(0xFF1A1A2E)
val TvError = Color(0xFFCF6679)
val TvFocusBorder = Color(0xFF7C4DFF)
val TvSkeletonBase = Color(0xFF2A2A3A)
val TvSkeletonShimmer = Color(0xFF3A3A4A)

/**
 * High contrast color scheme for accessibility.
 * Meets WCAG 2.1 AA contrast requirements (4.5:1 for normal text, 3:1 for large text).
 */
val TvHighContrastColorScheme = androidx.tv.material3.darkColorScheme(
    primary = Color(0xFFFFFFFF),           // Pure white - 21:1 on black
    onPrimary = Color(0xFF000000),         // Pure black
    primaryContainer = Color(0xFFCCCCCC),  // Light gray
    onPrimaryContainer = Color(0xFF000000), // Black
    secondary = Color(0xFFFFFF00),         // Yellow - 19:1 on black
    onSecondary = Color(0xFF000000),       // Black
    secondaryContainer = Color(0xFFCCCC00), // Dark yellow
    onSecondaryContainer = Color(0xFF000000), // Black
    tertiary = Color(0xFF00FFFF),          // Cyan - 18:1 on black
    onTertiary = Color(0xFF000000),        // Black
    tertiaryContainer = Color(0xFF00CCCC), // Dark cyan
    onTertiaryContainer = Color(0xFF000000), // Black
    background = Color(0xFF000000),        // Pure black
    onBackground = Color(0xFFFFFFFF),      // Pure white
    surface = Color(0xFF000000),           // Pure black
    onSurface = Color(0xFFFFFFFF),         // Pure white
    surfaceVariant = Color(0xFF333333),    // Dark gray
    onSurfaceVariant = Color(0xFFCCCCCC),  // Light gray
    error = Color(0xFFFF3333),             // Bright red
    onError = Color(0xFF000000),           // Black
    errorContainer = Color(0xFFCC0000),    // Dark red
    onErrorContainer = Color(0xFFFFFFFF),  // White
    outline = Color(0xFFFFFFFF),           // White borders
    outlineVariant = Color(0xFF999999),    // Medium gray
    shadow = Color(0xFF000000),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFFFFFFFF),
    inverseSurface = Color(0xFFFFFFFF),
    inverseOnSurface = Color(0xFF000000),
    inversePrimary = Color(0xFF000000)
)

/**
 * Standard dark theme color scheme (existing).
 */
val TvDarkColorScheme = androidx.tv.material3.darkColorScheme(
    primary = Color(0xFF7C4DFF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF651FFF).copy(alpha = 0.15f),
    onPrimaryContainer = Color(0xFF7C4DFF),
    secondary = Color(0xFF00E5FF),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF00B8D4).copy(alpha = 0.15f),
    onSecondaryContainer = Color(0xFF00E5FF),
    tertiary = Color(0xFF00E5FF),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF00B8D4).copy(alpha = 0.15f),
    onTertiaryContainer = Color(0xFF00E5FF),
    background = Color(0xFF0A0A0F),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF14141F),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF1E1E2E),
    onSurfaceVariant = Color(0xFFB0B0C0),
    error = Color(0xFFCF6679),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFCF6679).copy(alpha = 0.15f),
    onErrorContainer = Color(0xFFCF6679),
    outline = Color(0xFF7C4DFF).copy(alpha = 0.3f),
    outlineVariant = Color(0xFF7C4DFF).copy(alpha = 0.15f),
    shadow = Color(0xFF000000),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF7C4DFF),
    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFF0A0A0F),
    inversePrimary = Color(0xFF651FFF)
)