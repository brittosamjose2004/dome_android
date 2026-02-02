package com.example.webrtcstreamer.webrtc

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import okhttp3.*
import java.util.concurrent.TimeUnit

class SignalingClient(
    private val serverUrl: String,
    private val listener: Listener
) {
    
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    
    private val gson = Gson()
    private var clientId: String? = null
    private var streamId: String? = null
    
    interface Listener {
        fun onConnected(clientId: String)
        fun onStreamRegistered(streamId: String, embedUrl: String)
        fun onViewerJoined(viewerId: String)
        fun onAnswer(answer: String, senderId: String)
        fun onIceCandidate(candidate: String, senderId: String)
        fun onError(error: String)
    }
    
    suspend fun connect(url: String = serverUrl) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                println("WebSocket connected")
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listener.onError("Connection failed: ${t.message}")
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                println("WebSocket closing: $reason")
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                println("WebSocket closed: $reason")
            }
        })
    }
    
    private fun handleMessage(message: String) {
        try {
            val json = gson.fromJson(message, JsonObject::class.java)
            val type = json.get("type")?.asString
            
            when (type) {
                "connected" -> {
                    clientId = json.get("clientId")?.asString
                    clientId?.let {
                        listener.onConnected(it)
                        registerAsStreamer()
                    }
                }
                
                "registered" -> {
                    streamId = json.get("streamId")?.asString
                    val embedUrl = json.get("embedUrl")?.asString
                    if (streamId != null && embedUrl != null) {
                        listener.onStreamRegistered(streamId!!, embedUrl)
                    }
                }
                
                "viewer-joined" -> {
                    val viewerId = json.get("viewerId")?.asString
                    viewerId?.let { listener.onViewerJoined(it) }
                }
                
                "answer" -> {
                    val answer = json.get("answer")?.asString
                    val senderId = json.get("senderId")?.asString
                    if (answer != null && senderId != null) {
                        listener.onAnswer(answer, senderId)
                    }
                }
                
                "ice-candidate" -> {
                    val candidate = json.get("candidate")?.toString()
                    val senderId = json.get("senderId")?.asString
                    if (candidate != null && senderId != null) {
                        listener.onIceCandidate(candidate, senderId)
                    }
                }
                
                "error" -> {
                    val errorMsg = json.get("message")?.asString ?: "Unknown error"
                    listener.onError(errorMsg)
                }
            }
        } catch (e: Exception) {
            listener.onError("Failed to parse message: ${e.message}")
        }
    }
    
    private fun registerAsStreamer() {
        val message = JsonObject().apply {
            addProperty("type", "register-streamer")
        }
        send(message.toString())
    }
    
    fun sendOffer(offer: String, targetId: String) {
        val message = JsonObject().apply {
            addProperty("type", "offer")
            addProperty("offer", offer)
            addProperty("targetId", targetId)
        }
        send(message.toString())
    }
    
    fun sendAnswer(answer: String, targetId: String) {
        val message = JsonObject().apply {
            addProperty("type", "answer")
            addProperty("answer", answer)
            addProperty("targetId", targetId)
        }
        send(message.toString())
    }
    
    fun sendIceCandidate(candidate: String, targetId: String) {
        val message = JsonObject().apply {
            addProperty("type", "ice-candidate")
            addProperty("candidate", candidate)
            addProperty("targetId", targetId)
        }
        send(message.toString())
    }
    
    private fun send(message: String) {
        webSocket?.send(message)
    }
    
    fun disconnect() {
        if (streamId != null) {
            val message = JsonObject().apply {
                addProperty("type", "stop-stream")
            }
            send(message.toString())
        }
        webSocket?.close(1000, "Closing connection")
        webSocket = null
    }
}
