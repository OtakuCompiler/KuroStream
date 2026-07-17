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

package com.kurostream.extensions.watchparty

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRtcManager @Inject constructor(private val context: Context) {
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private val peerConnections = mutableMapOf<String, PeerConnection>()
    private val dataChannels = mutableMapOf<String, DataChannel>()

    private val _connectionState = MutableStateFlow<PeerConnection.IceConnectionState?>(null)
    val connectionState: StateFlow<PeerConnection.IceConnectionState?> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>()
    val incomingMessages: SharedFlow<String> = _incomingMessages.asSharedFlow()

    private val eglBase = EglBase.create()

    fun initialize() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    fun createPeerConnection(participantId: String, config: WebRtcConfig = WebRtcConfig()): PeerConnection? {
        val factory = peerConnectionFactory ?: return null
        val iceServers = config.iceServers.map {
            PeerConnection.IceServer.builder(it.urls).setUsername(it.username).setPassword(it.credential).createIceServer()
        }
        val pcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) { _connectionState.value = state }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dc: DataChannel?) { dc?.let { setupDataChannel(participantId, it) } }
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
        }
        val pc = factory.createPeerConnection(pcConfig, observer)
        pc?.let { peerConnections[participantId] = it }
        val init = DataChannel.Init().apply { ordered = true; maxRetransmits = 30 }
        pc?.createDataChannel("sync", init)?.let { setupDataChannel(participantId, it) }
        return pc
    }

    private fun setupDataChannel(participantId: String, dc: DataChannel) {
        dataChannels[participantId] = dc
        dc.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}
            override fun onStateChange() {}
            override fun onMessage(buffer: DataChannel.Buffer?) {
                buffer?.data?.let {
                    val bytes = ByteArray(it.remaining())
                    it.get(bytes)
                    _incomingMessages.tryEmit(String(bytes, Charsets.UTF_8))
                }
            }
        })
    }

    suspend fun createOffer(participantId: String): SessionDescription? = suspendCancellableCoroutine { cont ->
        val pc = peerConnections[participantId] ?: run { cont.resume(null) {}; return@suspendCancellableCoroutine }
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                pc.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() { cont.resume(desc!!) {} }
                    override fun onSetFailure(error: String?) { cont.resume(null) {} }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(error: String?) {}
                }, desc)
            }
            override fun onCreateFailure(error: String?) { cont.resume(null) {} }
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, MediaConstraints())
    }

    suspend fun setRemoteDescription(participantId: String, sdp: SessionDescription) = suspendCancellableCoroutine { cont ->
        peerConnections[participantId]?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() { cont.resume(Unit) {} }
            override fun onSetFailure(error: String?) { cont.resume(Unit) {} }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, sdp)
    }

    fun sendMessage(participantId: String, message: String): Boolean {
        val dc = dataChannels[participantId] ?: return false
        val buffer = DataChannel.Buffer(java.nio.ByteBuffer.wrap(message.toByteArray(Charsets.UTF_8)), false)
        return dc.send(buffer)
    }

    fun broadcastMessage(message: String) {
        dataChannels.forEach { (_, dc) ->
            if (dc.state() == DataChannel.State.OPEN) {
                dc.send(DataChannel.Buffer(java.nio.ByteBuffer.wrap(message.toByteArray(Charsets.UTF_8)), false))
            }
        }
    }

    fun closeConnection(participantId: String) {
        dataChannels.remove(participantId)?.close()
        peerConnections.remove(participantId)?.close()
    }

    fun dispose() {
        dataChannels.values.forEach { it.close() }; dataChannels.clear()
        peerConnections.values.forEach { it.close() }; peerConnections.clear()
        peerConnectionFactory?.dispose()
        eglBase.release()
    }
}
