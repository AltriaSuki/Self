package com.example.self.feature_pomodoro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.self.core.database.AppDatabase
import com.example.self.core.model.PomodoroSession
import com.example.self.core.model.PomodoroSettings
import com.example.self.core.model.PomodoroType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as Application
                PomodoroViewModel(application)
            }
        }
    }
    private val pomodoroDao = AppDatabase.getDatabase(application).pomodoroDao()
    
    // Settings
    private val _settings = MutableStateFlow(PomodoroSettings())
    val settings: StateFlow<PomodoroSettings> = _settings.asStateFlow()
    
    // Timer state
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    
    // Session count
    private val _completedWorkSessions = MutableStateFlow(0)
    val completedWorkSessions: StateFlow<Int> = _completedWorkSessions.asStateFlow()
    
    // Today's statistics
    private val startOfDay: Long
        get() {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }
    
    val todaySessionCount: StateFlow<Int> = pomodoroDao.getTodayWorkSessionCount(startOfDay)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val todayTotalMinutes: StateFlow<Int> = pomodoroDao.getTodayTotalWorkMinutes(startOfDay)
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    private var timerJob: Job? = null
    
    init {
        // Initialize timer with work duration
        resetTimer()
    }
    
    fun startTimer() {
        if (_timerState.value.isRunning) return
        
        _timerState.update { it.copy(isRunning = true, isPaused = false) }
        
        timerJob = viewModelScope.launch {
            while (_timerState.value.remainingSeconds > 0 && _timerState.value.isRunning) {
                delay(1000)
                _timerState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
            }
            
            if (_timerState.value.remainingSeconds <= 0) {
                onTimerComplete()
            }
        }
    }
    
    fun pauseTimer() {
        timerJob?.cancel()
        _timerState.update { it.copy(isRunning = false, isPaused = true) }
    }
    
    fun resetTimer() {
        timerJob?.cancel()
        val duration = when (_timerState.value.currentType) {
            PomodoroType.WORK -> _settings.value.workDuration
            PomodoroType.SHORT_BREAK -> _settings.value.shortBreakDuration
            PomodoroType.LONG_BREAK -> _settings.value.longBreakDuration
        }
        _timerState.update { 
            it.copy(
                remainingSeconds = duration * 60,
                totalSeconds = duration * 60,
                isRunning = false,
                isPaused = false
            )
        }
    }
    
    fun skipToNext() {
        timerJob?.cancel()
        moveToNextSession()
    }
    
    private fun onTimerComplete() {
        val currentType = _timerState.value.currentType
        
        // Save completed session
        viewModelScope.launch {
            val session = PomodoroSession(
                type = currentType,
                durationMinutes = when (currentType) {
                    PomodoroType.WORK -> _settings.value.workDuration
                    PomodoroType.SHORT_BREAK -> _settings.value.shortBreakDuration
                    PomodoroType.LONG_BREAK -> _settings.value.longBreakDuration
                }
            )
            pomodoroDao.insertSession(session)
        }
        
        if (currentType == PomodoroType.WORK) {
            _completedWorkSessions.update { it + 1 }
        }
        
        moveToNextSession()
    }
    
    private fun moveToNextSession() {
        val nextType = when (_timerState.value.currentType) {
            PomodoroType.WORK -> {
                if (_completedWorkSessions.value >= _settings.value.sessionsBeforeLongBreak) {
                    _completedWorkSessions.value = 0
                    PomodoroType.LONG_BREAK
                } else {
                    PomodoroType.SHORT_BREAK
                }
            }
            PomodoroType.SHORT_BREAK, PomodoroType.LONG_BREAK -> PomodoroType.WORK
        }
        
        val duration = when (nextType) {
            PomodoroType.WORK -> _settings.value.workDuration
            PomodoroType.SHORT_BREAK -> _settings.value.shortBreakDuration
            PomodoroType.LONG_BREAK -> _settings.value.longBreakDuration
        }
        
        _timerState.update {
            TimerState(
                currentType = nextType,
                remainingSeconds = duration * 60,
                totalSeconds = duration * 60,
                isRunning = false,
                isPaused = false
            )
        }
    }
    
    fun setSessionType(type: PomodoroType) {
        timerJob?.cancel()
        val duration = when (type) {
            PomodoroType.WORK -> _settings.value.workDuration
            PomodoroType.SHORT_BREAK -> _settings.value.shortBreakDuration
            PomodoroType.LONG_BREAK -> _settings.value.longBreakDuration
        }
        _timerState.value = TimerState(
            currentType = type,
            remainingSeconds = duration * 60,
            totalSeconds = duration * 60
        )
    }
    
    fun updateSettings(settings: PomodoroSettings) {
        _settings.value = settings
        resetTimer()
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

data class TimerState(
    val currentType: PomodoroType = PomodoroType.WORK,
    val remainingSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false
) {
    val progress: Float
        get() = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f
    
    val displayTime: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}
