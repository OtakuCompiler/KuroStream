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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunityNotesViewModel @Inject constructor(private val repository: CommunityNotesRepository) : ViewModel() {

    private val _allNotes = MutableStateFlow<List<CommunityNote>>(emptyList())
    private val _visibleNotes = MutableStateFlow<List<NoteDisplayState>>(emptyList())
    val visibleNotes: StateFlow<List<NoteDisplayState>> = _visibleNotes.asStateFlow()

    private val _showSpoilers = MutableStateFlow(false)
    val showSpoilers: StateFlow<Boolean> = _showSpoilers.asStateFlow()

    private val _currentTimeMs = MutableStateFlow(0L)
    private val expandedNotes = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            combine(_allNotes, _currentTimeMs, _showSpoilers) { notes, time, showSpoilers ->
                val windowMs = 60000L
                notes.filter { note ->
                    kotlin.math.abs(note.timestamp - time) <= windowMs && (showSpoilers || note.type != NoteType.SPOILER)
                }.map { note -> NoteDisplayState(note, isExpanded = expandedNotes.contains(note.id)) }
                    .sortedBy { kotlin.math.abs(it.note.timestamp - time) }
            }.collect { _visibleNotes.value = it }
        }
    }

    fun loadNotes(mediaId: String) {
        viewModelScope.launch { repository.getNotesForMedia(mediaId).collect { _allNotes.value = it } }
    }

    fun updateCurrentTime(timeMs: Long) { _currentTimeMs.value = timeMs }
    fun toggleSpoilers() { _showSpoilers.value = !_showSpoilers.value }

    fun toggleExpand(noteId: String) {
        if (expandedNotes.contains(noteId)) expandedNotes.remove(noteId) else expandedNotes.add(noteId)
        _visibleNotes.value = _visibleNotes.value.map { it.copy(isExpanded = expandedNotes.contains(it.note.id)) }
    }

    fun likeNote(noteId: String) {
        viewModelScope.launch {
            repository.likeNote(noteId).collect { result ->
                result.onSuccess { _allNotes.value = _allNotes.value.map { if (it.id == noteId) it.copy(likes = it.likes + 1) else it } }
            }
        }
    }

    fun postNote(mediaId: String, content: String, timestamp: Long, type: NoteType = NoteType.COMMENT) {
        viewModelScope.launch {
            val note = CommunityNote("", mediaId, null, timestamp, "You", null, content, type, 0, java.time.Instant.now().toString())
            repository.postNote(note).collect { result ->
                result.onSuccess { newNote -> _allNotes.value = _allNotes.value + newNote }
            }
        }
    }
}
