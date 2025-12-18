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
import com.yosea.skripsi.presentation.components.OverlayView
import java.util.concurrent.Executors

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State Deteksi
    var detectionResults by remember { mutableStateOf<List<Detection>>(emptyList()) }
    var imgHeight by remember { mutableStateOf(0) }
    var imgWidth by remember { mutableStateOf(0) }

    // --- STATE BARU UNTUK ZOOM ---
    var camera by remember { mutableStateOf<Camera?>(null) }
    var currentZoom by remember { mutableFloatStateOf(1f) } // Default 1x

    // Inisialisasi Helper TFLite
    val objectDetectorHelper = remember {
        ObjectDetectorHelper(
            context = context,
            threshold = 0.5f,
            currentDelegate = ObjectDetectorHelper.DELEGATE_GPU,
            objectDetectorListener = object : ObjectDetectorHelper.DetectorListener {
                override fun onError(error: String) {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
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
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Preview Kamera
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = ContextCompat.getMainExecutor(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                // --- LOGIC PINCH TO ZOOM (Cubit Layar) ---
                // Ini ditambahkan agar zoom terasa natural seperti kamera asli
                val scaleGestureDetector = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        val zoomState = camera?.cameraInfo?.zoomState?.value ?: return false
                        val currentRatio = zoomState.zoomRatio
                        val delta = detector.scaleFactor
                        val newRatio = currentRatio * delta

                        camera?.cameraControl?.setZoomRatio(newRatio)
                        currentZoom = newRatio // Update UI
                        return true
                    }
                })

                previewView.setOnTouchListener { _, event ->
                    scaleGestureDetector.onTouchEvent(event)
                    return@setOnTouchListener true
                }
                // -----------------------------------------

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // Image Analysis (Sesuai kodemu sebelumnya)
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

                        // --- SIMPAN INSTANCE KAMERA UNTUK KONTROL ZOOM ---
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

        // 2. Overlay Kotak Deteksi
        OverlayView(
            results = detectionResults,
            imageWidth = imgWidth,
            imageHeight = imgHeight
        )

        // 3. --- UI TOMBOL ZOOM ---
        // Kita gunakan Alignment.BottomCenter dengan Padding agar tidak tertutup Nav Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // Padding bottom 100.dp biasanya cukup aman untuk mengangkat tombol diatas Nav Bar standar
                .padding(bottom = 100.dp)
                .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tombol 1x
            ZoomButton(
                text = "1x",
                isSelected = currentZoom < 1.5f, // Highlight jika zoom dekat 1x
                onClick = {
                    camera?.cameraControl?.setZoomRatio(1f)
                    currentZoom = 1f
                }
            )

            // Tombol 2x
            ZoomButton(
                text = "2x",
                isSelected = currentZoom >= 1.5f, // Highlight jika zoom >= 1.5x
                onClick = {
                    camera?.cameraControl?.setZoomRatio(2f)
                    currentZoom = 2f
                }
            )
        }
    }
}

// --- KOMPONEN UI TOMBOL ZOOM ---
@Composable
fun ZoomButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp) // Ukuran lingkaran tombol
            .clip(CircleShape)
            .background(if (isSelected) Color.White else Color.Transparent)
            .border(1.dp, Color.White, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// --- HELPER TETAP SAMA PERSIS (PRESISI) ---
fun processImageProxy(helper: ObjectDetectorHelper, imageProxy: ImageProxy) {
    // Hapus pengecekan null karena toBitmap() dijamin ada isinya
    val bitmap = imageProxy.toBitmap()

    // Langsung proses rotasi (Manual Bitmap Creation - Presisi Tinggi)
    val rotation = imageProxy.imageInfo.rotationDegrees.toFloat()
    val matrix = android.graphics.Matrix()
    matrix.postRotate(rotation)

    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

    helper.detect(rotatedBitmap, 0)

    imageProxy.close()
}