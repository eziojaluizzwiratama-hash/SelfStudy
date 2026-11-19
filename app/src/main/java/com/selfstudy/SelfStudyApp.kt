package com.selfstudy

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.selfstudy.data.SelfStudyDatabase
import com.selfstudy.data.PreferencesManager
import com.selfstudy.data.repository.TaskRepository
import com.selfstudy.data.repository.StudySessionRepository
import com.selfstudy.data.repository.ScheduleRepository

class SelfStudyApp : Application() {
    val database by lazy { SelfStudyDatabase.getDatabase(this) }
    val taskRepository by lazy { TaskRepository(database.taskDao()) }
    val studySessionRepository by lazy { StudySessionRepository(database.studySessionDao()) }
    val scheduleRepository by lazy { ScheduleRepository(database.scheduleDao()) }
    val preferencesManager by lazy { PreferencesManager(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Pengingat Tugas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat jadwal dan tugas"
            }

            val timerChannel = NotificationChannel(
                TIMER_CHANNEL_ID,
                "Timer Belajar",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi timer belajar aktif"
            }

            val timerAlertChannel = NotificationChannel(
                TIMER_ALERT_CHANNEL_ID,
                "Alert Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi saat waktu belajar atau istirahat selesai"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(timerChannel)
            manager.createNotificationChannel(timerAlertChannel)
        }
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "reminder_channel"
        const val TIMER_CHANNEL_ID = "timer_channel"
        const val TIMER_ALERT_CHANNEL_ID = "timer_alert_channel"
    }
}
