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

package com.kurostream.domain.entity

import kotlinx.serialization.Serializable

@Serializable
data class ExtensionInfo(
    val id: String,
    val name: String,
    val author: String,
    val version: SemanticVersion,
    val description: String? = null,
    val iconUrl: String? = null,
    val packageName: String,
    val capabilities: Set<ExtensionCapability> = emptySet(),
    val isInstalled: Boolean = false,
    val isEnabled: Boolean = false,
    val isTrusted: Boolean = false,
    val installPath: String? = null,
    val signatureFingerprint: String? = null,
    val minAppVersion: SemanticVersion = SemanticVersion(1, 0, 0),
    val targetAppVersion: SemanticVersion? = null
)

@Serializable
data class SemanticVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
    override fun toString(): String = "$major.$minor.$patch"
}

enum class ExtensionCapability {
    CATALOG_BROWSE,
    CATALOG_SEARCH,
    EPISODE_LIST,
    VIDEO_SOURCE,
    SUBTITLE_SOURCE,
    TRACKING,
    SYNC_PROVIDER,
    TORRENT_STREAMING,
    TORRENT_DOWNLOAD,
    TORRENT_SEEDING,
    LIVE_TV,
    CLOUD_SYNC,
}