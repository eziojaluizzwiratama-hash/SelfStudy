package com.selfstudy.data.repository

import com.selfstudy.data.dao.TaskDao
import com.selfstudy.data.model.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()
    fun getActiveTasks(): Flow<List<Task>> = taskDao.getActiveTasks()
    fun getCompletedTasks(): Flow<List<Task>> = taskDao.getCompletedTasks()
    fun getActiveTaskCount(): Flow<Int> = taskDao.getActiveTaskCount()
    fun getCompletedTaskCount(): Flow<Int> = taskDao.getCompletedTaskCount()
    fun getTasksBetweenDates(start: Long, end: Long): Flow<List<Task>> = taskDao.getTasksBetweenDates(start, end)
    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)
    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    suspend fun setTaskCompleted(id: Long, completed: Boolean, completedAt: Long?) = taskDao.setTaskCompleted(id, completed, completedAt)
}
