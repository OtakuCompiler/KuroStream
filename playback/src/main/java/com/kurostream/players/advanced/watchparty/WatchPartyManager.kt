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

package com.kurostream.players.advanced.watchparty

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import org.webrtc.*
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * P2P Watch Party with CRDT state synchronization, STUN/TURN, and LAN discovery.
 * Enables synchronized video playback across multiple devices without central server.
 */
class WatchPartyManager(
    private val context: Context,
    private val eglBase: EglBase
) {

    companion object {
        const val TAG = "WatchParty"
        const val SERVICE_TYPE = "_kurostream._tcp."
        const val SERVICE_NAME = "StreamPulseWatchParty"
        const val SIGNALING_PORT = 8765
        const val DATA_CHANNEL_LABEL = "watchparty-sync"
        const val MAX_PEERS = 8
        const val HEARTBEAT_INTERVAL_MS = 3000L
        const val SYNC_TOLERANCE_MS = 100L
    }

    private val partyScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isHosting = AtomicBoolean(false)
    private val isJoined = AtomicBoolean(false)
    private val localPeerId = UUID.randomUUID().toString()

    // WebRTC
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private val peerConnections = ConcurrentHashMap<String, PeerConnection>()
    private val dataChannels = ConcurrentHashMap<String, DataChannel>()

    // LAN Discovery
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val discoveredServices = ConcurrentHashMap<String, NsdServiceInfo>()

    // CRDT State
    private val crdtState = AtomicReference(WatchPartyState())
    private val _stateFlow = MutableStateFlow(WatchPartyState())
    val stateFlow: StateFlow<WatchPartyState> = _stateFlow.asStateFlow()

    // Signaling
    private val signalingServer = SignalingServer(SIGNALING_PORT)
    private val signalingClient = AtomicReference<SignalingClient?>(null)

    data class WatchPartyState(
        val videoUrl: String = "",
        val isPlaying: Boolean = false,
        val currentPositionMs: Long = 0,
        val playbackRate: Float = 1.0f,
        val timestamp: Long = System.currentTimeMillis(),
        val peerId: String = "",
        val vectorClock: Map<String, Long> = emptyMap()
    ) : Comparable<WatchPartyState> {
        override fun compareTo(other: WatchPartyState): Int {
            val localTime = vectorClock[peerId] ?: 0
            val otherTime = other.vectorClock[other.peerId] ?: 0
            return localTime.compareTo(otherTime)
        }
    }

    data class PeerInfo(
        val id: String,
        val displayName: String,
        val isHost: Boolean,
        val connectionState: PeerConnection.IceConnectionState,
        val latencyMs: Long = 0
    )

    private val _peersFlow = MutableStateFlow<List<PeerInfo>>(emptyList())
    val peersFlow: StateFlow<List<PeerInfo>> = _peersFlow.asStateFlow()

    sealed class WatchPartyEvent {
        data class PeerJoined(val peerId: String, val displayName: String) : WatchPartyEvent()
        data class PeerLeft(val peerId: String) : WatchPartyEvent()
        data class StateSynced(val state: WatchPartyState) : WatchPartyEvent()
        data class ChatMessage(val peerId: String, val message: String) : WatchPartyEvent()
        data class Error(val message: String) : WatchPartyEvent()
    }

    private val _events = MutableSharedFlow<WatchPartyEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WatchPartyEvent> = _events.asSharedFlow()

    init {
        initializeWebRTC()
    }

    private fun initializeWebRTC() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun hostParty(roomName: String, displayName: String) {
        if (isHosting.get()) return
        isHosting.set(true)
        crdtState.set(WatchPartyState(peerId = localPeerId))
        signalingServer.start()
        registerLanService(roomName, displayName)
        partyScope.launch { heartbeatLoop() }
        Log.i(TAG, "Hosting watch party: $roomName")
    }

    fun joinParty(serviceInfo: NsdServiceInfo, displayName: String) {
        if (isJoined.get()) return
        isJoined.set(true)
        val hostAddress = serviceInfo.host
        val hostPort = serviceInfo.port
        signalingClient.set(SignalingClient(hostAddress, hostPort))
        signalingClient.get()?.connect { message ->
            handleSignalingMessage(message)
        }
        createPeerConnection(serviceInfo.serviceName, true)
        Log.i(TAG, "Joined watch party at $hostAddress:$hostPort")
    }

    fun discoverParties(): Flow<List<NsdServiceInfo>> = callbackFlow {
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Discovery started: $regType")
            }
            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType == SERVICE_TYPE) {
                    discoveredServices[service.serviceName] = service
                    trySend(discoveredServices.values.toList())
                }
            }
            override fun onServiceLost(service: NsdServiceInfo) {
                discoveredServices.remove(service.serviceName)
                trySend(discoveredServices.values.toList())
            }
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                close(Exception("Discovery failed: $errorCode"))
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        discoveryListener = listener
        nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        awaitClose {
            nsdManager?.stopServiceDiscovery(listener)
        }
    }

    private fun registerLanService(roomName: String, displayName: String) {
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "$SERVICE_NAME-$roomName"
            serviceType = SERVICE_TYPE
            port = SIGNALING_PORT
            setAttribute("peer_id", localPeerId)
            setAttribute("display_name", displayName)
            setAttribute("version", "1.0")
        }
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.i(TAG, "Service registered: ${info.serviceName}")
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Registration failed: $errorCode")
            }
            override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }
        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener!!)
    }

    private fun createPeerConnection(peerId: String, isInitiator: Boolean) {
        val factory = peerConnectionFactory ?: return
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("turn:turn.streampulse.io:3478")
                .setUsername("streampulse")
                .setPassword("watchparty2026")
                .createIceServer()
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                updatePeerState(peerId, state)
                if (state == PeerConnection.IceConnectionState.CONNECTED) {
                    _events.tryEmit(WatchPartyEvent.PeerJoined(peerId, ""))
                } else if (state == PeerConnection.IceConnectionState.DISCONNECTED ||
                           state == PeerConnection.IceConnectionState.FAILED) {
                    _events.tryEmit(WatchPartyEvent.PeerLeft(peerId))
                    removePeer(peerId)
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
            override fun onIceCandidate(candidate: IceCandidate) {
                sendSignalingMessage(peerId, "candidate", candidate.sdp)
            }
            override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {}
            override fun onAddStream(stream: MediaStream) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(channel: DataChannel) {
                setupDataChannel(peerId, channel)
            }
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver, streams: Array<MediaStream>) {}
        }
        val pc = factory.createPeerConnection(rtcConfig, observer)
        peerConnections[peerId] = pc!!
        val init = DataChannel.Init().apply {
            ordered = true
            maxRetransmits = 3
        }
        val dc = pc.createDataChannel(DATA_CHANNEL_LABEL, init)
        setupDataChannel(peerId, dc)
        if (isInitiator) {
            createOffer(peerId)
        }
    }

    private fun setupDataChannel(peerId: String, channel: DataChannel) {
        dataChannels[peerId] = channel
        channel.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}
            override fun onStateChange() {
                Log.d(TAG, "Data channel state: ${channel.state()}")
            }
            override fun onMessage(buffer: DataChannel.Buffer) {
                val data = ByteArray(buffer.data.remaining())
                buffer.data.get(data)
                handleDataMessage(peerId, String(data))
            }
        })
    }

    private fun handleDataMessage(peerId: String, message: String) {
        try {
            val json = JSONObject(message)
            when (json.getString("type")) {
                "state_sync" -> {
                    val state = parseState(json.getJSONObject("state"))
                    mergeState(state)
                }
                "chat" -> {
                    _events.tryEmit(WatchPartyEvent.ChatMessage(
                        peerId,
                        json.getString("message")
                    ))
                }
                "heartbeat" -> {
                    val sentTime = json.getLong("timestamp")
                    val latency = System.currentTimeMillis() - sentTime
                    updatePeerLatency(peerId, latency)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse data message", e)
        }
    }

    fun updatePlaybackState(
        isPlaying: Boolean? = null,
        positionMs: Long? = null,
        videoUrl: String? = null,
        playbackRate: Float? = null
    ) {
        val current = crdtState.get()
        val newVectorClock = current.vectorClock.toMutableMap().apply {
            this[localPeerId] = (this[localPeerId] ?: 0) + 1
        }
        val newState = current.copy(
            isPlaying = isPlaying ?: current.isPlaying,
            currentPositionMs = positionMs ?: current.currentPositionMs,
            videoUrl = videoUrl ?: current.videoUrl,
            playbackRate = playbackRate ?: current.playbackRate,
            timestamp = System.currentTimeMillis(),
            peerId = localPeerId,
            vectorClock = newVectorClock
        )
        crdtState.set(newState)
        _stateFlow.value = newState
        broadcastState(newState)
    }

    private fun mergeState(incoming: WatchPartyState) {
        val current = crdtState.get()
        val shouldMerge = when {
            incoming.vectorClock.size > current.vectorClock.size -> true
            incoming.timestamp > current.timestamp + SYNC_TOLERANCE_MS -> true
            else -> false
        }
        if (shouldMerge) {
            val mergedVectorClock = (current.vectorClock.keys + incoming.vectorClock.keys).associateWith { key ->
                maxOf(current.vectorClock[key] ?: 0, incoming.vectorClock[key] ?: 0)
            }
            val merged = incoming.copy(vectorClock = mergedVectorClock)
            crdtState.set(merged)
            _stateFlow.value = merged
            _events.tryEmit(WatchPartyEvent.StateSynced(merged))
        }
    }

    private fun broadcastState(state: WatchPartyState) {
        val message = JSONObject().apply {
            put("type", "state_sync")
            put("state", stateToJson(state))
        }.toString()
        dataChannels.values.forEach { channel ->
            if (channel.state() == DataChannel.State.OPEN) {
                val buffer = DataChannel.Buffer(
                    java.nio.ByteBuffer.wrap(message.toByteArray()),
                    false
                )
                channel.send(buffer)
            }
        }
    }

    fun sendChatMessage(message: String) {
        val json = JSONObject().apply {
            put("type", "chat")
            put("message", message)
            put("peer_id", localPeerId)
            put("timestamp", System.currentTimeMillis())
        }
        dataChannels.values.forEach { channel ->
            if (channel.state() == DataChannel.State.OPEN) {
                val buffer = DataChannel.Buffer(
                    java.nio.ByteBuffer.wrap(json.toString().toByteArray()),
                    false
                )
                channel.send(buffer)
            }
        }
    }

    private fun createOffer(peerId: String) {
        val pc = peerConnections[peerId] ?: return
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription) {
                pc.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        sendSignalingMessage(peerId, "offer", description.description)
                    }
                    override fun onSetFailure(error: String) {}
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(error: String) {}
                }, description)
            }
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String) {}
            override fun onCreateFailure(error: String) {
                Log.e(TAG, "Failed to create offer: $error")
            }
        }, MediaConstraints())
    }

    private fun handleSignalingMessage(message: SignalingMessage) {
        when (message.type) {
            "offer" -> handleOffer(message.peerId, message.sdp)
            "answer" -> handleAnswer(message.peerId, message.sdp)
            "candidate" -> handleCandidate(message.peerId, message.sdp)
        }
    }

    private fun handleOffer(peerId: String, sdp: String) {
        val pc = peerConnections[peerId] ?: createPeerConnection(peerId, false).let { peerConnections[peerId] } ?: return
        pc.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                pc.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(description: SessionDescription) {
                        pc.setLocalDescription(object : SdpObserver {
                            override fun onSetSuccess() {
                                sendSignalingMessage(peerId, "answer", description.description)
                            }
                            override fun onSetFailure(error: String) {}
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onCreateFailure(error: String) {}
                        }, description)
                    }
                    override fun onSetSuccess() {}
                    override fun onSetFailure(error: String) {}
                    override fun onCreateFailure(error: String) {}
                }, MediaConstraints())
            }
            override fun onSetFailure(error: String) {}
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(error: String) {}
        }, SessionDescription(SessionDescription.Type.OFFER, sdp))
    }

    private fun handleAnswer(peerId: String, sdp: String) {
        val pc = peerConnections[peerId] ?: return
        pc.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String) {
                Log.e(TAG, "Failed to set remote description: $error")
            }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(error: String) {}
        }, SessionDescription(SessionDescription.Type.ANSWER, sdp))
    }

    private fun handleCandidate(peerId: String, candidateSdp: String) {
        val pc = peerConnections[peerId] ?: return
        val candidate = IceCandidate("0", 0, candidateSdp)
        pc.addIceCandidate(candidate)
    }

    private fun sendSignalingMessage(peerId: String, type: String, sdp: String) {
        signalingClient.get()?.send(SignalingMessage(peerId, type, sdp))
    }

    private suspend fun heartbeatLoop() {
        while (isActive) {
            val heartbeat = JSONObject().apply {
                put("type", "heartbeat")
                put("timestamp", System.currentTimeMillis())
                put("peer_id", localPeerId)
            }
            dataChannels.values.forEach { channel ->
                if (channel.state() == DataChannel.State.OPEN) {
                    val buffer = DataChannel.Buffer(
                        java.nio.ByteBuffer.wrap(heartbeat.toString().toByteArray()),
                        false
                    )
                    channel.send(buffer)
                }
            }
            delay(HEARTBEAT_INTERVAL_MS)
        }
    }

    private fun updatePeerState(peerId: String, state: PeerConnection.IceConnectionState) {
        _peersFlow.value = _peersFlow.value.map {
            if (it.id == peerId) it.copy(connectionState = state) else it
        }
    }

    private fun updatePeerLatency(peerId: String, latencyMs: Long) {
        _peersFlow.value = _peersFlow.value.map {
            if (it.id == peerId) it.copy(latencyMs = latencyMs) else it
        }
    }

    private fun removePeer(peerId: String) {
        peerConnections.remove(peerId)?.close()
        dataChannels.remove(peerId)?.close()
        _peersFlow.value = _peersFlow.value.filter { it.id != peerId }
    }

    private fun stateToJson(state: WatchPartyState): JSONObject {
        return JSONObject().apply {
            put("video_url", state.videoUrl)
            put("is_playing", state.isPlaying)
            put("position_ms", state.currentPositionMs)
            put("playback_rate", state.playbackRate)
            put("timestamp", state.timestamp)
            put("peer_id", state.peerId)
            put("vector_clock", JSONObject(state.vectorClock as Map<*, *>))
        }
    }

    private fun parseState(json: JSONObject): WatchPartyState {
        val vectorClock = mutableMapOf<String, Long>()
        val vcJson = json.optJSONObject("vector_clock")
        vcJson?.keys()?.forEach { key ->
            vectorClock[key] = vcJson.getLong(key)
        }
        return WatchPartyState(
            videoUrl = json.optString("video_url", ""),
            isPlaying = json.optBoolean("is_playing", false),
            currentPositionMs = json.optLong("position_ms", 0),
            playbackRate = json.optDouble("playback_rate", 1.0).toFloat(),
            timestamp = json.optLong("timestamp", 0),
            peerId = json.optString("peer_id", ""),
            vectorClock = vectorClock
        )
    }

    fun leaveParty() {
        isHosting.set(false)
        isJoined.set(false)
        peerConnections.values.forEach { it.close() }
        peerConnections.clear()
        dataChannels.values.forEach { it.close() }
        dataChannels.clear()
        signalingServer.stop()
        signalingClient.get()?.disconnect()
        registrationListener?.let { nsdManager?.unregisterService(it) }
        discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        partyScope.cancel()
    }

    fun release() {
        leaveParty()
        peerConnectionFactory?.dispose()
    }
}
