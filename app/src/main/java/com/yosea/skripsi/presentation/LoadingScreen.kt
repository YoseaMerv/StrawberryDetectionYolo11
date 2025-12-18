package com.yosea.skripsi.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yosea.skripsi.R
import com.yosea.skripsi.data.tflite.ObjectDetectorHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoadingScreen(
    onLoadingFinished: () -> Unit
) {
    val context = LocalContext.current
    val currentOnLoadingFinished by rememberUpdatedState(onLoadingFinished)

    LaunchedEffect(Unit) {
        // Pindah ke background thread untuk inisialisasi berat
        withContext(Dispatchers.IO) {
            val jobInit = launch {
                // Cek apakah sudah ada, kalau belum buat baru
                if (ModelSession.detectorHelper == null) {
                    ModelSession.detectorHelper = ObjectDetectorHelper(
                        context = context,
                        threshold = 0.5f,
                        currentDelegate = ObjectDetectorHelper.DELEGATE_GPU
                    )
                }
            }

            val jobTimer = launch {
                delay(2000) // Tahan minimal 2 detik untuk animasi
            }

            // Tunggu keduanya selesai
            jobInit.join()
            jobTimer.join()
        }

        currentOnLoadingFinished()
    }

    // --- UI ANIMASI ---
    val infiniteTransition = rememberInfiniteTransition(label = "loading_transition")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_angle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_loading),
                contentDescription = "Loading",
                modifier = Modifier
                    .size(100.dp)
                    .rotate(angle)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Menyiapkan AI...",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

// --- SESSION OBJECT DISIMPAN DI SINI ---
object ModelSession {
    var detectorHelper: ObjectDetectorHelper? = null
}