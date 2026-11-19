package com.selfstudy.notification

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.selfstudy.R
import com.selfstudy.SelfStudyApp
import java.util.Calendar

object NotificationHelper {

    fun scheduleTaskReminders(context: Context, taskId: Long, title: String, dueDate: Long, reminderDays: String) {
        val days = reminderDays.split(",").mapNotNull { it.trim().toIntOrNull() }
        days.forEach { daysBefore ->
            val reminderTime = dueDate - (daysBefore * 24 * 60 * 60 * 1000L)
            if (reminderTime > System.currentTimeMillis()) {
                scheduleAlarm(context, taskId, daysBefore, title, reminderTime, "tugas")
            }
        }
    }

    fun scheduleScheduleReminders(context: Context, scheduleId: Long, title: String, date: Long, reminderDays: String) {
        val days = reminderDays.split(",").mapNotNull { it.trim().toIntOrNull() }
        days.forEach { daysBefore ->
            val reminderTime = date - (daysBefore * 24 * 60 * 60 * 1000L)
            if (reminderTime > System.currentTimeMillis()) {
                scheduleAlarm(context, scheduleId + 10000, daysBefore, title, reminderTime, "jadwal")
            }
        }
    }

    private fun scheduleAlarm(context: Context, baseId: Long, daysBefore: Int, title: String, triggerTime: Long, type: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("notification_id", (baseId * 100 + daysBefore).toInt())
            putExtra("title", title)
            putExtra("days_before", daysBefore)
            putExtra("type", type)
        }
        val requestCode = (baseId * 100 + daysBefore).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            // Fallback to inexact alarm
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelReminders(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(1, 3, 7).forEach { daysBefore ->
            val requestCode = (taskId * 100 + daysBefore).toInt()
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }

    fun showTimerNotification(context: Context, title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        val notification = NotificationCompat.Builder(context, SelfStudyApp.TIMER_ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }
}
