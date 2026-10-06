package com.fuellog.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuellog.app.ui.RecordsViewModel

@Composable
fun SettingsScreen(viewModel: RecordsViewModel) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    var showDeleteAllDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("⚙️ 設定", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C2B21))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("アプリについて", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("FuelLog 1.0.0", fontSize = 12.sp, color = Color(0xFF7A828F))
                Text("記録件数: ${entries.size} 件", fontSize = 12.sp, color = Color(0xFF7A828F))
                Text(
                    "テキスト認識: Google ML Kit(オンデバイス処理のため画像は端末外に送信されません)",
                    fontSize = 11.sp,
                    color = Color(0xFF7A828F)
                )
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("燃費の計算方法", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "・トリップメーターの値を優先して使用します\n" +
                        "・トリップ未入力時はオドメーター(総走行距離)の差分から計算します\n" +
                        "・オドメーター値はメモとして全期間保存されます\n" +
                        "・燃費 = 走行距離 ÷ 給油量",
                    fontSize = 11.sp,
                    color = Color(0xFF4A5560)
                )
            }
        }

        OutlinedButton(
            onClick = { showDeleteAllDialog = true },
            enabled = entries.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("全データを削除", color = Color(0xFFC64A22)) }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("全データの削除") },
            text = { Text("すべての給油記録を削除します。この操作は元に戻せません。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAllDialog = false
                    viewModel.deleteAll()
                }) { Text("削除", color = Color(0xFFC64A22)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("キャンセル") }
            }
        )
    }
}
