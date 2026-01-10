package com.yosea.skripsi.presentation.camera

import android.graphics.Bitmap
import android.util.Log
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.yosea.skripsi.data.tflite.Detection
import com.yosea.skripsi.data.tflite.ObjectDetectorHelper
import com.yosea.skripsi.presentation.ModelSession
import com.yosea.skripsi.presentation.components.OverlayView
import java.util.concurrent.Executors
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import android.os.SystemClock
@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State Deteksi
    var detectionResults by remember { mutableStateOf<List<Detection>>(emptyList()) }
    var imgHeight by remember { mutableStateOf(0) }
    var imgWidth by remember { mutableStateOf(0) }
    var inferenceTimeMs by remember { mutableLongStateOf(0L) }
    var fps by remember { mutableIntStateOf(0) }
    var lastFrameTimestamp by remember { mutableLongStateOf(0L) }

    // State Zoom
    var camera by remember { mutableStateOf<Camera?>(null) }
    var currentZoom by remember { mutableFloatStateOf(1f) }

    // 1. Ambil Helper
    val objectDetectorHelper = remember { ModelSession.detectorHelper }

    // 2. DEFINISIKAN LISTENER (Gunakan remember agar object reference tetap sama)
    val listener = remember {
        object : ObjectDetectorHelper.DetectorListener {
            override fun onError(error: String) {
                ContextCompat.getMainExecutor(context).execute {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResults(
                results: MutableList<Detection>?,
                inferenceTime: Long,
                imageHeight: Int,
                imageWidth: Int
            ) {
                detectionResults = results ?: emptyList()
                imgHeight = imageHeight
                imgWidth = imageWidth

                // 1. Simpan Inference Time (ms)
                inferenceTimeMs = inferenceTime

                // 2. Hitung FPS Real-time
                val currentTime = SystemClock.uptimeMillis()
                if (lastFrameTimestamp != 0L) {
                    val frameDiff = currentTime - lastFrameTimestamp
                    if (frameDiff > 0) {
                        fps = (1000 / frameDiff).toInt()
                    }
                }
                lastFrameTimestamp = currentTime
            }
        }
    }

    DisposableEffect(objectDetectorHelper) {
        if (objectDetectorHelper != null) {
            objectDetectorHelper.objectDetectorListener = listener
        }
        onDispose {
            if (objectDetectorHelper?.objectDetectorListener === listener) {
                objectDetectorHelper?.objectDetectorListener = null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (objectDetectorHelper != null) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val executor = ContextCompat.getMainExecutor(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    //Zoom
                    val scaleGestureDetector = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        override fun onScale(detector: ScaleGestureDetector): Boolean {
                            val zoomState = camera?.cameraInfo?.zoomState?.value ?: return false
                            val currentRatio = zoomState.zoomRatio
                            val delta = detector.scaleFactor
                            val newRatio = currentRatio * delta
                            camera?.cameraControl?.setZoomRatio(newRatio)
                            val maxZoom = zoomState.maxZoomRatio
                            val minZoom = zoomState.minZoomRatio
                            currentZoom = newRatio.coerceIn(minZoom, maxZoom)
                            return true
                        }
                    })

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                    processImageProxy(objectDetectorHelper, imageProxy)
                                }
                            }

                        try {
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalyzer
                            )
                        } catch (exc: Exception) {
                            Log.e("CAMERA", "Gagal bind kamera", exc)
                        }
                    }, executor)
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        OverlayView(
            results = detectionResults,
            imageWidth = imgWidth,
            imageHeight = imgHeight
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 40.dp, start = 16.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Text(
                text = "Inference: ${inferenceTimeMs}ms",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "FPS: $fps",
                color = Color.Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        // UI Zoom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ZoomButton("1x", currentZoom < 1.5f) {
                camera?.cameraControl?.setZoomRatio(1f); currentZoom = 1f
            }
            ZoomButton("2x", currentZoom >= 1.5f) {
                camera?.cameraControl?.setZoomRatio(2f); currentZoom = 2f
            }
        }
    }
}

@Composable
fun ZoomButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color.White else Color.Transparent)
            .border(1.dp, Color.White, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = if (isSelected) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

fun processImageProxy(helper: ObjectDetectorHelper, imageProxy: ImageProxy) {
    val bitmap = imageProxy.toBitmap()
    val rotation = imageProxy.imageInfo.rotationDegrees.toFloat()
    val matrix = android.graphics.Matrix()
    matrix.postRotate(rotation)
    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    helper.detect(rotatedBitmap, 0)
    imageProxy.close()
}