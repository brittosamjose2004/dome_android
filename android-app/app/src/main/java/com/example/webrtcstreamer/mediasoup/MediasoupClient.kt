package com.example.webrtcstreamer.mediasoup

import com.example.webrtcstreamer.gstreamer.GStreamerPipeline
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URISyntaxException

class MediasoupClient(
    private val serverUrl: String,
    private val pipeline: GStreamerPipeline,
    private val listener: Listener
) {
    
    interface Listener {
        fun onConnected(streamId: String)
        fun onDisconnected()
        fun onError(error: String)
        fun onStreamPublished(streamId: String)
    }
    
    private var socket: Socket? = null
    private var streamId: String? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    fun connect() {
        try {
            val options = IO.Options().apply {
                transports = arrayOf("websocket")
                reconnection = true
                reconnectionDelay = 1000
                reconnectionAttempts = 5
            }
            
            socket = IO.socket(serverUrl, options)
            
            socket?.apply {
                on(Socket.EVENT_CONNECT) {
                    scope.launch {
                        listener.onConnected("connected")
                        // Request to create producer transport
                        emit("createProducerTransport")
                    }
                }
                
                on(Socket.EVENT_DISCONNECT) {
                    listener.onDisconnected()
                }
                
                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    val error = args.getOrNull(0)?.toString() ?: "Connection error"
                    listener.onError(error)
                }
                
                on("producerTransportCreated") { args ->
                    scope.launch {
                        handleProducerTransportCreated(args)
                    }
                }
                
                on("producerCreated") { args ->
                    val data = args[0] as JSONObject
                    val producerId = data.getString("id")
                    streamId = producerId
                    listener.onStreamPublished(producerId)
                }
                
                on("error") { args ->
                    val error = args.getOrNull(0)?.toString() ?: "Unknown error"
                    listener.onError(error)
                }
                
                connect()
            }
        } catch (e: URISyntaxException) {
            listener.onError("Invalid server URL: ${e.message}")
        } catch (e: Exception) {
            listener.onError("Connection failed: ${e.message}")
        }
    }
    
    private fun handleProducerTransportCreated(args: Array<Any>) {
        try {
            val data = args[0] as JSONObject
            val transportId = data.getString("id")
            val iceParameters = data.getJSONObject("iceParameters")
            val iceCandidates = data.getJSONArray("iceCandidates")
            val dtlsParameters = data.getJSONObject("dtlsParameters")
            
            // In a full implementation, we would create a WebRTC transport here
            // For GStreamer pipeline, we'll use RTMP or WebSocket streaming instead
            
            // Start the GStreamer pipeline
            scope.launch {
                startStreaming(transportId)
            }
            
        } catch (e: Exception) {
            listener.onError("Failed to handle transport: ${e.message}")
        }
    }
    
    private fun startStreaming(transportId: String) {
        try {
            // Start pipeline with encoder
            pipeline.start()
            
            // Set frame callback to send encoded frames
            pipeline.onEncodedFrame = { frame ->
                // Send frame to mediasoup via WebSocket or RTMP
                sendFrame(frame)
            }
            
            // Notify server to create producer
            val params = JSONObject().apply {
                put("transportId", transportId)
                put("kind", "video")
                put("rtpParameters", JSONObject().apply {
                    put("codecs", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("mimeType", "video/H264")
                            put("clockRate", 90000)
                            put("payloadType", 96)
                        })
                    })
                })
            }
            
            socket?.emit("produce", params)
            
        } catch (e: Exception) {
            listener.onError("Failed to start streaming: ${e.message}")
        }
    }
    
    private fun sendFrame(frame: GStreamerPipeline.EncodedFrame) {
        // Send RTP packets via WebSocket
        scope.launch {
            try {
                // Convert frame to RTP packets
                val packets = pipeline.createRtpPackets(frame)
                
                // Send each packet
                packets.forEach { packet ->
                    socket?.emit("rtpPacket", packet)
                }
            } catch (e: Exception) {
                listener.onError("Failed to send frame: ${e.message}")
            }
        }
    }
    
    fun disconnect() {
        try {
            pipeline.stop()
            socket?.disconnect()
            socket?.close()
            socket = null
            scope.cancel()
        } catch (e: Exception) {
            listener.onError("Disconnect error: ${e.message}")
        }
    }
    
    fun getStreamId(): String? = streamId
}
