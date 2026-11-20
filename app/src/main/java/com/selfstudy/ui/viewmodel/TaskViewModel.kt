package com.selfstudy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.selfstudy.SelfStudyApp
import com.selfstudy.data.model.Task
import com.selfstudy.widget.SelfStudyWidgetProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TaskFormState(
    val title: String = "",
    val description: String = "",
    val subject: String = "",
    val dueDate: Long = System.currentTimeMillis(),
    val priority: Int = 0,
    val reminderDays: List<Int> = listOf(3),
    val category: String = "tugas",
    val isEditing: Boolean = false,
    val editingTaskId: Long? = null
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SelfStudyApp
    private val taskRepo = app.taskRepository

    val allTasks: StateFlow<List<Task>> = taskRepo.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTasks: StateFlow<List<Task>> = taskRepo.getActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _formState = MutableStateFlow(TaskFormState())
    val formState: StateFlow<TaskFormState> = _formState.asStateFlow()

    private val _showForm = MutableStateFlow(false)
    val showForm: StateFlow<Boolean> = _showForm.asStateFlow()

    fun showAddForm() {
        _formState.value = TaskFormState()
        _showForm.value = true
    }

    fun showEditForm(task: Task) {
        _formState.value = TaskFormState(
            title = task.title,
            description = task.description,
            subject = task.subject,
            dueDate = task.dueDate,
            priority = task.priority,
            reminderDays = task.reminderDays.split(",").mapNotNull { it.trim().toIntOrNull() },
            category = task.category,
            isEditing = true,
            editingTaskId = task.id
        )
        _showForm.value = true
    }

    fun hideForm() {
        _showForm.value = false
        _formState.value = TaskFormState()
    }

    fun updateTitle(title: String) { _formState.update { it.copy(title = title) } }
    fun updateDescription(desc: String) { _formState.update { it.copy(description = desc) } }
    fun updateSubject(subject: String) { _formState.update { it.copy(subject = subject) } }
    fun updateDueDate(date: Long) { _formState.update { it.copy(dueDate = date) } }
    fun updatePriority(priority: Int) { _formState.update { it.copy(priority = priority) } }
    fun updateCategory(category: String) { _formState.update { it.copy(category = category) } }
    fun updateReminderDays(days: List<Int>) { _formState.update { it.copy(reminderDays = days) } }

    fun saveTask(onSaved: (Long) -> Unit = {}) {
        val state = _formState.value
        if (state.title.isBlank()) return

        viewModelScope.launch {
            val task = Task(
                id = state.editingTaskId ?: 0,
                title = state.title.trim(),
                description = state.description.trim(),
                subject = state.subject.trim(),
                dueDate = state.dueDate,
                priority = state.priority,
                reminderDays = state.reminderDays.joinToString(","),
                category = state.category
            )
            val id = if (state.isEditing) {
                taskRepo.updateTask(task)
                task.id
            } else {
                taskRepo.insertTask(task)
            }
            hideForm()
            SelfStudyWidgetProvider.updateWidget(app)
            onSaved(id)
        }
    }

    fun toggleTaskCompleted(task: Task) {
        viewModelScope.launch {
            val newCompleted = !task.isCompleted
            taskRepo.setTaskCompleted(
                task.id,
                newCompleted,
                if (newCompleted) System.currentTimeMillis() else null
            )
            SelfStudyWidgetProvider.updateWidget(app)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepo.deleteTask(task)
            SelfStudyWidgetProvider.updateWidget(app)
        }
    }
}
