// This file is part of KuroStream.
//
// SubtitlePreferences — persisted user preferences for the subtitle engine.
// Stored in DataStore, consumed by KuroSubtitleEngine.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.domain.subtitle

import kotlinx.serialization.Serializable

@Serializable
data class SubtitlePreferences(
    val primaryLanguage: String = "en",
    val secondaryLanguage: String? = null,
    val autoDownload: Boolean = true,
    val preferAss: Boolean = true,
    val hearingImpaired: Boolean = false,
    val forcedOnly: Boolean = false,
    val fontSize: Int = 24,
    val backgroundColorOpacity: Float = 0.5f,
    val outlineEnabled: Boolean = true,
    val shadowEnabled: Boolean = true,
    val positionBottom: Boolean = true,
    val autoSync: Boolean = true,
    val syncOffsetMs: Int = 0,
)
