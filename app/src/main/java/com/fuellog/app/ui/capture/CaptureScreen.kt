package com.fuellog.app.ui.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fuellog.app.FuelLogApp
import com.fuellog.app.ocr.OcrEngine
import com.fuellog.app.ocr.RoiStore
import com.fuellog.app.ocr.RoiTarget
import com.fuellog.app.util.Formatters
import com.fuellog.app.util.ImageUtils
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CaptureScreen(
    viewModel: CaptureViewModel,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as FuelLogApp
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val ocrEngine = remember { OcrEngine(context.applicationContext) }
    val roiStore = remember { RoiStore(context.applicationContext) }
    var lastImageForRoi by remember { mutableStateOf<File?>(null) }
    var showRoiEditor by remember { mutableStateOf(false) }

    var target by remember { mutableStateOf(CaptureTarget.RECEIPT) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val imageCapture = remember { ImageCapture.Builder().build() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }

    fun processImage(file: File, currentTarget: CaptureTarget) {
        scope.launch {
            viewModel.updateProcessing(true)
            try {
                val exifDate = ImageUtils.readExifDate(file)
                val normalizedFile = ImageUtils.normalizeForDisplayAndOcr(file, app.photoDir) ?: file
                lastImageForRoi = normalizedFile
                val uri = Uri.fromFile(normalizedFile)
                val savedRois = roiStore.load()
                fun roi(target: RoiTarget) = savedRois.firstOrNull { it.target == target }
                when (currentTarget) {
                    CaptureTarget.RECEIPT -> {
                        viewModel.applyExifDate(exifDate)
                        val amountRoi = roi(RoiTarget.FUEL_AMOUNT)
                        val unitPriceRoi = roi(RoiTarget.UNIT_PRICE)
                        val totalRoi = roi(RoiTarget.TOTAL)
                        val dateRoi = roi(RoiTarget.DATE)
                        val roiConfigured = amountRoi != null && unitPriceRoi != null &&
                            totalRoi != null && dateRoi != null
                        val text = if (roiConfigured) {
                            val litersText = ocrEngine.recognizeRegion(normalizedFile, amountRoi!!, useJapanese = true)
                            val unitText = ocrEngine.recognizeRegion(normalizedFile, unitPriceRoi!!, useJapanese = true)
                            val totalText = ocrEngine.recognizeRegion(normalizedFile, totalRoi!!, useJapanese = true)
                            val dateText = ocrEngine.recognizeRegion(normalizedFile, dateRoi!!, useJapanese = true)
                            "給油量 $litersText\n単価 $unitText\n合計 $totalText\n日付 $dateText"
                        } else {
                            ocrEngine.recognizeReceipt(uri)
                        }
                        viewModel.onReceiptRecognized(normalizedFile.absolutePath, text)
                    }
                    CaptureTarget.ODOMETER -> {
                        val integerRoi = roi(RoiTarget.ODOMETER_INTEGER)
                        val decimalRoi = roi(RoiTarget.ODOMETER_DECIMAL)
                        if (integerRoi != null && decimalRoi != null) {
                            val value = ocrEngine.recognizeOdometer(normalizedFile, integerRoi, decimalRoi)
                            viewModel.onOdometerRecognized(
                                normalizedFile.absolutePath,
                                value?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: ""
                            )
                        } else {
                            viewModel.onOdometerRecognized(normalizedFile.absolutePath, ocrEngine.recognizeMeter(uri))
                        }
                    }
                    CaptureTarget.TRIP -> {
                        val integerRoi = roi(RoiTarget.TRIP_INTEGER)
                        val decimalRoi = roi(RoiTarget.TRIP_DECIMAL)
                        if (integerRoi != null && decimalRoi != null) {
                            val integerText = ocrEngine.recognizeRegion(normalizedFile, integerRoi)
                            val decimalText = ocrEngine.recognizeRegion(normalizedFile, decimalRoi)
                            val value = com.fuellog.app.ocr.OcrAnalyzer.combineOdometer(integerText, decimalText)
                            viewModel.onTripRecognized(
                                normalizedFile.absolutePath,
                                value?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: ""
                            )
                        } else {
                            viewModel.onTripRecognized(normalizedFile.absolutePath, ocrEngine.recognizeMeter(uri))
                        }
                    }
                }
            } catch (e: Exception) {
                viewModel.setError("読み取りに失敗しました。もう一度お試しください。")
            } finally {
                viewModel.updateProcessing(false)
            }
        }
    }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val file = ImageUtils.copyToPhotoDir(context, uri, app.photoDir)
            if (file != null) {
                processImage(file, target)
            } else {
                viewModel.setError("写真を読み込めませんでした")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF14171B))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る", tint = Color.White)
            }
            Text("記録を追加", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val provider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build()
                                preview.setSurfaceProvider(previewView.surfaceProvider)
                                provider.unbindAll()
                                camera = provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                viewModel.setError("カメラを起動できませんでした")
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 48.dp)) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.8f),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 16f))
                        ),
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                }
                Text(
                    text = target.guide,
                    color = Color.White,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp, start = 24.dp, end = 24.dp)
                )
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("カメラの使用が許可されていません", color = Color.White, fontSize = 14.sp)
                    Text(
                        "「過去の写真から読み取り」はそのまま利用できます",
                        color = Color(0xFF9AA3AD),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    TextButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("カメラを許可する")
                    }
                }
            }
            if (viewModel.isProcessing) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Text(
                            "読み取り中…",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            CaptureTarget.entries.forEach { t ->
                FilterChip(
                    selected = target == t,
                    onClick = { target = t },
                    label = { Text(t.label, fontSize = 11.sp) },
                    modifier = Modifier.padding(horizontal = 4.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF262B32),
                        labelColor = Color(0xFFCFD6DD),
                        selectedContainerColor = Color(0xFF2E7D32),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF262B32))
                        .clickable(enabled = !viewModel.isProcessing) {
                            pickLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) { Text("🖼️", fontSize = 18.sp) }
                Text(
                    "過去の写真\nから読み取り",
                    color = Color(0xFFCFD6DD),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(66.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .clickable(enabled = hasCameraPermission && !viewModel.isProcessing) {
                        takePhoto(
                            context = context,
                            imageCapture = imageCapture,
                            photoDir = app.photoDir,
                            onSaved = { file -> processImage(file, target) },
                            onError = { viewModel.setError(it) }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(50.dp).background(Color.White, CircleShape))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(enabled = camera != null) {
                            torchOn = !torchOn
                            camera?.cameraControl?.enableTorch(torchOn)
                        },
                    contentAlignment = Alignment.Center
                ) { Text(if (torchOn) "🔦" else "⚡", fontSize = 18.sp) }
                Text(
                    "フラッシュ",
                    color = Color(0xFFCFD6DD),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(
                enabled = lastImageForRoi != null && !viewModel.isProcessing,
                onClick = { showRoiEditor = true }
            ) { Text("ROI設定（読み取り範囲）") }
        }

        if (showRoiEditor && lastImageForRoi != null) {
            RoiEditorDialog(
                imageFile = lastImageForRoi!!,
                roiStore = roiStore,
                onDismiss = { showRoiEditor = false },
                onSaved = {
                    showRoiEditor = false
                    viewModel.setError(null)
                    viewModel.setInfo("ROI設定を保存しました。次回の読み取りから適用します")
                }
            )
        }

        Text(
            text = buildStatusText(viewModel),
            color = Color(0xFFCFD6DD),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        )
        viewModel.infoMessage?.let {
            Text(
                it,
                color = Color(0xFF8BC34A),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }
        viewModel.errorMessage?.let {
            Text(
                it,
                color = Color(0xFFFF8A80),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }

        Button(
            onClick = onConfirm,
            enabled = !viewModel.isProcessing,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) { Text("確認・修正へ進む") }
    }
}

private fun buildStatusText(viewModel: CaptureViewModel): String = buildString {
    append("レシート: ")
    append(
        viewModel.fuelLiters?.let { "${Formatters.fmt2(it)}L" }
            ?: if (viewModel.receiptPhotoPath != null) "撮影済" else "未撮影"
    )
    viewModel.costYen?.let { append(" / ¥${Formatters.money(it)}") }
    append("    オド: ")
    append(viewModel.odometerKm?.let { "${Formatters.fmt0(it)}km" } ?: "—")
    append("    トリップ: ")
    append(viewModel.tripKm?.let { "${Formatters.fmt1(it)}km" } ?: "—")
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    photoDir: File,
    onSaved: (File) -> Unit,
    onError: (String) -> Unit
) {
    val file = File(photoDir, "IMG_${System.currentTimeMillis()}.jpg")
    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(result: ImageCapture.OutputFileResults) = onSaved(file)
            override fun onError(exception: ImageCaptureException) =
                onError("撮影に失敗しました: ${exception.message ?: "不明なエラー"}")
        }
    )
}
