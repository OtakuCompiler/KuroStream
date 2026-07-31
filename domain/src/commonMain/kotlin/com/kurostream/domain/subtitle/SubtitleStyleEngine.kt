// This file is part of KuroStream.
//
// SubtitleStyleEngine — runtime subtitle styling (ASS/SRT/VTT).
// Applies user overrides for font, color, position, outline, shadow.
// Lightweight: <1MB memory.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.domain.subtitle

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleStyleEngine @Inject constructor(
    private val preferences: SubtitlePreferences,
) {
    data class Style(
        val fontSize: Int = preferences.fontSize,
        val primaryColor: Int = 0xFFFFFFFF,
        val outlineColor: Int = 0xFF000000.toInt(),
        val backgroundColor: Int = 0x00000000,
        val outlineThickness: Int = if (preferences.outlineEnabled) 2 else 0,
        val shadowDepth: Int = if (preferences.shadowEnabled) 2 else 0,
        val marginV: Int = if (preferences.positionBottom) 20 else 10,
    )

    fun getStyle(): Style = Style()

    fun applyAssOverride(assContent: String): String {
        val style = getStyle()
        return assContent
            .replace(Regex("""FontSize=\d+"""), "FontSize=${style.fontSize}")
            .replace(Regex("""Outline=\d+(\.\d+)?"""), "Outline=${style.outlineThickness}")
            .replace(Regex("""Shadow=\d+(\.\d+)?"""), "Shadow=${style.shadowDepth}")
    }
}
