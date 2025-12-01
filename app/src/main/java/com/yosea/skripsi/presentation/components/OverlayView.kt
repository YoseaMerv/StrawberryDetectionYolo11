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
import kotlin.math.max
import kotlin.math.min

@Composable
fun OverlayView(
    results: List<Detection>,
    imageWidth: Int,
    imageHeight: Int,
    isFit: Boolean = false // Default false (Kamera), True (Galeri)
) {
    val classColors = listOf(Color.Green, Color.Red, Color(0xFFFFA500))
    val defaultColor = Color.White

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // LOGIC SCALE
        val scale = if (isFit) {
            // Mode Galeri (Fit) -> Gunakan min scale agar gambar muat dalam layar
            min(canvasWidth / imageWidth, canvasHeight / imageHeight)
        } else {
            // Mode Kamera (Fill) -> Gunakan max scale agar penuh layar
            max(canvasWidth / imageWidth, canvasHeight / imageHeight)
        }

        // LOGIC OFFSET (Center)
        val offsetX = (canvasWidth - imageWidth * scale) / 2
        val offsetY = (canvasHeight - imageHeight * scale) / 2

        val textPaint = Paint().apply {
            textSize = 40f
            typeface = Typeface.DEFAULT_BOLD
            style = Paint.Style.FILL
        }

        for (result in results) {
            val box = result.boundingBox

            val left = box.left * scale + offsetX
            val top = box.top * scale + offsetY
            val width = box.width() * scale
            val height = box.height() * scale

            val category = result.categories.firstOrNull()
            val index = category?.index ?: 0
            val label = category?.label ?: "Unknown"
            val score = category?.score ?: 0f
            val boxColor = if (index in classColors.indices) classColors[index] else defaultColor

            drawRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(width, height),
                style = Stroke(width = 8f)
            )

            drawContext.canvas.nativeCanvas.apply {
                textPaint.color = boxColor.toArgb()
                val textWidth = textPaint.measureText(label) + 130f
                val textHeight = 60f
                val textTop = if (top - textHeight < 0) top else top - textHeight
                val textBottom = if (top - textHeight < 0) top + textHeight else top

                drawRect(left, textTop, left + textWidth, textBottom, textPaint)
                textPaint.color = android.graphics.Color.WHITE
                drawText("$label ${(score * 100).toInt()}%", left + 15f, textBottom - 15f, textPaint)
            }
        }
    }
}