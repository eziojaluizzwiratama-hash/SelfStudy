package com.selfstudy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.selfstudy.SelfStudyApp
import com.selfstudy.data.model.Schedule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class ScheduleFormState(
    val title: String = "",
    val description: String = "",
    val subject: String = "",
    val date: Long = System.currentTimeMillis(),
    val startTime: String = "",
    val endTime: String = "",
    val type: String = "belajar",
    val reminderDays: List<Int> = listOf(3),
    val isEditing: Boolean = false,
    val editingScheduleId: Long? = null
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SelfStudyApp
    private val scheduleRepo = app.scheduleRepository

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    val allSchedules: StateFlow<List<Schedule>> = scheduleRepo.getAllSchedules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val schedulesForSelectedDate: StateFlow<List<Schedule>> = _selectedDate.flatMapLatest { date ->
        val cal = Calendar.getInstance().apply { timeInMillis = date }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis
        scheduleRepo.getSchedulesByDate(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _formState = MutableStateFlow(ScheduleFormState())
    val formState: StateFlow<ScheduleFormState> = _formState.asStateFlow()

    private val _showForm = MutableStateFlow(false)
    val showForm: StateFlow<Boolean> = _showForm.asStateFlow()

    fun selectDate(dateMillis: Long) { _selectedDate.value = dateMillis }

    fun showAddForm() {
        _formState.value = ScheduleFormState(date = _selectedDate.value)
        _showForm.value = true
    }

    fun showEditForm(schedule: Schedule) {
        _formState.value = ScheduleFormState(
            title = schedule.title,
            description = schedule.description,
            subject = schedule.subject,
            date = schedule.date,
            startTime = schedule.startTime,
            endTime = schedule.endTime,
            type = schedule.type,
            reminderDays = schedule.reminderDays.split(",").mapNotNull { it.trim().toIntOrNull() },
            isEditing = true,
            editingScheduleId = schedule.id
        )
        _showForm.value = true
    }

    fun hideForm() {
        _showForm.value = false
        _formState.value = ScheduleFormState()
    }

    fun updateTitle(title: String) { _formState.update { it.copy(title = title) } }
    fun updateDescription(desc: String) { _formState.update { it.copy(description = desc) } }
    fun updateSubject(subject: String) { _formState.update { it.copy(subject = subject) } }
    fun updateDate(date: Long) { _formState.update { it.copy(date = date) } }
    fun updateStartTime(time: String) { _formState.update { it.copy(startTime = time) } }
    fun updateEndTime(time: String) { _formState.update { it.copy(endTime = time) } }
    fun updateType(type: String) { _formState.update { it.copy(type = type) } }
    fun updateReminderDays(days: List<Int>) { _formState.update { it.copy(reminderDays = days) } }

    fun saveSchedule(onSaved: (Long) -> Unit = {}) {
        val state = _formState.value
        if (state.title.isBlank()) return

        viewModelScope.launch {
            val schedule = Schedule(
                id = state.editingScheduleId ?: 0,
                title = state.title.trim(),
                description = state.description.trim(),
                subject = state.subject.trim(),
                date = state.date,
                startTime = state.startTime,
                endTime = state.endTime,
                type = state.type,
                reminderDays = state.reminderDays.joinToString(",")
            )
            val id = if (state.isEditing) {
                scheduleRepo.updateSchedule(schedule)
                schedule.id
            } else {
                scheduleRepo.insertSchedule(schedule)
            }
            hideForm()
            onSaved(id)
        }
    }

    fun toggleScheduleCompleted(schedule: Schedule) {
        viewModelScope.launch {
            scheduleRepo.setScheduleCompleted(schedule.id, !schedule.isCompleted)
        }
    }

    fun deleteSchedule(schedule: Schedule) {
        viewModelScope.launch { scheduleRepo.deleteSchedule(schedule) }
    }
}
