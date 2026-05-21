package com.kurostream.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/**
 * Kuro Stream Color Palette
 * 
 * Dark-first design optimized for TV viewing in dim environments.
 * Colors are carefully chosen for:
 * - High contrast for readability
 * - Eye comfort during extended viewing
 * - Clear visual hierarchy
 */
object KuroStreamColors {
    // Primary brand colors
    val Primary = Color(0xFF7C4DFF)           // Vibrant purple - main accent
    val PrimaryVariant = Color(0xFF651FFF)    // Deeper purple
    val PrimaryDark = Color(0xFF4527A0)       // Dark purple for backgrounds
    
    // Secondary colors
    val Secondary = Color(0xFF00E5FF)         // Cyan - secondary accent
    val SecondaryVariant = Color(0xFF00B8D4)  // Deeper cyan
    
    // Background colors
    val Background = Color(0xFF0D0D0F)        // Near black
    val BackgroundElevated = Color(0xFF1A1A1E) // Slightly elevated
    val Surface = Color(0xFF1E1E24)           // Card surfaces
    val SurfaceVariant = Color(0xFF2A2A32)    // Higher elevation
    
    // Text colors
    val OnPrimary = Color(0xFFFFFFFF)
    val OnSecondary = Color(0xFF000000)
    val OnBackground = Color(0xFFE8E8E8)
    val OnSurface = Color(0xFFE8E8E8)
    val OnSurfaceVariant = Color(0xFFB0B0B0)
    
    // Semantic colors
    val Error = Color(0xFFFF5252)
    val OnError = Color(0xFFFFFFFF)
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFFC107)
    val Info = Color(0xFF2196F3)
    
    // Focus/Selection colors (crucial for TV navigation)
    val FocusBorder = Color(0xFF7C4DFF)
    val FocusBackground = Color(0x337C4DFF)
    val Selected = Color(0xFF7C4DFF)
    val SelectedVariant = Color(0xFF5E35B1)
    
    // Progress colors
    val ProgressTrack = Color(0xFF3A3A42)
    val ProgressIndicator = Color(0xFF7C4DFF)
    val BufferIndicator = Color(0x807C4DFF)
    
    // Gradient colors
    val GradientStart = Color(0xFF7C4DFF)
    val GradientEnd = Color(0xFF00E5FF)
    
    // Overlay colors
    val Overlay = Color(0xCC000000)           // 80% black
    val OverlayLight = Color(0x66000000)      // 40% black
    val Scrim = Color(0x99000000)             // 60% black
}

/**
 * Kuro Stream Typography
 * 
 * TV-optimized typography with larger sizes for readability
 * from typical viewing distances (10+ feet).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
object KuroStreamTypography {
    val displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.25).sp
    )
    
    val displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    )
    
    val displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )
    
    val headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    )
    
    val headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    )
    
    val headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    )
    
    val titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    )
    
    val titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )
    
    val titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    )
    
    val bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp
    )
    
    val bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    )
    
    val bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp
    )
    
    val labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    )
    
    val labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    )
    
    val labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
}

/**
 * Custom spacing values for TV layouts.
 */
data class KuroStreamSpacing(
    val extraSmall: Int = 4,
    val small: Int = 8,
    val medium: Int = 16,
    val large: Int = 24,
    val extraLarge: Int = 32,
    val huge: Int = 48,
    val cardPadding: Int = 16,
    val screenPadding: Int = 48,    // TV safe zone padding
    val rowSpacing: Int = 24,
    val cardSpacing: Int = 16
)

val LocalKuroStreamSpacing = staticCompositionLocalOf { KuroStreamSpacing() }

/**
 * Kuro Stream TV Material Theme.
 * 
 * Dark theme optimized for TV viewing with proper focus states,
 * high contrast colors, and TV-appropriate typography.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun KuroStreamTheme(
    darkTheme: Boolean = true, // Always dark for TV
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = KuroStreamColors.Primary,
        onPrimary = KuroStreamColors.OnPrimary,
        primaryContainer = KuroStreamColors.PrimaryDark,
        onPrimaryContainer = KuroStreamColors.OnPrimary,
        secondary = KuroStreamColors.Secondary,
        onSecondary = KuroStreamColors.OnSecondary,
        secondaryContainer = KuroStreamColors.SecondaryVariant,
        onSecondaryContainer = KuroStreamColors.OnSecondary,
        background = KuroStreamColors.Background,
        onBackground = KuroStreamColors.OnBackground,
        surface = KuroStreamColors.Surface,
        onSurface = KuroStreamColors.OnSurface,
        surfaceVariant = KuroStreamColors.SurfaceVariant,
        onSurfaceVariant = KuroStreamColors.OnSurfaceVariant,
        error = KuroStreamColors.Error,
        onError = KuroStreamColors.OnError,
        errorContainer = KuroStreamColors.Error.copy(alpha = 0.3f),
        onErrorContainer = KuroStreamColors.Error,
        inverseSurface = KuroStreamColors.OnBackground,
        inverseOnSurface = KuroStreamColors.Background,
        inversePrimary = KuroStreamColors.PrimaryDark,
        border = KuroStreamColors.SurfaceVariant,
        borderVariant = KuroStreamColors.Surface,
        scrim = KuroStreamColors.Scrim
    )

    CompositionLocalProvider(
        LocalKuroStreamSpacing provides KuroStreamSpacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

/**
 * Extension to access custom spacing from theme.
 */
object KuroStreamTheme {
    val spacing: KuroStreamSpacing
        @Composable
        get() = LocalKuroStreamSpacing.current
        
    val colors: KuroStreamColors = KuroStreamColors
    val typography: KuroStreamTypography = KuroStreamTypography
}
