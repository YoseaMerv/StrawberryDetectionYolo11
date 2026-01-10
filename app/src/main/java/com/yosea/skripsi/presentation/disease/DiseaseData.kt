package com.yosea.skripsi.presentation.disease

import androidx.compose.ui.graphics.Color
import com.yosea.skripsi.R

data class DiseaseModel(
    val id: String,
    val title: String,
    val description: String,
    val solution: String,
    val iconRes: Int,
    val color: Color
)

val GlobalDiseaseList = listOf(
    DiseaseModel(
        id = "Healthy",
        title = "Healthy (Sehat)",
        description = "Daun stroberi berwarna hijau segar, bentuk utuh, dan tidak memiliki bercak atau serbuk putih.",
        solution = "Pertahankan perawatan rutin:\n1. Penyiraman teratur.\n2. Pemupukan NPK seimbang.\n3. Penyiangan gulma.",
        iconRes = R.drawable.icon_healthy,
        color = Color(0xFF4CAF50)
    ),
    DiseaseModel(
        id = "Leaf Spot",
        title = "Leaf Spot (Bercak Daun)",
        description = "Penyakit jamur dengan gejala bercak kecil ungu kemerahan yang membesar menjadi coklat.",
        solution = "1. Pangkas daun terinfeksi.\n2. Hindari menyiram daun langsung.\n3. Semprot fungisida Tembaga.",
        iconRes = R.drawable.icon_leafspot,
        color = Color(0xFFF44336)
    ),
    DiseaseModel(
        id = "Powdery Mildew",
        title = "Powdery Mildew (Embun Tepung)",
        description = "Lapisan serbuk putih seperti tepung pada permukaan daun. Daun mengeriting dan kering.",
        solution = "1. Tingkatkan sirkulasi udara.\n2. Kurangi pupuk Nitrogen.\n3. Gunakan fungisida Sulfur.",
        iconRes = R.drawable.icon_powderymildew,
        color = Color(0xFFFFA500)
    )
)