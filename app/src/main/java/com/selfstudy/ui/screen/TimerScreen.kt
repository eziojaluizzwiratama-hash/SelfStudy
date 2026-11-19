package com.selfstudy.ui.screen

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.selfstudy.notification.NotificationHelper
import com.selfstudy.timer.TimerService
import com.selfstudy.ui.theme.*
import com.selfstudy.ui.viewmodel.StudyMethod
import com.selfstudy.ui.viewmodel.TimerState
import com.selfstudy.ui.viewmodel.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    viewModel: TimerViewModel = viewModel()
) {
    val context = LocalContext.current
    val timerState by viewModel.timerState.collectAsState()
    val remainingMillis by viewModel.remainingMillis.collectAsState()
    val totalSessionMillis by viewModel.totalSessionMillis.collectAsState()
    val currentCycle by viewModel.currentCycle.collectAsState()
    val totalCycles by viewModel.totalCycles.collectAsState()
    val selectedMethod by viewModel.selectedMethod.collectAsState()
    val studyMinutes by viewModel.studyMinutes.collectAsState()
    val breakMinutes by viewModel.breakMinutes.collectAsState()
    val totalStudiedMillis by viewModel.totalStudiedMillis.collectAsState()

    // Setup timer phase complete callback
    LaunchedEffect(Unit) {
        viewModel.onTimerPhaseComplete = { phase ->
            val (title, message) = when (phase) {
                TimerState.STUDYING -> "Istirahat!" to "Waktu belajar selesai. Saatnya istirahat sejenak."
                TimerState.BREAK -> "Lanjut Belajar!" to "Istirahat selesai. Ayo lanjut belajar!"
                else -> "Timer" to "Phase complete"
            }
            NotificationHelper.showTimerNotification(context, title, message)
        }
    }

    // Update foreground service
    LaunchedEffect(timerState, remainingMillis) {
        when (timerState) {
            TimerState.STUDYING, TimerState.BREAK -> {
                val phaseText = when (timerState) {
                    TimerState.STUDYING -> "Belajar"
                    TimerState.BREAK -> "Istirahat"
                    else -> ""
                }
                try {
                    val intent = Intent(context, TimerService::class.java).apply {
                        action = TimerService.ACTION_UPDATE
                        putExtra("phase", phaseText)
                        putExtra("remaining", formatTime(remainingMillis))
                        putExtra("cycle", currentCycle)
                        putExtra("totalCycles", totalCycles)
                    }
                    context.startService(intent)
                } catch (_: Exception) {}
            }
            TimerState.IDLE, TimerState.FINISHED -> {
                try {
                    val intent = Intent(context, TimerService::class.java).apply {
                        action = TimerService.ACTION_STOP
                    }
                    context.startService(intent)
                } catch (_: Exception) {}
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Timer Belajar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Method selector (only when idle)
            if (timerState == TimerState.IDLE || timerState == TimerState.FINISHED) {
                item {
                    Text(
                        "Metode Belajar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(TimerViewModel.STUDY_METHODS) { method ->
                            MethodCard(
                                method = method,
                                isSelected = selectedMethod.id == method.id,
                                onClick = { viewModel.selectMethod(method) }
                            )
                        }
                    }
                }

                // Duration settings
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Pengaturan Durasi",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            DurationSetting(
                                label = "Belajar",
                                value = studyMinutes,
                                unit = "menit",
                                onDecrease = { viewModel.setStudyMinutes(studyMinutes - 5) },
                                onIncrease = { viewModel.setStudyMinutes(studyMinutes + 5) },
                                enabled = selectedMethod.id == "custom"
                            )
                            DurationSetting(
                                label = "Istirahat",
                                value = breakMinutes,
                                unit = "menit",
                                onDecrease = { viewModel.setBreakMinutes(breakMinutes - 1) },
                                onIncrease = { viewModel.setBreakMinutes(breakMinutes + 1) },
                                enabled = selectedMethod.id == "custom"
                            )
                            DurationSetting(
                                label = "Siklus",
                                value = totalCycles,
                                unit = "kali",
                                onDecrease = { viewModel.setTotalCycles(totalCycles - 1) },
                                onIncrease = { viewModel.setTotalCycles(totalCycles + 1) },
                                enabled = selectedMethod.id == "custom"
                            )
                        }
                    }
                }
            }

            // Timer display
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TimerCircle(
                            remainingMillis = remainingMillis,
                            totalMillis = totalSessionMillis,
                            timerState = timerState,
                            currentCycle = currentCycle,
                            totalCycles = totalCycles
                        )
                    }
                }
            }

            // Session info
            if (timerState != TimerState.IDLE) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$currentCycle/$totalCycles",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Siklus",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                formatTime(totalStudiedMillis),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                "Total Belajar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                selectedMethod.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                "Metode",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Control buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (timerState) {
                        TimerState.IDLE -> {
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(context, TimerService::class.java).apply {
                                            action = TimerService.ACTION_START
                                            putExtra("method", selectedMethod.name)
                                            putExtra("phase", "Belajar")
                                        }
                                        context.startForegroundService(intent)
                                    } catch (_: Exception) {}
                                    viewModel.startTimer()
                                },
                                modifier = Modifier
                                    .height(56.dp)
                                    .fillMaxWidth(0.6f),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mulai Belajar", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        TimerState.STUDYING, TimerState.BREAK -> {
                            // Skip button
                            OutlinedIconButton(
                                onClick = { viewModel.skipPhase() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.SkipNext, "Lewati")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            // Pause button
                            FilledIconButton(
                                onClick = { viewModel.pauseTimerWithState() },
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Filled.Pause, "Jeda", modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            // Stop button
                            OutlinedIconButton(
                                onClick = { viewModel.stopTimer() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Stop, "Berhenti")
                            }
                        }
                        TimerState.PAUSED -> {
                            // Resume button
                            FilledIconButton(
                                onClick = { viewModel.resumeTimer() },
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Filled.PlayArrow, "Lanjut", modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            // Stop button
                            OutlinedIconButton(
                                onClick = { viewModel.stopTimer() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Stop, "Berhenti")
                            }
                        }
                        TimerState.FINISHED -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("🎉", fontSize = 48.sp)
                                Text(
                                    "Sesi Selesai!",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Total belajar: ${formatTime(totalStudiedMillis)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { viewModel.resetTimer() },
                                    shape = RoundedCornerShape(28.dp)
                                ) {
                                    Text("Mulai Lagi")
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TimerCircle(
    remainingMillis: Long,
    totalMillis: Long,
    timerState: TimerState,
    currentCycle: Int,
    totalCycles: Int
) {
    val progress = if (totalMillis > 0) {
        1f - (remainingMillis.toFloat() / totalMillis.toFloat())
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(100),
        label = "progress"
    )

    val phaseColor = when (timerState) {
        TimerState.STUDYING -> MaterialTheme.colorScheme.primary
        TimerState.BREAK -> MaterialTheme.colorScheme.tertiary
        TimerState.PAUSED -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.primary
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    val phaseText = when (timerState) {
        TimerState.STUDYING -> "Belajar"
        TimerState.BREAK -> "Istirahat"
        TimerState.PAUSED -> "Dijeda"
        TimerState.FINISHED -> "Selesai"
        TimerState.IDLE -> "Siap"
    }

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val strokeWidth = 10.dp.toPx()
            val padding = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

            // Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(padding, padding),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress
            if (timerState != TimerState.IDLE) {
                drawArc(
                    color = phaseColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(padding, padding),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                phaseText,
                style = MaterialTheme.typography.labelLarge,
                color = phaseColor
            )
            Text(
                formatTime(remainingMillis),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (timerState != TimerState.IDLE && timerState != TimerState.FINISHED) {
                Text(
                    "Siklus $currentCycle/$totalCycles",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MethodCard(
    method: StudyMethod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                method.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${method.studyMinutes}/${method.breakMinutes} menit",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${method.cycles} siklus",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DurationSetting(
    label: String,
    value: Int,
    unit: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onDecrease,
                enabled = enabled,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Remove,
                    "Kurangi",
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                "$value $unit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(80.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(
                onClick = onIncrease,
                enabled = enabled,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    "Tambah",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
