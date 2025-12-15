package com.yosea.skripsi.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yosea.skripsi.R

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // BAGIAN ATAS: Judul & Logo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 60.dp)
        ) {
            // Placeholder Logo (Lingkaran)
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)), // Hijau Muda background
                contentAlignment = Alignment.Center
            ) {
                // Pastikan gambar logo_strawberry ada di drawable
                Image(
                    painter = painterResource(id = R.drawable.logo_strawberry),
                    contentDescription = "Logo",
                    modifier = Modifier.scale(0.8f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "RedGuard",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 36.sp
                ),
                color = Color(0xFF4CAF50) // Warna Hijau Utama
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Deteksi dini penyakit tanaman stroberi\ndengan teknologi AI.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        }

        Button(
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth()
                .height( 64.dp)
                .padding(bottom = 18.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50) // Hijau
            ),
            elevation = ButtonDefaults.buttonElevation(8.dp)
        ) {
            Text(
                text = "Mulai Deteksi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)

// --- PREVIEW ---
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WelcomeScreenPreview() {
    // Kita berikan fungsi kosong {} untuk onStartClick karena ini hanya preview
    WelcomeScreen(onStartClick = {})
}