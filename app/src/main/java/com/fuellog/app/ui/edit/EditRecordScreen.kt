package com.fuellog.app.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fuellog.app.FuelLogApp
import com.fuellog.app.data.FuelRecord
import com.fuellog.app.ui.capture.CaptureViewModel
import com.fuellog.app.util.Formatters
import java.io.File
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecordScreen(
    recordId: Long,
    captureViewModel: CaptureViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onRetake: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as FuelLogApp
    val viewModel: EditViewModel = viewModel(factory = EditViewModel.factory(app.repository, recordId))
    val existing = viewModel.existing

    var date by rememberSaveable { mutableStateOf(captureViewModel.recognizedDate.toString()) }
    var fuelText by rememberSaveable { mutableStateOf("") }
    var costText by rememberSaveable { mutableStateOf("") }
    var priceText by rememberSaveable { mutableStateOf("") }
    var odoText by rememberSaveable { mutableStateOf("") }
    var tripAText by rememberSaveable { mutableStateOf("") }
    var tripText by rememberSaveable { mutableStateOf("") }
    var vehicleEconomyText by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var prefilled by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    LaunchedEffect(existing) {
        if (prefilled) return@LaunchedEffect
        if (recordId > 0) {
            val r = existing ?: return@LaunchedEffect
            date = r.date
            fuelText = Formatters.editable2(r.fuelLiters)
            costText = if (r.costYen > 0) r.costYen.toString() else ""
            priceText = r.unitPrice?.let { Formatters.editable2(it) } ?: ""
            odoText = r.odometerKm?.let { Formatters.editable(it) } ?: ""
            tripAText = r.tripAKm?.let { Formatters.editable(it) } ?: ""
            tripText = r.tripKm?.let { Formatters.editable(it) } ?: ""
            vehicleEconomyText = r.vehicleFuelEconomyKmPerLiter?.let { Formatters.editable(it) } ?: ""
            prefilled = true
        } else {
            date = captureViewModel.recognizedDate.toString()
            captureViewModel.fuelLiters?.let { fuelText = Formatters.editable2(it) }
            captureViewModel.costYen?.let { costText = it.toString() }
            captureViewModel.unitPrice?.let { priceText = Formatters.editable2(it) }
            captureViewModel.odometerKm?.let { odoText = Formatters.editable(it) }
            captureViewModel.tripAKm?.let { tripAText = Formatters.editable(it) }
            captureViewModel.tripKm?.let { tripText = Formatters.editable(it) }
            captureViewModel.vehicleFuelEconomyKmPerLiter?.let { vehicleEconomyText = Formatters.editable(it) }
            prefilled = true
        }
    }

    val receiptPath = existing?.receiptPhotoPath ?: captureViewModel.receiptPhotoPath
    // 旧記録に保存済みの画像だけを表示。新しいメーター写真は一時表示のみ。
    val meterPath = existing?.meterPhotoPath
    val meterAPhotoPath = captureViewModel.meterAPhotoPath
    val meterBPhotoPath = captureViewModel.meterBPhotoPath

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "戻る",
                    tint = Color(0xFF2E7D32)
                )
            }
            Text("読み取り結果の確認", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        if (receiptPath != null || meterPath != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                receiptPath?.let { path ->
                    PhotoThumbnail(path, "🧾 レシート", Modifier.weight(1f), onRetake)
                }
                meterPath?.let { path ->
                    PhotoThumbnail(path, "🚗 メーター", Modifier.weight(1f), onRetake)
                }
            }
        }
        if (meterAPhotoPath != null || meterBPhotoPath != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                meterAPhotoPath?.let { path ->
                    PhotoThumbnail(path, "🚗 メーターA", Modifier.fillMaxWidth(), onRetake)
                }
                meterBPhotoPath?.let { path ->
                    PhotoThumbnail(path, "🔁 メーターB", Modifier.fillMaxWidth(), onRetake)
                }
            }
            Text(
                "メーター写真は確認用の一時画像です。保存後に削除されます。",
                fontSize = 10.sp,
                color = Color(0xFF7A828F)
            )
        }

        LabeledField(
            label = "給油日",
            badge = captureViewModel.dateOrigin.label,
            value = date,
            onValueChange = {
                date = it
                it.toLocalDateOrNullForPicker()?.let(captureViewModel::setDate)
            },
            keyboardType = KeyboardType.Text,
            placeholder = "2026-10-06"
        )

        OutlinedButton(
            onClick = {
                date.toLocalDateOrNullForPicker()?.let {
                    datePickerState.selectedDateMillis = it.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                }
                showDatePicker = true
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("カレンダーから日付を選択") }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledField(
                label = "給油量 (L)",
                badge = "OCR読取",
                value = fuelText,
                onValueChange = { fuelText = it },
                keyboardType = KeyboardType.Decimal,
                placeholder = "42.5",
                modifier = Modifier.weight(1f)
            )
            LabeledField(
                label = "金額 (円)",
                badge = "OCR読取",
                value = costText,
                onValueChange = { costText = it },
                keyboardType = KeyboardType.Number,
                placeholder = "7225",
                modifier = Modifier.weight(1f)
            )
        }

        LabeledField(
            label = "単価 (円/L) ※任意",
            badge = "OCR読取",
            value = priceText,
            onValueChange = { priceText = it },
            keyboardType = KeyboardType.Decimal,
            placeholder = "170.0"
        )

        LabeledField(
            label = "オドメーター：総走行距離 (km)",
            badge = "メーターBから読取",
            value = odoText,
            onValueChange = { odoText = it },
            keyboardType = KeyboardType.Decimal,
            placeholder = "48532"
        )

        LabeledField(
            label = "トリップA：前回満タンからの総走行距離 (km)",
            badge = "燃費計算に使用",
            value = tripAText,
            onValueChange = { tripAText = it },
            keyboardType = KeyboardType.Decimal,
            placeholder = "123456.7"
        )

        LabeledField(
            label = "トリップB：オドメーター補完用 (km)",
            badge = "ODO小数部の照合用",
            value = tripText,
            onValueChange = { tripText = it },
            keyboardType = KeyboardType.Decimal,
            placeholder = "512.3"
        )

        LabeledField(
            label = "車載燃費 (km/L)",
            badge = "メーターAから読取",
            value = vehicleEconomyText,
            onValueChange = { vehicleEconomyText = it },
            keyboardType = KeyboardType.Decimal,
            placeholder = "15.6"
        )

        Text(
            "※ すべての項目は修正できます\n※ 燃費計算にはトリップAを優先し、未入力時はオドメーター差分を使用します\n※ トリップBはオドメーターの小数部補完に利用します",
            fontSize = 10.sp,
            color = Color(0xFF7A828F)
        )

        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }

        Button(
            onClick = {
                val liters = fuelText.replace(",", "").toDoubleOrNull()
                when {
                    !Regex("""\d{4}-\d{2}-\d{2}""").matches(date) ->
                        errorMessage = "日付は yyyy-MM-dd 形式で入力してください"
                    liters == null || liters <= 0 ->
                        errorMessage = "給油量を正しく入力してください"
                    else -> {
                        errorMessage = null
                        val record = FuelRecord(
                            id = existing?.id ?: 0,
                            date = date,
                            fuelLiters = captureViewModel.fuelLiters?.takeIf {
                                recordId <= 0 && fuelText == Formatters.editable2(it)
                            } ?: existing?.takeIf {
                                fuelText == Formatters.editable2(it.fuelLiters)
                            }?.fuelLiters ?: liters,
                            costYen = costText.replace(",", "").toIntOrNull() ?: 0,
                            unitPrice = captureViewModel.unitPrice?.takeIf {
                                recordId <= 0 && priceText == Formatters.editable2(it)
                            } ?: existing?.takeIf { it.unitPrice != null &&
                                priceText == Formatters.editable2(it.unitPrice) }?.unitPrice
                                ?: priceText.replace(",", "").toDoubleOrNull(),
                            odometerKm = captureViewModel.odometerKm?.takeIf {
                                recordId <= 0 && odoText == Formatters.editable(it)
                            } ?: existing?.takeIf { it.odometerKm != null &&
                                odoText == Formatters.editable(it.odometerKm) }?.odometerKm
                                ?: odoText.replace(",", "").toDoubleOrNull(),
                            tripAKm = captureViewModel.tripAKm?.takeIf {
                                recordId <= 0 && tripAText == Formatters.editable(it)
                            } ?: existing?.takeIf { it.tripAKm != null &&
                                tripAText == Formatters.editable(it.tripAKm) }?.tripAKm
                                ?: tripAText.replace(",", "").toDoubleOrNull(),
                            tripKm = captureViewModel.tripKm?.takeIf {
                                recordId <= 0 && tripText == Formatters.editable(it)
                            } ?: existing?.takeIf { it.tripKm != null &&
                                tripText == Formatters.editable(it.tripKm) }?.tripKm
                                ?: tripText.replace(",", "").toDoubleOrNull(),
                            vehicleFuelEconomyKmPerLiter = captureViewModel.vehicleFuelEconomyKmPerLiter?.takeIf {
                                recordId <= 0 && vehicleEconomyText == Formatters.editable(it)
                            } ?: existing?.takeIf { it.vehicleFuelEconomyKmPerLiter != null &&
                                vehicleEconomyText == Formatters.editable(it.vehicleFuelEconomyKmPerLiter) }?.vehicleFuelEconomyKmPerLiter
                                ?: vehicleEconomyText.replace(",", "").toDoubleOrNull(),
                            receiptPhotoPath = receiptPath,
                            meterPhotoPath = meterPath,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        )
                        viewModel.save(record) {
                            captureViewModel.reset()
                            onSaved()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("保存して燃費を記録") }

        if (existing != null) {
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("この記録を削除", color = Color(0xFFC64A22)) }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }


    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        captureViewModel.setDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("決定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("記録の削除") },
            text = { Text("この給油記録を削除しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete { onSaved() }
                }) { Text("削除", color = Color(0xFFC64A22)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("キャンセル") }
            }
        )
    }
}

@Composable
private fun LabeledField(
    label: String,
    badge: String?,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7A828F))
            if (badge != null) {
                Text(
                    badge,
                    fontSize = 8.sp,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE3F2E6))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text(placeholder, color = Color(0xFFB7BEC7)) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}

@Composable
private fun PhotoThumbnail(
    path: String,
    label: String,
    modifier: Modifier = Modifier,
    onRetake: () -> Unit
) {
    var showPreview by remember { mutableStateOf(false) }
    Box(modifier = modifier.height(180.dp).clip(RoundedCornerShape(12.dp))) {
        AsyncImage(
            model = File(path),
            contentDescription = "$label（タップして拡大）",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().clickable { showPreview = true }
        )
        Text(
            label,
            color = Color.White,
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(5.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Text(
            "再撮影",
            color = Color.White,
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(5.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onRetake)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
    if (showPreview) {
        AlertDialog(
            onDismissRequest = { showPreview = false },
            title = { Text(label) },
            text = {
                AsyncImage(
                    model = File(path),
                    contentDescription = label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(360.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { showPreview = false }) { Text("閉じる") }
            }
        )
    }
}


private fun String.toLocalDateOrNullForPicker(): LocalDate? =
    try { LocalDate.parse(this) } catch (_: Exception) { null }
