package com.kurostream.app.watchparty

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.UUID

class WatchPartyManager(private val db: FirebaseFirestore) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listener: ListenerRegistration? = null
    private val _partyState = MutableStateFlow<PartyState>(PartyState.Idle)
    val partyState: StateFlow<PartyState> = _partyState.asStateFlow()

    private val commandChannel = Channel<PartyCommand>(Channel.BUFFERED)
    private var currentRoomId: String? = null
    private var isHost: Boolean = false

    fun createRoom(mediaId: String, mediaTitle: String, hostName: String): String {
        val roomId = generateRoomCode()
        isHost = true
        currentRoomId = roomId

        val room = WatchPartyRoom(
            roomId = roomId,
            hostId = getDeviceId(),
            mediaId = mediaId,
            mediaTitle = mediaTitle,
            status = PartyStatus.WAITING,
            currentPositionMs = 0,
            isPlaying = false,
            participants = listOf(Participant(getDeviceId(), hostName, true)),
            createdAt = System.currentTimeMillis(),
        )

        db.collection("watch_parties").document(roomId).set(room)
        _partyState.value = PartyState.Hosting(roomId, room)

        startCommandProcessor()
        listenToRoom(roomId)

        return roomId
    }

    fun joinRoom(roomId: String, userName: String) {
        isHost = false
        currentRoomId = roomId

        db.collection("watch_parties").document(roomId)
            .update("participants", com.google.firebase.firestore.FieldValue.arrayUnion(
                Participant(getDeviceId(), userName, false)
            ))

        _partyState.value = PartyState.Joining(roomId)
        listenToRoom(roomId)
        startCommandProcessor()
    }

    fun sendPlay(positionMs: Long) {
        scope.launch { commandChannel.send(PartyCommand.Play(positionMs)) }
    }

    fun sendPause(positionMs: Long) {
        scope.launch { commandChannel.send(PartyCommand.Pause(positionMs)) }
    }

    fun sendSeek(positionMs: Long) {
        scope.launch { commandChannel.send(PartyCommand.Seek(positionMs)) }
    }

    fun leaveRoom() {
        currentRoomId?.let { roomId ->
            if (!isHost) {
                db.collection("watch_parties").document(roomId)
                    .update("participants", com.google.firebase.firestore.FieldValue.arrayRemove(
                        mapOf("id" to getDeviceId())
                    ))
            } else {
                db.collection("watch_parties").document(roomId).delete()
            }
        }
        listener?.remove()
        listener = null
        currentRoomId = null
        _partyState.value = PartyState.Idle
    }

    private fun listenToRoom(roomId: String) {
        listener = db.collection("watch_parties").document(roomId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Watch party listen failed")
                    return@addSnapshotListener
                }
                snapshot?.toObject(WatchPartyRoom::class.java)?.let { room ->
                    if (!isHost) {
                        _partyState.value = PartyState.InRoom(roomId, room)
                    } else {
                        _partyState.value = PartyState.Hosting(roomId, room)
                    }
                }
            }
    }

    private fun startCommandProcessor() {
        scope.launch {
            for (command in commandChannel) {
                if (!isHost) continue // Only host sends commands
                val roomId = currentRoomId ?: continue
                val update = when (command) {
                    is PartyCommand.Play -> mapOf(
                        "status" to PartyStatus.PLAYING.name,
                        "currentPositionMs" to command.positionMs,
                        "isPlaying" to true,
                        "lastCommandAt" to System.currentTimeMillis(),
                    )
                    is PartyCommand.Pause -> mapOf(
                        "status" to PartyStatus.PAUSED.name,
                        "currentPositionMs" to command.positionMs,
                        "isPlaying" to false,
                        "lastCommandAt" to System.currentTimeMillis(),
                    )
                    is PartyCommand.Seek -> mapOf(
                        "currentPositionMs" to command.positionMs,
                        "lastCommandAt" to System.currentTimeMillis(),
                    )
                }
                db.collection("watch_parties").document(roomId).update(update)
            }
        }
    }

    private fun generateRoomCode(): String {
        return UUID.randomUUID().toString().take(6).uppercase()
    }

    private fun getDeviceId(): String {
        return "device_${System.currentTimeMillis()}"
    }
}

sealed class PartyState {
    object Idle : PartyState()
    data class Hosting(val roomId: String, val room: WatchPartyRoom) : PartyState()
    data class Joining(val roomId: String) : PartyState()
    data class InRoom(val roomId: String, val room: WatchPartyRoom) : PartyState()
}

sealed class PartyCommand {
    data class Play(val positionMs: Long) : PartyCommand()
    data class Pause(val positionMs: Long) : PartyCommand()
    data class Seek(val positionMs: Long) : PartyCommand()
}

data class WatchPartyRoom(
    val roomId: String = "",
    val hostId: String = "",
    val mediaId: String = "",
    val mediaTitle: String = "",
    val status: PartyStatus = PartyStatus.WAITING,
    val currentPositionMs: Long = 0,
    val isPlaying: Boolean = false,
    val participants: List<Participant> = emptyList(),
    val createdAt: Long = 0,
    val lastCommandAt: Long = 0,
)

data class Participant(
    val id: String = "",
    val name: String = "",
    val isHost: Boolean = false,
)

enum class PartyStatus { WAITING, PLAYING, PAUSED, ENDED }