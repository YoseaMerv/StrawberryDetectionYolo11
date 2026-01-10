package com.yosea.skripsi.presentation

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
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

val GreenShadowColor = Color(0xFF4CAF50)
val GreenLightColor = Color(0xFFE8F5E9)

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(Screen.Camera, Screen.Gallery, Screen.Disease)

    Scaffold(
        containerColor = Color.Transparent, // Pastikan transparan
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
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
        ) {
            // 1. Camera
            composable(Screen.Camera.route) {
                CameraScreen()
            }

            // 2. Gallery
            composable(Screen.Gallery.route) {
                GalleryScreen()
            }

            // 3. Disease
            composable(Screen.Disease.route) {
                DiseaseScreen()
            }
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

    //  Shadow
    val shadowColor = GreenShadowColor
    val shadowRadius = 16.dp
    val shadowOffsetY = (-4).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .drawBehind {
                val shadowColorArgb = shadowColor.toArgb()
                val shadowRadiusPx = shadowRadius.toPx()
                val shadowOffsetYPx = shadowOffsetY.toPx()
                val cornerRadiusPx = 24.dp.toPx()

                drawIntoCanvas { canvas ->
                    val paint = Paint()
                    val frameworkPaint = paint.asFrameworkPaint()
                    frameworkPaint.color = shadowColorArgb

                    frameworkPaint.maskFilter = BlurMaskFilter(
                        shadowRadiusPx,
                        BlurMaskFilter.Blur.NORMAL
                    )

                    val path = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = 0f,
                                top = shadowOffsetYPx,
                                right = size.width,
                                bottom = size.height + shadowRadiusPx,
                                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                            )
                        )
                    }
                    canvas.drawPath(path, paint)
                }
            }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
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
                                    .size(28.dp)
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
}

@Preview(showBackground = true)
@Composable
fun CustomBottomBarPreview() {
    val navController = rememberNavController()
    val items = listOf(Screen.Camera, Screen.Gallery, Screen.Disease)
    Box(modifier = Modifier.padding(20.dp)) {
        CustomBottomBar(navController = navController, items = items)
    }
}