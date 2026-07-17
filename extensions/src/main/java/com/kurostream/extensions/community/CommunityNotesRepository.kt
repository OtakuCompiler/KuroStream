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

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityNotesRepository @Inject constructor() {

    private val demoNotes = mapOf(
        "demo_1" to listOf(
            CommunityNote("n1", "demo_1", null, 45000, "AnimeFan99", null, "This opening scene is absolutely incredible! The animation quality here is top tier.", NoteType.COMMENT, 234, "2026-07-01T10:00:00Z"),
            CommunityNote("n2", "demo_1", null, 120000, "SubWatcher", null, "The foreshadowing in this scene becomes important in episode 12. Pay attention to the background details!", NoteType.FUN_FACT, 567, "2026-07-02T14:30:00Z"),
            CommunityNote("n3", "demo_1", null, 180000, "SpoilerAlert", null, "[SPOILER] The character who appears in the background here is actually the main antagonist.", NoteType.SPOILER, 89, "2026-07-03T08:15:00Z"),
            CommunityNote("n4", "demo_1", null, 240000, "TranslatorPro", null, "The original Japanese line here has a double meaning that doesn't translate well to English.", NoteType.TRANSLATION, 312, "2026-07-04T16:45:00Z"),
            CommunityNote("n5", "demo_1", null, 300000, "MemeLord", null, "This is where the \"I am the bone of my sword\" meme originated from!", NoteType.MEME, 1024, "2026-07-05T20:00:00Z")
        )
    )

    fun getNotesForMedia(mediaId: String): Flow<List<CommunityNote>> = flow {
        delay(300)
        emit(demoNotes[mediaId] ?: emptyList())
    }

    fun getNotesAtTimestamp(mediaId: String, currentTimeMs: Long, windowMs: Long = 30000): Flow<List<CommunityNote>> = flow {
        val notes = demoNotes[mediaId] ?: emptyList()
        emit(notes.filter { it.timestamp in (currentTimeMs - windowMs)..(currentTimeMs + windowMs) })
    }

    fun postNote(note: CommunityNote): Flow<Result<CommunityNote>> = flow {
        delay(500)
        emit(Result.success(note.copy(id = "new_${System.currentTimeMillis()}")))
    }

    fun likeNote(noteId: String): Flow<Result<Boolean>> = flow {
        delay(200)
        emit(Result.success(true))
    }
}