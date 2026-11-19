package com.selfstudy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.selfstudy.SelfStudyApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = (application as SelfStudyApp).preferencesManager

    val darkMode: StateFlow<Boolean?> = prefs.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val defaultStudyMethod: StateFlow<String> = prefs.defaultStudyMethod
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "pomodoro")

    val isFirstLaunch: StateFlow<Boolean> = prefs.isFirstLaunch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setDarkMode(enabled) }
    }

    fun setDefaultStudyMethod(method: String) {
        viewModelScope.launch { prefs.setDefaultStudyMethod(method) }
    }

    fun markFirstLaunchDone() {
        viewModelScope.launch { prefs.setFirstLaunch(false) }
    }
}
