package com.yosea.skripsi.presentation.navigation

import com.yosea.skripsi.R

sealed class Screen(val route: String, val title: String, val iconRes: Int) {
    object Camera : Screen("camera", "Scan", R.drawable.ic_scan)

    object Gallery : Screen("gallery", "Galeri", R.drawable.ic_gallery)

    object Disease : Screen("informasi", "Informasi", R.drawable.ic_information)
}