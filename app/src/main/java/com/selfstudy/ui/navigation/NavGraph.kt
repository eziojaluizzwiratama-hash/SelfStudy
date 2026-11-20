package com.selfstudy.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String) {
    data object Splash : Screen("splash", "Splash")
    data object Main : Screen("main", "Utama")
    data object Dashboard : Screen("dashboard", "Beranda")
    data object Schedule : Screen("schedule", "Jadwal")
    data object Timer : Screen("timer", "Timer")
    data object Settings : Screen("settings", "Pengaturan")
}

data class BottomNavItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    BottomNavItem(Screen.Schedule, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem(Screen.Timer, Icons.Filled.Timer, Icons.Outlined.Timer)
)
