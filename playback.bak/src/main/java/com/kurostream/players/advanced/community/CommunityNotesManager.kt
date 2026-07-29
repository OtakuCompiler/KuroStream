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

package com.kurostream.players.advanced.community

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Community Notes system with creation UI, moderation, and Firestore storage.
 * Allows users to add contextual notes to videos and rate their helpfulness.
 */
class CommunityNotesManager(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        const val COLLECTION_NOTES = "community_notes"
        const val COLLECTION_RATINGS = "note_ratings"
        const val COLLECTION_MODERATION = "moderation_queue"
        const val MIN_HELPFULNESS_THRESHOLD = 0.6f
        const val MAX_NOTES_PER_VIDEO = 100
        const val MAX_NOTE_LENGTH = 500
    }

    private val notesScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val localCache = ConcurrentHashMap<String, MutableList<CommunityNote>>()

    data class CommunityNote(
        val id: String = UUID.randomUUID().toString(),
        val videoId: String = "",
        val content: String = "",
        val authorId: String = "",
        val authorName: String = "",
        val timestampMs: Long = System.currentTimeMillis(),
        val helpfulCount: Int = 0,
        val notHelpfulCount: Int = 0,
        val helpfulnessScore: Float = 0f,
        val isPublished: Boolean = false,
        val moderationStatus: ModerationStatus = ModerationStatus.PENDING,
        val tags: List<String> = emptyList(),
        val sourceUrl: String? = null,
        val sourceTitle: String? = null
    ) {
        enum class ModerationStatus {
            PENDING, APPROVED, REJECTED, FLAGGED
        }
    }

    data class NoteRating(
        val noteId: String = "",
        val userId: String = "",
        val isHelpful: Boolean = true,
        val reason: String? = null,
        val timestampMs: Long = System.currentTimeMillis()
    )

    data class ModerationAction(
        val noteId: String = "",
        val moderatorId: String = "",
        val action: Action,
        val reason: String = "",
        val timestampMs: Long = System.currentTimeMillis()
    ) {
        enum class Action {
            APPROVE, REJECT, FLAG, ESCALATE
        }
    }

    // Flows
    private val _notesFlow = MutableStateFlow<List<CommunityNote>>(emptyList())
    val notesFlow: StateFlow<List<CommunityNote>> = _notesFlow.asStateFlow()

    private val _pendingNotesFlow = MutableStateFlow<List<CommunityNote>>(emptyList())
    val pendingNotesFlow: StateFlow<List<CommunityNote>> = _pendingNotesFlow.asStateFlow()

    /**
     * Create a new community note for a video.
     */
    suspend fun createNote(
        videoId: String,
        content: String,
        authorId: String,
        authorName: String,
        tags: List<String> = emptyList(),
        sourceUrl: String? = null,
        sourceTitle: String? = null
    ): Result<CommunityNote> = withContext(Dispatchers.IO) {
        try {
            // Validate input
            if (content.length > MAX_NOTE_LENGTH) {
                return@withContext Result.failure(IllegalArgumentException(
                    "Note exceeds maximum length of $MAX_NOTE_LENGTH characters"
                ))
            }

            if (content.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Note content cannot be empty"))
            }

            // Run content moderation
            val moderationResult = runModeration(content)
            if (moderationResult.isRejected) {
                return@withContext Result.failure(SecurityException(
                    "Note rejected by automated moderation: ${moderationResult.reason}"
                ))
            }

            val note = CommunityNote(
                videoId = videoId,
                content = content.trim(),
                authorId = authorId,
                authorName = authorName,
                tags = tags,
                sourceUrl = sourceUrl,
                sourceTitle = sourceTitle,
                moderationStatus = if (moderationResult.requiresReview) {
                    CommunityNote.ModerationStatus.PENDING
                } else {
                    CommunityNote.ModerationStatus.APPROVED
                },
                isPublished = !moderationResult.requiresReview
            )

            // Store in Firestore
            firestore.collection(COLLECTION_NOTES)
                .document(note.id)
                .set(note.toMap())
                .await()

            // Update local cache
            localCache.getOrPut(videoId) { mutableListOf() }.add(note)

            Result.success(note)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Rate a note as helpful or not helpful.
     */
    suspend fun rateNote(
        noteId: String,
        userId: String,
        isHelpful: Boolean,
        reason: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rating = NoteRating(
                noteId = noteId,
                userId = userId,
                isHelpful = isHelpful,
                reason = reason
            )

            // Store rating
            firestore.collection(COLLECTION_RATINGS)
                .document("${noteId}_$userId")
                .set(rating.toMap())
                .await()

            // Update note helpfulness score
            updateHelpfulnessScore(noteId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch notes for a video with real-time updates.
     */
    fun observeNotesForVideo(videoId: String): Flow<List<CommunityNote>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_NOTES)
            .whereEqualTo("videoId", videoId)
            .whereEqualTo("isPublished", true)
            .orderBy("helpfulnessScore", Query.Direction.DESCENDING)
            .limit(MAX_NOTES_PER_VIDEO.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val notes = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CommunityNote::class.java)
                } ?: emptyList()

                localCache[videoId] = notes.toMutableList()
                trySend(notes)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Fetch pending notes for moderation.
     */
    fun observePendingNotes(): Flow<List<CommunityNote>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_NOTES)
            .whereEqualTo("moderationStatus", CommunityNote.ModerationStatus.PENDING.name)
            .orderBy("timestampMs", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val notes = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CommunityNote::class.java)
                } ?: emptyList()

                trySend(notes)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Moderate a note (approve/reject/flag).
     */
    suspend fun moderateNote(
        noteId: String,
        moderatorId: String,
        action: ModerationAction.Action,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val moderationAction = ModerationAction(
                noteId = noteId,
                moderatorId = moderatorId,
                action = action,
                reason = reason
            )

            // Store moderation action
            firestore.collection(COLLECTION_MODERATION)
                .add(moderationAction.toMap())
                .await()

            // Update note status
            val newStatus = when (action) {
                ModerationAction.Action.APPROVE -> {
                    CommunityNote.ModerationStatus.APPROVED to true
                }
                ModerationAction.Action.REJECT -> {
                    CommunityNote.ModerationStatus.REJECTED to false
                }
                ModerationAction.Action.FLAG -> {
                    CommunityNote.ModerationStatus.FLAGGED to false
                }
                ModerationAction.Action.ESCALATE -> {
                    CommunityNote.ModerationStatus.PENDING to false
                }
            }

            firestore.collection(COLLECTION_NOTES)
                .document(noteId)
                .update(
                    mapOf(
                        "moderationStatus" to newStatus.first.name,
                        "isPublished" to newStatus.second
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateHelpfulnessScore(noteId: String) {
        val ratingsSnapshot = firestore.collection(COLLECTION_RATINGS)
            .whereEqualTo("noteId", noteId)
            .get()
            .await()

        val ratings = ratingsSnapshot.documents.mapNotNull { doc ->
            doc.toObject(NoteRating::class.java)
        }

        val helpfulCount = ratings.count { it.isHelpful }
        val totalCount = ratings.size
        val score = if (totalCount > 0) helpfulCount.toFloat() / totalCount else 0f

        firestore.collection(COLLECTION_NOTES)
            .document(noteId)
            .update(
                mapOf(
                    "helpfulCount" to helpfulCount,
                    "notHelpfulCount" to (totalCount - helpfulCount),
                    "helpfulnessScore" to score
                )
            )
            .await()
    }

    private fun runModeration(content: String): ModerationResult {
        // Automated content moderation
        val blockedPatterns = listOf(
            Regex("\b(hate|violence|spam|scam)\b", RegexOption.IGNORE_CASE),
            Regex("(https?://)?[\w-]+\.(com|net|org)\b.*\b(click|win|free|urgent)", RegexOption.IGNORE_CASE)
        )

        for (pattern in blockedPatterns) {
            if (pattern.containsMatchIn(content)) {
                return ModerationResult(
                    isRejected = true,
                    requiresReview = false,
                    reason = "Content violates community guidelines"
                )
            }
        }

        // Flag for review if contains external links
        val hasExternalLink = Regex("https?://").containsMatchIn(content)

        return ModerationResult(
            isRejected = false,
            requiresReview = hasExternalLink,
            reason = if (hasExternalLink) "Contains external link, requires review" else null
        )
    }

    data class ModerationResult(
        val isRejected: Boolean,
        val requiresReview: Boolean,
        val reason: String? = null
    )

    private fun CommunityNote.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "videoId" to videoId,
        "content" to content,
        "authorId" to authorId,
        "authorName" to authorName,
        "timestampMs" to timestampMs,
        "helpfulCount" to helpfulCount,
        "notHelpfulCount" to notHelpfulCount,
        "helpfulnessScore" to helpfulnessScore,
        "isPublished" to isPublished,
        "moderationStatus" to moderationStatus.name,
        "tags" to tags,
        "sourceUrl" to sourceUrl,
        "sourceTitle" to sourceTitle
    )

    private fun NoteRating.toMap(): Map<String, Any?> = mapOf(
        "noteId" to noteId,
        "userId" to userId,
        "isHelpful" to isHelpful,
        "reason" to reason,
        "timestampMs" to timestampMs
    )

    private fun ModerationAction.toMap(): Map<String, Any?> = mapOf(
        "noteId" to noteId,
        "moderatorId" to moderatorId,
        "action" to action.name,
        "reason" to reason,
        "timestampMs" to timestampMs
    )

    fun release() {
        notesScope.cancel()
    }
}
