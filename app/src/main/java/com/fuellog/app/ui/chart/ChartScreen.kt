package com.fuellog.app.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.StrokeCap
import androidx.compose.ui.graphics.drawscope.StrokeJoin
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuellog.app.data.FuelEntry
import com.fuellog.app.ui.RecordsViewModel
import com.fuellog.app.util.Formatters
import com.fuellog.app.util.toLocalDateOrNull
import java.time.LocalDate

@Composable
fun ChartScreen(viewModel: RecordsViewModel) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    var period by rememberSaveable { mutableStateOf(1) }
    val periodLabels = listOf("1か月", "6か月", "1年", "全期間")

    val today = LocalDate.now()
    val filtered = entries.filter { e ->
        if (e.economyKmPerLiter == null) return@filter false
        val d = e.record.date.toLocalDateOrNull() ?: return@filter false
        when (period) {
            0 -> !d.isBefore(today.minusMonths(1))
            1 -> !d.isBefore(today.minusMonths(6))
            2 -> !d.isBefore(today.minusMonths(12))
            else -> true
        }
    }
    val values = filtered.map { it.economyKmPerLiter!! }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("📈 燃費の推移", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C2B21))
        Spacer(modifier = Modifier.height(12.dp))

        TabRow(selectedTabIndex = period) {
            periodLabels.forEachIndexed { index, label ->
                Tab(
                    selected = period == index,
                    onClick = { period = index },
                    text = { Text(label, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("燃費 (km/L)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A5560))
                if (values.isEmpty()) {
                    Text(
                        "この期間のデータがありません",
                        fontSize = 12.sp,
                        color = Color(0xFF7A828F),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)
                    )
                } else {
                    FuelEconomyChart(filtered)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    LegendLine(Color(0xFF2E7D32), "燃費")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendLine(
                        Color(0xFFFF9800),
                        "平均" + if (values.isNotEmpty()) " " + Formatters.fmt1(values.average()) else ""
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("最高燃費", values.maxOrNull()?.let { Formatters.fmt1(it) } ?: "—", Modifier.weight(1f))
            StatBox(
                "平均",
                if (values.isNotEmpty()) Formatters.fmt1(values.average()) else "—",
                Modifier.weight(1f)
            )
            StatBox("最低燃費", values.minOrNull()?.let { Formatters.fmt1(it) } ?: "—", Modifier.weight(1f))
        }
    }
}

@Composable
private fun LegendLine(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(label, fontSize = 10.sp, color = Color(0xFF7A828F), modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.sp, color = Color(0xFF7A828F))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C2B21))
                Text(" km/L", fontSize = 9.sp, color = Color(0xFF7A828F))
            }
        }
    }
}

@Composable
private fun FuelEconomyChart(entries: List<FuelEntry>) {
    val values = entries.map { it.economyKmPerLiter!! }
    val avg = values.average()
    val maxV = values.max()
    val minV = values.min()
    val range = (maxV - minV).coerceAtLeast(1.0)
    val top = maxV + range * 0.2
    val bottom = (minV - range * 0.25).coerceAtLeast(0.0)

    val green = Color(0xFF2E7D32)
    val orange = Color(0xFFFF9800)
    val gridColor = Color(0xFFEEF1F4)

    val labelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#9AA3AD")
            textSize = 26f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val valuePaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#2E7D32")
            textSize = 28f
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
            isFakeBoldText = true
        }
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(200.dp).padding(top = 12.dp)) {
        val labelSpace = 34f
        val w = size.width
        val h = size.height - labelSpace
        val n = values.size

        fun x(i: Int): Float = if (n == 1) w / 2f else 24f + (w - 48f) * i / (n - 1)
        fun y(v: Double): Float = (h * (1 - (v - bottom) / (top - bottom))).toFloat()

        for (g in 1..3) {
            val gy = h * g / 4f
            drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1.dp.toPx())
        }

        drawLine(
            orange,
            Offset(0f, y(avg)),
            Offset(w, y(avg)),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
        )

        val path = Path()
        values.forEachIndexed { i, v ->
            if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v))
        }
        drawPath(
            path,
            green,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        values.forEachIndexed { i, v ->
            drawCircle(green, radius = 3.5.dp.toPx(), center = Offset(x(i), y(v)))
        }

        val lastY = (y(values.last()) - 14f).coerceAtLeast(26f)
        drawContext.canvas.nativeCanvas.drawText(
            Formatters.fmt1(values.last()),
            x(n - 1) - 10f,
            lastY,
            valuePaint
        )

        val indices = if (n <= 6) (0 until n).toList() else (0..5).map { it * (n - 1) / 5 }.distinct()
        indices.forEach { i ->
            val d = entries[i].record.date
            val label = if (d.length >= 10) d.substring(5).replace('-', '/') else d
            drawContext.canvas.nativeCanvas.drawText(label, x(i), h + 28f, labelPaint)
        }
    }
}
