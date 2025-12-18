package com.yosea.skripsi.presentation.scan

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yosea.skripsi.R
import com.yosea.skripsi.data.tflite.Detection
import com.yosea.skripsi.data.tflite.ObjectDetectorHelper
import com.yosea.skripsi.presentation.ModelSession
import com.yosea.skripsi.presentation.components.OverlayView
import com.yosea.skripsi.presentation.disease.GlobalDiseaseList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detectionResults by remember { mutableStateOf<List<Detection>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // State Popup Detail
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedResult by remember { mutableStateOf<Detection?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // --- STATE PANEL EXPAND/COLLAPSE ---
    var isPanelExpanded by remember { mutableStateOf(true) }

    // Logic Auto-Expand saat ada hasil baru
    LaunchedEffect(detectionResults) {
        if (detectionResults.isNotEmpty()) {
            isPanelExpanded = true
        }
    }

    // 1. AMBIL HELPER DARI SESSION
    val objectDetectorHelper = remember { ModelSession.detectorHelper }

    // 2. LISTENER
    val listener = remember {
        object : ObjectDetectorHelper.DetectorListener {
            override fun onError(error: String) {
                Handler(Looper.getMainLooper()).post {
                    if (isLoading) {
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        isLoading = false
                    }
                }
            }

            override fun onResults(
                results: MutableList<Detection>?,
                inferenceTime: Long,
                imageHeight: Int,
                imageWidth: Int
            ) {
                Handler(Looper.getMainLooper()).post {
                    if (!isLoading) return@post
                    detectionResults = results ?: emptyList()
                    isLoading = false
                }
            }
        }
    }

    // 3. PASANG LISTENER (Safe Dispose)
    DisposableEffect(objectDetectorHelper) {
        if (objectDetectorHelper != null) {
            objectDetectorHelper.objectDetectorListener = listener
        }
        onDispose {
            if (objectDetectorHelper?.objectDetectorListener === listener) {
                objectDetectorHelper?.objectDetectorListener = null
            }
        }
    }

    // Fungsi Proses Gambar
    fun processImage(uri: android.net.Uri? = null, bitmap: Bitmap? = null) {
        if (uri == null && bitmap == null) return
        isLoading = true
        detectionResults = emptyList()
        scope.launch(Dispatchers.IO) {
            try {
                val fixedBitmap = if (uri != null) BitmapUtils.getBitmapFromUri(context, uri) else bitmap
                if (fixedBitmap != null) {
                    val argbBitmap = fixedBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    withContext(Dispatchers.Main) { imageBitmap = argbBitmap }

                    if (objectDetectorHelper != null) {
                        objectDetectorHelper.detect(argbBitmap, 0)
                    } else {
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            Toast.makeText(context, "Model AI belum siap", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) { isLoading = false }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> processImage(uri = uri) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap -> processImage(bitmap = bitmap) }

    val uniqueResults = remember(detectionResults) {
        detectionResults
            .sortedByDescending { it.categories.firstOrNull()?.score ?: 0f }
            .distinctBy { it.categories.firstOrNull()?.label }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // --- 1. AREA PREVIEW ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 50.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .drawWithContent {
                    drawContent()
                    val stroke = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(25f, 25f), 0f))
                    drawRoundRect(color = Color.White.copy(alpha = 0.3f), style = stroke, cornerRadius = CornerRadius(24.dp.toPx()))
                },
            contentAlignment = Alignment.Center
        ) {
            if (imageBitmap != null) {
                Image(bitmap = imageBitmap!!.asImageBitmap(), contentDescription = "Selected Image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                if (!isLoading) OverlayView(results = detectionResults, imageWidth = imageBitmap!!.width, imageHeight = imageBitmap!!.height, isFit = true)
                if (isLoading) LoadingOverlay()
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_gallery),
                        contentDescription = "Placeholder",
                        modifier = Modifier.size(80.dp),
                        colorFilter = ColorFilter.tint(Color.Gray.copy(alpha = 0.6f))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Area Preview", color = Color.Gray.copy(alpha = 0.8f))
                }
            }
        }

        // --- 2. PANEL KONTROL (COLLAPSIBLE) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFF121212))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Disable click through */ }
                .animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HANDLE BAR (UNTUK EXPAND/COLLAPSE) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isPanelExpanded = !isPanelExpanded } // Area tarik/klik
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Garis Handle Visual
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Gray.copy(alpha = 0.5f))
                )
            }

            // --- KONTEN UTAMA PANEL ---
            AnimatedVisibility(
                visible = isPanelExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isLoading) {
                        Text("Sedang Menganalisis...", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(vertical = 16.dp))
                    } else if (uniqueResults.isNotEmpty()) {

                        // --- UPDATE: Tombol Sembunyikan Dihapus ---
                        // Text Judul digeser ke Kiri agar rapi
                        Text(
                            text = "Hasil Deteksi",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Start) // Rata Kiri
                        )

                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uniqueResults) { result ->
                                val label = result.categories.firstOrNull()?.label ?: "Unknown"
                                val score = result.categories.firstOrNull()?.score ?: 0f
                                val diseaseData = GlobalDiseaseList.find { it.id.trim().equals(label.trim(), ignoreCase = true) }
                                val displayTitle = diseaseData?.title ?: "$label (Data Tidak Ditemukan)"

                                ResultCard(
                                    label = displayTitle,
                                    score = score,
                                    onClick = {
                                        selectedResult = result
                                        showBottomSheet = true
                                    }
                                )
                            }
                        }

                        Button(onClick = { imageBitmap = null; detectionResults = emptyList() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))) {
                            Icon(Icons.Rounded.Refresh, null, tint = Color.Black); Spacer(Modifier.width(8.dp)); Text("Scan Ulang", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                    } else if (imageBitmap != null) {
                        // Tidak ada deteksi
                        Icon(Icons.Rounded.Warning, null, tint = Color(0xFFFFC107), modifier = Modifier.size(48.dp))
                        Text("Tidak ada daun terdeteksi", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Button(onClick = { imageBitmap = null; detectionResults = emptyList() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))) {
                            Text("Coba Lagi", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Menu Awal
                        Button(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                            Icon(painter = painterResource(id = R.drawable.ic_camera), contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp)); Text("Ambil Foto Langsung", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))) {
                            Icon(painter = painterResource(id = R.drawable.ic_add_image), contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp)); Text("Pilih dari Galeri")
                        }
                    }
                    Spacer(modifier = Modifier.height(50.dp))
                }
            }

            // --- HEADER SAAT COLLAPSED (RINGKASAN) ---
            if (!isPanelExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uniqueResults.isNotEmpty()) {
                        Text("${uniqueResults.size} Penyakit Terdeteksi", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Ketuk panel ini untuk melihat detail", color = Color.Gray, fontSize = 12.sp)
                    } else if (imageBitmap != null) {
                        Text("Analisis Selesai", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Menu Scan", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    // --- POPUP DETAIL & LOADING (TETAP SAMA) ---
    if (showBottomSheet && selectedResult != null) {
        val label = selectedResult!!.categories.firstOrNull()?.label ?: "Unknown"
        val diseaseData = GlobalDiseaseList.find { it.id.trim().equals(label.trim(), ignoreCase = true) }
        val displayTitle = diseaseData?.title ?: label
        val description = diseaseData?.description ?: "ID label: '$label'."
        val solution = diseaseData?.solution ?: "Belum ada solusi."
        val iconRes = diseaseData?.iconRes ?: R.drawable.icon_healthy
        val themeColor = diseaseData?.color ?: Color.Gray

        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, sheetState = sheetState, containerColor = Color.White) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(60.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = displayTitle, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = themeColor, modifier = Modifier.weight(1f))
                }
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                Text("Deskripsi", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                Text(description, style = MaterialTheme.typography.bodyLarge, color = Color.DarkGray)
                Spacer(Modifier.height(16.dp))
                Text("Solusi", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                Card(colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.1f)), modifier = Modifier.fillMaxWidth()) {
                    Text(solution, modifier = Modifier.padding(16.dp), color = Color.Black.copy(alpha = 0.8f))
                }
                Spacer(Modifier.height(32.dp))
                Button(onClick = { showBottomSheet = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) {
                    Text("Tutup", color = Color.Black)
                }
            }
        }
    }
}

// ... LoadingOverlay dan ResultCard tetap sama ...
@Composable
fun LoadingOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha")
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(id = R.drawable.ic_fade_loading), contentDescription = "Loading", modifier = Modifier.size(80.dp).alpha(alpha), colorFilter = ColorFilter.tint(Color(0xFF4CAF50)))
            Spacer(Modifier.height(16.dp))
            Text("Mendeteksi...", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
    }
}

@Composable
fun ResultCard(label: String, score: Float, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                Text(String.format(Locale.US, "Akurasi: %.0f%% • Klik untuk detail", score * 100), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Icon(Icons.Rounded.KeyboardArrowRight, null, tint = Color.Gray)
        }
    }
}