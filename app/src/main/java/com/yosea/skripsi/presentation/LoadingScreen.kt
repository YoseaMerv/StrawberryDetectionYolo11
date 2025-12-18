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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yosea.skripsi.R
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(
    onLoadingFinished: () -> Unit
) {
    // Menyimpan state callback terbaru agar aman saat recomposition
    val currentOnLoadingFinished by rememberUpdatedState(onLoadingFinished)

    // Timer: Tunggu 2 detik (2000ms) lalu pindah halaman
    LaunchedEffect(Unit) {
        delay(2000)
        currentOnLoadingFinished()
    }

    // Logic Animasi Rotasi (Berputar terus menerus)
    val infiniteTransition = rememberInfiniteTransition(label = "loading_transition")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing), // Putaran selesai dalam 1.5 detik
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
            // Gambar ic_loading yang berputar
            Image(
                painter = painterResource(id = R.drawable.ic_loading),
                contentDescription = "Loading",
                modifier = Modifier
                    .size(100.dp) // Sesuaikan ukuran icon
                    .rotate(angle) // Terapkan rotasi
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