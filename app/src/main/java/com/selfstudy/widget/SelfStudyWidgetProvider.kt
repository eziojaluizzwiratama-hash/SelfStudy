package com.selfstudy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.selfstudy.MainActivity
import com.selfstudy.R
import com.selfstudy.data.SelfStudyDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SelfStudyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateSingleWidget(context, appWidgetManager, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun updateSingleWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_self_study)

        // 1. Tanggal Hari Ini
        val dateFormat = SimpleDateFormat("EEE, dd MMM", Locale("id", "ID"))
        val dateText = dateFormat.format(Date())
        views.setTextViewText(R.id.widget_tv_date, dateText)

        try {
            val db = SelfStudyDatabase.getDatabase(context)

            // 2. Data Tugas Terdekat
            val activeTasks = db.taskDao().getActiveTasks().first()
            if (activeTasks.isEmpty()) {
                views.setTextViewText(R.id.widget_tv_task_badge, "TUGAS")
                views.setTextViewText(R.id.widget_tv_task_count, "(0 aktif)")
                views.setTextViewText(R.id.widget_tv_task_title, "Semua tugas tuntas!")
                views.setTextViewText(R.id.widget_tv_task_subtitle, "Tidak ada tugas tertunda")
            } else {
                val nextTask = activeTasks.minByOrNull { it.dueDate } ?: activeTasks.first()
                val diff = nextTask.dueDate - System.currentTimeMillis()
                val daysLeft = (diff / (24 * 60 * 60 * 1000)).toInt()
                val urgencyText = when {
                    daysLeft < 0 -> "Terlambat"
                    daysLeft == 0 -> "Hari ini"
                    daysLeft == 1 -> "Besok"
                    else -> "$daysLeft hari lagi"
                }

                views.setTextViewText(R.id.widget_tv_task_badge, "TUGAS")
                views.setTextViewText(R.id.widget_tv_task_count, "(${activeTasks.size} aktif)")
                views.setTextViewText(R.id.widget_tv_task_title, nextTask.title)
                val subText = if (nextTask.subject.isNotBlank()) {
                    "${nextTask.subject} • Deadline $urgencyText"
                } else {
                    "Deadline $urgencyText"
                }
                views.setTextViewText(R.id.widget_tv_task_subtitle, subText)
            }

            // 3. Data Jadwal / Ujian Terdekat
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = cal.timeInMillis

            val activeSchedules = db.scheduleDao().getActiveSchedules().first()
            val upcomingSchedules = activeSchedules.filter { it.date >= todayStart }

            if (upcomingSchedules.isEmpty()) {
                views.setTextViewText(R.id.widget_tv_schedule_badge, "JADWAL")
                views.setTextViewText(R.id.widget_tv_schedule_type, "Kosong")
                views.setTextViewText(R.id.widget_tv_schedule_title, "Belum ada jadwal terdekat")
                views.setTextViewText(R.id.widget_tv_schedule_subtitle, "Ketuk untuk melihat agenda")
            } else {
                val nextSchedule = upcomingSchedules.minByOrNull { it.date } ?: upcomingSchedules.first()

                val scheduleDayCal = Calendar.getInstance().apply { timeInMillis = nextSchedule.date }
                val todayCal = Calendar.getInstance()
                val isToday = scheduleDayCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                        scheduleDayCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

                val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                val isTomorrow = scheduleDayCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                        scheduleDayCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)

                val dateLabel = when {
                    isToday -> "Hari ini"
                    isTomorrow -> "Besok"
                    else -> SimpleDateFormat("dd MMM", Locale("id", "ID")).format(Date(nextSchedule.date))
                }

                val timeLabel = when {
                    nextSchedule.startTime.isNotBlank() && nextSchedule.endTime.isNotBlank() ->
                        "${nextSchedule.startTime} - ${nextSchedule.endTime}"
                    nextSchedule.startTime.isNotBlank() -> "Jam ${nextSchedule.startTime}"
                    else -> "Waktu fleksibel"
                }

                val isUjian = nextSchedule.type.equals("ujian", ignoreCase = true)
                views.setTextViewText(R.id.widget_tv_schedule_badge, if (isUjian) "UJIAN" else "JADWAL")
                views.setTextViewText(
                    R.id.widget_tv_schedule_type,
                    if (isUjian) "Penting" else if (nextSchedule.type == "tugas") "Tugas" else "Belajar"
                )
                views.setTextViewText(R.id.widget_tv_schedule_title, nextSchedule.title)

                val subText = if (nextSchedule.subject.isNotBlank()) {
                    "$dateLabel • $timeLabel • ${nextSchedule.subject}"
                } else {
                    "$dateLabel • $timeLabel"
                }
                views.setTextViewText(R.id.widget_tv_schedule_subtitle, subText)
            }
        } catch (_: Exception) {
            views.setTextViewText(R.id.widget_tv_task_title, "SelfStudy")
            views.setTextViewText(R.id.widget_tv_schedule_title, "Ketuk untuk membuka")
        }

        // 4. PendingIntents
        // Klik background utama -> buka Beranda (page 0)
        val dashboardIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_page", 0)
        }
        val dashboardPendingIntent = PendingIntent.getActivity(
            context,
            100,
            dashboardIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, dashboardPendingIntent)

        // Klik kartu Tugas atau Jadwal -> buka Jadwal & Tugas (page 1)
        val taskIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_page", 1)
        }
        val taskPendingIntent = PendingIntent.getActivity(
            context,
            101,
            taskIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_task_container, taskPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_schedule_container, taskPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_btn_add_task, taskPendingIntent)

        // Klik "Mulai Timer" -> buka Timer (page 2)
        val timerIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_page", 2)
        }
        val timerPendingIntent = PendingIntent.getActivity(
            context,
            102,
            timerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_timer, timerPendingIntent)

        // 5. Update ke AppWidgetManager
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        fun updateWidget(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, SelfStudyWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds.isNotEmpty()) {
                    val intent = Intent(context, SelfStudyWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (_: Exception) {}
        }
    }
}
