package com.example.webrtcstreamer.webrtc

import android.app.Application
import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WebRTCClient(
    private val application: Application,
    private val observer: PeerConnectionObserver
) {
    
    val eglBase: EglBase = EglBase.create()
    private val peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    
    var onIceCandidate: ((String) -> Unit)? = null
    
    init {
        initPeerConnectionFactory(application)
        peerConnectionFactory = createPeerConnectionFactory()
    }
    
    private fun initPeerConnectionFactory(context: Context) {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }
    
    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    eglBase.eglBaseContext,
                    true,
                    true
                )
            )
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = false
                disableNetworkMonitor = false
            })
            .createPeerConnectionFactory()
    }
    
    fun startLocalVideoCapture(localView: SurfaceViewRenderer) {
        val surfaceTextureHelper = SurfaceTextureHelper.create(
            Thread.currentThread().name,
            eglBase.eglBaseContext
        )
        
        videoCapturer = createCameraCapturer()
        
        val videoSource = peerConnectionFactory.createVideoSource(videoCapturer!!.isScreencast)
        videoCapturer!!.initialize(
            surfaceTextureHelper,
            application,
            videoSource.capturerObserver
        )
        
        videoCapturer!!.startCapture(1280, 720, 30)
        
        localVideoTrack = peerConnectionFactory.createVideoTrack("local_video_track", videoSource)
        localVideoTrack?.addSink(localView)
        
        // Create audio track
        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("local_audio_track", audioSource)
        
        observer.onStreamReady()
    }
    
    private fun createCameraCapturer(): CameraVideoCapturer {
        val enumerator = Camera2Enumerator(application)
        
        val deviceNames = enumerator.deviceNames
        
        // Try to find front camera first
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) return capturer
            }
        }
        
        // If no front camera, try back camera
        for (deviceName in deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) return capturer
            }
        }
        
        throw RuntimeException("No camera found")
    }
    
    fun createPeerConnection(): PeerConnection? {
        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
            )
        ).apply {
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType = PeerConnection.KeyType.ECDSA
        }
        
        peerConnection = peerConnectionFactory.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let {
                        val candidateJson = """
                            {
                                "candidate": "${it.sdp}",
                                "sdpMid": "${it.sdpMid}",
                                "sdpMLineIndex": ${it.sdpMLineIndex}
                            }
                        """.trimIndent()
                        onIceCandidate?.invoke(candidateJson)
                    }
                }
                
                override fun onDataChannel(channel: DataChannel?) {}
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onAddStream(stream: MediaStream?) {}
                override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
            }
        )
        
        // Add local tracks to peer connection
        localVideoTrack?.let { peerConnection?.addTrack(it, listOf("local_stream")) }
        localAudioTrack?.let { peerConnection?.addTrack(it, listOf("local_stream")) }
        
        return peerConnection
    }
    
    suspend fun createOffer(): String {
        val pc = createPeerConnection() ?: throw RuntimeException("Failed to create peer connection")
        
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        
        return suspendCancellableCoroutine { continuation ->
            pc.createOffer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    pc.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            sdp?.let {
                                continuation.resume(it.description)
                            }
                        }
                        override fun onSetFailure(error: String?) {
                            continuation.resumeWithException(RuntimeException(error))
                        }
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                    }, sdp)
                }
                
                override fun onCreateFailure(error: String?) {
                    continuation.resumeWithException(RuntimeException(error))
                }
                
                override fun onSetSuccess() {}
                override fun onSetFailure(error: String?) {}
            }, constraints)
        }
    }
    
    suspend fun setRemoteAnswer(answerSdp: String) {
        val sdp = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
        
        return suspendCancellableCoroutine { continuation ->
            peerConnection?.setRemoteDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    continuation.resume(Unit)
                }
                
                override fun onSetFailure(error: String?) {
                    continuation.resumeWithException(RuntimeException(error))
                }
                
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onCreateFailure(p0: String?) {}
            }, sdp)
        }
    }
    
    suspend fun addRemoteIceCandidate(candidateJson: String) {
        try {
            // Parse JSON and create IceCandidate
            // This is a simplified version - you might want to use a proper JSON parser
            val candidate = IceCandidate("", 0, candidateJson)
            peerConnection?.addIceCandidate(candidate)
        } catch (e: Exception) {
            observer.onStreamError("Failed to add ICE candidate: ${e.message}")
        }
    }
    
    fun close() {
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        peerConnection?.close()
        peerConnectionFactory.dispose()
        eglBase.release()
    }
    
    open class PeerConnectionObserver {
        open fun onStreamReady() {}
        open fun onStreamError(error: String) {}
    }
}
