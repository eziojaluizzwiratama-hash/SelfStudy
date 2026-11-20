package com.selfstudy.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.selfstudy.ui.navigation.Screen
import com.selfstudy.ui.navigation.bottomNavItems
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    navController: NavController,
    initialPageIndex: Int = 0
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex.coerceIn(0, 2),
        pageCount = { 3 }
    )

    var scheduleInitialTab by remember { mutableIntStateOf(0) }
    var scheduleShowCompleted by remember { mutableStateOf(false) }

    // Jika pengguna sedang tidak di halaman Beranda (halaman 0), tombol Back kembali ke Beranda
    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                bottomNavItems.forEachIndexed { index, item ->
                    val selected = pagerState.currentPage == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (pagerState.currentPage != index) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
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
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            verticalAlignment = Alignment.Top
        ) { page ->
            when (page) {
                0 -> DashboardScreen(
                    onNavigateToSchedule = {
                        scheduleInitialTab = 0
                        scheduleShowCompleted = false
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    onNavigateToTasks = { showCompleted ->
                        scheduleInitialTab = 1
                        scheduleShowCompleted = showCompleted
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    onNavigateToTimer = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(2)
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
                1 -> ScheduleScreen(
                    initialTab = scheduleInitialTab,
                    showCompletedTasks = scheduleShowCompleted
                )
                2 -> TimerScreen()
            }
        }
    }
}
