package com.example.webrtcstreamer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.webrtcstreamer.databinding.ActivityMainBinding
import com.example.webrtcstreamer.gstreamer.GStreamerPipeline
import com.example.webrtcstreamer.mediasoup.MediasoupClient

class StreamActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var pipeline: GStreamerPipeline
    private lateinit var mediasoupClient: MediasoupClient
    
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    
    private var isStreaming = false
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        
        private const val MEDIASOUP_SERVER = "https://zany-adventure-pj7j4xpp49653r6x7-3002.app.github.dev"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        if (checkPermissions()) {
            initializePipeline()
        } else {
            requestPermissions()
        }
        
        setupUI()
    }
    
    private fun checkPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializePipeline()
            } else {
                Toast.makeText(this, "Permissions required for streaming", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    private fun initializePipeline() {
        // Initialize GStreamer pipeline with 720p, 30fps, 2Mbps
        pipeline = GStreamerPipeline(
            width = 1280,
            height = 720,
            fps = 30,
            bitrate = 2_000_000
        )
        pipeline.initializeEncoder()
        
        // Initialize mediasoup client
        mediasoupClient = MediasoupClient(
            serverUrl = MEDIASOUP_SERVER,
            pipeline = pipeline,
            listener = object : MediasoupClient.Listener {
                override fun onConnected(streamId: String) {
                    runOnUiThread {
                        updateStatus("Connected to mediasoup")
                    }
                }
                
                override fun onDisconnected() {
                    runOnUiThread {
                        updateStatus("Disconnected")
                        isStreaming = false
                        binding.startButton.text = "Start Streaming"
                    }
                }
                
                override fun onError(error: String) {
                    runOnUiThread {
                        updateStatus("Error: $error")
                        Toast.makeText(this@StreamActivity, error, Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onStreamPublished(streamId: String) {
                    runOnUiThread {
                        updateStatus("Streaming")
                        val embedUrl = "$MEDIASOUP_SERVER/view/$streamId"
                        binding.streamIdText.text = "Stream ID: $streamId"
                        binding.embedUrlText.text = "View URL: $embedUrl"
                    }
                }
            }
        )
        
        updateStatus("Ready")
    }
    
    private fun setupUI() {
        binding.startButton.setOnClickListener {
            if (!isStreaming) {
                startStreaming()
            } else {
                stopStreaming()
            }
        }
        
        binding.copyUrlButton.setOnClickListener {
            val embedUrl = binding.embedUrlText.text.toString()
            if (embedUrl.contains("http")) {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("View URL", embedUrl)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Hide server URL input since we're using hardcoded mediasoup server
        binding.serverUrlInput.setText(MEDIASOUP_SERVER)
        binding.serverUrlInput.isEnabled = false
    }
    
    private fun startStreaming() {
        startBackgroundThread()
        
        // Connect to mediasoup server
        mediasoupClient.connect()
        
        // Setup camera
        setupCamera()
        
        isStreaming = true
        binding.startButton.text = "Stop Streaming"
        updateStatus("Starting...")
    }
    
    private fun stopStreaming() {
        mediasoupClient.disconnect()
        closeCamera()
        stopBackgroundThread()
        
        isStreaming = false
        binding.startButton.text = "Start Streaming"
        binding.streamIdText.text = ""
        binding.embedUrlText.text = ""
        updateStatus("Ready")
    }
    
    private fun setupCamera() {
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        try {
            // Get back camera
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: return
            
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession()
                }
                
                override fun onDisconnected(camera: CameraDevice) {
                    cameraDevice?.close()
                    cameraDevice = null
                }
                
                override fun onError(camera: CameraDevice, error: Int) {
                    cameraDevice?.close()
                    cameraDevice = null
                    updateStatus("Camera error: $error")
                }
            }, backgroundHandler)
            
        } catch (e: Exception) {
            updateStatus("Failed to open camera: ${e.message}")
        }
    }
    
    private fun createCaptureSession() {
        try {
            // Get encoder surface from pipeline
            val surface = pipeline.getInputSurface()
            if (surface == null) {
                updateStatus("Failed to get encoder surface")
                return
            }
            
            // Create capture request
            val captureRequest = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)?.apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            }
            
            // Create capture session
            cameraDevice?.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        
                        try {
                            // Start repeating request
                            captureRequest?.let {
                                session.setRepeatingRequest(it.build(), null, backgroundHandler)
                            }
                            
                            updateStatus("Camera started")
                        } catch (e: Exception) {
                            updateStatus("Failed to start capture: ${e.message}")
                        }
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        updateStatus("Failed to configure camera")
                    }
                },
                backgroundHandler
            )
            
        } catch (e: Exception) {
            updateStatus("Failed to create capture session: ${e.message}")
        }
    }
    
    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
    }
    
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }
    
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
    
    private fun updateStatus(status: String) {
        binding.statusText.text = "Status: $status"
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isStreaming) {
            stopStreaming()
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (isStreaming) {
            closeCamera()
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (isStreaming) {
            startBackgroundThread()
            setupCamera()
        }
    }
}
