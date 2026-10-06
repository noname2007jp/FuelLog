package com.fuellog.app.ui.history

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuellog.app.data.FuelEntry
import com.fuellog.app.ui.RecordsViewModel
import com.fuellog.app.util.CsvExporter
import com.fuellog.app.util.Formatters
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val W_DATE = 1.2f
private const val W_ODO = 1.5f
private const val W_DIST = 1.1f
private const val W_FUEL = 1.0f
private const val W_ECO = 1.2f
private const val W_COST = 1.3f

@Composable
fun HistoryScreen(viewModel: RecordsViewModel, onRecordClick: (Long) -> Unit) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    os.write(CsvExporter.build(entries).toByteArray(Charsets.UTF_8))
                }
            } catch (_: Exception) {
            }
        }
    }

    val economies = entries.mapNotNull { it.economyKmPerLiter }
    val average = if (economies.isEmpty()) null else economies.average()
    val reversed = remember(entries) { entries.reversed() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "📋 燃費履歴",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C2B21),
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    val name = "fuellog_" +
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv"
                    csvLauncher.launch(name)
                },
                enabled = entries.isNotEmpty()
            ) { Text("⬇ CSV出力", fontSize = 12.sp) }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(Color(0xFF2E7D32))
                .padding(vertical = 8.dp)
        ) {
            HeaderCell("日付", W_DATE)
            HeaderCell("オド(km)", W_ODO)
            HeaderCell("走行(km)", W_DIST)
            HeaderCell("給油(L)", W_FUEL)
            HeaderCell("燃費", W_ECO)
            HeaderCell("金額(円)", W_COST)
        }

        if (reversed.isEmpty()) {
            Text(
                "記録がありません",
                fontSize = 12.sp,
                color = Color(0xFF7A828F),
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(modifier = Modifier.weight(1f).background(Color.White)) {
            items(reversed, key = { it.record.id }) { entry ->
                EntryRow(entry, average, onClick = { onRecordClick(entry.record.id) })
                HorizontalDivider(color = Color(0xFFEEF1F4))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                .background(Color(0xFFEEF5EF))
                .padding(vertical = 8.dp)
        ) {
            Text(
                "平均",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(W_DATE + W_ODO + W_DIST + W_FUEL)
            )
            Text(
                average?.let { Formatters.fmt1(it) } ?: "—",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(W_ECO)
            )
            Text(
                "—",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(W_COST)
            )
        }
    }
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text,
        color = Color.White,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(weight)
    )
}

@Composable
private fun RowScope.BodyCell(text: String, weight: Float) {
    Text(
        text,
        fontSize = 10.sp,
        color = Color(0xFF333333),
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(weight)
    )
}

@Composable
private fun EntryRow(entry: FuelEntry, average: Double?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val d = entry.record.date
        BodyCell(if (d.length >= 10) d.substring(5).replace('-', '/') else d, W_DATE)
        BodyCell(entry.record.odometerKm?.let { Formatters.fmt0(it) } ?: "—", W_ODO)
        BodyCell(entry.distanceKm?.let { Formatters.fmt0(it) } ?: "—", W_DIST)
        BodyCell(Formatters.fmt1(entry.record.fuelLiters), W_FUEL)
        Box(modifier = Modifier.weight(W_ECO), contentAlignment = Alignment.Center) {
            val eco = entry.economyKmPerLiter
            val (bg, fg) = ecoBadgeColors(eco, average)
            Text(
                eco?.let { Formatters.fmt1(it) } ?: "—",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(bg)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
        BodyCell(if (entry.record.costYen > 0) Formatters.money(entry.record.costYen) else "—", W_COST)
    }
}

private fun ecoBadgeColors(eco: Double?, average: Double?): Pair<Color, Color> = when {
    eco == null || average == null -> Color(0xFFEEF1F4) to Color(0xFF7A828F)
    eco >= average -> Color(0xFFE3F2E6) to Color(0xFF2E7D32)
    eco >= average * 0.92 -> Color(0xFFFFF6DF) to Color(0xFFA87B00)
    else -> Color(0xFFFDEAE4) to Color(0xFFC64A22)
}
