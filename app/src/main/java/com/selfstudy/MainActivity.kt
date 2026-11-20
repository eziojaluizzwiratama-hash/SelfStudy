package com.selfstudy

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.selfstudy.ui.navigation.Screen
import com.selfstudy.ui.screen.MainScreen
import com.selfstudy.ui.screen.SettingsScreen
import com.selfstudy.ui.screen.SplashScreen
import com.selfstudy.ui.theme.SelfStudyTheme
import com.selfstudy.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private var targetPageFromIntent by mutableIntStateOf(0)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()

        targetPageFromIntent = intent?.getIntExtra("target_page", 0) ?: 0

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val darkModePreference by settingsViewModel.darkMode.collectAsState()
            val systemDark = isSystemInDarkTheme()

            val isDark = darkModePreference ?: systemDark

            SelfStudyTheme(darkTheme = isDark) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Splash.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Splash.route) {
                        SplashScreen(
                            onSplashFinished = {
                                settingsViewModel.markFirstLaunchDone()
                                navController.navigate(Screen.Main.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Screen.Main.route) {
                        MainScreen(
                            navController = navController,
                            initialPageIndex = targetPageFromIntent
                        )
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val target = intent.getIntExtra("target_page", -1)
        if (target != -1) {
            targetPageFromIntent = target
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
