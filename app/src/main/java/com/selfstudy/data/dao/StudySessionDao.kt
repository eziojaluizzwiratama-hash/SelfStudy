package com.selfstudy.data.dao

import androidx.room.*
import com.selfstudy.data.model.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getSessionsBetweenDates(startDate: Long, endDate: Long): Flow<List<StudySession>>

    @Query("SELECT SUM(totalStudyTimeMillis) FROM study_sessions WHERE date BETWEEN :startDate AND :endDate")
    fun getTotalStudyTimeBetween(startDate: Long, endDate: Long): Flow<Long?>

    @Query("SELECT SUM(totalStudyTimeMillis) FROM study_sessions")
    fun getTotalStudyTime(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM study_sessions WHERE isCompleted = 1")
    fun getCompletedSessionCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long

    @Update
    suspend fun updateSession(session: StudySession)

    @Delete
    suspend fun deleteSession(session: StudySession)
}
