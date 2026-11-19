package com.selfstudy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val subject: String = "",
    val date: Long, // epoch millis
    val startTime: String = "08:00", // HH:mm
    val endTime: String = "09:00",
    val type: String = "belajar", // belajar, tugas, ujian
    val reminderDays: String = "3",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
