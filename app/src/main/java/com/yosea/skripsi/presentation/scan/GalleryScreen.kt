package com.yosea.skripsi.presentation.scan

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yosea.skripsi.data.tflite.Detection
import com.yosea.skripsi.data.tflite.ObjectDetectorHelper
import com.yosea.skripsi.presentation.components.OverlayView

@Composable
fun GalleryScreen() {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detectionResults by remember { mutableStateOf<List<Detection>>(emptyList()) }

    // Helper Detektor
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
                    detectionResults = results ?: emptyList()
                }
            }
        )
    }

    // 1. LAUNCHER GALERI (Existing)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fixedBitmap = BitmapUtils.getBitmapFromUri(context, it)
            if (fixedBitmap != null) {
                val argbBitmap = fixedBitmap.copy(Bitmap.Config.ARGB_8888, true)
                imageBitmap = argbBitmap
                objectDetectorHelper.detect(argbBitmap, 0)
            } else {
                Toast.makeText(context, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 2. LAUNCHER KAMERA (New - TakePicturePreview)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            // Bitmap dari kamera biasanya thumbnail, tapi cukup untuk preview cepat
            // Pastikan formatnya ARGB_8888 untuk TFLite
            val argbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            imageBitmap = argbBitmap
            objectDetectorHelper.detect(argbBitmap, 0)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // --- BAGIAN TENGAH: PREVIEW GAMBAR ---
        if (imageBitmap != null) {
            // Tampilkan Gambar
            Image(
                bitmap = imageBitmap!!.asImageBitmap(),
                contentDescription = "Selected Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Tampilkan Overlay
            OverlayView(
                results = detectionResults,
                imageWidth = imageBitmap!!.width,
                imageHeight = imageBitmap!!.height,
                isFit = true
            )

        } else {
            // Tampilan Kosong (Placeholder)
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Belum ada gambar", color = Color.Gray)
            }
        }

        // --- BAGIAN BAWAH: TOMBOL KONTROL ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f)) // Background transparan biar teks jelas
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // A. TOMBOL KAMERA (Utama)
            Button(
                onClick = { cameraLauncher.launch(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ambil Foto Langsung", fontWeight = FontWeight.Bold)
            }

            // B. TOMBOL GALERI (Secondary / Icon style)
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pilih dari Galeri")
            }
        }
    }
}