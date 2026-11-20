package com.selfstudy.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.selfstudy.data.model.Schedule
import com.selfstudy.data.model.Task
import com.selfstudy.notification.NotificationHelper
import com.selfstudy.ui.theme.*
import com.selfstudy.ui.viewmodel.ScheduleViewModel
import com.selfstudy.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    initialTab: Int = 0,
    showCompletedTasks: Boolean = false,
    scheduleViewModel: ScheduleViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel()
) {
    val context = LocalContext.current
    val selectedDate by scheduleViewModel.selectedDate.collectAsState()
    val schedulesForDate by scheduleViewModel.schedulesForSelectedDate.collectAsState()
    val allSchedules by scheduleViewModel.allSchedules.collectAsState()
    val showScheduleForm by scheduleViewModel.showForm.collectAsState()
    val showTaskForm by taskViewModel.showForm.collectAsState()
    val activeTasks by taskViewModel.activeTasks.collectAsState()
    val allTasks by taskViewModel.allTasks.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val tabPagerState = rememberPagerState(
        initialPage = initialTab.coerceIn(0, 1),
        pageCount = { 2 }
    )

    LaunchedEffect(initialTab) {
        val target = initialTab.coerceIn(0, 1)
        if (tabPagerState.currentPage != target) {
            tabPagerState.animateScrollToPage(target)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Jadwal & Tugas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (tabPagerState.currentPage == 0) scheduleViewModel.showAddForm()
                    else taskViewModel.showAddForm()
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, "Tambah", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = tabPagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = tabPagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch {
                            tabPagerState.animateScrollToPage(0)
                        }
                    },
                    text = { Text("Jadwal") },
                    icon = { Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = tabPagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch {
                            tabPagerState.animateScrollToPage(1)
                        }
                    },
                    text = { Text("Tugas") },
                    icon = { Icon(Icons.Outlined.Assignment, null, modifier = Modifier.size(18.dp)) }
                )
            }

            HorizontalPager(
                state = tabPagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.Top
            ) { page ->
                when (page) {
                    0 -> ScheduleTab(
                        selectedDate = selectedDate,
                        schedules = schedulesForDate,
                        allSchedules = allSchedules,
                        onDateSelected = { scheduleViewModel.selectDate(it) },
                        onToggle = { scheduleViewModel.toggleScheduleCompleted(it) },
                        onEdit = { scheduleViewModel.showEditForm(it) },
                        onDelete = { scheduleViewModel.deleteSchedule(it) }
                    )
                    1 -> TaskTab(
                        tasks = if (showCompletedTasks) allTasks.filter { it.isCompleted } else activeTasks,
                        showingCompleted = showCompletedTasks,
                        onToggle = { taskViewModel.toggleTaskCompleted(it) },
                        onEdit = { taskViewModel.showEditForm(it) },
                        onDelete = { taskViewModel.deleteTask(it) }
                    )
                }
            }
        }

        // Schedule Form Dialog
        if (showScheduleForm) {
            ScheduleFormDialog(
                viewModel = scheduleViewModel,
                onDismiss = { scheduleViewModel.hideForm() },
                onSave = {
                    scheduleViewModel.saveSchedule { id ->
                        val form = scheduleViewModel.formState.value
                        NotificationHelper.scheduleScheduleReminders(
                            context, id, form.title, form.date, form.reminderDays.joinToString(",")
                        )
                    }
                }
            )
        }

        // Task Form Dialog
        if (showTaskForm) {
            TaskFormDialog(
                viewModel = taskViewModel,
                onDismiss = { taskViewModel.hideForm() },
                onSave = {
                    taskViewModel.saveTask { id ->
                        val form = taskViewModel.formState.value
                        NotificationHelper.scheduleTaskReminders(
                            context, id, form.title, form.dueDate, form.reminderDays.joinToString(",")
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun ScheduleTab(
    selectedDate: Long,
    schedules: List<Schedule>,
    allSchedules: List<Schedule>,
    onDateSelected: (Long) -> Unit,
    onToggle: (Schedule) -> Unit,
    onEdit: (Schedule) -> Unit,
    onDelete: (Schedule) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Date selector
        WeekDateSelector(
            selectedDate = selectedDate,
            schedules = allSchedules,
            onDateSelected = onDateSelected
        )

        if (schedules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Tidak ada jadwal untuk hari ini",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(schedules, key = { it.id }) { schedule ->
                    ScheduleItem(
                        schedule = schedule,
                        onToggle = { onToggle(schedule) },
                        onEdit = { onEdit(schedule) },
                        onDelete = { onDelete(schedule) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekDateSelector(
    selectedDate: Long,
    schedules: List<Schedule>,
    onDateSelected: (Long) -> Unit
) {
    val calendar = remember { Calendar.getInstance() }
    val dates = remember {
        val list = mutableListOf<Long>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3)
        repeat(14) {
            list.add(cal.timeInMillis)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val dayFormat = remember { SimpleDateFormat("EEE", Locale("id", "ID")) }
    val dateFormat = remember { SimpleDateFormat("dd", Locale("id", "ID")) }
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale("id", "ID")) }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            monthFormat.format(Date(selectedDate)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(dates) { dateMillis ->
                val shownDay = Calendar.getInstance().apply { timeInMillis = dateMillis }
                val hasSchedule = schedules.any { schedule ->
                    val scheduleDay = Calendar.getInstance().apply { timeInMillis = schedule.date }
                    scheduleDay.get(Calendar.YEAR) == shownDay.get(Calendar.YEAR) &&
                            scheduleDay.get(Calendar.DAY_OF_YEAR) == shownDay.get(Calendar.DAY_OF_YEAR)
                }
                val isSelected = run {
                    val selCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                    val dateCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                    selCal.get(Calendar.YEAR) == dateCal.get(Calendar.YEAR) &&
                            selCal.get(Calendar.DAY_OF_YEAR) == dateCal.get(Calendar.DAY_OF_YEAR)
                }
                val isToday = run {
                    val todayCal = Calendar.getInstance()
                    val dateCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                    todayCal.get(Calendar.YEAR) == dateCal.get(Calendar.YEAR) &&
                            todayCal.get(Calendar.DAY_OF_YEAR) == dateCal.get(Calendar.DAY_OF_YEAR)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .width(58.dp)
                        .height(112.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .clickable { onDateSelected(dateMillis) }
                        .padding(horizontal = 6.dp, vertical = 12.dp)
                ) {
                    Text(
                        dayFormat.format(Date(dateMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        dateFormat.format(Date(dateMillis)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasSchedule) {
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.primary
                                } else androidx.compose.ui.graphics.Color.Transparent
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleItem(
    schedule: Schedule,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val typeColor = when (schedule.type) {
        "tugas" -> CategoryTugas
        "ujian" -> CategoryUjian
        else -> CategoryBelajar
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(typeColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    schedule.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (schedule.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        scheduleTimeLabel(schedule.startTime, schedule.endTime),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (schedule.subject.isNotBlank()) {
                        Text(
                            " • ${schedule.subject}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Checkbox(
                checked = schedule.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = typeColor,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.MoreVert, "Opsi", modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { showMenu = false; onEdit() },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus") },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null) }
                    )
                }
            }
        }
    }
}

private fun scheduleTimeLabel(startTime: String, endTime: String): String = when {
    startTime.isNotBlank() && endTime.isNotBlank() -> "$startTime - $endTime"
    startTime.isNotBlank() -> "Mulai $startTime"
    endTime.isNotBlank() -> "Selesai $endTime"
    else -> "Waktu fleksibel"
}

@Composable
private fun TaskTab(
    tasks: List<Task>,
    showingCompleted: Boolean,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onDelete: (Task) -> Unit
) {
    if (tasks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (showingCompleted) Icons.Outlined.AssignmentLate else Icons.Outlined.CheckCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    if (showingCompleted) "Belum ada tugas selesai" else "Semua tugas sudah selesai!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskItemFull(
                    task = task,
                    onToggle = { onToggle(task) },
                    onEdit = { onEdit(task) },
                    onDelete = { onDelete(task) }
                )
            }
        }
    }
}

@Composable
private fun TaskItemFull(
    task: Task,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
    val daysLeft = remember(task.dueDate) {
        val diff = task.dueDate - System.currentTimeMillis()
        (diff / (24 * 60 * 60 * 1000)).toInt()
    }
    val priorityColor = when (task.priority) {
        2 -> PriorityHigh
        1 -> PriorityMedium
        else -> PriorityNormal
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
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
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (task.description.isNotBlank()) {
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Priority badge
                    val priorityText = when (task.priority) {
                        2 -> "Tinggi"
                        1 -> "Sedang"
                        else -> "Normal"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(priorityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            priorityText,
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor
                        )
                    }

                    if (task.subject.isNotBlank()) {
                        Text(
                            task.subject,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        dateFormat.format(Date(task.dueDate)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!task.isCompleted) {
                        val urgencyColor = when {
                            daysLeft < 0 -> PriorityHigh
                            daysLeft <= 1 -> PriorityHigh
                            daysLeft <= 3 -> PriorityMedium
                            else -> PriorityNormal
                        }
                        val urgencyText = when {
                            daysLeft < 0 -> "Terlambat!"
                            daysLeft == 0 -> "Hari ini"
                            daysLeft == 1 -> "Besok"
                            else -> "H-${daysLeft}"
                        }
                        Text(
                            urgencyText,
                            style = MaterialTheme.typography.labelSmall,
                            color = urgencyColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.MoreVert, "Opsi", modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { showMenu = false; onEdit() },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus") },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleFormDialog(
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val formState by viewModel.formState.collectAsState()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = formState.date)
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePickerFor by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (formState.isEditing) "Edit Jadwal" else "Tambah Jadwal")
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = formState.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        label = { Text("Judul") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                }
                item {
                    OutlinedTextField(
                        value = formState.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        label = { Text("Deskripsi (opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                }
                item {
                    OutlinedTextField(
                        value = formState.subject,
                        onValueChange = { viewModel.updateSubject(it) },
                        label = { Text("Mata Pelajaran (opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )
                }
                item {
                    // Type selector
                    Text("Tipe", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("belajar" to "Belajar", "tugas" to "Tugas", "ujian" to "Ujian").forEach { (value, label) ->
                            FilterChip(
                                selected = formState.type == value,
                                onClick = { viewModel.updateType(value) },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
                item {
                    // Date
                    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")) }
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                dateFormat.format(Date(formState.date)),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                item {
                    // Times are optional and selected with the clock picker.
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimePickerField(
                            label = "Mulai",
                            value = formState.startTime,
                            modifier = Modifier.weight(1f),
                            onClick = { showTimePickerFor = "start" },
                            onClear = { viewModel.updateStartTime("") }
                        )
                        TimePickerField(
                            label = "Selesai",
                            value = formState.endTime,
                            modifier = Modifier.weight(1f),
                            onClick = { showTimePickerFor = "end" },
                            onClear = { viewModel.updateEndTime("") }
                        )
                    }
                }
                item {
                    Text(
                        "Waktu boleh dikosongkan jika jadwal tidak terikat jam tertentu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    // Reminder selector
                    Text("Pengingat", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(7 to "H-7", 3 to "H-3", 1 to "H-1").forEach { (days, label) ->
                            FilterChip(
                                selected = days in formState.reminderDays,
                                onClick = {
                                    val current = formState.reminderDays.toMutableList()
                                    if (days in current) current.remove(days) else current.add(days)
                                    viewModel.updateReminderDays(current)
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = formState.title.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.updateDate(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    showTimePickerFor?.let { field ->
        val currentTime = if (field == "start") formState.startTime else formState.endTime
        val timeParts = currentTime.split(":")
        var selectedHour by remember(field, currentTime) {
            mutableIntStateOf(timeParts.getOrNull(0)?.toIntOrNull() ?: 0)
        }
        var selectedMinute by remember(field, currentTime) {
            mutableIntStateOf(timeParts.getOrNull(1)?.toIntOrNull() ?: 0)
        }
        AlertDialog(
            onDismissRequest = { showTimePickerFor = null },
            title = { Text(if (field == "start") "Pilih waktu mulai" else "Pilih waktu selesai") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Pilih jam dan menit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeWheelColumn(
                            label = "Jam",
                            count = 24,
                            selectedValue = selectedHour,
                            modifier = Modifier.weight(1f),
                            onSelected = { selectedHour = it }
                        )
                        Text(":", style = MaterialTheme.typography.headlineMedium)
                        TimeWheelColumn(
                            label = "Menit",
                            count = 60,
                            selectedValue = selectedMinute,
                            modifier = Modifier.weight(1f),
                            onSelected = { selectedMinute = it }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "%02d:%02d".format(selectedHour, selectedMinute),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val time = "%02d:%02d".format(selectedHour, selectedMinute)
                    if (field == "start") viewModel.updateStartTime(time)
                    else viewModel.updateEndTime(time)
                    showTimePickerFor = null
                }) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerFor = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun TimePickerField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    OutlinedCard(
        modifier = modifier
            .height(64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    value.ifBlank { "Opsional" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
            if (value.isBlank()) {
                Icon(Icons.Outlined.AccessTime, contentDescription = "Pilih waktu")
            } else {
                IconButton(onClick = onClear) {
                    Icon(Icons.Outlined.Close, contentDescription = "Hapus waktu")
                }
            }
        }
    }
}

@Composable
private fun TimeWheelColumn(
    label: String,
    count: Int,
    selectedValue: Int,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedValue - 2).coerceAtLeast(0)
    )
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 4.dp)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().height(216.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(count) { value ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (value == selectedValue) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { onSelected(value) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "%02d".format(value),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (value == selectedValue) FontWeight.Bold else FontWeight.Normal,
                        color = if (value == selectedValue) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskFormDialog(
    viewModel: TaskViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val formState by viewModel.formState.collectAsState()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = formState.dueDate)
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (formState.isEditing) "Edit Tugas" else "Tambah Tugas")
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = formState.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        label = { Text("Judul Tugas") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                }
                item {
                    OutlinedTextField(
                        value = formState.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        label = { Text("Deskripsi (opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                }
                item {
                    OutlinedTextField(
                        value = formState.subject,
                        onValueChange = { viewModel.updateSubject(it) },
                        label = { Text("Mata Pelajaran (opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )
                }
                item {
                    // Category selector
                    Text("Kategori", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("tugas" to "Tugas", "ujian" to "Ujian", "belajar" to "Belajar").forEach { (value, label) ->
                            FilterChip(
                                selected = formState.category == value,
                                onClick = { viewModel.updateCategory(value) },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
                item {
                    // Priority
                    Text("Prioritas", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Normal", 1 to "Sedang", 2 to "Tinggi").forEach { (value, label) ->
                            FilterChip(
                                selected = formState.priority == value,
                                onClick = { viewModel.updatePriority(value) },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
                item {
                    // Due date
                    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")) }
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Deadline: ${dateFormat.format(Date(formState.dueDate))}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                item {
                    // Reminder selector
                    Text("Pengingat", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(7 to "H-7", 3 to "H-3", 1 to "H-1").forEach { (days, label) ->
                            FilterChip(
                                selected = days in formState.reminderDays,
                                onClick = {
                                    val current = formState.reminderDays.toMutableList()
                                    if (days in current) current.remove(days) else current.add(days)
                                    viewModel.updateReminderDays(current)
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = formState.title.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.updateDueDate(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
