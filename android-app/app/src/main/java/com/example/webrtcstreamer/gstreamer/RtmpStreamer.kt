package com.example.webrtcstreamer.gstreamer

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer

/**
 * RTMP Streamer (GStreamer rtmpsink equivalent)
 * Sends H.264 encoded video to RTMP server
 */
class RtmpStreamer(
    private val rtmpUrl: String
) {
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false
    
    companion object {
        private const val TAG = "RtmpStreamer"
    }
    
    /**
     * Connect to RTMP server
     */
    fun connect(): Boolean {
        return try {
            // Parse RTMP URL: rtmp://server:port/app/stream
            val url = rtmpUrl.replace("rtmp://", "")
            val parts = url.split(":")
            val host = parts[0]
            val port = if (parts.size > 1) parts[1].split("/")[0].toInt() else 1935
            
            socket = Socket(host, port)
            outputStream = socket?.getOutputStream()
            isConnected = true
            
            Log.d(TAG, "Connected to RTMP server: $host:$port")
            sendRtmpHandshake()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to RTMP server", e)
            false
        }
    }
    
    /**
     * RTMP handshake (simplified version)
     */
    private fun sendRtmpHandshake() {
        try {
            // C0 + C1
            val c0 = ByteArray(1) { 0x03 } // RTMP version 3
            val c1 = ByteArray(1536) { 0x00 }
            
            // Timestamp
            ByteBuffer.wrap(c1).putInt(0)
            
            outputStream?.write(c0)
            outputStream?.write(c1)
            outputStream?.flush()
            
            // Read S0 + S1 + S2 (simplified - skip validation)
            socket?.getInputStream()?.skip(1 + 1536 + 1536)
            
            // Send C2
            outputStream?.write(c1)
            outputStream?.flush()
            
            Log.d(TAG, "RTMP handshake completed")
        } catch (e: Exception) {
            Log.e(TAG, "RTMP handshake failed", e)
        }
    }
    
    /**
     * Send encoded frame
     */
    fun sendFrame(frame: GStreamerPipeline.EncodedFrame) {
        if (!isConnected) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create FLV video tag
                val flvTag = createFlvVideoTag(frame)
                outputStream?.write(flvTag)
                outputStream?.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send frame", e)
            }
        }
    }
    
    /**
     * Create FLV video tag from H.264 frame
     */
    private fun createFlvVideoTag(frame: GStreamerPipeline.EncodedFrame): ByteArray {
        val buffer = ByteBuffer.allocate(frame.data.size + 16)
        
        // FLV tag type (0x09 = video)
        buffer.put(0x09.toByte())
        
        // Data size (24-bit)
        val dataSize = frame.data.size + 5
        buffer.put((dataSize shr 16 and 0xFF).toByte())
        buffer.put((dataSize shr 8 and 0xFF).toByte())
        buffer.put((dataSize and 0xFF).toByte())
        
        // Timestamp (24-bit) + extended timestamp (8-bit)
        val timestamp = (frame.timestamp / 1000).toInt()
        buffer.put((timestamp shr 16 and 0xFF).toByte())
        buffer.put((timestamp shr 8 and 0xFF).toByte())
        buffer.put((timestamp and 0xFF).toByte())
        buffer.put((timestamp shr 24 and 0xFF).toByte())
        
        // Stream ID (24-bit, always 0)
        buffer.put(0x00)
        buffer.put(0x00)
        buffer.put(0x00)
        
        // Video data
        // Frame type (4 bits) + codec ID (4 bits)
        val frameType = if (frame.isKeyFrame) 0x10 else 0x20 // 1=keyframe, 2=inter
        val codecId = 0x07 // AVC/H.264
        buffer.put((frameType or codecId).toByte())
        
        // AVC packet type (0x01 = NALU)
        buffer.put(0x01)
        
        // Composition time (24-bit, 0 for this simple implementation)
        buffer.put(0x00)
        buffer.put(0x00)
        buffer.put(0x00)
        
        // NAL unit data
        buffer.put(frame.data)
        
        // Previous tag size (32-bit)
        val tagSize = frame.data.size + 16
        buffer.putInt(tagSize)
        
        return buffer.array()
    }
    
    /**
     * Disconnect from server
     */
    fun disconnect() {
        try {
            outputStream?.close()
            socket?.close()
            isConnected = false
            Log.d(TAG, "Disconnected from RTMP server")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }
    }
}

/**
 * Alternative: WebSocket streamer for mediasoup
 */
class WebSocketStreamer(
    private val wsUrl: String,
    private val streamId: String
) {
    // Implementation for sending to mediasoup via WebSocket
    // This allows integration with the mediasoup SFU we created
    
    fun connect() {
        // Connect to mediasoup server via Socket.IO
    }
    
    fun sendFrame(frame: GStreamerPipeline.EncodedFrame) {
        // Send encoded frame to mediasoup
    }
}
