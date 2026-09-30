package com.example.studentattendance

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    // Networking
    private lateinit var nsdManager: NsdManager
    private val serviceType = "_attendance._tcp."
    private var serverIp: String? = null
    private var serverPort: Int = 8080 // Now dynamic
    private val okHttpClient = OkHttpClient()

    // Camera & AI
    private lateinit var cameraExecutor: ExecutorService
    private var hasMarkedAttendance = false

    // UI
    private lateinit var viewFinder: PreviewView
    private lateinit var instructionText: TextView

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.viewFinder)
        instructionText = findViewById(R.id.instructionText)
        cameraExecutor = Executors.newSingleThreadExecutor()
        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
                startDiscovery()
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show()
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
            startDiscovery()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(imageProxy: ImageProxy) {
        if (hasMarkedAttendance) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    for (face in faces) {
                        val leftEye = face.leftEyeOpenProbability ?: 1.0f
                        val rightEye = face.rightEyeOpenProbability ?: 1.0f

                        if (leftEye < 0.2f && rightEye < 0.2f) {
                            if (serverIp != null) {
                                hasMarkedAttendance = true
                                runOnUiThread { instructionText.text = "Blink detected! Submitting..." }
                                submitAttendance()
                            } else {
                                runOnUiThread { instructionText.text = "Blink detected, waiting for network..." }
                            }
                        }
                    }
                }
                .addOnFailureListener { e -> Log.e("MLKit", "Face detection failed", e) }
                .addOnCompleteListener { imageProxy.close() }
        }
    }

    private fun submitAttendance() {
        // Now dynamically injecting both IP and Port
        val url = "http://$serverIp:$serverPort/api/attendance"

        val prefs = getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE)
        val studentName = prefs.getString("studentName", "Unknown Student")
        val enrollmentId = prefs.getString("enrollmentId", "Unknown ID")

        val jsonPayload = JSONObject().apply {
            put("studentName", studentName)
            put("enrollmentId", enrollmentId)
            put("timestamp", System.currentTimeMillis())
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                hasMarkedAttendance = false
                runOnUiThread { instructionText.text = "Network Error. Please blink to retry." }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        instructionText.text = "Attendance Marked Successfully!"
                        instructionText.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
                        triggerHapticFeedback()
                    }
                } else if (response.code == 409) {
                    // Handle the duplicate block from the server
                    runOnUiThread {
                        instructionText.text = "Already marked present today!"
                        instructionText.setBackgroundColor(android.graphics.Color.parseColor("#FF9800")) // Orange
                        triggerHapticFeedback()
                    }
                } else {
                    hasMarkedAttendance = false
                    runOnUiThread { instructionText.text = "Server Error: ${response.code}" }
                }
            }
        })
    }

    private fun triggerHapticFeedback() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(300)
        }
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {}
        override fun onServiceFound(service: NsdServiceInfo) {
            if (service.serviceType.contains("_attendance._tcp")) {
                nsdManager.resolveService(service, resolveListener)
            }
        }
        override fun onServiceLost(service: NsdServiceInfo) {}
        override fun onDiscoveryStopped(serviceType: String) {}
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            nsdManager.stopServiceDiscovery(this)
        }
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            nsdManager.stopServiceDiscovery(this)
        }
    }

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            serverIp = serviceInfo.host.hostAddress
            serverPort = serviceInfo.port // Capturing dynamic port

            runOnUiThread {
                if (!hasMarkedAttendance) {
                    instructionText.text = "Connected! Please blink to mark attendance."
                }
            }
        }
    }

    private fun startDiscovery() {
        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        nsdManager.stopServiceDiscovery(discoveryListener)
        cameraExecutor.shutdown()
    }
}