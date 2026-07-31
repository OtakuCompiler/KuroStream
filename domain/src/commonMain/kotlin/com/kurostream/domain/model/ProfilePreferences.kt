// This file is part of KuroStream.
//
// ProfilePreferences — per-profile user preferences.
// Stored as JSON in Profile.preferencesJson.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfilePreferences(
    val language: String = "en",
    val subtitleLanguage: String = "en",
    val audioLanguage: String = "original",
    val playbackQuality: String = "auto",
    val kuroVisionMode: String? = null,
    val kuroVisionUpscale: String? = null,
    val theme: String = "Dark",
    val skin: String = "Arctic Fuse 3",
    val favorites: List<String> = emptyList(),
    val watchHistory: List<String> = emptyList(),
)
