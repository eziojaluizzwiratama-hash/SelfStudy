package com.selfstudy.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.selfstudy.data.model.Task
import com.selfstudy.ui.theme.*
import com.selfstudy.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSchedule: () -> Unit,
    onNavigateToTasks: (showCompleted: Boolean) -> Unit,
    onNavigateToTimer: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val activeTaskCount by viewModel.activeTaskCount.collectAsState()
    val completedTaskCount by viewModel.completedTaskCount.collectAsState()
    val totalStudyTime by viewModel.totalStudyTime.collectAsState()
    val completedSessionCount by viewModel.completedSessionCount.collectAsState()
    val todayTasks by viewModel.todayTasks.collectAsState()
    val activeTasks by viewModel.activeTasks.collectAsState()
    val upcomingSchedules by viewModel.upcomingSchedules.collectAsState()

    val dateFormat = remember { SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")) }
    val today = remember { dateFormat.format(Date()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "SelfStudy",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            today,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Pengaturan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bento summary: one featured metric beside two compact counts.
            item {
                Text(
                    "Ringkasan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.fillMaxWidth().height(86.dp),
                            icon = Icons.Outlined.Assignment,
                            label = "Belum selesai",
                            value = activeTaskCount.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            compact = true,
                            onClick = { onNavigateToTasks(false) }
                        )
                        StatCard(
                            modifier = Modifier.fillMaxWidth().height(86.dp),
                            icon = Icons.Outlined.CheckCircle,
                            label = "Selesai",
                            value = completedTaskCount.toString(),
                            color = PriorityNormal,
                            compact = true,
                            onClick = { onNavigateToTasks(true) }
                        )
                    }
                    StatCard(
                        modifier = Modifier.weight(1f).height(184.dp),
                        icon = Icons.Outlined.Timer,
                        label = "Waktu Belajar",
                        value = formatStudyTime(totalStudyTime),
                        color = MaterialTheme.colorScheme.tertiary,
                        featured = true,
                        onClick = onNavigateToTimer
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.fillMaxWidth().height(82.dp),
                        icon = Icons.Outlined.School,
                        label = "Sesi Selesai",
                        value = completedSessionCount.toString(),
                        color = PriorityMedium,
                        compact = true,
                        onClick = onNavigateToTimer
                    )
                }
            }

            // Quick Actions
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Aksi Cepat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.fillMaxWidth().height(76.dp),
                        icon = Icons.Filled.Timer,
                        label = "Mulai Belajar",
                        onClick = onNavigateToTimer
                    )
                    QuickActionCard(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        icon = Icons.Filled.CalendarMonth,
                        label = "Atur Jadwal",
                        onClick = onNavigateToSchedule,
                        secondary = true
                    )
                }
            }

            // Upcoming schedules
            if (upcomingSchedules.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Jadwal Mendatang",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(upcomingSchedules.take(3), key = { it.id }) { schedule ->
                    val schedDateFormat = remember { SimpleDateFormat("dd MMM", Locale("id", "ID")) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val typeColor = when (schedule.type) {
                                "tugas" -> CategoryTugas
                                "ujian" -> CategoryUjian
                                else -> CategoryBelajar
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(typeColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    schedule.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${schedDateFormat.format(Date(schedule.date))} • ${dashboardScheduleTime(schedule.startTime, schedule.endTime)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val typeLabel = when (schedule.type) {
                                "tugas" -> "Tugas"
                                "ujian" -> "Ujian"
                                else -> "Belajar"
                            }
                            Text(
                                typeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = typeColor
                            )
                        }
                    }
                }
            }

            // Today's tasks
            if (todayTasks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tugas Hari Ini",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(todayTasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onToggle = { viewModel.toggleTaskCompleted(task) }
                    )
                }
            }

            // Active tasks
            if (activeTasks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Tugas Aktif",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = onNavigateToSchedule) {
                            Text("Lihat Semua")
                        }
                    }
                }
                items(activeTasks.take(5), key = { "active_${it.id}" }) { task ->
                    TaskItem(
                        task = task,
                        onToggle = { viewModel.toggleTaskCompleted(task) }
                    )
                }
            }

            // Empty state
            if (activeTasks.isEmpty() && todayTasks.isEmpty() && upcomingSchedules.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📚", style = MaterialTheme.typography.headlineLarge)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Belum ada tugas atau jadwal",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Mulai dengan menambah jadwal belajar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    compact: Boolean = false,
    featured: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (featured) MaterialTheme.colorScheme.primary
            else color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = if (featured) Arrangement.SpaceBetween else Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (featured) MaterialTheme.colorScheme.onPrimary else color,
                modifier = Modifier.size(if (compact) 17.dp else 21.dp)
            )
            Text(
                value,
                style = if (featured) MaterialTheme.typography.headlineMedium
                else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (featured) MaterialTheme.colorScheme.onPrimary else color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (featured) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    secondary: Boolean = false
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (secondary) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (secondary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(start = if (secondary) 4.dp else 2.dp)
                    .size(if (secondary) 18.dp else 24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                style = if (secondary) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (secondary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun TaskItem(
    task: Task,
    onToggle: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
    val daysLeft = remember(task.dueDate) {
        val diff = task.dueDate - System.currentTimeMillis()
        (diff / (24 * 60 * 60 * 1000)).toInt()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.subject.isNotBlank()) {
                        Text(
                            task.subject,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            " • ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        dateFormat.format(Date(task.dueDate)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!task.isCompleted) {
                val urgencyColor = when {
                    daysLeft < 0 -> PriorityHigh
                    daysLeft <= 1 -> PriorityHigh
                    daysLeft <= 3 -> PriorityMedium
                    else -> PriorityNormal
                }
                val urgencyText = when {
                    daysLeft < 0 -> "Terlambat"
                    daysLeft == 0 -> "Hari ini"
                    daysLeft == 1 -> "Besok"
                    else -> "${daysLeft} hari"
                }
                Text(
                    urgencyText,
                    style = MaterialTheme.typography.labelSmall,
                    color = urgencyColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatStudyTime(millis: Long): String {
    val totalMinutes = millis / 60000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}j ${minutes}m" else "${minutes}m"
}

private fun dashboardScheduleTime(startTime: String, endTime: String): String = when {
    startTime.isNotBlank() && endTime.isNotBlank() -> "$startTime - $endTime"
    startTime.isNotBlank() -> "Mulai $startTime"
    endTime.isNotBlank() -> "Selesai $endTime"
    else -> "Waktu fleksibel"
}
