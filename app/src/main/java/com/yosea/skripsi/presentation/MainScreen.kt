package com.yosea.skripsi.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yosea.skripsi.presentation.camera.CameraScreen
import com.yosea.skripsi.presentation.disease.DiseaseScreen
import com.yosea.skripsi.presentation.home.HomeScreen
import com.yosea.skripsi.presentation.navigation.Screen
import com.yosea.skripsi.presentation.scan.GalleryScreen
import com.yosea.skripsi.presentation.scan.ScanSelectionScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    val items = listOf(Screen.Home, Screen.Scan, Screen.Disease)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    // Logic agar icon aktif jika berada di sub-halaman (Kamera/Galeri)
                    val isSelected = currentRoute == screen.route ||
                            (screen == Screen.Scan && (currentRoute == Screen.Camera.route || currentRoute == Screen.Gallery.route))

                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onCameraClick = {
                        // Langsung buka Kamera
                        navController.navigate(Screen.Camera.route) {
                            launchSingleTop = true
                        }
                    },
                    onGalleryClick = {
                        // Langsung buka Galeri
                        navController.navigate(Screen.Gallery.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // 1. MENU TENGAH: Halaman Pilihan (Kamera / Galeri)
            composable(Screen.Scan.route) {
                ScanSelectionScreen(
                    onCameraClick = { navController.navigate(Screen.Camera.route) },
                    onGalleryClick = { navController.navigate(Screen.Gallery.route) }
                )
            }

            // 2. Sub-Menu: Kamera Realtime
            composable(Screen.Camera.route) {
                CameraScreen()
            }

            // 3. Sub-Menu: Input Galeri
            composable(Screen.Gallery.route) {
                GalleryScreen()
            }

            composable(Screen.Disease.route) {
                DiseaseScreen()
            }
        }
    }
}