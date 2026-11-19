package com.selfstudy.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.selfstudy.SelfStudyApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as SelfStudyApp
            CoroutineScope(Dispatchers.IO).launch {
                // Re-schedule task reminders
                val tasks = app.taskRepository.getActiveTasks().first()
                tasks.forEach { task ->
                    NotificationHelper.scheduleTaskReminders(
                        context, task.id, task.title, task.dueDate, task.reminderDays
                    )
                }
                // Re-schedule schedule reminders
                val schedules = app.scheduleRepository.getActiveSchedules().first()
                schedules.forEach { schedule ->
                    NotificationHelper.scheduleScheduleReminders(
                        context, schedule.id, schedule.title, schedule.date, schedule.reminderDays
                    )
                }
            }
        }
    }
}
