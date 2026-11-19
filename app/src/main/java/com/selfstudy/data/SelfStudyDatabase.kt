package com.selfstudy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.selfstudy.data.dao.TaskDao
import com.selfstudy.data.dao.StudySessionDao
import com.selfstudy.data.dao.ScheduleDao
import com.selfstudy.data.model.Task
import com.selfstudy.data.model.StudySession
import com.selfstudy.data.model.Schedule

@Database(
    entities = [Task::class, StudySession::class, Schedule::class],
    version = 1,
    exportSchema = false
)
abstract class SelfStudyDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: SelfStudyDatabase? = null

        fun getDatabase(context: Context): SelfStudyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SelfStudyDatabase::class.java,
                    "selfstudy_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
