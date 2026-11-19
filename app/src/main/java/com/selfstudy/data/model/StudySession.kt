package com.selfstudy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    val method: String = "pomodoro", // pomodoro, 52_17, custom, flowtime
    val studyDurationMinutes: Int = 25,
    val breakDurationMinutes: Int = 5,
    val longBreakDurationMinutes: Int = 15,
    val totalCycles: Int = 4,
    val completedCycles: Int = 0,
    val totalStudyTimeMillis: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)
