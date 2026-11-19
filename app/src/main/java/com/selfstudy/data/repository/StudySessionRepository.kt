package com.selfstudy.data.repository

import com.selfstudy.data.dao.StudySessionDao
import com.selfstudy.data.model.StudySession
import kotlinx.coroutines.flow.Flow

class StudySessionRepository(private val sessionDao: StudySessionDao) {
    fun getAllSessions(): Flow<List<StudySession>> = sessionDao.getAllSessions()
    fun getSessionsBetweenDates(start: Long, end: Long): Flow<List<StudySession>> = sessionDao.getSessionsBetweenDates(start, end)
    fun getTotalStudyTimeBetween(start: Long, end: Long): Flow<Long?> = sessionDao.getTotalStudyTimeBetween(start, end)
    fun getTotalStudyTime(): Flow<Long?> = sessionDao.getTotalStudyTime()
    fun getCompletedSessionCount(): Flow<Int> = sessionDao.getCompletedSessionCount()
    suspend fun insertSession(session: StudySession): Long = sessionDao.insertSession(session)
    suspend fun updateSession(session: StudySession) = sessionDao.updateSession(session)
    suspend fun deleteSession(session: StudySession) = sessionDao.deleteSession(session)
}
