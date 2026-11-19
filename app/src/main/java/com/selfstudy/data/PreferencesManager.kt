package com.selfstudy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val DEFAULT_STUDY_METHOD_KEY = stringPreferencesKey("default_study_method")
        val DEFAULT_STUDY_DURATION_KEY = intPreferencesKey("default_study_duration")
        val DEFAULT_BREAK_DURATION_KEY = intPreferencesKey("default_break_duration")
        val DEFAULT_CYCLES_KEY = intPreferencesKey("default_cycles")
        val SHOW_COMPLETED_TASKS_KEY = booleanPreferencesKey("show_completed_tasks")
        val FIRST_LAUNCH_KEY = booleanPreferencesKey("first_launch")
    }

    val darkMode: Flow<Boolean?> = context.dataStore.data.map { it[DARK_MODE_KEY] }

    val defaultStudyMethod: Flow<String> = context.dataStore.data.map { it[DEFAULT_STUDY_METHOD_KEY] ?: "pomodoro" }

    val defaultStudyDuration: Flow<Int> = context.dataStore.data.map { it[DEFAULT_STUDY_DURATION_KEY] ?: 25 }

    val defaultBreakDuration: Flow<Int> = context.dataStore.data.map { it[DEFAULT_BREAK_DURATION_KEY] ?: 5 }

    val defaultCycles: Flow<Int> = context.dataStore.data.map { it[DEFAULT_CYCLES_KEY] ?: 4 }

    val showCompletedTasks: Flow<Boolean> = context.dataStore.data.map { it[SHOW_COMPLETED_TASKS_KEY] ?: false }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { it[FIRST_LAUNCH_KEY] ?: true }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE_KEY] = enabled }
    }

    suspend fun setDefaultStudyMethod(method: String) {
        context.dataStore.edit { it[DEFAULT_STUDY_METHOD_KEY] = method }
    }

    suspend fun setDefaultStudyDuration(minutes: Int) {
        context.dataStore.edit { it[DEFAULT_STUDY_DURATION_KEY] = minutes }
    }

    suspend fun setDefaultBreakDuration(minutes: Int) {
        context.dataStore.edit { it[DEFAULT_BREAK_DURATION_KEY] = minutes }
    }

    suspend fun setDefaultCycles(cycles: Int) {
        context.dataStore.edit { it[DEFAULT_CYCLES_KEY] = cycles }
    }

    suspend fun setShowCompletedTasks(show: Boolean) {
        context.dataStore.edit { it[SHOW_COMPLETED_TASKS_KEY] = show }
    }

    suspend fun setFirstLaunch(isFirst: Boolean) {
        context.dataStore.edit { it[FIRST_LAUNCH_KEY] = isFirst }
    }
}
