package com.selfstudy.notification

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.selfstudy.R
import com.selfstudy.SelfStudyApp

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", 0)
        val title = intent.getStringExtra("title") ?: "Pengingat"
        val daysBefore = intent.getIntExtra("days_before", 0)
        val type = intent.getStringExtra("type") ?: "tugas"

        val message = when {
            daysBefore == 1 -> "[Penting] $title ${if (type == "tugas") "dikumpulkan" else "dimulai"} besok!"
            daysBefore == 0 -> "[Hari Ini] $title ${if (type == "tugas") "dikumpulkan" else "dimulai"} hari ini!"
            else -> "[Pengingat] $title ${if (type == "tugas") "dikumpulkan" else "dimulai"} dalam $daysBefore hari lagi"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val notification = NotificationCompat.Builder(context, SelfStudyApp.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Pengingat SelfStudy")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
