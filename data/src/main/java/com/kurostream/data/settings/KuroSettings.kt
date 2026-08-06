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

package com.kurostream.data.settings

import com.kurostream.domain.repository.AppTheme

data class KuroSettings(
    val themeMode: AppTheme = AppTheme.DARK,
    val glassCards: Boolean = true,
    val blurEffects: Boolean = true,
    val oledBlack: Boolean = false,
    val tagStyle: String = "BOX",
    val defaultEngine: String = "Auto",
    val defaultQuality: String = "Auto",
    val bufferSizeMs: Int = 30000,
    val autoPlayNext: Boolean = true,
    val refreshRateSwitching: Boolean = false,
    val upscaleAlgorithm: String = "LANCZOS3",
    val colorProfile: String = "NATURAL",
    val contrastAdaptiveSharpening: Boolean = false,
    val fakeHdr: Boolean = false,
    val oledMode: Boolean = false,
    val passthroughMode: String = "AUTO",
    val audioDelayMs: Int = 0,
    val nightModeDrc: Boolean = false,
    val dialogueBoost: Boolean = false,
    val subtitleLanguagePriority: List<String> = listOf("en", "ja"),
    val subtitleProviders: List<String> = listOf("opensubtitles", "subdl"),
    val subtitleSize: Float = 1.0f,
    val subtitleSyncOffset: Int = 0,
    val enabledExtensions: Set<String> = emptySet(),
    val extensionAutoUpdate: Boolean = true,
    val sandboxStrictMode: Boolean = true,
    val dohProvider: String = "Cloudflare",
    val certificatePinning: Boolean = false,
    val kidsMode: Boolean = false,
    val pinHash: String? = null,
    val parentalRatingLimit: String = "PG-13",
    val traktSync: Boolean = false,
    val anilistSync: Boolean = false,
    val malSync: Boolean = false,
    val defaultHub: String = "Home",
    val maxRows: Int = 5,
    val heroAutoScroll: Boolean = true,
)
