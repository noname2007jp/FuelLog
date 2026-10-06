package com.fuellog.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FuelLogColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    secondary = Color(0xFF66BB6A),
    background = Color(0xFFF3F5F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFE4E9EC)
)

@Composable
fun FuelLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FuelLogColors, content = content)
}
