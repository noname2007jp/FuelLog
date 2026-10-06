package com.fuellog.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fuellog.app.FuelLogApp
import com.fuellog.app.ui.RecordsViewModel
import com.fuellog.app.ui.capture.CaptureScreen
import com.fuellog.app.ui.capture.CaptureViewModel
import com.fuellog.app.ui.chart.ChartScreen
import com.fuellog.app.ui.edit.EditRecordScreen
import com.fuellog.app.ui.history.HistoryScreen
import com.fuellog.app.ui.home.HomeScreen
import com.fuellog.app.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val CHART = "chart"
    const val SETTINGS = "settings"
    const val CAPTURE = "capture"
    const val EDIT = "edit?recordId={recordId}"
    fun edit(recordId: Long = -1L) = "edit?recordId=$recordId"
}

private data class Tab(val route: String, val label: String, val emoji: String)

private val tabs = listOf(
    Tab(Routes.HOME, "ホーム", "🏠"),
    Tab(Routes.HISTORY, "履歴", "📋"),
    Tab(Routes.CHART, "グラフ", "📈"),
    Tab(Routes.SETTINGS, "設定", "⚙️")
)

@Composable
fun FuelLogNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as FuelLogApp
    val recordsViewModel: RecordsViewModel = viewModel(factory = RecordsViewModel.factory(app.repository))
    val captureViewModel: CaptureViewModel = viewModel()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isMainTab = currentRoute in tabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (isMainTab) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(tab.emoji, fontSize = 20.sp) },
                            label = { Text(tab.label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == Routes.HOME) {
                FloatingActionButton(onClick = { navController.navigate(Routes.CAPTURE) }) {
                    Icon(Icons.Filled.Add, contentDescription = "記録を追加")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = recordsViewModel,
                    onRecordClick = { id -> navController.navigate(Routes.edit(id)) }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    viewModel = recordsViewModel,
                    onRecordClick = { id -> navController.navigate(Routes.edit(id)) }
                )
            }
            composable(Routes.CHART) {
                ChartScreen(viewModel = recordsViewModel)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(viewModel = recordsViewModel)
            }
            composable(Routes.CAPTURE) {
                CaptureScreen(
                    viewModel = captureViewModel,
                    onBack = { navController.popBackStack() },
                    onConfirm = { navController.navigate(Routes.edit()) }
                )
            }
            composable(
                route = Routes.EDIT,
                arguments = listOf(
                    navArgument("recordId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { entry ->
                EditRecordScreen(
                    recordId = entry.arguments?.getLong("recordId") ?: -1L,
                    captureViewModel = captureViewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onRetake = { navController.popBackStack() }
                )
            }
        }
    }
}
