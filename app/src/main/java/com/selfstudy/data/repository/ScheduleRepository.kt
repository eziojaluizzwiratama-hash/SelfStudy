package com.selfstudy.data.repository

import com.selfstudy.data.dao.ScheduleDao
import com.selfstudy.data.model.Schedule
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val scheduleDao: ScheduleDao) {
    fun getAllSchedules(): Flow<List<Schedule>> = scheduleDao.getAllSchedules()
    fun getActiveSchedules(): Flow<List<Schedule>> = scheduleDao.getActiveSchedules()
    fun getSchedulesByDate(start: Long, end: Long): Flow<List<Schedule>> = scheduleDao.getSchedulesByDate(start, end)
    suspend fun getScheduleById(id: Long): Schedule? = scheduleDao.getScheduleById(id)
    suspend fun insertSchedule(schedule: Schedule): Long = scheduleDao.insertSchedule(schedule)
    suspend fun updateSchedule(schedule: Schedule) = scheduleDao.updateSchedule(schedule)
    suspend fun deleteSchedule(schedule: Schedule) = scheduleDao.deleteSchedule(schedule)
    suspend fun setScheduleCompleted(id: Long, completed: Boolean) = scheduleDao.setScheduleCompleted(id, completed)
}
