package com.fuellog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fuellog.app.ui.navigation.FuelLogNavHost
import com.fuellog.app.ui.theme.FuelLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FuelLogTheme {
                FuelLogNavHost()
            }
        }
    }
}
