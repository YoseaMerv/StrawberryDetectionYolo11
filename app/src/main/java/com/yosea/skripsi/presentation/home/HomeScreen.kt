package com.yosea.skripsi.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yosea.skripsi.R

@Composable
fun HomeScreen(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // Background Putih Bersih
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Header Ilustrasi (Logo Besar)
        Box(
            modifier = Modifier.size(220.dp), // Ukuran diperbesar sedikit agar proporsional
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_strawberry),
                contentDescription = "Logo RedGuard",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Judul Aplikasi
        Text(
            text = "RedGuard",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = Color(0xFF000000) // Hijau Tua Gelap (mirip di gambar)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 3. Kartu Deskripsi
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)), // Abu-abu sangat muda
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Flat style
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RedGuard adalah solusi pintar untuk mendeteksi penyakit tanaman stroberi secara cepat dan akurat melalui kamera smartphone Anda.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 24.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 4. Menu Pilihan (Tombol Besar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tombol Kamera (Hijau)
            HomeOptionCard(
                title = "Kamera",
                icon = Icons.Rounded.CameraAlt,
                color = Color(0xFF8BC34A), // Hijau Muda Segar
                modifier = Modifier.weight(1f),
                onClick = onCameraClick
            )

            // Tombol Galeri (Biru)
            HomeOptionCard(
                title = "Galeri",
                icon = Icons.Rounded.AddPhotoAlternate,
                color = Color(0xFF64B5F6), // Biru Langit
                modifier = Modifier.weight(1f),
                onClick = onGalleryClick
            )
        }
    }
}

@Composable
fun HomeOptionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(120.dp), // Tombol sedikit lebih tinggi
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.35f)), // Alpha lebih tebal agar warna lebih keluar
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF263238), // Ikon warna gelap agar kontras
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238) // Teks warna gelap
            )
        }
    }
}