package com.yosea.skripsi.presentation.camera

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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

    // State untuk menyimpan hasil deteksi agar UI terupdate otomatis
    var detectionResults by remember { mutableStateOf<List<Detection>>(emptyList()) }
    var imgHeight by remember { mutableStateOf(0) }
    var imgWidth by remember { mutableStateOf(0) }

    // Inisialisasi Helper TFLite
    val objectDetectorHelper = remember {
        ObjectDetectorHelper(
            context = context,
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
                    // Update State UI
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
                        cameraProvider.bindToLifecycle(
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

        // 2. Overlay Kotak Deteksi (Modular)
        OverlayView(
            results = detectionResults,
            imageWidth = imgWidth,
            imageHeight = imgHeight
        )
    }
}



// Helper function untuk convert ImageProxy ke Bitmap & Rotasi
fun processImageProxy(helper: ObjectDetectorHelper, imageProxy: ImageProxy) {
    // Hapus pengecekan null karena toBitmap() dijamin ada isinya
    val bitmap = imageProxy.toBitmap()

    // Langsung proses rotasi
    val rotation = imageProxy.imageInfo.rotationDegrees.toFloat()
    val matrix = android.graphics.Matrix()
    matrix.postRotate(rotation)

    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

    helper.detect(rotatedBitmap, 0)

    imageProxy.close()
}