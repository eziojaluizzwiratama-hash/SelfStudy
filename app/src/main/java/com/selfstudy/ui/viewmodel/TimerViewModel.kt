package com.selfstudy.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.selfstudy.SelfStudyApp
import com.selfstudy.data.model.StudySession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TimerState { IDLE, STUDYING, BREAK, PAUSED, FINISHED }

data class StudyMethod(
    val id: String,
    val name: String,
    val description: String,
    val studyMinutes: Int,
    val breakMinutes: Int,
    val cycles: Int
)

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SelfStudyApp
    private val sessionRepo = app.studySessionRepository

    companion object {
        val STUDY_METHODS = listOf(
            StudyMethod("custom", "Atur Sendiri", "Atur sendiri durasi belajar dan istirahat sesuai kebutuhan", 25, 5, 4),
            StudyMethod("pomodoro", "Pomodoro", "25 menit belajar, 5 menit istirahat dalam 4 siklus", 25, 5, 4),
            StudyMethod("52_17", "52/17", "52 menit fokus, 17 menit istirahat", 52, 17, 3),
            StudyMethod("90_20", "90/20", "90 menit belajar mendalam, 20 menit istirahat", 90, 20, 2),
            StudyMethod("25_5_short", "Sprint Pendek", "15 menit fokus, 3 menit istirahat — cocok untuk tugas kecil", 15, 3, 6),
            StudyMethod("flowtime", "Flowtime", "50 menit belajar, 10 menit istirahat — fleksibel untuk deep work", 50, 10, 3)
        )
    }

    private val _selectedMethod = MutableStateFlow(STUDY_METHODS[0])
    val selectedMethod: StateFlow<StudyMethod> = _selectedMethod.asStateFlow()

    private val _studyMinutes = MutableStateFlow(25)
    val studyMinutes: StateFlow<Int> = _studyMinutes.asStateFlow()

    private val _breakMinutes = MutableStateFlow(5)
    val breakMinutes: StateFlow<Int> = _breakMinutes.asStateFlow()

    private val _totalCycles = MutableStateFlow(4)
    val totalCycles: StateFlow<Int> = _totalCycles.asStateFlow()

    private val _currentCycle = MutableStateFlow(1)
    val currentCycle: StateFlow<Int> = _currentCycle.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _remainingMillis = MutableStateFlow(0L)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    private val _totalSessionMillis = MutableStateFlow(0L)
    val totalSessionMillis: StateFlow<Long> = _totalSessionMillis.asStateFlow()

    private val _totalStudiedMillis = MutableStateFlow(0L)
    val totalStudiedMillis: StateFlow<Long> = _totalStudiedMillis.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartTime: Long = 0

    // Callback for timer events (used by service)
    var onTimerPhaseComplete: ((TimerState) -> Unit)? = null

    fun selectMethod(method: StudyMethod) {
        if (_timerState.value != TimerState.IDLE && _timerState.value != TimerState.FINISHED) return
        _selectedMethod.value = method
        _studyMinutes.value = method.studyMinutes
        _breakMinutes.value = method.breakMinutes
        _totalCycles.value = method.cycles
    }

    fun setStudyMinutes(min: Int) { _studyMinutes.value = min.coerceIn(1, 180) }
    fun setBreakMinutes(min: Int) { _breakMinutes.value = min.coerceIn(1, 60) }
    fun setTotalCycles(cycles: Int) { _totalCycles.value = cycles.coerceIn(1, 20) }

    fun startTimer() {
        _currentCycle.value = 1
        _totalStudiedMillis.value = 0L
        sessionStartTime = System.currentTimeMillis()
        startStudyPhase()
    }

    private fun startStudyPhase() {
        _timerState.value = TimerState.STUDYING
        val durationMillis = _studyMinutes.value * 60 * 1000L
        _totalSessionMillis.value = durationMillis
        _remainingMillis.value = durationMillis
        startCountdown(durationMillis) {
            _totalStudiedMillis.value += durationMillis
            onTimerPhaseComplete?.invoke(TimerState.STUDYING)
            if (_currentCycle.value >= _totalCycles.value) {
                finishSession()
            } else {
                startBreakPhase()
            }
        }
    }

    private fun startBreakPhase() {
        _timerState.value = TimerState.BREAK
        val durationMillis = _breakMinutes.value * 60 * 1000L
        _totalSessionMillis.value = durationMillis
        _remainingMillis.value = durationMillis
        startCountdown(durationMillis) {
            onTimerPhaseComplete?.invoke(TimerState.BREAK)
            _currentCycle.value++
            startStudyPhase()
        }
    }

    private fun startCountdown(durationMillis: Long, onComplete: () -> Unit) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = durationMillis
            while (remaining > 0) {
                delay(100)
                remaining -= 100
                _remainingMillis.value = remaining.coerceAtLeast(0)
            }
            onComplete()
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        if (_timerState.value == TimerState.STUDYING || _timerState.value == TimerState.BREAK) {
            _timerState.value = TimerState.PAUSED
        }
    }

    private var pausedState: TimerState = TimerState.IDLE

    fun pauseTimerWithState() {
        timerJob?.cancel()
        val current = _timerState.value
        if (current == TimerState.STUDYING || current == TimerState.BREAK) {
            pausedState = current
            _timerState.value = TimerState.PAUSED
        }
    }

    fun resumeTimer() {
        if (_timerState.value != TimerState.PAUSED) return
        val remaining = _remainingMillis.value
        val previousState = pausedState
        _timerState.value = previousState

        startCountdown(remaining) {
            when (previousState) {
                TimerState.STUDYING -> {
                    _totalStudiedMillis.value += _studyMinutes.value * 60 * 1000L
                    onTimerPhaseComplete?.invoke(TimerState.STUDYING)
                    if (_currentCycle.value >= _totalCycles.value) {
                        finishSession()
                    } else {
                        startBreakPhase()
                    }
                }
                TimerState.BREAK -> {
                    onTimerPhaseComplete?.invoke(TimerState.BREAK)
                    _currentCycle.value++
                    startStudyPhase()
                }
                else -> {}
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        saveSession()
        resetTimer()
    }

    fun skipPhase() {
        timerJob?.cancel()
        when (_timerState.value) {
            TimerState.STUDYING -> {
                _totalStudiedMillis.value += (_studyMinutes.value * 60 * 1000L - _remainingMillis.value)
                if (_currentCycle.value >= _totalCycles.value) {
                    finishSession()
                } else {
                    startBreakPhase()
                }
            }
            TimerState.BREAK -> {
                _currentCycle.value++
                startStudyPhase()
            }
            else -> {}
        }
    }

    private fun finishSession() {
        _timerState.value = TimerState.FINISHED
        timerJob?.cancel()
        saveSession()
    }

    private fun saveSession() {
        viewModelScope.launch {
            val session = StudySession(
                method = _selectedMethod.value.id,
                studyDurationMinutes = _studyMinutes.value,
                breakDurationMinutes = _breakMinutes.value,
                longBreakDurationMinutes = _breakMinutes.value,
                totalCycles = _totalCycles.value,
                completedCycles = _currentCycle.value,
                totalStudyTimeMillis = _totalStudiedMillis.value,
                date = sessionStartTime,
                isCompleted = _timerState.value == TimerState.FINISHED
            )
            sessionRepo.insertSession(session)
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState.IDLE
        _currentCycle.value = 1
        _remainingMillis.value = 0L
        _totalSessionMillis.value = 0L
        _totalStudiedMillis.value = 0L
    }
}
