package com.kurostream.tv.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Extended Kuro Stream color palette.
 *
 * Theme primitives (background, primary accent, etc.) live in [KuroStreamColors]
 * inside Theme.kt.  This object holds named semantic colors referenced across
 * the UI — player overlays, card accents, state indicators — that don't map
 * directly to Material3 roles.
 */
object KuroColors {

    // ── Player overlay ────────────────────────────────────────────────────────

    /** Deep crimson used for error states, skip-intro button, and active progress. */
    val Crimson = Color(0xFFDC143C)

    /** Softer red for "live" badges and recording indicators. */
    val LiveRed = Color(0xFFFF3B30)

    // ── Card surfaces ─────────────────────────────────────────────────────────

    /** Semi-transparent card background for overlaid content. */
    val CardBackground = Color(0xCC1A1A2E)

    /** Slightly lighter card surface used for focused/hovered cards. */
    val CardBackgroundFocused = Color(0xCC252545)

    /** Scrim behind dialogs and bottom sheets. */
    val Scrim = Color(0x99000000)

    // ── Text helpers ──────────────────────────────────────────────────────────

    /** Primary text on dark backgrounds. */
    val TextPrimary = Color(0xFFE8E8F0)

    /** Secondary / muted text. */
    val TextSecondary = Color(0xFFAAAAAA)

    /** Disabled / placeholder text. */
    val TextDisabled = Color(0xFF555566)

    // ── Status indicators ─────────────────────────────────────────────────────

    /** Completed / success green. */
    val StatusComplete = Color(0xFF4CAF50)

    /** Airing / in-progress teal. */
    val StatusAiring = Color(0xFF26C6DA)

    /** Not-yet-released grey. */
    val StatusPending = Color(0xFF757575)

    // ── Focus ring ────────────────────────────────────────────────────────────

    /** Purple focus ring — matches KuroStreamColors.Primary. */
    val FocusRing = Color(0xFF7C4DFF)

    // ── Hero gradient stops ───────────────────────────────────────────────────

    /** Bottom gradient start (opaque) for hero banners. */
    val HeroGradientBottom = Color(0xFF0D0D0F)

    /** Left/side gradient start (opaque) for hero banners. */
    val HeroGradientSide = Color(0xFF0D0D0F)

    // ── Skip overlay ──────────────────────────────────────────────────────────

    /** Skip-intro / skip-recap button fill. */
    val SkipButtonFill = Color(0xE6DC143C)

    /** Skip button text / icon. */
    val SkipButtonContent = Color(0xFFFFFFFF)

    // ── Progress bar ─────────────────────────────────────────────────────────

    /** Watched portion of episode progress bar. */
    val ProgressWatched = Color(0xFF7C4DFF)

    /** Unwatched buffer portion. */
    val ProgressBuffered = Color(0x557C4DFF)

    /** Track background. */
    val ProgressTrack = Color(0xFF2A2A3E)
}
