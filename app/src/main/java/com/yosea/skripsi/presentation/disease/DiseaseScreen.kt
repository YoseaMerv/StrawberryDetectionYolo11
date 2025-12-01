package com.yosea.skripsi.presentation.disease

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DiseaseScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Daftar Penyakit",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
            DiseaseCard(
                title = "Healthy (Sehat)",
                desc = "Daun stroberi berwarna hijau segar, bentuk utuh, dan tidak memiliki bercak atau serbuk putih.",
                color = Color(0xFF4CAF50) // Hijau
            )
        }
        item {
            DiseaseCard(
                title = "Leaf Spot (Bercak Daun)",
                desc = "Penyakit yang disebabkan jamur. Gejala berupa bercak-bercak kecil berwarna ungu kemerahan yang kemudian membesar menjadi coklat di tengah.",
                color = Color(0xFFF44336) // Merah
            )
        }
        item {
            DiseaseCard(
                title = "Powdery Mildew (Embun Tepung)",
                desc = "Ditandai dengan lapisan serbuk putih seperti tepung pada permukaan daun. Menyebabkan daun mengeriting dan mengering.",
                color = Color(0xFFFFA500) // Orange
            )
        }
    }
}

@Composable
fun DiseaseCard(title: String, desc: String, color: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}