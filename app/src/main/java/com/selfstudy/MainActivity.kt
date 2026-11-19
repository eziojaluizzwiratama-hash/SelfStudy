package com.selfstudy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.selfstudy.ui.navigation.Screen
import com.selfstudy.ui.navigation.bottomNavItems
import com.selfstudy.ui.screen.*
import com.selfstudy.ui.theme.SelfStudyTheme
import com.selfstudy.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val darkModePreference by settingsViewModel.darkMode.collectAsState()
            val systemDark = isSystemInDarkTheme()

            val isDark = darkModePreference ?: systemDark

            SelfStudyTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStack?.destination?.route
                val currentScreenRoute = currentRoute?.substringBefore("?")

                val showBottomBar = currentScreenRoute in listOf(
                    Screen.Dashboard.route,
                    Screen.Schedule.route,
                    Screen.Timer.route
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.background,
                                tonalElevation = 0.dp
                            ) {
                                bottomNavItems.forEach { item ->
                                    val selected = currentScreenRoute == item.screen.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            if (currentScreenRoute != item.screen.route) {
                                                navController.navigate(item.screen.route) {
                                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.screen.title
                                            )
                                        },
                                        label = { Text(item.screen.title) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Splash.route) {
                            SplashScreen(
                                onSplashFinished = {
                                    settingsViewModel.markFirstLaunchDone()
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                onNavigateToSchedule = {
                                    navController.navigate(Screen.Schedule.route) {
                                        popUpTo(Screen.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToTasks = { showCompleted ->
                                    navController.navigate(
                                        "schedule?tab=1&showCompletedTasks=$showCompleted"
                                    ) {
                                        popUpTo(Screen.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToTimer = {
                                    navController.navigate(Screen.Timer.route) {
                                        popUpTo(Screen.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToSettings = {
                                    navController.navigate(Screen.Settings.route)
                                }
                            )
                        }
                        composable(
                            route = "schedule?tab={tab}&showCompletedTasks={showCompletedTasks}",
                            arguments = listOf(
                                navArgument("tab") {
                                    type = NavType.IntType
                                    defaultValue = 0
                                },
                                navArgument("showCompletedTasks") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            )
                        ) { entry ->
                            ScheduleScreen(
                                initialTab = entry.arguments?.getInt("tab") ?: 0,
                                showCompletedTasks = entry.arguments?.getBoolean("showCompletedTasks") ?: false
                            )
                        }
                        composable(Screen.Timer.route) {
                            TimerScreen()
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
