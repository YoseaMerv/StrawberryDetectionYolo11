package com.yosea.skripsi.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    // Menu Bawah (Bottom Bar)
    object Home : Screen("home", "Beranda", Icons.Default.Home)
    object Scan : Screen("scan_selection", "Scan", Icons.Default.QrCodeScanner)
    object Disease : Screen("disease", "Penyakit", Icons.Default.Info)

    // Sub-Menu (Tidak tampil di Bottom Bar, tapi butuh rute)
    object Camera : Screen("scan_camera", "Kamera Realtime", Icons.Default.QrCodeScanner)
    object Gallery : Screen("scan_gallery", "Input Galeri", Icons.Default.QrCodeScanner)
}