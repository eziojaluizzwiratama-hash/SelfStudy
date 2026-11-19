package com.selfstudy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val subject: String = "",
    val dueDate: Long, // epoch millis
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val priority: Int = 0, // 0=normal, 1=medium, 2=high
    val reminderDays: String = "3", // comma separated: "7,3,1"
    val category: String = "tugas" // tugas, ujian, belajar
)
