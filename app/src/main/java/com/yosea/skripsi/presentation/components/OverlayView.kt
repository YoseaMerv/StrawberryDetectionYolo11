package com.yosea.skripsi.presentation.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.yosea.skripsi.data.tflite.Detection

@Composable
fun OverlayView(
    results: List<Detection>,
    imageWidth: Int,
    imageHeight: Int
) {
    // --- 1. DAFTAR WARNA ---
    // Sesuaikan urutan ini dengan urutan label di ObjectDetectorHelper.kt
    // Index 0: Healthy, Index 1: Leaf Spot, Index 2: Powdery Mildew
    val classColors = listOf(
        Color.Green,      // Index 0: Healthy (Sehat -> Hijau)
        Color.Red,        // Index 1: Leaf Spot (Penyakit -> Merah)
        Color(0xFFFFA500) // Index 2: Powdery Mildew (Penyakit -> Orange)
    )

    // Warna cadangan jika index di luar jangkauan
    val defaultColor = Color.White

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Hitung skala agar kotak pas di layar HP
        val scaleX = size.width / imageWidth.coerceAtLeast(1)
        val scaleY = size.height / imageHeight.coerceAtLeast(1)

        // Setup Paint untuk menggambar Teks (Native Android)
        val textPaint = Paint().apply {
            textSize = 40f // Ukuran font
            typeface = Typeface.DEFAULT_BOLD
            style = Paint.Style.FILL
        }

        for (result in results) {
            val box = result.boundingBox
            val left = box.left * scaleX
            val top = box.top * scaleY
            val width = box.width() * scaleX
            val height = box.height() * scaleY

            // Ambil data kategori
            val category = result.categories.firstOrNull()
            val index = category?.index ?: 0
            val label = category?.label ?: "Unknown"
            val score = category?.score ?: 0f

            // --- 2. PILIH WARNA BERDASARKAN INDEX ---
            val boxColor = if (index in classColors.indices) {
                classColors[index]
            } else {
                defaultColor
            }

            // --- 3. GAMBAR KOTAK ---
            drawRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(width, height),
                style = Stroke(width = 8f) // Ketebalan garis
            )

            // --- 4. GAMBAR LABEL TEKS ---
            drawContext.canvas.nativeCanvas.apply {
                // A. Gambar Background Teks (Kotak kecil di atas)
                textPaint.color = boxColor.toArgb()
                val textWidth = textPaint.measureText(label) + 120f // Lebar kotak menyesuaikan teks
                val textHeight = 60f

                // Pastikan teks tidak keluar layar bagian atas
                val textTop = if (top - textHeight < 0) top else top - textHeight
                val textBottom = if (top - textHeight < 0) top + textHeight else top

                drawRect(
                    left,
                    textTop,
                    left + textWidth,
                    textBottom,
                    textPaint
                )

                // B. Gambar Tulisan (Nama + Persen)
                textPaint.color = android.graphics.Color.WHITE // Warna tulisan Putih
                val labelString = "$label ${(score * 100).toInt()}%"

                drawText(
                    labelString,
                    left + 10f,
                    textBottom - 15f,
                    textPaint
                )
            }
        }
    }
}