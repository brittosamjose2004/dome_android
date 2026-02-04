package com.example.webrtcstreamer.gstreamer

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoFrame
import org.webrtc.VideoSource
import java.nio.ByteBuffer

/**
 * GStreamer-inspired pipeline for Android
 * Handles: Camera → Encoder → RTP Packager → Network
 */
class GStreamerPipeline(
    private val context: Context,
    private val width: Int = 1280,
    private val height: Int = 720,
    private val fps: Int = 30,
    private val bitrate: Int = 2000000 // 2 Mbps
) {
    
    private var encoder: MediaCodec? = null
    private var isRunning = false
    
    data class EncodedFrame(
        val data: ByteArray,
        val timestamp: Long,
        val isKeyFrame: Boolean
    )
    
    var onEncodedFrame: ((EncodedFrame) -> Unit)? = null
    
    /**
     * Initialize H.264 encoder (similar to GStreamer's x264enc)
     */
    fun initializeEncoder(): Boolean {
        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
            
            // Configure encoder parameters
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2) // Keyframe every 2 seconds
            
            // Low latency settings
            format.setInteger(MediaFormat.KEY_LATENCY, 0)
            format.setInteger(MediaFormat.KEY_PRIORITY, 0) // Realtime priority
            
            // Profile settings for compatibility
            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
            format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31)
            
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Get input surface for camera frames
     */
    fun getInputSurface(): Surface? {
        return encoder?.createInputSurface()
    }
    
    /**
     * Start encoding pipeline
     */
    fun start() {
        encoder?.start()
        isRunning = true
        startEncoderThread()
    }
    
    /**
     * Process encoded output (similar to GStreamer's appsink)
     */
    private fun startEncoderThread() {
        Thread {
            val bufferInfo = MediaCodec.BufferInfo()
            
            while (isRunning) {
                try {
                    val outputBufferId = encoder?.dequeueOutputBuffer(bufferInfo, 10000) ?: continue
                    
                    if (outputBufferId >= 0) {
                        val outputBuffer = encoder?.getOutputBuffer(outputBufferId)
                        
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            // Extract encoded data
                            val data = ByteArray(bufferInfo.size)
                            outputBuffer.get(data)
                            
                            val isKeyFrame = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
                            
                            // Send to callback (like GStreamer's appsink)
                            onEncodedFrame?.invoke(
                                EncodedFrame(
                                    data = data,
                                    timestamp = bufferInfo.presentationTimeUs,
                                    isKeyFrame = isKeyFrame
                                )
                            )
                        }
                        
                        encoder?.releaseOutputBuffer(outputBufferId, false)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }
    
    /**
     * Stop the pipeline
     */
    fun stop() {
        isRunning = false
        try {
            encoder?.stop()
            encoder?.release()
            encoder = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Create RTP packets from encoded frames (GStreamer's rtph264pay equivalent)
     */
    fun createRtpPackets(frame: EncodedFrame): List<ByteArray> {
        val packets = mutableListOf<ByteArray>()
        val maxPacketSize = 1400 // MTU size
        
        var offset = 0
        while (offset < frame.data.size) {
            val remainingSize = frame.data.size - offset
            val packetSize = minOf(remainingSize, maxPacketSize)
            
            val packet = ByteArray(packetSize)
            System.arraycopy(frame.data, offset, packet, 0, packetSize)
            packets.add(packet)
            
            offset += packetSize
        }
        
        return packets
    }
}

/**
 * Pipeline builder (GStreamer-style API)
 */
class PipelineBuilder {
    private var width = 1280
    private var height = 720
    private var fps = 30
    private var bitrate = 2000000
    
    fun setResolution(w: Int, h: Int) = apply {
        width = w
        height = h
    }
    
    fun setFrameRate(rate: Int) = apply {
        fps = rate
    }
    
    fun setBitrate(rate: Int) = apply {
        bitrate = rate
    }
    
    fun build(context: Context): GStreamerPipeline {
        return GStreamerPipeline(context, width, height, fps, bitrate)
    }
}
