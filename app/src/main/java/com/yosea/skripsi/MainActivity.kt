package com.yosea.skripsi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yosea.skripsi.presentation.LoadingScreen // Pastikan ini di-import
import com.yosea.skripsi.presentation.MainScreen
import com.yosea.skripsi.presentation.SkripsiTheme
import com.yosea.skripsi.presentation.WelcomeScreen

class MainActivity : ComponentActivity() {

    // --- 1. LOGIKA IZIN KAMERA ---
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(this, "Izin kamera wajib untuk aplikasi ini!", Toast.LENGTH_LONG).show()
            }
        }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkCameraPermission()

        setContent {
            SkripsiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val rootNavController = rememberNavController()

                    NavHost(
                        navController = rootNavController,
                        startDestination = "welcome_screen"
                    ) {
                        // A. HALAMAN WELCOME
                        composable("welcome_screen") {
                            WelcomeScreen(
                                onStartClick = {
                                    // Pindah ke Loading Screen
                                    rootNavController.navigate("loading_screen")
                                }
                            )
                        }

                        // B. HALAMAN LOADING (BARU)
                        composable("loading_screen") {
                            LoadingScreen(
                                onLoadingFinished = {
                                    // Setelah loading selesai, pindah ke Main Screen
                                    // dan hapus riwayat backstack agar tidak bisa kembali ke welcome
                                    rootNavController.navigate("main_screen") {
                                        popUpTo("welcome_screen") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // C. HALAMAN UTAMA (MainScreen berisi Navbar & GalleryScreen)
                        composable("main_screen") {
                            MainScreen()
                        }
                    }
                }
            }
        }
    }
}