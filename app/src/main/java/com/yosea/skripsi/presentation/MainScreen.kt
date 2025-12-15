package com.yosea.skripsi.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yosea.skripsi.presentation.camera.CameraScreen
import com.yosea.skripsi.presentation.disease.DiseaseScreen
import com.yosea.skripsi.presentation.navigation.Screen
import com.yosea.skripsi.presentation.scan.GalleryScreen

// 1. DEFINISI WARNA UTAMA
val GreenShadowColor = Color(0xFF4CAF50)
val GreenLightColor = Color(0xFFE8F5E9) // Hijau muda untuk background bubble

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    val items = listOf(Screen.Camera, Screen.Gallery, Screen.Disease)

    Scaffold(
        bottomBar = {
            CustomBottomBar(
                navController = navController,
                items = items
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Camera.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Camera.route) { CameraScreen() }
            composable(Screen.Gallery.route) { GalleryScreen() }
            composable(Screen.Disease.route) { DiseaseScreen() }
        }
    }
}

@Composable
fun CustomBottomBar(
    navController: androidx.navigation.NavController,
    items: List<Screen>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Container Utama langsung menggunakan Surface agar bentuk melengkung tetap ada
    // tanpa ada kotak shadow tambahan di belakangnya.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp), // Tinggi Nav Bar
        color = Color.White, // Warna Nav Bar Putih
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), // Melengkung
        // Set 0.dp jika ingin benar-benar flat/transparan di belakangnya,
        // atau ganti ke 4.dp-8.dp jika ingin bayangan halus standar Android.
        shadowElevation = 0.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent, // Transparan agar mengikuti warna Surface (Putih)
            tonalElevation = 0.dp
        ) {
            items.forEach { screen ->
                val isSelected = currentRoute == screen.route

                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = screen.iconRes),
                            contentDescription = screen.title,
                            modifier = Modifier
                                .size(28.dp) // Ukuran icon seragam
                                .padding(bottom = 4.dp)
                        )
                    },
                    label = {
                        Text(
                            text = screen.title,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    selected = isSelected,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GreenShadowColor,
                        selectedTextColor = GreenShadowColor,
                        indicatorColor = GreenLightColor,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CustomBottomBarPreview() {
    val navController = rememberNavController()
    val items = listOf(Screen.Camera, Screen.Gallery, Screen.Disease)
    Surface(color = Color.LightGray) {
        CustomBottomBar(navController = navController, items = items)
    }
}