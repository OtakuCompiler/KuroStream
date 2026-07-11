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

package com.kurostream.extensions.community

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CommunityNote(
    @Json(name = "id") val id: String,
    @Json(name = "mediaId") val mediaId: String,
    @Json(name = "episodeId") val episodeId: String? = null,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "author") val author: String,
    @Json(name = "authorAvatar") val authorAvatar: String? = null,
    @Json(name = "content") val content: String,
    @Json(name = "type") val type: NoteType = NoteType.COMMENT,
    @Json(name = "likes") val likes: Int = 0,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "replies") val replies: List<CommunityNoteReply> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CommunityNoteReply(
    @Json(name = "id") val id: String,
    @Json(name = "author") val author: String,
    @Json(name = "content") val content: String,
    @Json(name = "createdAt") val createdAt: String
)

enum class NoteType { COMMENT, SPOILER, MEME, TRANSLATION, CORRECTION, FUN_FACT }

data class NoteDisplayState(val note: CommunityNote, val isVisible: Boolean = true, val isExpanded: Boolean = false)
