package com.yosea.skripsi


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RedGuardTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    // Column utama yang mengisi seluruh layar
    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // --- Bagian Atas (Putih) - 70% dari layar ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f) // Mengambil 70% dari tinggi
                .background(Color(0xFFFDFDFD)) // Putih gading
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Logo
            Image(
                // Ganti R.drawable.redguard_logo dengan nama file logo Anda
                // Pastikan Anda sudah menambahkannya ke folder res/drawable
                painter = painterResource(id = R.drawable.redguard_logo),
                contentDescription = "RedGuard Logo",
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Teks Deskripsi
            Text(
                text = "RedGuard adalah solusi pintar untuk mendeteksi penyakit tanaman stroberi secara cepat dan akurat melalui kamera smartphone Anda.",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = Color.DarkGray
            )
        }

        // --- Bagian Bawah (Merah) - 30% dari layar ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.3f) // Mengambil 30% dari tinggi
                .background(Color(0xFFE53935)), // Warna Merah
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Row untuk menampung dua tombol
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly, // Memberi jarak merata
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 3. Tombol Kamera (Baru)
                ActionButton(
                    text = "Kamera",
                    icon = Icons.Filled.PhotoCamera,
                    onClick = {
                        // TODO: Tambahkan logika untuk membuka kamera
                    }
                )

                // 4. Tombol Galeri (Baru)
                ActionButton(
                    text = "Galeri",
                    icon = Icons.Filled.PhotoLibrary,
                    onClick = {
                        // TODO: Tambahkan logika untuk membuka galeri gambar
                    }
                )
            }
        }
    }
}

/**
 * Composable kustom untuk tombol agar tidak duplikat kode
 */
@Composable
fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    // Tombol dengan outline putih
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, Color.White),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        modifier = Modifier.size(width = 140.dp, height = 100.dp)
    ) {
        // Column di dalam tombol untuk ikon dan teks
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = text, fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    RedGuardTheme {
        MainScreen()
    }
}