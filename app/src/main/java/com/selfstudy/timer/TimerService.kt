package com.selfstudy.timer

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.selfstudy.MainActivity
import com.selfstudy.R
import com.selfstudy.SelfStudyApp

class TimerService : Service() {
    private val binder = TimerBinder()
    private var isRunning = false

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundTimer(
                intent.getStringExtra("phase") ?: "Belajar",
                intent.getStringExtra("method") ?: "Pomodoro"
            )
            ACTION_STOP -> stopForegroundTimer()
            ACTION_UPDATE -> updateNotification(
                intent.getStringExtra("phase") ?: "Belajar",
                intent.getStringExtra("remaining") ?: "00:00",
                intent.getIntExtra("cycle", 1),
                intent.getIntExtra("totalCycles", 4)
            )
        }
        return START_STICKY
    }

    private fun startForegroundTimer(phase: String, method: String) {
        isRunning = true
        val notification = buildNotification(phase, "Memulai sesi $method...", 1, 4)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopForegroundTimer() {
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification(phase: String, remaining: String, cycle: Int, totalCycles: Int) {
        if (!isRunning) return
        val notification = buildNotification(phase, "$remaining tersisa", cycle, totalCycles)
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(phase: String, text: String, cycle: Int, totalCycles: Int): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SelfStudyApp.TIMER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$phase — Siklus $cycle/$totalCycles")
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val ACTION_START = "com.selfstudy.timer.START"
        const val ACTION_STOP = "com.selfstudy.timer.STOP"
        const val ACTION_UPDATE = "com.selfstudy.timer.UPDATE"
        const val NOTIFICATION_ID = 1001
    }
}
