package com.fuellog.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuellog.app.data.FuelEntry
import com.fuellog.app.ui.RecordsViewModel
import com.fuellog.app.util.Formatters
import java.time.YearMonth
import kotlin.math.abs

@Composable
fun HomeScreen(viewModel: RecordsViewModel, onRecordClick: (Long) -> Unit) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    val withEco = entries.filter { it.economyKmPerLiter != null }
    val latest = withEco.lastOrNull()
    val previous = withEco.dropLast(1).lastOrNull()
    val avgEco = if (withEco.isEmpty()) null else withEco.map { it.economyKmPerLiter!! }.average()

    val ym = YearMonth.now().toString()
    val monthEntries = entries.filter { it.record.date.startsWith(ym) }
    val monthDistance = monthEntries.mapNotNull { it.distanceKm }.sum()
    val monthFuel = monthEntries.sumOf { it.record.fuelLiters }
    val recent = entries.takeLast(3).reversed()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("⛽ FuelLog", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C2B21))
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))))
                    .padding(16.dp)
            ) {
                Text(
                    text = "最新の燃費" + latest?.record?.let { " (${it.date} 給油)" }.orEmpty(),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = latest?.economyKmPerLiter?.let { Formatters.fmt1(it) } ?: "--.-",
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        " km/L",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 9.dp)
                    )
                }
                if (latest != null && previous != null) {
                    val diff = latest.economyKmPerLiter!! - previous.economyKmPerLiter!!
                    Text(
                        text = (if (diff >= 0) "▲ 前回比 +" else "▼ 前回比 -") +
                            Formatters.fmt1(abs(diff)) + " km/L",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStatCard("平均燃費", avgEco?.let { Formatters.fmt1(it) } ?: "--.-", " km/L", Modifier.weight(1f))
                MiniStatCard("今月の走行", Formatters.fmt0(monthDistance), " km", Modifier.weight(1f))
                MiniStatCard("今月の給油", Formatters.fmt1(monthFuel), " L", Modifier.weight(1f))
            }
        }

        item {
            Text("最近の記録", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A5560))
        }

        if (recent.isEmpty()) {
            item {
                Text(
                    "まだ記録がありません。\n右下の＋ボタンからレシート・メーターを撮影して記録を始めましょう。",
                    fontSize = 12.sp,
                    color = Color(0xFF7A828F)
                )
            }
        }
        items(recent, key = { it.record.id }) { entry ->
            RecentRecordCard(entry = entry, onClick = { onRecordClick(entry.record.id) })
        }
    }
}

@Composable
private fun MiniStatCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.sp, color = Color(0xFF7A828F))
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 3.dp)) {
                Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C2B21))
                Text(unit, fontSize = 9.sp, color = Color(0xFF7A828F), modifier = Modifier.padding(bottom = 1.dp))
            }
        }
    }
}

@Composable
private fun RecentRecordCard(entry: FuelEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.distanceKm?.let { "${Formatters.fmt0(it)} km 走行" }
                        ?: "走行距離不明(オド/トリップ未入力)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C2B21)
                )
                Text(
                    text = "${entry.record.date}・${Formatters.fmt1(entry.record.fuelLiters)} L" +
                        if (entry.record.costYen > 0) "・¥${Formatters.money(entry.record.costYen)}" else "",
                    fontSize = 11.sp,
                    color = Color(0xFF7A828F)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.economyKmPerLiter?.let { Formatters.fmt1(it) } ?: "--.-",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Text("km/L", fontSize = 9.sp, color = Color(0xFF7A828F))
            }
        }
    }
}
