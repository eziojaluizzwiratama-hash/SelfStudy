package com.selfstudy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.selfstudy.SelfStudyApp
import com.selfstudy.data.model.Task
import com.selfstudy.data.model.Schedule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SelfStudyApp
    private val taskRepo = app.taskRepository
    private val scheduleRepo = app.scheduleRepository
    private val sessionRepo = app.studySessionRepository

    val activeTasks: StateFlow<List<Task>> = taskRepo.getActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedTasks: StateFlow<List<Task>> = taskRepo.getCompletedTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTaskCount: StateFlow<Int> = taskRepo.getActiveTaskCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedTaskCount: StateFlow<Int> = taskRepo.getCompletedTaskCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalStudyTime: StateFlow<Long> = sessionRepo.getTotalStudyTime()
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val completedSessionCount: StateFlow<Int> = sessionRepo.getCompletedSessionCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val upcomingSchedules: StateFlow<List<Schedule>> = scheduleRepo.getActiveSchedules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTasks: StateFlow<List<Task>> = run {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = cal.timeInMillis
        taskRepo.getTasksBetweenDates(startOfDay, endOfDay)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun toggleTaskCompleted(task: Task) {
        viewModelScope.launch {
            val newCompleted = !task.isCompleted
            taskRepo.setTaskCompleted(
                task.id,
                newCompleted,
                if (newCompleted) System.currentTimeMillis() else null
            )
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepo.deleteTask(task)
        }
    }
}
