package com.selfstudy.data.dao

import androidx.room.*
import com.selfstudy.data.model.Schedule
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY date ASC, startTime ASC")
    fun getAllSchedules(): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules WHERE isCompleted = 0 ORDER BY date ASC, startTime ASC")
    fun getActiveSchedules(): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules WHERE date BETWEEN :startDate AND :endDate ORDER BY startTime ASC")
    fun getSchedulesByDate(startDate: Long, endDate: Long): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): Schedule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: Schedule): Long

    @Update
    suspend fun updateSchedule(schedule: Schedule)

    @Delete
    suspend fun deleteSchedule(schedule: Schedule)

    @Query("UPDATE schedules SET isCompleted = :completed WHERE id = :id")
    suspend fun setScheduleCompleted(id: Long, completed: Boolean)
}
