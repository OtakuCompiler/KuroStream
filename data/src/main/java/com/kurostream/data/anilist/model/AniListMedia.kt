// KuroStream - Anime Streaming for Android TV
// Copyright (C) 2026 KuroStream Contributors
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// SPDX-License-Identifier: GPL-3.0-only

package com.kurostream.data.anilist.model

data class AniListMedia(
    val id: Int,
    val idMal: Int?,
    val title: String,
    val coverImage: String,
    val bannerImage: String,
    val description: String,
    val episodes: Int?,
    val duration: Int?,
    val status: String,
    val season: String,
    val seasonYear: Int?,
    val averageScore: Int?,
    val genres: List<String>,
    val studios: List<String>,
    val nextAiringEpisode: NextAiringEpisode?,
) {
    data class NextAiringEpisode(
        val episode: Int,
        val timeUntilAiring: Int,
    )
}
